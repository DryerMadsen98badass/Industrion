package net.mads.industron.registry;

import net.mads.industron.Industron;
import net.mads.industron.recipe.CERecipe;
import net.mads.industron.recipe.CERecipeSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RecipeRegistry {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Industron.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Industron.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, CERecipeSerializer> MACHINE_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("machine", CERecipeSerializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<CERecipe>> MACHINE_RECIPE_TYPE =
            RECIPE_TYPES.register("machine", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return Industron.MOD_ID + ":machine";
                }
            });

    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
    }
}