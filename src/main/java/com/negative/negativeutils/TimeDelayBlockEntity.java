package com.negative.negativeutils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TimeDelayBlockEntity extends BlockEntity {
    private static final int DEFAULT_DELAY_SECONDS = 20;
    private static final int OUTPUT_PULSE_TICKS = 2;

    private int delaySeconds = DEFAULT_DELAY_SECONDS;
    private int remainingTicks;
    private int outputTicks;
    private boolean inputWasPowered;

    public TimeDelayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.TIME_DELAY_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            TimeDelayBlockEntity blockEntity
    ) {
        if (level.isClientSide) {
            return;
        }

        boolean powered = level.hasNeighborSignal(pos);

        if (!powered) {
            blockEntity.inputWasPowered = false;
        } else if (!blockEntity.inputWasPowered) {
            blockEntity.inputWasPowered = true;

            if (blockEntity.remainingTicks <= 0
                    && blockEntity.outputTicks <= 0) {
                blockEntity.remainingTicks = blockEntity.delaySeconds * 20;
                blockEntity.setChanged();
            }
        }

        boolean wasEmitting = blockEntity.outputTicks > 0;

        if (blockEntity.remainingTicks > 0) {
            blockEntity.remainingTicks--;

            if (blockEntity.remainingTicks == 0) {
                blockEntity.outputTicks = OUTPUT_PULSE_TICKS;
            }
        } else if (blockEntity.outputTicks > 0) {
            blockEntity.outputTicks--;
        }

        boolean isEmitting = blockEntity.outputTicks > 0;

        if (wasEmitting != isEmitting) {
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    public boolean isEmittingRedstone() {
        return outputTicks > 0;
    }

    public int getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(int seconds) {
        delaySeconds = Math.max(1, Math.min(86400, seconds));
        setChanged();

        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("DelaySeconds", delaySeconds);
        tag.putInt("RemainingTicks", remainingTicks);
        tag.putInt("OutputTicks", outputTicks);
        tag.putBoolean("InputWasPowered", inputWasPowered);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        delaySeconds = Math.max(1, tag.getInt("DelaySeconds"));
        remainingTicks = Math.max(0, tag.getInt("RemainingTicks"));
        outputTicks = Math.max(0, tag.getInt("OutputTicks"));
        inputWasPowered = tag.getBoolean("InputWasPowered");
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

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}