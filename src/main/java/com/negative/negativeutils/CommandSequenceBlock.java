package com.negative.negativeutils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CommandSequenceBlock extends BaseEntityBlock {
    public CommandSequenceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CommandSequenceBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
            CommandSequenceNetwork.requestEditor(pos);
        } else if (player instanceof ServerPlayer serverPlayer
                && !serverPlayer.hasPermissions(2)) {
            serverPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "Solo un operador puede editar este bloque."
                    ),
                    true
            );
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
                ModBlocks.COMMAND_SEQUENCE_BLOCK_ENTITY.get(),
                CommandSequenceBlockEntity::serverTick
        );
    }
}
