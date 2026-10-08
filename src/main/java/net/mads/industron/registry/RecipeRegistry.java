package net.mads.industron.registry;

import net.mads.industron.Industron;
import net.mads.industron.recipe.CERecipe;
import net.mads.industron.recipe.CERecipeSerializer;
import net.mads.industron.recipe.chiseling.ChiselingRecipe;
import net.mads.industron.recipe.chiseling.ChiselingRecipeSerializer;
import net.mads.industron.recipe.stone_shaping.StoneShapingRecipe;
import net.mads.industron.recipe.stone_shaping.StoneShapingRecipeSerializer;
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

    public static final DeferredHolder<RecipeSerializer<?>, StoneShapingRecipeSerializer> STONE_SHAPING_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("stone_shaping", StoneShapingRecipeSerializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<StoneShapingRecipe>> STONE_SHAPING_RECIPE_TYPE =
            RECIPE_TYPES.register("stone_shaping", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return Industron.MOD_ID + ":stone_shaping";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, ChiselingRecipeSerializer> CHISELING_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("chiseling", ChiselingRecipeSerializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<ChiselingRecipe>> CHISELING_RECIPE_TYPE =
            RECIPE_TYPES.register("chiseling", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return Industron.MOD_ID + ":chiseling";
                }
            });

    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
    }
}