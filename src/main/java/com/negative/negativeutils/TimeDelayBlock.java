package com.negative.negativeutils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class TimeDelayBlock extends BaseEntityBlock {

    public TimeDelayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TimeDelayBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof TimeDelayBlockEntity timer
                && timer.isEmittingRedstone()) {
            return 15;
        }

        return 0;
    }

    @Override
    public int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction
    ) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            TimeDelayNetwork.requestEditor(pos);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }

        return createTickerHelper(
                type,
                ModBlocks.TIME_DELAY_BLOCK_ENTITY.get(),
                TimeDelayBlockEntity::serverTick
        );
    }
}