package net.mads.industron.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
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
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.registry.ItemRegistry;
import net.mads.industron.registry.RecipeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JeiPlugin
public class IndustronJeiPlugin implements IModPlugin, IRecipeManagerPlugin {
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
        for (var recipeType : CERecipeTypes.ALL) {
            RecipeType<RecipeHolder<CERecipe>> jeiRecipeType = RecipeType.createRecipeHolderType(recipeType.id());
            CERecipeCategory category = new CERecipeCategory(recipeType, jeiRecipeType, registration.getJeiHelpers().getGuiHelper(), getCategoryIcon(recipeType.id()));
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
        for (var recipeType : CERecipeTypes.ALL) {
            var filtered = machineRecipes.stream()
                    .filter(r -> r.value().recipeType().equals(recipeType.id()))
                    .toList();
            if (!filtered.isEmpty()) {
                registration.addRecipes(RECIPE_TYPES.get(recipeType.id()), filtered);
            }
        }
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
        ItemRegistry.getAllMultiblockControllerItems().forEach(item -> registration.addRecipeCatalyst(item.get(), MultiblockStructureCategory.TYPE));
        Set<ResourceLocation> recipeTypesWithMachineCatalyst = new HashSet<>();
        for (SingleBlockMachineInstance instance : MachineDefinition.INSTANCES) {
            DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.BlockItem> machineItem = ItemRegistry.SINGLE_BLOCK_MACHINES.get(instance.registryName());
            if (machineItem == null) {
                continue;
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
        for (var entry : RECIPE_TYPES.entrySet()) {
            if (!recipeTypesWithMachineCatalyst.contains(entry.getKey())) {
                registration.addRecipeCatalyst(getCategoryIcon(entry.getKey()), entry.getValue());
            }
        }
    }

    @Override
    public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        List<RecipeType<?>> recipeTypes = new ArrayList<>(RECIPE_TYPES.values());
        recipeTypes.add(MultiblockStructureCategory.TYPE);
        recipeTypes.add(AssemblyJeiCategory.TYPE);
        recipeTypes.add(AssemblyComponentJeiCategory.TYPE);
        return recipeTypes;
    }

    @Override
    public <T> List<T> getRecipes(IRecipeCategory<T> category) {
        return getRecipes(category, null);
    }

    @Override
    public <T, V> List<T> getRecipes(IRecipeCategory<T> category, IFocus<V> focus) {
        if (category.getRecipeType().getUid().equals(MultiblockStructureCategory.TYPE.getUid())) {
            return MultiblockDefinitions.ALL.stream().map(MultiblockJeiRecipe::new).map(recipe -> (T) recipe).toList();
        }
        if (category.getRecipeType().getUid().equals(AssemblyJeiCategory.TYPE.getUid())) {
            return AssemblyJeiCategory.allRecipes().stream().map(recipe -> (T) recipe).toList();
        }
        if (category.getRecipeType().getUid().equals(AssemblyComponentJeiCategory.TYPE.getUid())) {
            return AssemblyComponentJeiCategory.allRecipes().stream().map(recipe -> (T) recipe).toList();
        }
        if (Minecraft.getInstance().level == null) {
            return List.of();
        }
        var recipeTypeUid = category.getRecipeType().getUid();
        return Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(RecipeRegistry.MACHINE_RECIPE_TYPE.get()).stream()
                .filter(r -> r.value().recipeType().equals(recipeTypeUid))
                .map(r -> (T) r)
                .toList();
    }

    private static ItemStack iconStack() {
        return new ItemStack(Items.COMMAND_BLOCK);
    }

    private ItemStack getCategoryIcon(ResourceLocation recipeTypeId) {
        for (SingleBlockMachineInstance instance : MachineDefinition.INSTANCES) {
            if (!instance.definition().recipeTypes().contains(recipeTypeId)) {
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
