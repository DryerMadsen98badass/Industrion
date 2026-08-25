package net.mads.industron.mixin;

import com.google.gson.JsonElement;
import net.mads.industron.material.ExternalMaterialSuppression;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

/** Filters external material progression before RecipeManager deserializes recipes. */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @ModifyVariable(method = "apply", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Map<ResourceLocation, JsonElement> industron$removeExternalMaterialRecipes(
            Map<ResourceLocation, JsonElement> recipes
    ) {
        return ExternalMaterialSuppression.filterExternalRecipes(recipes);
    }
}
