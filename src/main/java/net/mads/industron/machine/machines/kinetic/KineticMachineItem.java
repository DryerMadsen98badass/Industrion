package net.mads.industron.machine.machines.kinetic;

import net.mads.industron.recipe.RecipeTypeDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import java.util.List;

/** Native Create machine metadata; process execution remains in the composed CE adapter. */
public final class KineticMachineItem extends BlockItem {
    private final RecipeTypeDefinition recipeType;
    private final int minRpm;
    public KineticMachineItem(Block block, Properties properties, RecipeTypeDefinition recipeType, int minRpm) {
        super(block, properties); this.recipeType = recipeType; this.minRpm = minRpm;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("Process: " + recipeType.displayName()).withStyle(ChatFormatting.AQUA));
        tooltip.add(net.mads.industron.machine.MachineProcessingTooltip.tier(net.mads.industron.integration.create.kinetic.KineticProcessingRules.TIER));
        net.mads.industron.machine.MachineProcessingTooltip.processing(tooltip, net.mads.industron.integration.create.kinetic.KineticProcessingRules.PROFILE);
        tooltip.add(Component.translatable("tooltip.industron.kinetic_rpm", minRpm, net.mads.industron.integration.create.kinetic.KineticProcessingRules.MAX_RPM).withStyle(ChatFormatting.GRAY));
    }
}
