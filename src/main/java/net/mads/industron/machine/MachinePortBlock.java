package net.mads.industron.machine;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility;
import net.mads.industron.menu.MachineControlScheduleMenu;
import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class MachinePortBlock extends DirectionalKineticBlock implements IBE<MachinePortBlockEntity> {
    public static final MapCodec<MachinePortBlock> CODEC = simpleCodec(properties ->
            new MachinePortBlock(MachineTier.LV, MachinePortType.INPUT_BUS, properties));

    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

    private final MachineTier tier;
    private final MachinePortType portType;
    private final StaticMachinePortType staticPortType;

    public MachinePortBlock(MachineTier tier, MachinePortType portType) {
        this(tier, portType, BlockBehaviour.Properties.of()
                .requiresCorrectToolForDrops()
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL));
    }

    public MachinePortBlock(MachineTier tier, MachinePortType portType, BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = tier;
        this.portType = portType;
        this.staticPortType = null;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public MachinePortBlock(StaticMachinePortType staticPortType) {
        this(staticPortType, BlockBehaviour.Properties.of()
                .requiresCorrectToolForDrops()
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL));
    }

    public MachinePortBlock(StaticMachinePortType staticPortType, BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = null;
        this.portType = null;
        this.staticPortType = staticPortType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public MachineTier tier() {
        return tier;
    }

    public boolean hasTier() {
        return tier != null;
    }

    public MachinePortType portType() {
        return portType;
    }

    public StaticMachinePortType staticPortType() {
        return staticPortType;
    }

    public boolean usesTint() {
        return tier != null || (staticPortType != null && staticPortType.tinted());
    }

    public int tintColor() {
        if (tier != null) {
            return tier.color();
        }

        return staticPortType != null ? staticPortType.tintColor() : -1;
    }

    public MachineTier effectiveTier() {
        return tier != null ? tier : staticPortType.tier();
    }

    public Set<MultiblockAbility> abilities() {
        return portType != null ? portType.abilities() : staticPortType.abilities();
    }

    public boolean isKineticPort() {
        return abilities().contains(MultiblockAbility.KINETIC_INPUT) || abilities().contains(MultiblockAbility.KINETIC_OUTPUT);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(machineTierTooltip(effectiveTier()));
        tooltip.add(abilityTooltip(abilities()));

        if (abilities().contains(MultiblockAbility.MUFFLER)) {
            tooltip.add(coloredValueLine(
                    "Recipe Duration: ",
                    MachineTierStats.mufflerDurationPercent(effectiveTier()) + "%",
                    ChatFormatting.GOLD
            ));
            tooltip.add(Component.literal("Requires open space above").withStyle(ChatFormatting.YELLOW));
        }
        if (abilities().contains(MultiblockAbility.KINETIC_INPUT)) {
            tooltip.add(coloredValueLine(
                    "Kinetic Input: ",
                    formatNumber(MachineTierStats.kineticStressPerRpm(effectiveTier())) + " SU/RPM",
                    ChatFormatting.RED
            ));
        } else if (abilities().contains(MultiblockAbility.KINETIC_OUTPUT)) {
            tooltip.add(coloredValueLine(
                    "Kinetic Output: ",
                    formatNumber(MachineTierStats.kineticStressPerRpm(effectiveTier())) + " SU/RPM",
                    ChatFormatting.GREEN
            ));
        }
    }

    private static Component machineTierTooltip(MachineTier tier) {
        String displayName = tier == MachineTier.NONE ? "Any" : tier.displayName();
        return Component.literal("Machine Tier: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(displayName)
                        .withStyle(style -> style.withColor(TextColor.fromRgb(tier.color()))));
    }

    private static Component abilityTooltip(Set<MultiblockAbility> abilities) {
        MutableComponent line = Component.literal("Ability: ").withStyle(ChatFormatting.GRAY);
        int index = 0;
        for (MultiblockAbility ability : abilities) {
            if (index++ > 0) {
                line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            }
            line.append(Component.literal(abilityName(ability)).withStyle(abilityColor(ability)));
        }
        return line;
    }

    private static String abilityName(MultiblockAbility ability) {
        return switch (ability) {
            case ITEM_INPUT -> "Item In";
            case ITEM_OUTPUT -> "Item Out";
            case FLUID_INPUT -> "Fluid In";
            case CB_INPUT -> "CB In";
            case FLUID_OUTPUT -> "Fluid Out";
            case ENERGY_INPUT -> "Energy In";
            case ENERGY_OUTPUT -> "Energy Out";
            case KINETIC_INPUT -> "Kinetic In";
            case KINETIC_OUTPUT -> "Kinetic Out";
            case IO_INTERFACE -> "I/O";
            case MUFFLER -> "Muffler";
            case REDSTONE -> "Redstone";
        };
    }

    private static ChatFormatting abilityColor(MultiblockAbility ability) {
        return switch (ability) {
            case ITEM_INPUT, FLUID_INPUT -> ChatFormatting.AQUA;
            case ITEM_OUTPUT, FLUID_OUTPUT -> ChatFormatting.GREEN;
            case ENERGY_INPUT, KINETIC_INPUT, REDSTONE -> ChatFormatting.RED;
            case ENERGY_OUTPUT, KINETIC_OUTPUT -> ChatFormatting.GREEN;
            case CB_INPUT -> ChatFormatting.LIGHT_PURPLE;
            case MUFFLER -> ChatFormatting.GOLD;
            case IO_INTERFACE -> ChatFormatting.YELLOW;
        };
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
    protected MapCodec<? extends DirectionalKineticBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction nearestLookingDirection = context.getNearestLookingDirection();
        boolean sneakPlacing = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        return defaultBlockState().setValue(FACING, sneakPlacing ? nearestLookingDirection : nearestLookingDirection.getOpposite());
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        Direction facing = originalState.getValue(FACING);
        if (facing.getAxis() == targetedFace.getAxis()) {
            return originalState;
        }

        return originalState.setValue(FACING, facing.getClockWise(targetedFace.getAxis()));
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
        if (!(level.getBlockEntity(pos) instanceof MachinePortBlockEntity port)
                || !port.hasMachineControlSchedule(context.getClickedFace())) {
            return false;
        }

        if (!level.isClientSide() && context.getPlayer() != null) {
            MachineControlScheduleMenu.open(context.getPlayer(), port, context.getClickedFace());
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
        if (!(level.getBlockEntity(pos) instanceof MachinePortBlockEntity port)
                || !port.supportsBucketInteraction(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        return port.interactWithBucket(player, hand, stack)
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (WrenchPickupHelper.isHoldingWrench(player)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachinePortBlockEntity port) {
            ((IPlayerExtension) player).openMenu(port, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof MachinePortBlockEntity machine
                ? machine.machineControlSignal(direction.getOpposite())
                : 0;
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
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return isKineticPort() && face == state.getValue(FACING);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public Class<MachinePortBlockEntity> getBlockEntityClass() {
        return MachinePortBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MachinePortBlockEntity> getBlockEntityType() {
        return BlockEntityRegistry.MACHINE_PORT.get();
    }
}
