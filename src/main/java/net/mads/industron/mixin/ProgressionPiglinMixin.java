package net.mads.industron.mixin;

import net.mads.industron.progression.ProgressionMaterials;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Piglins admire the same metal they accept as barter currency, including dropped nuggets. */
@Mixin(PiglinAi.class)
public abstract class ProgressionPiglinMixin {
    @Redirect(method={"stopHoldingOffHandItem","wantsToPickup","canAdmire"},at=@At(value="INVOKE",
            target="Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private static boolean industron$currency(ItemStack stack) {
        return ProgressionMaterials.isCurrency(stack);
    }
    @Inject(method="isLovedItem",at=@At("HEAD"),cancellable=true)
    private static void industron$netherMetal(ItemStack stack,CallbackInfoReturnable<Boolean> callback) {
        var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(id.getNamespace().equals("industron")&&id.getPath().startsWith(ProgressionMaterials.GOLD+"_")) {
            callback.setReturnValue(true);
        } else if(stack.getItem() instanceof net.mads.industron.tool.MaterialEquipment equipment
                && equipment.data(stack)!=null
                && equipment.data(stack).materials().containsValue("industrial/"+ProgressionMaterials.GOLD)) {
            callback.setReturnValue(true);
        } else if(ProgressionMaterials.replacement(id,true)!=null)
            callback.setReturnValue(false);
    }
}
