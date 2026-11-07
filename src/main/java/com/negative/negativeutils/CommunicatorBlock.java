package com.negative.negativeutils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CommunicatorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

    private static final VoxelShape FLOOR_SHAPE =
            Block.box(5.0D, 0.0D, 6.0D, 11.0D, 2.0D, 10.0D);
    private static final VoxelShape CEILING_SHAPE =
            Block.box(5.0D, 14.0D, 6.0D, 11.0D, 16.0D, 10.0D);
    private static final VoxelShape NORTH_SHAPE =
            Block.box(5.0D, 6.0D, 0.0D, 11.0D, 10.0D, 2.0D);
    private static final VoxelShape SOUTH_SHAPE =
            Block.box(5.0D, 6.0D, 14.0D, 11.0D, 10.0D, 16.0D);
    private static final VoxelShape WEST_SHAPE =
            Block.box(0.0D, 6.0D, 5.0D, 2.0D, 10.0D, 11.0D);
    private static final VoxelShape EAST_SHAPE =
            Block.box(14.0D, 6.0D, 5.0D, 16.0D, 10.0D, 11.0D);

    public CommunicatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.WALL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        BlockState state;

        if (clickedFace == Direction.UP) {
            state = defaultBlockState()
                    .setValue(FACE, AttachFace.FLOOR)
                    .setValue(FACING, context.getHorizontalDirection().getOpposite());
        } else if (clickedFace == Direction.DOWN) {
            state = defaultBlockState()
                    .setValue(FACE, AttachFace.CEILING)
                    .setValue(FACING, context.getHorizontalDirection().getOpposite());
        } else {
            state = defaultBlockState()
                    .setValue(FACE, AttachFace.WALL)
                    .setValue(FACING, clickedFace);
        }

        return canSurvive(state, context.getLevel(), context.getClickedPos())
                ? state
                : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction supportDirection = getSupportDirection(state);
        BlockPos supportPos = pos.relative(supportDirection);
        return level.getBlockState(supportPos).isFaceSturdy(
                level,
                supportPos,
                supportDirection.getOpposite()
        );
    }

    private static Direction getSupportDirection(BlockState state) {
        AttachFace face = state.getValue(FACE);
        if (face == AttachFace.FLOOR) {
            return Direction.DOWN;
        }
        if (face == AttachFace.CEILING) {
            return Direction.UP;
        }
        return state.getValue(FACING).getOpposite();
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (direction == getSupportDirection(state)
                && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        if (state.getValue(FACE) == AttachFace.FLOOR) {
            return FLOOR_SHAPE;
        }
        if (state.getValue(FACE) == AttachFace.CEILING) {
            return CEILING_SHAPE;
        }

        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> Shapes.block();
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CommunicatorBlockEntity(pos, state);
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

        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof CommunicatorBlockEntity communicator) {
            if (player instanceof ServerPlayer serverPlayer
                    && serverPlayer.isCrouching()
                    && serverPlayer.hasPermissions(2)) {
                CommunicatorNetwork.openEditor(
                        serverPlayer,
                        pos,
                        communicator.getCommands()
                );
            } else {
                communicator.startSequence();
            }
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
                ModBlocks.COMMUNICATOR_BLOCK_ENTITY.get(),
                CommunicatorBlockEntity::serverTick
        );
    }
}
