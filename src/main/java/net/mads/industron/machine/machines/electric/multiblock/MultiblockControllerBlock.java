package net.mads.industron.machine.machines.electric.multiblock;

import net.mads.industron.block.loot.AssemblySalvageBlock;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.mads.industron.machine.MachineModelTintResolver;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.WrenchPickupHelper;
import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MultiblockControllerBlock extends HorizontalDirectionalBlock implements EntityBlock, IWrenchable, AssemblySalvageBlock {
    public static final MapCodec<MultiblockControllerBlock> CODEC = simpleCodec(properties ->
            new MultiblockControllerBlock(MultiblockControllerDefinition.of(
                    "test_multiblock",
                    "Test Multiblock",
                    "industron:block/lv_machine_casing",
                    "block/machines/overlay/test_machine/overlay_front",
                    "block/machines/overlay/test_machine/overlay_front"
            ), properties));
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final MultiblockControllerDefinition definition;
    private final ResourceLocation controllerId;

    public MultiblockControllerBlock(String controllerName) {
        this(MultiblockControllerDefinition.of(
                controllerName,
                controllerName,
                "industron:block/lv_machine_casing",
                "block/machines/overlay/test_machine/overlay_front",
                "block/machines/overlay/test_machine/overlay_front"
        ));
    }

    public MultiblockControllerBlock(MultiblockControllerDefinition definition) {
        this(definition, properties(definition));
    }

    private static BlockBehaviour.Properties properties(MultiblockControllerDefinition definition) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .requiresCorrectToolForDrops()
                .strength(definition.hardness(), definition.resistance());
        properties.sound(definition.soundOverride() == null ? SoundType.METAL : definition.soundOverride());
        return properties;
    }

    public MultiblockControllerBlock(String controllerName, BlockBehaviour.Properties properties) {
        this(MultiblockControllerDefinition.of(
                controllerName,
                controllerName,
                "industron:block/lv_machine_casing",
                "block/machines/overlay/test_machine/overlay_front",
                "block/machines/overlay/test_machine/overlay_front"
        ), properties);
    }

    public MultiblockControllerBlock(MultiblockControllerDefinition definition, BlockBehaviour.Properties properties) {
        super(properties);
        this.definition = definition;
        this.controllerId = definition.id();
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false)
                .setValue(ACTIVE, false));
    }

    public ResourceLocation controllerId() {
        return controllerId;
    }

    public MultiblockControllerDefinition definition() {
        return definition;
    }

    public boolean usesTint() {
        return java.util.Arrays.stream(MultiblockControllerDefinition.Side.values()).anyMatch(definition::hasSideTextureColor)
                || MachineModelTintResolver.resolve(definition.model()) != null;
    }

    public boolean usesTint(int tintIndex) {
        MultiblockControllerDefinition.Side side = MultiblockControllerDefinition.Side.fromTintIndex(tintIndex);
        if (side != null && definition.hasSideTextureColor(side)) {
            return true;
        }
        return tintIndex == 0 && MachineModelTintResolver.resolve(definition.model()) != null;
    }

    public int tintColor() {
        Integer sideColor = java.util.Arrays.stream(MultiblockControllerDefinition.Side.values())
                .map(definition::sideTextureColor)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (sideColor != null) {
            return sideColor;
        }
        Integer modelColor = MachineModelTintResolver.resolve(definition.model());
        return modelColor == null ? -1 : modelColor;
    }

    public int tintColor(int tintIndex) {
        MultiblockControllerDefinition.Side side = MultiblockControllerDefinition.Side.fromTintIndex(tintIndex);
        if (side != null) {
            Integer color = definition.sideTextureColor(side);
            if (color != null) {
                return color;
            }
        }
        if (tintIndex == 0) {
            Integer modelColor = MachineModelTintResolver.resolve(definition.model());
            if (modelColor != null) {
                return modelColor;
            }
        }
        return -1;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        MultiblockRegistry.byController(controllerId).ifPresent(multiblock -> {
            if (multiblock.hasTierRestriction()) {
                tooltip.add(machineTierTooltip(multiblock));
            }

            switch (multiblock.drive()) {
                case ELECTRIC -> {
                    if (multiblock.hasContinuousEnergyUsage()) {
                        tooltip.add(coloredValueLine(
                                "Energy Usage: ",
                                "Dynamic",
                                ChatFormatting.AQUA
                        ));
                    } else {
                        tooltip.add(coloredValueLine(
                                "Base Energy Usage: ",
                                multiblock.energyUsage() + " CE/t",
                                ChatFormatting.AQUA
                        ));
                    }
                }
                case STEAM -> tooltip.add(coloredValueLine(
                        "Steam Usage: ",
                        multiblock.steamUsage() + " mB/t",
                        ChatFormatting.AQUA
                ));
                case KINETIC -> {
                    multiblock.minRpm().ifPresent(rpm -> tooltip.add(coloredValueLine(
                            "Minimum Speed: ",
                            rpm + " RPM",
                            ChatFormatting.GOLD
                    )));
                    multiblock.maxRpm().ifPresent(rpm -> tooltip.add(coloredValueLine(
                            "Maximum Speed: ",
                            rpm + " RPM",
                            ChatFormatting.GOLD
                    )));
                }
                case KINETIC_OUTPUT -> multiblock.outputRpm().ifPresent(rpm -> tooltip.add(coloredValueLine(
                        "Output Speed: ",
                        rpm + " RPM",
                        ChatFormatting.GREEN
                )));
                case NONE -> {
                }
            }

            for (net.mads.industron.machine.interaction.MachineArea area : multiblock.areas()) {
                tooltip.add(coloredValueLine(
                        "Operating Area: ",
                        area.dimensions(net.mads.industron.machine.MachineTier.ULV,
                                net.mads.industron.machine.MachineTier.ULV).tooltipText(),
                        ChatFormatting.GREEN
                ));
            }

            for (String line : multiblock.tooltip()) {
                tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        });
    }

    private static Component coloredValueLine(String label, String value, ChatFormatting valueColor) {
        return Component.literal(label)
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(valueColor));
    }

    private static Component machineTierTooltip(MultiblockDefinition multiblock) {
        return Component.literal("Max Recipe Tier: ").withStyle(ChatFormatting.GRAY).append(coloredTier(multiblock.maxTier()));
    }

    private static Component coloredTier(MachineTier tier) {
        return Component.literal(tier.displayName())
                .withStyle(style -> style.withColor(TextColor.fromRgb(tier.color())));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(FORMED, false)
                .setValue(ACTIVE, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, ACTIVE);
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
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        markDirty(level, pos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        markDirty(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MultiblockControllerBlockEntity controller) {
            controller.clearFormation();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        return definition.wrenchable()
                ? WrenchPickupHelper.pickup(this, state, context)
                : InteractionResult.PASS;
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
        MultiblockDefinition multiblock = MultiblockRegistry.byController(controllerId).orElse(null);
        if (multiblock == null || multiblock.activationItems().isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        boolean validActivationItem = multiblock.activationItems().stream()
                .map(BuiltInRegistries.ITEM::get)
                .anyMatch(stack::is);
        if (!validActivationItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!(level.getBlockEntity(pos) instanceof MultiblockControllerBlockEntity controller)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!controller.grantManualActivation()) {
            // The configured activation item belongs to this multiblock interaction.
            // Consume the click even when the current batch cannot be activated so
            // vanilla Flint & Steel behaviour cannot place fire on/around the controller.
            return ItemInteractionResult.SUCCESS;
        }

        if (!player.getAbilities().instabuild) {
            if (stack.isDamageableItem()) {
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND);
            } else {
                stack.shrink(1);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (WrenchPickupHelper.isHoldingWrench(player)) {
            return InteractionResult.PASS;
        }
        if (!definition.openMenu()) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown() && !state.getValue(FORMED) && GogglesItem.isWearingGoggles(player)) {
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MultiblockControllerBlockEntity controller) {
            ((IPlayerExtension) player).openMenu(controller, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiblockControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != BlockEntityRegistry.MULTIBLOCK_CONTROLLER.get()) {
            return null;
        }

        return (tickLevel, tickPos, tickState, blockEntity) ->
                MultiblockControllerBlockEntity.tick(tickLevel, tickPos, tickState, (MultiblockControllerBlockEntity) blockEntity);
    }

    private static void markDirty(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MultiblockControllerBlockEntity controller) {
            controller.markStructureDirty();
        }
    }
}
