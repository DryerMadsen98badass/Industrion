package net.mads.industron.energy;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.mads.industron.machine.WrenchPickupHelper;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPropertyCalculator;
import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.SingleBlockMachineBlock;
import net.mads.industron.machine.SingleBlockMachinePower;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility;
import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EnergyWireBlock extends Block implements EntityBlock, IWrenchable {
    private static final Map<WireThickness, VoxelShape[]> SHAPES_BY_THICKNESS = new ConcurrentHashMap<>();

    private final IndustrialMaterial material;
    private final WireThickness thickness;
    private final boolean insulated;
    private final VoxelShape[] shapes;

    public EnergyWireBlock(IndustrialMaterial material, WireThickness thickness, boolean insulated) {
        super(BlockBehaviour.Properties.of()
                .strength(1.0F, 2.0F)
                .sound(insulated ? SoundType.WOOL : SoundType.METAL)
                .dynamicShape()
                .noOcclusion());
        this.material = material;
        this.thickness = thickness;
        this.insulated = insulated;
        this.shapes = SHAPES_BY_THICKNESS.computeIfAbsent(thickness, key -> buildShapes(key.pixels()));
    }

    public IndustrialMaterial material() {
        return material;
    }

    public MachineTier tier() {
        return material.tier();
    }

    public WireThickness thickness() {
        return thickness;
    }

    public boolean insulated() {
        return insulated;
    }

    public int maxAmps() {
        int amps = MaterialPropertyCalculator.wireBaseAmps(material.properties()) * thickness.ampMultiplier();
        if (!insulated || amps <= 0) {
            return amps;
        }
        return Math.max(1, (int) Math.round(amps * 1.25D));
    }

    public static boolean hasEnabledConnection(BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
        return state.getBlock() instanceof EnergyWireBlock
                && level.getBlockEntity(pos) instanceof EnergyWireBlockEntity wire
                && wire.isConnected(direction)
                && !wire.isConnectionDisabled(direction);
    }

    public static boolean wiresConnect(
            BlockGetter level,
            BlockPos pos,
            BlockState state,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState
    ) {
        return state.getBlock() instanceof EnergyWireBlock
                && neighborState.getBlock() instanceof EnergyWireBlock
                && hasEnabledConnection(level, pos, state, direction)
                && hasEnabledConnection(level, neighborPos, neighborState, direction.getOpposite());
    }

    public String registryName() {
        return registryName(material, thickness, insulated);
    }

    public static String registryName(IndustrialMaterial material, WireThickness thickness, boolean insulated) {
        String base = thickness.materialPart().registryName(material);
        return insulated ? "insulated_" + base : base;
    }

    public static String displayName(IndustrialMaterial material, WireThickness thickness, boolean insulated) {
        String base = material.displayName() + " " + thickness.displayName() + " Wire";
        return insulated ? "Insulated " + base : base;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 1);
        }
        CEEnergyNetwork.invalidate(level);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            CEEnergyNetwork.invalidate(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving
    ) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide()) {
            return;
        }

        Direction direction = directionToNeighbor(pos, fromPos);
        if (direction != null) {
            refreshConnection(level, pos, direction);
            CEEnergyNetwork.invalidate(level);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        refreshAllConnections(level, pos);
        CEEnergyNetwork.invalidate(level);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Direction direction = targetedConnection(context);
        BlockPos pos = context.getClickedPos();
        BlockPos neighborPos = pos.relative(direction);
        LevelAccessor level = context.getLevel();

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof EnergyWireBlockEntity wireEntity)) {
            return InteractionResult.PASS;
        }

        boolean disabled = !wireEntity.isConnectionDisabled(direction);
        wireEntity.setConnectionDisabled(direction, disabled);

        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof EnergyWireBlock
                && level.getBlockEntity(neighborPos) instanceof EnergyWireBlockEntity neighborEntity) {
            neighborEntity.setConnectionDisabled(direction.getOpposite(), disabled);
        }

        if (level instanceof Level serverLevel) {
            refreshConnection(serverLevel, pos, direction);
            if (neighborState.getBlock() instanceof EnergyWireBlock) {
                refreshConnection(serverLevel, neighborPos, direction.getOpposite());
            }
            CEEnergyNetwork.invalidate(serverLevel);
        }

        IWrenchable.playRotateSound(context.getLevel(), pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        return WrenchPickupHelper.pickup(this, state, context);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        if (level.getBlockEntity(pos) instanceof EnergyWireBlockEntity wire) {
            mask = wire.visualConnectionsMask();
        }
        return shapes[mask & 0x3F];
    }

    private static boolean connectsTo(BlockState state) {
        if (state.getBlock() instanceof EnergyWireBlock || state.getBlock() instanceof CreativeEnergyBlock) {
            return true;
        }
        if (state.getBlock() instanceof SingleBlockMachineBlock machine) {
            return machine.instance() != null
                    && machine.instance().definition().power() == SingleBlockMachinePower.ELECTRIC;
        }
        return state.getBlock() instanceof MachinePortBlock port
                && (port.abilities().contains(MultiblockAbility.ENERGY_INPUT)
                || port.abilities().contains(MultiblockAbility.ENERGY_OUTPUT));
    }

    private static boolean shouldConnect(
            BlockGetter level,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState
    ) {
        if (isConnectionDisabled(level, pos, direction)) {
            return false;
        }
        if (neighborState.getBlock() instanceof EnergyWireBlock) {
            return !isConnectionDisabled(level, neighborPos, direction.getOpposite());
        }
        return connectsTo(neighborState);
    }

    private static boolean isConnectionDisabled(BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof EnergyWireBlockEntity wire
                && wire.isConnectionDisabled(direction);
    }

    static void refreshAllConnections(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof EnergyWireBlockEntity wireEntity)) {
            return;
        }

        int mask = 0;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (shouldConnect(level, pos, direction, neighborPos, neighborState)) {
                mask |= bit(direction);
            }
        }
        wireEntity.setConnectionsMask(mask);
    }

    private static void refreshConnection(Level level, BlockPos pos, Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof EnergyWireBlockEntity wireEntity)) {
            return;
        }

        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        wireEntity.setConnected(direction, shouldConnect(level, pos, direction, neighborPos, neighborState));
    }

    private static Direction directionToNeighbor(BlockPos pos, BlockPos neighborPos) {
        for (Direction direction : Direction.values()) {
            if (pos.relative(direction).equals(neighborPos)) {
                return direction;
            }
        }
        return null;
    }

    private static Direction targetedConnection(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Vec3 hit = context.getClickLocation();
        double x = hit.x - (pos.getX() + 0.5D);
        double y = hit.y - (pos.getY() + 0.5D);
        double z = hit.z - (pos.getZ() + 0.5D);
        double absX = Math.abs(x);
        double absY = Math.abs(y);
        double absZ = Math.abs(z);

        if (absX >= absY && absX >= absZ) {
            return x >= 0 ? Direction.EAST : Direction.WEST;
        }
        if (absY >= absX && absY >= absZ) {
            return y >= 0 ? Direction.UP : Direction.DOWN;
        }
        return z >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static VoxelShape[] buildShapes(int pixels) {
        VoxelShape[] result = new VoxelShape[64];
        double min = (16 - pixels) / 2.0D;
        double max = min + pixels;
        VoxelShape center = Block.box(min, min, min, max, max, max);

        for (int index = 0; index < result.length; index++) {
            VoxelShape shape = center;
            if ((index & bit(Direction.DOWN)) != 0) {
                shape = Shapes.or(shape, Block.box(min, 0, min, max, min, max));
            }
            if ((index & bit(Direction.UP)) != 0) {
                shape = Shapes.or(shape, Block.box(min, max, min, max, 16, max));
            }
            if ((index & bit(Direction.NORTH)) != 0) {
                shape = Shapes.or(shape, Block.box(min, min, 0, max, max, min));
            }
            if ((index & bit(Direction.SOUTH)) != 0) {
                shape = Shapes.or(shape, Block.box(min, min, max, max, max, 16));
            }
            if ((index & bit(Direction.WEST)) != 0) {
                shape = Shapes.or(shape, Block.box(0, min, min, min, max, max));
            }
            if ((index & bit(Direction.EAST)) != 0) {
                shape = Shapes.or(shape, Block.box(max, min, min, 16, max, max));
            }
            result[index] = shape;
        }
        return result;
    }

    private static int bit(Direction direction) {
        return 1 << direction.ordinal();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyWireBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (blockEntityType != BlockEntityRegistry.ENERGY_WIRE.get()) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) ->
                EnergyWireBlockEntity.tick(tickLevel, tickPos, tickState, (EnergyWireBlockEntity) blockEntity);
    }
}
