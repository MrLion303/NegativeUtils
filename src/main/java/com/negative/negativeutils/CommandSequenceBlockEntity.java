package com.negative.negativeutils;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class CommandSequenceBlockEntity extends BlockEntity {
    public static final int MAX_COMMANDS_LENGTH = 8192;
    private static final int MAX_WAIT_SECONDS = 86400;
    private static final Logger LOGGER = LogUtils.getLogger();

    private String commands = "";
    private boolean inputWasPowered;
    private boolean running;
    private int lineIndex;
    private int waitTicks;

    public CommandSequenceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.COMMAND_SEQUENCE_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CommandSequenceBlockEntity blockEntity
    ) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean powered = level.hasNeighborSignal(pos);
        if (!powered) {
            blockEntity.inputWasPowered = false;
        } else if (!blockEntity.inputWasPowered) {
            blockEntity.inputWasPowered = true;
            if (!blockEntity.running && !blockEntity.commands.isBlank()) {
                blockEntity.running = true;
                blockEntity.lineIndex = 0;
                blockEntity.waitTicks = 0;
            }
        }

        if (blockEntity.running) {
            blockEntity.runUntilWait(serverLevel);
        }
    }

    public String getCommands() {
        return commands;
    }

    public void setCommands(String commands) {
        this.commands = sanitize(commands);
        running = false;
        lineIndex = 0;
        waitTicks = 0;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    3
            );
        }
    }

    private void runUntilWait(ServerLevel serverLevel) {
        if (waitTicks > 0) {
            waitTicks--;
            if (waitTicks > 0) {
                return;
            }
        }

        String[] lines = commands.split("\\n", -1);
        while (lineIndex < lines.length) {
            int currentLine = lineIndex++;
            String line = lines[currentLine].trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.regionMatches(true, 0, "wait", 0, 4)
                    && (line.length() == 4
                    || Character.isWhitespace(line.charAt(4)))) {
                waitTicks = parseWaitTicks(line, currentLine);
                if (waitTicks > 0) {
                    setChanged();
                    return;
                }
                continue;
            }

            BlockCommandRunner.execute(
                    serverLevel,
                    worldPosition,
                    null,
                    line,
                    currentLine
            );
        }

        running = false;
        lineIndex = 0;
        waitTicks = 0;
        setChanged();
    }

    private int parseWaitTicks(String line, int lineNumber) {
        String[] parts = line.split("\\s+");
        if (parts.length != 2) {
            LOGGER.warn(
                    "Invalid wait directive at command block {} line {}: {}",
                    worldPosition,
                    lineNumber + 1,
                    line
            );
            return 0;
        }

        try {
            int seconds = Integer.parseInt(parts[1]);
            if (seconds < 0 || seconds > MAX_WAIT_SECONDS) {
                throw new NumberFormatException("outside allowed range");
            }
            return seconds * 20;
        } catch (NumberFormatException exception) {
            LOGGER.warn(
                    "Invalid wait directive at command block {} line {}: {}",
                    worldPosition,
                    lineNumber + 1,
                    line
            );
            return 0;
        }
    }

    private static String sanitize(String commands) {
        if (commands == null) {
            return "";
        }
        String normalized = commands.replace("\r\n", "\n").replace('\r', '\n');
        return normalized.length() > MAX_COMMANDS_LENGTH
                ? normalized.substring(0, MAX_COMMANDS_LENGTH)
                : normalized;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Commands", commands);
        tag.putBoolean("InputWasPowered", inputWasPowered);
        tag.putBoolean("Running", running);
        tag.putInt("LineIndex", lineIndex);
        tag.putInt("WaitTicks", waitTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        commands = sanitize(tag.getString("Commands"));
        inputWasPowered = tag.getBoolean("InputWasPowered");
        running = tag.getBoolean("Running");
        lineIndex = Math.max(0, tag.getInt("LineIndex"));
        waitTicks = Math.max(0, tag.getInt("WaitTicks"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
}
