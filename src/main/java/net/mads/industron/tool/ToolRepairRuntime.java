package net.mads.industron.tool;

import net.mads.industron.Industron;
import net.mads.industron.material.*;
import net.mads.industron.recipe.recipes.assembly.ToolDefinitions;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Empty-anvil repair: finished tool in main hand, one matching hot metal part in offhand. */
@EventBusSubscriber(modid=Industron.MOD_ID)
public final class ToolRepairRuntime {
    private static final String CREDIT="industron_repair_credit", SIGNATURE="industron_repair_head";
    private ToolRepairRuntime() {}
    public record Reference(IndustrialMaterial material,MaterialPart part) {}
    public static Reference reference(ItemStack stack) {
        if(stack.isEmpty() || !stack.isDamageableItem())return null;
        ToolDefinition definition=stack.getItem() instanceof MaterialEquipment equipment?equipment.definition():EquipmentExistingItems.definition(stack);
        if(definition!=null) {
            var data=stack.get(ToolComponents.PARTS.get());var slot=definition.strengthReferencePart();
            if(data==null || slot==null)return null;
            var material=ToolMaterialResolver.resolve(data.materialKey(slot.role()));
            return material instanceof IndustrialMaterial metal && slot.part().materialAmountMb()>0
                ?new Reference(metal,slot.part()):null;
        }
        var target=MaterialLookup.find(stack);
        if(target==null || target.part().materialAmountMb()<=0)return null;
        boolean tool=ToolDefinitions.ALL.stream().anyMatch(d->d.isDirectPartTool() && d.parts().stream().anyMatch(p->p.part()==target.part()));
        return tool?new Reference(target.material(),target.part()):null;
    }
    private static String signature(ItemStack tool,Reference head) {
        return head.material().id()+"/"+head.part().id()+"/"+tool.getMaxDamage();
    }
    private static long credit(ItemStack tool,Reference head) {
        CustomData data=tool.get(DataComponents.CUSTOM_DATA);if(data==null)return 0;
        CompoundTag tag=data.copyTag();return signature(tool,head).equals(tag.getString(SIGNATURE))?tag.getLong(CREDIT):0;
    }
    public static boolean tryRepair(ServerLevel level,BlockPos pos,ServerPlayer player) {
        ItemStack tool=player.getMainHandItem();Reference head=reference(tool);
        if(head==null)return false;
        ItemStack supplied=player.getOffhandItem();
        if(tool.getDamageValue()==0) {player.displayClientMessage(Component.literal("This tool is already fully repaired."),true);return true;}
        var target=MaterialLookup.find(supplied);
        if(target==null || !target.part().isHotForgePart() || target.part().materialAmountMb()<=0
                || !target.material().id().equals(head.material().id())) {
            player.displayClientMessage(Component.literal("Repair needs a hot "+head.material().displayName()+" part in your offhand."),true);return true;
        }
        var result=ToolRepairRules.repair(tool.getDamageValue(),tool.getMaxDamage(),head.part().materialAmountMb(),
                target.part().materialAmountMb(),credit(tool,head));
        CustomData custom=tool.get(DataComponents.CUSTOM_DATA);CompoundTag tag=custom==null?new CompoundTag():custom.copyTag();
        if(result.remainder()==0) {tag.remove(CREDIT);tag.remove(SIGNATURE);}
        else {tag.putLong(CREDIT,result.remainder());tag.putString(SIGNATURE,signature(tool,head));}
        tool.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));tool.setDamageValue(tool.getDamageValue()-result.repaired());
        if(!player.getAbilities().instabuild)supplied.shrink(1);
        level.playSound(null,pos,SoundEvents.ANVIL_USE,SoundSource.BLOCKS,0.7F,1.0F);
        player.displayClientMessage(Component.literal("Repaired "+result.repaired()+" durability. "+(tool.getMaxDamage()-tool.getDamageValue())+" / "+tool.getMaxDamage()),true);
        return true;
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        ItemStack stack=event.getItemStack();Reference head=reference(stack);if(head==null || stack.getDamageValue()==0)return;
        double needed=ToolRepairRules.neededMb(stack.getDamageValue(),stack.getMaxDamage(),head.part().materialAmountMb(),credit(stack,head));
        event.getToolTip().add(Component.literal(String.format(java.util.Locale.ROOT,"Repair: %.2f mB hot %s",needed,head.material().displayName())));
    }
}
