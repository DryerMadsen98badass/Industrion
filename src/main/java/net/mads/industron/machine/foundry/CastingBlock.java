package net.mads.industron.machine.foundry;

import net.mads.industron.material.IndustrialMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class CastingBlock extends Block implements EntityBlock, net.mads.industron.block.loot.AssemblySalvageBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape TABLE = shape(CastingGeometry.CASTER, Direction.NORTH);
    private static final java.util.Map<Direction, VoxelShape> FAUCET_SHAPES = new java.util.EnumMap<>(Direction.class);
    static {
        for (Direction side : Direction.Plane.HORIZONTAL) FAUCET_SHAPES.put(side, shape(CastingGeometry.FAUCET, side));
    }
    private static VoxelShape shape(java.util.List<CastingGeometry.Cuboid> boxes, Direction facing) {
        VoxelShape result = Shapes.empty();
        int turns = switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
        for (var part : boxes) {
            double x0 = part.x0(), x1 = part.x1(), z0 = part.z0(), z1 = part.z1();
            for (int i = 0; i < turns; i++) {
                double nextX0 = 16 - z1, nextX1 = 16 - z0;
                z0 = x0; z1 = x1; x0 = nextX0; x1 = nextX1;
            }
            result = Shapes.or(result, box(x0, part.y0(), z0, x1, part.y1(), z1));
        }
        return result;
    }
    private final IndustrialMaterial clay;
    private final boolean faucet;
    public CastingBlock(IndustrialMaterial clay, boolean faucet) {
        super(Properties.of().strength(2.0F).noOcclusion().sound(SoundType.DEEPSLATE_BRICKS));
        this.clay = clay;
        this.faucet = faucet;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    public IndustrialMaterial clay() { return clay; }
    public boolean faucet() { return faucet; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = faucet && context.getClickedFace().getAxis().isHorizontal()
                ? context.getClickedFace() : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return faucet ? FAUCET_SHAPES.get(state.getValue(FACING)) : TABLE;
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CastingBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == CastingRegistry.ENTITY.get()
                ? (world, pos, blockState, entity) -> ((CastingBlockEntity) entity).tick() : null;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                      Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CastingBlockEntity caster) {
            if (!level.isClientSide()) caster.interact(player, hand);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CastingBlockEntity caster)
            caster.interact(player, InteractionHand.MAIN_HAND);
        return InteractionResult.SUCCESS;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (state.getBlock() != next.getBlock() && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof CastingBlockEntity caster) caster.dropContents();
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
