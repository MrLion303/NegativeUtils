package com.negative.negativeutils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class CommunicatorBlockEntity extends CommandSequenceBlockEntity {
    public CommunicatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.COMMUNICATOR_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CommunicatorBlockEntity communicator
    ) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        communicator.tickSequence(serverLevel);
    }
}
