package com.negative.negativeutils;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class CommandSequenceBlockEntity extends BlockEntity {
    public static final int MAX_COMMANDS_LENGTH = 8192;
    private static final int MAX_WAIT_SECONDS = 86400;
    private static final Logger LOGGER = LogUtils.getLogger();

    private String commands = "";
    private boolean inputWasPowered;
    private final List<SequenceExecution> executions = new ArrayList<>();

    public CommandSequenceBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlocks.COMMAND_SEQUENCE_BLOCK_ENTITY.get(), pos, state);
    }

    protected CommandSequenceBlockEntity(
            net.minecraft.world.level.block.entity.BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    /**
     * Inicia una ejecución sin jugador asociado (por ejemplo, mediante redstone).
     * Solo se permite una ejecución de este tipo a la vez.
     */
    public boolean startSequence() {
        return startSequenceInternal(null);
    }

    /**
     * Inicia una ejecución independiente para el jugador que activó el Comunicador.
     * Un jugador no puede duplicar su propia ejecución mientras siga activa,
     * pero otros jugadores pueden iniciar la misma secuencia simultáneamente.
     */
    public boolean startSequence(ServerPlayer activatingPlayer) {
        return startSequenceInternal(
                activatingPlayer == null ? null : activatingPlayer.getUUID()
        );
    }

    private boolean startSequenceInternal(UUID activatingPlayerUuid) {
        if (commands.isBlank()) {
            return false;
        }

        for (SequenceExecution execution : executions) {
            if (activatingPlayerUuid == null
                    ? execution.activatingPlayerUuid == null
                    : activatingPlayerUuid.equals(execution.activatingPlayerUuid)) {
                return false;
            }
        }

        executions.add(new SequenceExecution(activatingPlayerUuid));
        setChanged();
        return true;
    }

    public void tickSequence(ServerLevel serverLevel) {
        if (executions.isEmpty()) {
            return;
        }

        boolean changed = false;
        Iterator<SequenceExecution> iterator = executions.iterator();
        while (iterator.hasNext()) {
            SequenceExecution execution = iterator.next();

            // Cada ejecución pertenece a su activador. Si se desconecta,
            // se cancela solamente su secuencia, sin afectar a los demás.
            if (execution.activatingPlayerUuid != null
                    && serverLevel.getServer()
                    .getPlayerList()
                    .getPlayer(execution.activatingPlayerUuid) == null) {
                iterator.remove();
                changed = true;
                continue;
            }

            if (execution.waitTicks > 0) {
                execution.waitTicks--;
                if (execution.waitTicks > 0) {
                    continue;
                }
            }

            if (!runUntilWait(serverLevel, execution)) {
                iterator.remove();
                changed = true;
            } else {
                changed = true;
            }
        }

        if (changed) {
            setChanged();
        }
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
            blockEntity.startSequence();
        }

        blockEntity.tickSequence(serverLevel);
    }

    public String getCommands() {
        return commands;
    }

    public void setCommands(String commands) {
        this.commands = sanitize(commands);
        executions.clear();
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

    /**
     * Ejecuta las líneas de una instancia hasta completar la secuencia
     * o encontrar una espera. Devuelve true mientras la instancia siga activa.
     */
    private boolean runUntilWait(
            ServerLevel serverLevel,
            SequenceExecution execution
    ) {
        String[] lines = commands.split("\\n", -1);
        while (execution.lineIndex < lines.length) {
            int currentLine = execution.lineIndex++;
            String line = lines[currentLine].trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.regionMatches(true, 0, "wait", 0, 4)
                    && (line.length() == 4
                    || Character.isWhitespace(line.charAt(4)))) {
                execution.waitTicks = parseWaitTicks(line, currentLine);
                if (execution.waitTicks > 0) {
                    return true;
                }
                continue;
            }

            BlockCommandRunner.executeForInitiator(
                    serverLevel,
                    worldPosition,
                    execution.activatingPlayerUuid,
                    line,
                    currentLine
            );
        }

        return false;
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
        String normalized = commands.replace("\\r\\n", "\\n").replace('\\r', '\\n');
        return normalized.length() > MAX_COMMANDS_LENGTH
                ? normalized.substring(0, MAX_COMMANDS_LENGTH)
                : normalized;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Commands", commands);
        tag.putBoolean("InputWasPowered", inputWasPowered);

        ListTag savedExecutions = new ListTag();
        for (SequenceExecution execution : executions) {
            CompoundTag savedExecution = new CompoundTag();
            if (execution.activatingPlayerUuid != null) {
                savedExecution.putUUID("ActivationPlayer", execution.activatingPlayerUuid);
            }
            savedExecution.putInt("LineIndex", execution.lineIndex);
            savedExecution.putInt("WaitTicks", execution.waitTicks);
            savedExecutions.add(savedExecution);
        }
        tag.put("Executions", savedExecutions);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        commands = sanitize(tag.getString("Commands"));
        inputWasPowered = tag.getBoolean("InputWasPowered");
        executions.clear();

        if (tag.contains("Executions", Tag.TAG_LIST)) {
            ListTag savedExecutions = tag.getList("Executions", Tag.TAG_COMPOUND);
            for (int i = 0; i < savedExecutions.size(); i++) {
                CompoundTag savedExecution = savedExecutions.getCompound(i);
                UUID activatingPlayerUuid = savedExecution.hasUUID("ActivationPlayer")
                        ? savedExecution.getUUID("ActivationPlayer")
                        : null;
                int savedLineIndex = Math.max(0, savedExecution.getInt("LineIndex"));
                int savedWaitTicks = Math.max(0, savedExecution.getInt("WaitTicks"));
                executions.add(new SequenceExecution(
                        activatingPlayerUuid,
                        savedLineIndex,
                        savedWaitTicks
                ));
            }
        } else if (tag.getBoolean("Running")) {
            // Compatibilidad con mundos guardados por la versión anterior,
            // que solamente podía conservar una ejecución a la vez.
            UUID activatingPlayerUuid = tag.hasUUID("ActivationPlayer")
                    ? tag.getUUID("ActivationPlayer")
                    : null;
            executions.add(new SequenceExecution(
                    activatingPlayerUuid,
                    Math.max(0, tag.getInt("LineIndex")),
                    Math.max(0, tag.getInt("WaitTicks"))
            ));
        }
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

    private static final class SequenceExecution {
        private final UUID activatingPlayerUuid;
        private int lineIndex;
        private int waitTicks;

        private SequenceExecution(UUID activatingPlayerUuid) {
            this(activatingPlayerUuid, 0, 0);
        }

        private SequenceExecution(
                UUID activatingPlayerUuid,
                int lineIndex,
                int waitTicks
        ) {
            this.activatingPlayerUuid = activatingPlayerUuid;
            this.lineIndex = lineIndex;
            this.waitTicks = waitTicks;
        }
    }
}
