package net.mads.industron.mixin;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=BasinOperatingBlockEntity.class,remap=false)
public interface CreateBasinRecipeAccess {
    @Accessor("currentRecipe") Recipe<?> industron$currentRecipe();
    @Accessor("currentRecipe") void industron$currentRecipe(Recipe<?> recipe);
}
