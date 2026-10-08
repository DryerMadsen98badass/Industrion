package net.mads.industron.recipe.stone_shaping;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mads.industron.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.BitSet;
import java.util.List;

public final class StoneShapingRecipe implements Recipe<StoneShapingRecipeInput> {
    public static final MapCodec<StoneShapingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.CODEC.fieldOf("main_hand").forGetter(StoneShapingRecipe::mainHand),
            ItemStack.CODEC.fieldOf("off_hand").forGetter(StoneShapingRecipe::offHand),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(StoneShapingRecipe::texture),
            com.mojang.serialization.Codec.STRING.listOf().fieldOf("pattern").forGetter(StoneShapingRecipe::pattern),
            ItemStack.CODEC.fieldOf("result").forGetter(StoneShapingRecipe::result)
    ).apply(instance, StoneShapingRecipe::new));

    private final ItemStack mainHand;
    private final ItemStack offHand;
    private final ResourceLocation texture;
    private final List<String> pattern;
    private final ItemStack result;
    private final BitSet patternMask;

    public StoneShapingRecipe(ItemStack mainHand, ItemStack offHand, ResourceLocation texture, List<String> pattern, ItemStack result) {
        if (mainHand == null || mainHand.isEmpty() || offHand == null || offHand.isEmpty()) {
            throw new IllegalArgumentException("Stone Shaping requires non-empty main-hand and off-hand inputs");
        }
        if (texture == null) throw new IllegalArgumentException("Stone Shaping texture cannot be null");
        if (result == null || result.isEmpty()) throw new IllegalArgumentException("Stone Shaping result cannot be empty");
        StoneShapingPattern.validate(pattern);
        this.mainHand = mainHand.copyWithCount(Math.max(1, mainHand.getCount()));
        this.offHand = offHand.copyWithCount(Math.max(1, offHand.getCount()));
        this.texture = texture;
        this.pattern = List.copyOf(pattern);
        this.result = result.copy();
        this.patternMask = StoneShapingPattern.fromRows(pattern);
    }

    public ItemStack mainHand() { return mainHand.copy(); }
    public ItemStack offHand() { return offHand.copy(); }
    public ResourceLocation texture() { return texture; }
    public List<String> pattern() { return pattern; }
    public ItemStack result() { return result.copy(); }

    public boolean matchesRemaining(BitSet remaining) {
        return StoneShapingPattern.matchesTranslated(patternMask, remaining);
    }

    public boolean patternPixel(int x, int y) {
        return patternMask.get(StoneShapingPattern.index(x, y));
    }

    @Override
    public boolean matches(StoneShapingRecipeInput input, Level level) {
        return stackMatches(input.mainHand(), mainHand) && stackMatches(input.offHand(), offHand);
    }

    private static boolean stackMatches(ItemStack actual, ItemStack required) {
        return !actual.isEmpty()
                && actual.getCount() >= required.getCount()
                && ItemStack.isSameItemSameComponents(actual, required);
    }

    @Override
    public ItemStack assemble(StoneShapingRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(mainHand));
        ingredients.add(Ingredient.of(offHand));
        return ingredients;
    }

    @Override
    public ItemStack getToastSymbol() {
        return mainHand.copyWithCount(1);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.STONE_SHAPING_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegistry.STONE_SHAPING_RECIPE_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
