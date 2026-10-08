package net.mads.industron.recipe.chiseling;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** One block-face chiseling pattern. Hammer + Chisel are intrinsic to this recipe type. */
public final class ChiselingRecipe implements Recipe<ChiselingRecipeInput> {
    public static final MapCodec<ChiselingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("base_block_input").forGetter(ChiselingRecipe::singleBaseBlockInputForCodec),
            ResourceLocation.CODEC.listOf().optionalFieldOf("base_block_inputs", List.of()).forGetter(ChiselingRecipe::multipleBaseBlockInputsForCodec),
            Codec.INT.listOf().fieldOf("pattern").forGetter(ChiselingRecipe::pattern),
            ResourceLocation.CODEC.optionalFieldOf("base_block_output").forGetter(ChiselingRecipe::baseBlockOutputId),
            ResourceLocation.CODEC.optionalFieldOf("item_output").forGetter(ChiselingRecipe::itemOutputId),
            ResourceLocation.CODEC.optionalFieldOf("dust_output").forGetter(ChiselingRecipe::dustOutputId),
            Codec.STRING.fieldOf("tier").forGetter(recipe -> recipe.tier().id())
    ).apply(instance, ChiselingRecipe::new));

    private final List<ResourceLocation> baseBlockInputIds;
    private final List<Integer> pattern;
    private final Optional<ResourceLocation> baseBlockOutputId;
    private final Optional<ResourceLocation> itemOutputId;
    private final Optional<ResourceLocation> dustOutputId;
    private final MachineTier tier;

    public ChiselingRecipe(
            Optional<ResourceLocation> singleBaseBlockInputId,
            List<ResourceLocation> multipleBaseBlockInputIds,
            List<Integer> pattern,
            Optional<ResourceLocation> baseBlockOutputId,
            Optional<ResourceLocation> itemOutputId,
            Optional<ResourceLocation> dustOutputId,
            String tierId
    ) {
        if (singleBaseBlockInputId == null || multipleBaseBlockInputIds == null
                || baseBlockOutputId == null || itemOutputId == null || dustOutputId == null) {
            throw new IllegalArgumentException("Chiseling ids cannot be null");
        }
        if (singleBaseBlockInputId.isPresent() && !multipleBaseBlockInputIds.isEmpty()) {
            throw new IllegalArgumentException("Chiseling recipe must use either base_block_input or base_block_inputs, not both");
        }

        LinkedHashSet<ResourceLocation> uniqueInputs = new LinkedHashSet<>();
        singleBaseBlockInputId.ifPresent(uniqueInputs::add);
        uniqueInputs.addAll(multipleBaseBlockInputIds);
        if (uniqueInputs.isEmpty()) {
            throw new IllegalArgumentException("Chiseling recipe needs at least one base block input");
        }
        if (uniqueInputs.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("Chiseling base block input ids cannot contain null");
        }

        if (baseBlockOutputId.isPresent() == itemOutputId.isPresent()) {
            throw new IllegalArgumentException("Chiseling recipe needs exactly one output: base_block_output or item_output");
        }

        if (pattern == null || pattern.size() != ChiselingGrid.SIZE * ChiselingGrid.SIZE) {
            throw new IllegalArgumentException("Chiseling pattern must contain exactly 9 hits");
        }
        for (int cell : pattern) {
            if (cell < 1 || cell > 9) {
                throw new IllegalArgumentException("Chiseling pattern cells must be between 1 and 9, got " + cell);
            }
        }

        this.baseBlockInputIds = List.copyOf(uniqueInputs);
        this.pattern = List.copyOf(pattern);
        this.baseBlockOutputId = baseBlockOutputId;
        this.itemOutputId = itemOutputId;
        this.dustOutputId = dustOutputId;
        this.tier = tierById(tierId);
    }

    private Optional<ResourceLocation> singleBaseBlockInputForCodec() {
        return baseBlockInputIds.size() == 1
                ? Optional.of(baseBlockInputIds.getFirst())
                : Optional.empty();
    }

    private List<ResourceLocation> multipleBaseBlockInputsForCodec() {
        return baseBlockInputIds.size() > 1 ? baseBlockInputIds : List.of();
    }

    public List<ResourceLocation> baseBlockInputIds() {
        return baseBlockInputIds;
    }

    /** Kept for callers that only need a representative input, such as a toast icon. */
    public ResourceLocation baseBlockInputId() {
        return baseBlockInputIds.getFirst();
    }

    public List<Integer> pattern() {
        return pattern;
    }

    public Optional<ResourceLocation> baseBlockOutputId() {
        return baseBlockOutputId;
    }

    public Optional<ResourceLocation> itemOutputId() {
        return itemOutputId;
    }

    public Optional<ResourceLocation> dustOutputId() {
        return dustOutputId;
    }

    public MachineTier tier() {
        return tier;
    }

    public List<Block> baseBlockInputs() {
        List<Block> blocks = new ArrayList<>(baseBlockInputIds.size());
        for (ResourceLocation id : baseBlockInputIds) {
            blocks.add(BuiltInRegistries.BLOCK.getOptional(id)
                    .orElseThrow(() -> new IllegalStateException("Unknown Chiseling input block: " + id)));
        }
        return List.copyOf(blocks);
    }

    public Block baseBlockInput() {
        ResourceLocation id = baseBlockInputIds.getFirst();
        return BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new IllegalStateException("Unknown Chiseling input block: " + id));
    }

    public Block baseBlockOutput() {
        ResourceLocation id = baseBlockOutputId
                .orElseThrow(() -> new IllegalStateException("Chiseling recipe has an item output, not a block output"));
        return BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new IllegalStateException("Unknown Chiseling output block: " + id));
    }

    public List<ItemStack> baseBlockInputStacks() {
        return baseBlockInputs().stream()
                .map(block -> new ItemStack(block.asItem()))
                .filter(stack -> !stack.isEmpty())
                .toList();
    }

    public ItemStack baseBlockInputStack() {
        return new ItemStack(baseBlockInput().asItem());
    }

    public ItemStack resultStack() {
        if (itemOutputId.isPresent()) {
            return BuiltInRegistries.ITEM.getOptional(itemOutputId.get())
                    .map(ItemStack::new)
                    .orElseThrow(() -> new IllegalStateException("Unknown Chiseling item output: " + itemOutputId.get()));
        }
        return new ItemStack(baseBlockOutput().asItem());
    }

    public ItemStack baseBlockOutputStack() {
        return baseBlockOutputId.isPresent() ? new ItemStack(baseBlockOutput().asItem()) : ItemStack.EMPTY;
    }

    public ItemStack dustOutputStack() {
        if (dustOutputId.isEmpty()) return ItemStack.EMPTY;
        return BuiltInRegistries.ITEM.getOptional(dustOutputId.get())
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    public boolean hasBlockOutput() {
        return baseBlockOutputId.isPresent();
    }

    public boolean hasItemOutput() {
        return itemOutputId.isPresent();
    }

    public boolean matchesBase(BlockState state) {
        if (state == null) return false;
        ResourceLocation stateId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return stateId != null && baseBlockInputIds.contains(stateId);
    }

    public boolean matchesPrefix(List<Integer> completed) {
        if (completed == null || completed.size() > pattern.size()) return false;
        for (int i = 0; i < completed.size(); i++) {
            if (!pattern.get(i).equals(completed.get(i))) return false;
        }
        return true;
    }

    public boolean complete(List<Integer> completed) {
        return completed != null && completed.size() == pattern.size() && matchesPrefix(completed);
    }

    @Override
    public boolean matches(ChiselingRecipeInput input, Level level) {
        if (input == null || input.baseBlock().isEmpty()) return false;
        if (baseBlockInputs().stream().noneMatch(block -> input.baseBlock().is(block.asItem()))) return false;
        return AssemblyTools.find(Tool.HAMMER.type(), input.mainHand()) != null
                && AssemblyTools.find(Tool.CHISEL.type(), input.offHand()) != null;
    }

    @Override
    public ItemStack assemble(ChiselingRecipeInput input, HolderLookup.Provider registries) {
        return resultStack();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return resultStack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        Block[] baseBlocks = baseBlockInputs().toArray(Block[]::new);
        ingredients.add(Ingredient.of(baseBlocks));
        return ingredients;
    }

    @Override
    public ItemStack getToastSymbol() {
        ItemStack chisel = AssemblyTools.exampleStack(Tool.CHISEL.type());
        return chisel.isEmpty() ? baseBlockInputStack() : chisel;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.CHISELING_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegistry.CHISELING_RECIPE_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    private static MachineTier tierById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Chiseling tier cannot be blank");
        }
        return MachineTier.ALL.stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Chiseling tier: " + id));
    }
}
