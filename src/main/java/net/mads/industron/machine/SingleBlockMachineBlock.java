package net.mads.industron.machine;

import net.minecraft.resources.ResourceLocation;

import net.mads.industron.block.loot.AssemblySalvageBlock;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import net.mads.industron.energy.CEEnergyNetwork;
import net.mads.industron.machine.interaction.BlockInteraction;
import net.mads.industron.menu.MachineControlScheduleMenu;
import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;

public class SingleBlockMachineBlock extends KineticBlock implements EntityBlock, AssemblySalvageBlock {
    public static final MapCodec<SingleBlockMachineBlock> CODEC =
            simpleCodec(properties -> new SingleBlockMachineBlock(properties, null));

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape BASIN_SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 2, 15),
            Block.box(1, 2, 1, 3, 10, 15),
            Block.box(13, 2, 1, 15, 10, 15),
            Block.box(3, 2, 1, 13, 10, 3),
            Block.box(3, 2, 13, 13, 10, 15)
    );

    private final SingleBlockMachineInstance instance;

    public SingleBlockMachineBlock(
            SingleBlockMachineInstance instance
    ) {
        this(
                BlockBehaviour.Properties.of()
                        .strength(
                                instance.definition().hardness(),
                                instance.definition().resistance()
                        )
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.METAL),
                instance
        );
    }

    public SingleBlockMachineBlock(
            BlockBehaviour.Properties properties,
            @Nullable SingleBlockMachineInstance instance
    ) {
        super(properties);
        this.instance = instance;
        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(ACTIVE, false)
        );
    }

    @Override
    protected MapCodec<? extends KineticBlock> codec() {
        return CODEC;
    }

    public SingleBlockMachineInstance instance() {
        return instance;
    }

    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction face
    ) {
        return instance != null
                && instance.definition().usesKinetic()
                && face == kineticDirection(state);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        Direction kineticDirection = kineticDirection(state);
        return kineticDirection == null
                ? state.getValue(FACING).getAxis()
                : kineticDirection.getAxis();
    }

    @Nullable
    public Direction kineticDirection(BlockState state) {
        if (instance == null || instance.definition().kineticSide() == null) {
            return null;
        }

        Direction facing = state.getValue(FACING);
        return switch (instance.definition().kineticSide()) {
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getCounterClockWise();
            case RIGHT -> facing.getClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }

    /** Backwards-compatible name for existing input-only call sites. */
    @Nullable
    public Direction kineticInputDirection(BlockState state) {
        return instance != null && instance.definition().usesKineticInput()
                ? kineticDirection(state)
                : null;
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(
            BlockState oldState,
            BlockState newState
    ) {
        if (oldState.getBlock() != newState.getBlock()) {
            return false;
        }

        if (instance == null || !instance.definition().usesKinetic()) {
            return true;
        }

        return kineticDirection(oldState) == kineticDirection(newState);
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new SingleBlockMachineBlockEntity(
                pos,
                state
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (instance == null) {
            return;
        }

        if (instance.tier() != MachineTier.NONE) {
            tooltip.add(MachineProcessingTooltip.tier(instance.tier()));
        }

        MachineProcessingTooltip.processing(tooltip, instance.definition().processingProfile(instance.tier()));
        for (ResourceLocation typeId : instance.definition().recipeTypes()) {
            var process = net.mads.industron.recipe.CERecipeTypes.byId(typeId);
            if (process != null) tooltip.add(Component.literal("Process: " + process.displayName()).withStyle(ChatFormatting.AQUA));
        }
        if (instance.definition().steamConversionRecipe() != null) {
            tooltip.add(Component.literal("Process: Evaporation (Water to Steam)").withStyle(ChatFormatting.AQUA));
        }
        for (String text : instance.definition().tooltips()) {
            tooltip.add(Component.literal(text)
                    .withStyle(ChatFormatting.GRAY));
        }

        if (instance.definition().resource() == SingleBlockMachineResource.STEAM) {
            String label = instance.definition().resourceMode() == SingleBlockMachineResourceMode.PRODUCES
                    ? "Steam Output: "
                    : "Steam Usage: ";
            tooltip.add(coloredValueLine(label, instance.steamUsage() + " mB/t", ChatFormatting.AQUA));
        }

        if (instance.definition().resource() == SingleBlockMachineResource.ENERGY) {
            String label = instance.definition().resourceMode() == SingleBlockMachineResourceMode.PRODUCES
                    ? "Energy Output: "
                    : "Energy Usage: ";
            tooltip.add(coloredValueLine(label, instance.energyUsage() + " CE/t", ChatFormatting.AQUA));
        }

        if (instance.definition().usesKineticInput()) {
            tooltip.add(coloredValueLine(
                    "Kinetic Input: ",
                    formatNumber(instance.kineticSuPerRpm()) + " SU/RPM",
                    ChatFormatting.RED
            ));
            instance.definition().minRpm().ifPresent(rpm -> tooltip.add(coloredValueLine(
                    "Minimum Speed: ",
                    rpm + " RPM",
                    ChatFormatting.GOLD
            )));
            instance.definition().maxRpm().ifPresent(rpm -> tooltip.add(coloredValueLine(
                    "Maximum Speed: ",
                    rpm + " RPM",
                    ChatFormatting.GOLD
            )));
        } else if (instance.definition().usesKineticOutput()) {
            tooltip.add(coloredValueLine(
                    "Kinetic Output: ",
                    formatNumber(instance.kineticSuPerRpm()) + " SU/RPM",
                    ChatFormatting.GREEN
            ));
            instance.definition().outputRpm().ifPresent(rpm -> tooltip.add(coloredValueLine(
                    "Output Speed: ",
                    rpm + " RPM",
                    ChatFormatting.GREEN
            )));
        }

        for (net.mads.industron.machine.interaction.MachineArea area : instance.definition().areas()) {
            net.mads.industron.machine.interaction.MachineArea.Dimensions dimensions =
                    area.dimensions(instance.tier(), instance.definition().startTier());
            tooltip.add(coloredValueLine("Operating Area: ", dimensions.tooltipText(), ChatFormatting.GREEN));
        }

        instance.definition().blockInteractions().stream()
                .filter(interaction -> interaction.type() == BlockInteraction.Type.SPRINKLER)
                .findFirst()
                .ifPresent(interaction -> {
                    int tierSteps = Math.max(0, MachineTierStats.tierIndex(instance.tier().recipeTier())
                            - MachineTierStats.tierIndex(instance.definition().startTier().recipeTier()));
                    int actions = interaction.actionsPerInterval();
                    for (int i = 0; i < tierSteps; i++) actions *= interaction.actionMultiplierPerTier();
                    tooltip.add(coloredValueLine("Growth Interval: ", interaction.interval() + " ticks", ChatFormatting.GOLD));
                    tooltip.add(coloredValueLine("Growth Attempts: ", Integer.toString(actions), ChatFormatting.GREEN));
                });

        boolean extractsFromTrees = instance.definition().blockInteractions().stream()
                .anyMatch(interaction -> interaction.type() == BlockInteraction.Type.TREE_EXTRACT);

        if (extractsFromTrees) {
            tooltip.add(Component.literal("Must be placed next to the bottom log of a naturally grown tree.")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    private static Component coloredValueLine(String label, String value, ChatFormatting valueColor) {
        return Component.literal(label)
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(valueColor));
    }

    private static String formatNumber(double value) {
        return BigDecimal.valueOf(value)
                .stripTrailingZeros()
                .toPlainString();
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection().getOpposite()
                );
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(
                FACING,
                rotation.rotate(state.getValue(FACING))
        );
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(
                mirror.getRotation(state.getValue(FACING))
        );
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return type == BlockEntityRegistry.SINGLE_BLOCK_MACHINE.get()
                ? (tickerLevel, pos, tickerState, blockEntity) ->
                ((SingleBlockMachineBlockEntity) blockEntity).tick()
                : null;
    }

    @Override
    public RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (instance != null && instance.definition().power() == SingleBlockMachinePower.ELECTRIC) {
            CEEnergyNetwork.invalidate(level, pos);
        }
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SingleBlockMachineBlockEntity machine) {
                machine.dropStoredItemsAndDiscardFluids();
            }
            if (instance != null && instance.definition().power() == SingleBlockMachinePower.ELECTRIC) {
                CEEnergyNetwork.invalidate(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (openMachineControlSchedule(context)) {
            return InteractionResult.SUCCESS;
        }
        return super.onWrenched(state, context);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        return WrenchPickupHelper.pickup(this, state, context);
    }

    private boolean openMachineControlSchedule(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof SingleBlockMachineBlockEntity machine)
                || !machine.hasMachineControlSchedule(context.getClickedFace())) {
            return false;
        }

        if (!level.isClientSide() && context.getPlayer() != null) {
            MachineControlScheduleMenu.open(context.getPlayer(), machine, context.getClickedFace());
        }
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof SingleBlockMachineBlockEntity machine)
                || !isDirectPrimitiveInteraction()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        return machine.primitiveInsert(player, hand, stack)
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (WrenchPickupHelper.isHoldingWrench(player)) {
            return InteractionResult.PASS;
        }

        if (isDirectPrimitiveInteraction()
                && level.getBlockEntity(pos) instanceof SingleBlockMachineBlockEntity machine) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            return machine.primitiveExtract(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof SingleBlockMachineBlockEntity machine) {
            ((IPlayerExtension) player).openMenu(machine, pos);
        }

        return InteractionResult.SUCCESS;
    }

    private boolean isDirectPrimitiveInteraction() {
        if (instance == null) return false;
        String id = instance.definition().id();
        return id.endsWith("_drying_rack") || id.endsWith("_brick_mold") || id.endsWith("_basin");
    }

    @Override
    public void stepOn(
            Level level,
            BlockPos pos,
            BlockState state,
            Entity entity
    ) {
        if (!level.isClientSide()
                && state.getValue(ACTIVE)
                && entity instanceof LivingEntity living) {
            living.hurt(
                    level.damageSources().hotFloor(),
                    2.0F
            );
        }

        super.stepOn(
                level,
                pos,
                state,
                entity
        );
    }

    @Override
    public void animateTick(
            BlockState state,
            Level level,
            BlockPos pos,
            RandomSource random
    ) {
        if (!state.getValue(ACTIVE)
                || instance == null
                || instance.definition().power() != SingleBlockMachinePower.STEAM
                || instance.definition().resourceMode() != SingleBlockMachineResourceMode.CONSUMES) {
            return;
        }

        level.addParticle(
                ParticleTypes.CLOUD,
                pos.getX() + 0.5D,
                pos.getY() + 1.05D,
                pos.getZ() + 0.5D,
                (random.nextDouble() - 0.5D) * 0.02D,
                0.04D,
                (random.nextDouble() - 0.5D) * 0.02D
        );
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof SingleBlockMachineBlockEntity machine
                ? machine.machineControlSignal(direction.getOpposite())
                : 0;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);
        builder.add(
                FACING,
                ACTIVE
        );
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        if (instance != null) {
            String id = instance.definition().id();
            if (id.endsWith("_drying_rack")) return Block.box(0, 0, 0, 16, 14, 16);
            if (id.endsWith("_brick_mold")) return Block.box(2, 0, 4, 14, 7, 12);
            if (id.endsWith("_basin")) return BASIN_SHAPE;
        }
        return Block.box(
                0,
                0,
                0,
                16,
                16,
                16
        );
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (instance != null) {
            String id = instance.definition().id();
            if (id.endsWith("_drying_rack") || id.endsWith("_brick_mold") || id.endsWith("_basin")) {
                return Shapes.empty();
            }
        }
        return super.getOcclusionShape(state, level, pos);
    }
}
