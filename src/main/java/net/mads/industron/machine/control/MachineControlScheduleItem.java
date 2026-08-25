package net.mads.industron.machine.control;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.mads.industron.menu.MachineControlScheduleMenu;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinition;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockRegistry;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockScheduleBuilder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class MachineControlScheduleItem extends Item {
    private static final String DATA_KEY = "MachineControlSchedule";
    private static final String BUILD_TARGET_KEY = "MultiblockBuildTarget";
    private static final String BUILD_DEFINITION_KEY = "Definition";
    private static final String BUILD_VARIANT_KEY = "Variant";
    private static final String BUILD_TIER_KEY = "Tier";

    public MachineControlScheduleItem(Properties properties) {
        super(properties);
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) MachineControlScheduleMenu.openItem(player, hand, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return install(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return install(context);
    }

    private InteractionResult install(UseOnContext context) {
        Level level = context.getLevel();
        Direction side = context.getClickedFace();
        if (context.getPlayer() != null
                && !context.getPlayer().mayUseItemAt(
                context.getClickedPos(),
                side,
                context.getItemInHand()
        )) {
            return InteractionResult.FAIL;
        }

        MultiblockBuildTarget buildTarget = multiblockBuildTarget(context.getItemInHand());
        if (buildTarget != null) {
            BlockState clickedState = level.getBlockState(context.getClickedPos());
            if (!(clickedState.getBlock() instanceof MultiblockControllerBlock controllerBlock)) {
                return InteractionResult.FAIL;
            }

            MultiblockDefinition definition = MultiblockRegistry.all().stream()
                    .filter(candidate -> candidate.id().equals(buildTarget.definitionId()))
                    .findFirst()
                    .orElse(null);
            if (definition == null || !definition.controllerId().equals(controllerBlock.controllerId())) {
                return InteractionResult.FAIL;
            }

            if (!level.isClientSide() && level instanceof ServerLevel serverLevel && context.getPlayer() instanceof ServerPlayer serverPlayer) {
                Direction facing = clickedState.getValue(MultiblockControllerBlock.FACING);
                boolean built = MultiblockScheduleBuilder.build(
                        serverLevel,
                        serverPlayer,
                        context.getClickedPos(),
                        facing,
                        buildTarget.definitionId(),
                        buildTarget.variantId(),
                        buildTarget.tierId()
                );
                return built ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());

        if (!(blockEntity instanceof MachineControlScheduleHost host)
                || !host.acceptsMachineControlSchedules()) {
            return InteractionResult.PASS;
        }

        if (host.hasMachineControlSchedule(side)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            if (!host.installMachineControlSchedule(side, scheduleFromStack(context.getItemInHand()))) {
                return InteractionResult.FAIL;
            }

            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        MachineControlSchedule schedule = scheduleFromStack(stack);
        tooltip.add(Component.literal(schedule.nodes().size() + " program blocks")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(schedule.variables().size() + " variables")
                .withStyle(ChatFormatting.DARK_GRAY));
        MultiblockBuildTarget buildTarget = multiblockBuildTarget(stack);
        if (buildTarget != null) {
            tooltip.add(Component.literal("Multiblock: " + buildTarget.definitionId())
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal("Variant: " + buildTarget.variantId() + "  Tier: " + buildTarget.tierId().toUpperCase())
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    public static MachineControlSchedule scheduleFromStack(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        return root.contains(DATA_KEY, Tag.TAG_COMPOUND)
                ? MachineControlSchedule.load(root.getCompound(DATA_KEY))
                : new MachineControlSchedule();
    }

    public static void setScheduleOnStack(ItemStack stack, MachineControlSchedule schedule) {
        CompoundTag root = stack.get(DataComponents.CUSTOM_DATA) == null ? new CompoundTag() : stack.get(DataComponents.CUSTOM_DATA).copyTag();
        root.put(DATA_KEY, schedule.save());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static MultiblockBuildTarget multiblockBuildTarget(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag root = customData.copyTag();
        if (!root.contains(BUILD_TARGET_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag target = root.getCompound(BUILD_TARGET_KEY);
        String definitionId = target.getString(BUILD_DEFINITION_KEY);
        String variantId = target.getString(BUILD_VARIANT_KEY);
        String tierId = target.getString(BUILD_TIER_KEY);
        if (definitionId.isBlank() || variantId.isBlank() || tierId.isBlank()) {
            return null;
        }
        return new MultiblockBuildTarget(definitionId, variantId, tierId);
    }

    public static void setMultiblockBuildTarget(ItemStack stack, MultiblockBuildTarget buildTarget) {
        CompoundTag root = stack.get(DataComponents.CUSTOM_DATA) == null
                ? new CompoundTag()
                : stack.get(DataComponents.CUSTOM_DATA).copyTag();
        CompoundTag target = new CompoundTag();
        target.putString(BUILD_DEFINITION_KEY, buildTarget.definitionId());
        target.putString(BUILD_VARIANT_KEY, buildTarget.variantId());
        target.putString(BUILD_TIER_KEY, buildTarget.tierId());
        root.put(BUILD_TARGET_KEY, target);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public record MultiblockBuildTarget(String definitionId, String variantId, String tierId) {
    }

    public static ItemStack stackForSchedule(Item item, MachineControlSchedule schedule) {
        ItemStack stack = new ItemStack(item);
        setScheduleOnStack(stack, schedule);
        return stack;
    }
}
