package net.mads.industron.progression;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Single-use LV assembly component; all ingredients are available before entering Nether. */
public final class PortalActivatorItem extends Item {
    public PortalActivatorItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override public InteractionResult useOn(UseOnContext context) {
        var pos=context.getClickedPos().relative(context.getClickedFace());
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        if(!PortalActivation.activate(context.getLevel(),pos))return InteractionResult.FAIL;
        if(context.getPlayer()==null || !context.getPlayer().getAbilities().instabuild)context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        tooltip.add(Component.literal("Use inside an obsidian frame to open a Nether portal").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Consumed when the portal opens").withStyle(ChatFormatting.GRAY));
    }
}
