package net.mads.industron.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.mads.industron.Industron;
import net.mads.industron.integration.jei.assembly.AssemblyComponentJeiCategory;
import net.mads.industron.integration.jei.assembly.AssemblyJeiCategory;
import net.mads.industron.machine.MachineDefinition;
import net.mads.industron.material.ExternalMaterialSuppression;
import net.mads.industron.machine.SingleBlockMachineInstance;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinition;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinitions;
import net.mads.industron.recipe.CERecipe;
import net.mads.industron.recipe.RecipeTypeDefinition;
import net.mads.industron.recipe.chiseling.ChiselingRecipe;
import net.mads.industron.recipe.chiseling.ChiselingRecipeInput;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.recipe.stone_shaping.StoneShapingRecipe;
import net.mads.industron.recipe.stone_shaping.StoneShapingRecipeInput;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.registry.ItemRegistry;
import net.mads.industron.registry.RecipeRegistry;
import net.mads.industron.mixin.CreateRecipeManagerAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.ref.WeakReference;
import java.util.function.Function;
import java.util.function.Supplier;

@JeiPlugin
public class IndustronJeiPlugin implements IModPlugin {
    private static final Map<ResourceLocation, RecipeType<RecipeHolder<CERecipe>>> RECIPE_TYPES = new HashMap<>();

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MultiblockStructureCategory(registration.getJeiHelpers().getGuiHelper(), iconStack()));
        registration.addRecipeCategories(new AssemblyJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new AssemblyComponentJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new StoneShapingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ChiselingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        for (var recipeType : CERecipeTypes.ALL) {
            RecipeType<RecipeHolder<CERecipe>> jeiRecipeType = RecipeType.createRecipeHolderType(recipeType.id());
            CERecipeCategory category = new CERecipeCategory(recipeType, jeiRecipeType, registration.getJeiHelpers().getGuiHelper(), getCategoryIcon(recipeType));
            registration.addRecipeCategories(category);
            RECIPE_TYPES.put(recipeType.id(), jeiRecipeType);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MultiblockStructureCategory.TYPE, MultiblockDefinitions.ALL.stream().map(MultiblockJeiRecipe::new).toList());
        registration.addRecipes(AssemblyJeiCategory.TYPE, AssemblyJeiCategory.allRecipes());
        registration.addRecipes(AssemblyComponentJeiCategory.TYPE, AssemblyComponentJeiCategory.allRecipes());
        if (Minecraft.getInstance().level == null) {
            return;
        }
        var recipeManager = Minecraft.getInstance().level.getRecipeManager();
        var machineRecipes = recipeManager.getAllRecipesFor(RecipeRegistry.MACHINE_RECIPE_TYPE.get());
        Map<ResourceLocation, List<RecipeHolder<CERecipe>>> byType = new HashMap<>();
        for (var holder : machineRecipes) {
            byType.computeIfAbsent(holder.value().recipeType(), ignored -> new ArrayList<>()).add(holder);
        }
        for (var recipeType : CERecipeTypes.ALL) {
            var filtered = byType.getOrDefault(recipeType.id(), List.of());
            if (!filtered.isEmpty()) {
                registration.addRecipes(RECIPE_TYPES.get(recipeType.id()), filtered);
            }
        }
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        // Stone Shaping lives in Minecraft's RecipeManager, which is world-bound and may not exist
        // during JEI startup. A typed dynamic plugin lets JEI query it after a world is joined without
        // duplicating the normal CE/assembly recipe registrations.
        registration.addTypedRecipeManagerPlugin(
                StoneShapingJeiCategory.TYPE,
                new StoneShapingRecipeManagerPlugin()
        );
        registration.addTypedRecipeManagerPlugin(
                ChiselingJeiCategory.TYPE,
                new ChiselingRecipeManagerPlugin()
        );
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        var ingredientManager = jeiRuntime.getIngredientManager();
        List<ItemStack> hiddenStacks = ingredientManager.getAllItemStacks().stream()
                .filter(stack -> ExternalMaterialSuppression.isSuppressedExternalMaterial(
                        BuiltInRegistries.ITEM.getKey(stack.getItem())
                ))
                .toList();

        if (!hiddenStacks.isEmpty()) {
            ingredientManager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hiddenStacks);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ItemRegistry.ASSEMBLY_WORKBENCH.get(), AssemblyJeiCategory.TYPE);
        registration.addRecipeCatalyst(ItemRegistry.INDUSTRIAL_ASSEMBLY_WORKBENCH.get(), AssemblyJeiCategory.TYPE);
        ItemStack hammer = AssemblyTools.exampleStack(Tool.HAMMER.type());
        ItemStack chisel = AssemblyTools.exampleStack(Tool.CHISEL.type());
        if (!hammer.isEmpty()) registration.addRecipeCatalyst(hammer, ChiselingJeiCategory.TYPE);
        if (!chisel.isEmpty()) registration.addRecipeCatalyst(chisel, ChiselingJeiCategory.TYPE);
        for (var stone : StoneMaterials.ALL) {
            var pebble = ItemRegistry.getStructureMaterialFormItem(stone, MaterialPart.PEBBLE);
            if (pebble != null) registration.addRecipeCatalyst(pebble.get(), StoneShapingJeiCategory.TYPE);
        }
        ItemRegistry.getAllMultiblockControllerItems().forEach(item -> registration.addRecipeCatalyst(item.get(), MultiblockStructureCategory.TYPE));
        Set<ResourceLocation> recipeTypesWithMachineCatalyst = new HashSet<>();
        for (var holder : net.mads.industron.machine.machines.kinetic.KineticMachines.blocks()) {
            var process = net.mads.industron.machine.machines.kinetic.KineticMachines.recipeType(holder.get());
            var jeiType = RECIPE_TYPES.get(process.id());
            if (jeiType != null) {
                registration.addRecipeCatalyst(holder.get().asItem(), jeiType);
                recipeTypesWithMachineCatalyst.add(process.id());
            }
        }

        for (SingleBlockMachineInstance instance : MachineDefinition.INSTANCES) {
            DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.BlockItem> machineItem = ItemRegistry.SINGLE_BLOCK_MACHINES.get(instance.registryName());
            if (machineItem == null) {
                continue;
            }
            if (instance.definition().steamConversionRecipe() != null) {
                var evaporation = RECIPE_TYPES.get(CERecipeTypes.EVAPORATION.id());
                if (evaporation != null) {
                    registration.addRecipeCatalyst(machineItem.get(), evaporation);
                    recipeTypesWithMachineCatalyst.add(CERecipeTypes.EVAPORATION.id());
                }
            }
            for (ResourceLocation recipeTypeId : instance.definition().recipeTypes()) {
                RecipeType<RecipeHolder<CERecipe>> jeiRecipeType = RECIPE_TYPES.get(recipeTypeId);
                if (jeiRecipeType != null) {
                    registration.addRecipeCatalyst(machineItem.get(), jeiRecipeType);
                    recipeTypesWithMachineCatalyst.add(recipeTypeId);
                }
            }
        }
        for (MultiblockDefinition definition : MultiblockDefinitions.ALL) {
            DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.BlockItem> controllerItem = ItemRegistry.MULTIBLOCK_CONTROLLERS.get(definition.controller().registryName());
            if (controllerItem == null) {
                continue;
            }
            for (ResourceLocation recipeTypeId : definition.recipeTypes()) {
                RecipeType<RecipeHolder<CERecipe>> jeiRecipeType = RECIPE_TYPES.get(recipeTypeId);
                if (jeiRecipeType != null) {
                    registration.addRecipeCatalyst(controllerItem.get(), jeiRecipeType);
                    recipeTypesWithMachineCatalyst.add(recipeTypeId);
                }
            }
        }
        for (RecipeTypeDefinition recipeType : CERecipeTypes.ALL) {
            if (recipeTypesWithMachineCatalyst.contains(recipeType.id())) continue;
            RecipeType<RecipeHolder<CERecipe>> jeiRecipeType = RECIPE_TYPES.get(recipeType.id());
            if (jeiRecipeType != null) {
                registration.addRecipeCatalyst(getCategoryIcon(recipeType), jeiRecipeType);
            }
        }
    }

    private static final class StoneShapingRecipeManagerPlugin
            implements ISimpleRecipeManagerPlugin<RecipeHolder<StoneShapingRecipe>> {
        private final WorldRecipeIndex<StoneShapingRecipeInput, StoneShapingRecipe> recipes = new WorldRecipeIndex<>(
                () -> RecipeRegistry.STONE_SHAPING_RECIPE_TYPE.get(),
                recipe -> List.of(recipe.mainHand(), recipe.offHand()),
                recipe -> List.of(recipe.result()));

        @Override
        public boolean isHandledInput(ITypedIngredient<?> input) {
            ItemStack stack = input.getItemStack().orElse(ItemStack.EMPTY);
            return !stack.isEmpty() && recipes.current().hasInput(stack);
        }

        @Override
        public boolean isHandledOutput(ITypedIngredient<?> output) {
            ItemStack stack = output.getItemStack().orElse(ItemStack.EMPTY);
            return !stack.isEmpty() && recipes.current().hasOutput(stack);
        }

        @Override
        public List<RecipeHolder<StoneShapingRecipe>> getRecipesForInput(ITypedIngredient<?> input) {
            ItemStack stack = input.getItemStack().orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) return List.of();
            return recipes.current().inputs(stack);
        }

        @Override
        public List<RecipeHolder<StoneShapingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) {
            ItemStack stack = output.getItemStack().orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) return List.of();
            return recipes.current().outputs(stack);
        }

        @Override
        public List<RecipeHolder<StoneShapingRecipe>> getAllRecipes() {
            return recipes.current().all();
        }
    }

    private static final class ChiselingRecipeManagerPlugin
            implements ISimpleRecipeManagerPlugin<RecipeHolder<ChiselingRecipe>> {
        private final WorldRecipeIndex<ChiselingRecipeInput, ChiselingRecipe> recipes = new WorldRecipeIndex<>(
                () -> RecipeRegistry.CHISELING_RECIPE_TYPE.get(), ChiselingRecipe::baseBlockInputStacks,
                recipe -> List.of(recipe.resultStack(), recipe.dustOutputStack()));

        @Override
        public boolean isHandledInput(ITypedIngredient<?> input) {
            ItemStack stack = input.getItemStack().orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) return false;
            var index = recipes.current();
            return !index.all().isEmpty() && (isTool(stack) || index.hasInput(stack));
        }

        @Override
        public boolean isHandledOutput(ITypedIngredient<?> output) {
            ItemStack stack = output.getItemStack().orElse(ItemStack.EMPTY);
            return !stack.isEmpty() && recipes.current().hasOutput(stack);
        }

        @Override
        public List<RecipeHolder<ChiselingRecipe>> getRecipesForInput(ITypedIngredient<?> input) {
            ItemStack stack = input.getItemStack().orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) return List.of();
            var index = recipes.current();
            return isTool(stack) ? index.all() : index.inputs(stack);
        }

        @Override
        public List<RecipeHolder<ChiselingRecipe>> getRecipesForOutput(ITypedIngredient<?> output) {
            ItemStack stack = output.getItemStack().orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) return List.of();
            return recipes.current().outputs(stack);
        }

        @Override
        public List<RecipeHolder<ChiselingRecipe>> getAllRecipes() {
            return recipes.current().all();
        }

        private static boolean isTool(ItemStack stack) {
            return AssemblyTools.find(Tool.HAMMER.type(), stack) != null
                    || AssemblyTools.find(Tool.CHISEL.type(), stack) != null;
        }
    }

    /** RecipeManager replaces its byName map on reload/sync. Do not retain that whole map or a world. */
    private static final class WorldRecipeIndex<I extends RecipeInput, T extends Recipe<I>> {
        private final Supplier<net.minecraft.world.item.crafting.RecipeType<T>> type;
        private final Function<T, List<ItemStack>> inputs;
        private final Function<T, List<ItemStack>> outputs;
        private WeakReference<RecipeManager> manager = new WeakReference<>(null);
        private WeakReference<Object> source = new WeakReference<>(null);
        private RecipeItemIndex<RecipeHolder<T>> index = empty();

        private WorldRecipeIndex(Supplier<net.minecraft.world.item.crafting.RecipeType<T>> type,
                                 Function<T, List<ItemStack>> inputs, Function<T, List<ItemStack>> outputs) {
            this.type = type;
            this.inputs = inputs;
            this.outputs = outputs;
        }

        private RecipeItemIndex<RecipeHolder<T>> current() {
            var level = Minecraft.getInstance().level;
            if (level == null) {
                if (source.get() != null || !index.all().isEmpty()) {
                    manager.clear(); source.clear(); index = empty();
                }
                return index;
            }
            RecipeManager current = level.getRecipeManager();
            Object recipes = ((CreateRecipeManagerAccess) (Object) current).industron$recipesByName();
            if (manager.get() != current || source.get() != recipes) {
                index = new RecipeItemIndex<>(current.getAllRecipesFor(type.get()),
                        holder -> inputs.apply(holder.value()), holder -> outputs.apply(holder.value()));
                manager = new WeakReference<>(current);
                source = new WeakReference<>(recipes);
            }
            return index;
        }

        private RecipeItemIndex<RecipeHolder<T>> empty() {
            return new RecipeItemIndex<>(List.of(), ignored -> List.of(), ignored -> List.of());
        }
    }

    private static ItemStack iconStack() {
        return new ItemStack(Items.COMMAND_BLOCK);
    }

    private ItemStack getCategoryIcon(RecipeTypeDefinition recipeType) {
        ItemStack toolIcon = recipeType.jeiToolIcon()
                .flatMap(AssemblyTools::findType)
                .map(AssemblyTools::exampleStack)
                .orElse(ItemStack.EMPTY);
        if (!toolIcon.isEmpty()) return toolIcon;

        ResourceLocation recipeTypeId = recipeType.id();
        for (var holder : net.mads.industron.machine.machines.kinetic.KineticMachines.blocks()) {
            if (net.mads.industron.machine.machines.kinetic.KineticMachines.recipeType(holder.get()).id().equals(recipeTypeId)) {
                return new ItemStack(holder.get().asItem());
            }
        }

        for (SingleBlockMachineInstance instance : MachineDefinition.INSTANCES) {
            if (!instance.definition().recipeTypes().contains(recipeTypeId)
                    && !(instance.definition().steamConversionRecipe() != null && recipeTypeId.equals(CERecipeTypes.EVAPORATION.id()))) {
                continue;
            }
            DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.BlockItem> machineItem = ItemRegistry.SINGLE_BLOCK_MACHINES.get(instance.registryName());
            if (machineItem != null) {
                return new ItemStack(machineItem.get());
            }
        }
        for (MultiblockDefinition definition : MultiblockDefinitions.ALL) {
            if (!definition.recipeTypes().contains(recipeTypeId)) {
                continue;
            }
            DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.BlockItem> controllerItem = ItemRegistry.MULTIBLOCK_CONTROLLERS.get(definition.controller().registryName());
            if (controllerItem != null) {
                return new ItemStack(controllerItem.get());
            }
        }
        return iconStack();
    }
}
