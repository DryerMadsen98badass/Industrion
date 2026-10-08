package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.Industron;
import net.mads.industron.energy.WireThickness;
import net.mads.industron.block.PebbleWorldgenBlock;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialCatalog;
import net.mads.industron.material.MaterialCategory;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.recipes.MaterialRecipeHelper;
import net.mads.industron.material.structure.MetalMaterial;
import net.mads.industron.material.structure.MetalMaterials;
import net.mads.industron.material.structure.MetalStructureCatalog;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterialItem;
import net.mads.industron.registry.ItemRegistry;
import net.mads.industron.tool.ToolMaterialLookup;
import net.mads.industron.tool.ToolMaterialRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembly-facing material catalog.
 *
 * <p>Normal processing keeps {@link MaterialCatalog} IndustrialMaterial-only, while Assembly has to
 * accept physical parts declared by every material definition family. This catalog therefore merges
 * IndustrialMaterials with WoodMaterials and StoneMaterials without duplicating recipe definitions.</p>
 */
public final class AssemblyMaterialCatalog {
    private static final List<IndustrialSubstance> ALL = buildAll();
    private static final Map<Item, Target> STRUCTURE_ITEM_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static volatile boolean structureCacheBuilt;

    private AssemblyMaterialCatalog() {
    }

    public static List<IndustrialSubstance> all() {
        return ALL;
    }

    public static List<IndustrialSubstance> candidates(AssemblyMaterialSelector selector, MaterialPart part) {
        if (selector == null || part == null) return List.of();

        if (selector.isToolSelector()) {
            List<IndustrialSubstance> result = new ArrayList<>();
            for (IndustrialSubstance candidate : ToolMaterialRules.candidates(part)) {
                if (selector.matchesTool(candidate, part)) result.add(candidate);
            }
            return List.copyOf(result);
        }

        return ALL.stream()
                .filter(selector::matchesSubstance)
                .filter(candidate -> exposesPart(candidate, part))
                .toList();
    }

    public static boolean exposesPart(IndustrialSubstance material, MaterialPart part) {
        if (material == null || part == null || part.isFluid()) return false;
        if (material instanceof IndustrialMaterial industrial) {
            if (industrial.has(part)) return true;
            // BARS is a logical Assembly role backed by the metal structure library rather than
            // by a second generated <metal>_bars material block. Every concrete bars style remains
            // a normal structure block, while recipes can address the family once as MaterialPart.BARS.
            return part == MaterialPart.BARS && !metalBars(industrial).isEmpty();
        }
        if (material instanceof StructureMaterial structure) {
            if (structure.hasExistingPart(part)) return true;
            if (structure.generatedForms().contains(part)) return true;
            if (!part.isBlock()) return false;
            return StructureMaterialGenerator.blockDefinitions(structure).stream()
                    .anyMatch(definition -> definition.part().orElse(null) == part);
        }
        return false;
    }

    public static ItemStack stackFor(IndustrialSubstance material, MaterialPart part) {
        if (!exposesPart(material, part)) return ItemStack.EMPTY;

        if (material instanceof IndustrialMaterial industrial) {
            if (part == MaterialPart.BARS) {
                List<ItemStack> bars = metalBarStacks(industrial);
                return bars.isEmpty() ? ItemStack.EMPTY : bars.getFirst().copy();
            }
            if (industrial.hasExistingPart(part)) {
                return BuiltInRegistries.ITEM.getOptional(industrial.existingPart(part))
                        .map(ItemStack::new)
                        .orElse(ItemStack.EMPTY);
            }

            for (WireThickness thickness : WireThickness.ALL) {
                if (thickness.materialPart() != part) continue;
                var materialWires = ItemRegistry.ENERGY_WIRES.get(industrial.id());
                var holder = materialWires == null ? null : materialWires.get(thickness);
                return holder == null ? ItemStack.EMPTY : new ItemStack(holder.get());
            }

            var holder = ItemRegistry.getMaterialItem(industrial, part);
            if (holder != null) return new ItemStack(holder.get());

            Item item = BuiltInRegistries.ITEM.getOptional(
                    ResourceLocation.parse(MaterialRecipeHelper.itemId(industrial, part))
            ).orElse(Items.AIR);
            return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        }

        StructureMaterial structure = (StructureMaterial) material;
        if (structure.hasExistingPart(part)) {
            Item item = BuiltInRegistries.ITEM.getOptional(structure.existingPart(part)).orElse(Items.AIR);
            return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        }

        var formItem = ItemRegistry.getStructureMaterialFormItem(structure, part);
        if (formItem != null) return new ItemStack(formItem.get());

        ResourceLocation blockId = StructureMaterialGenerator.blockDefinitions(structure).stream()
                .filter(definition -> definition.part().orElse(null) == part)
                .map(StructureBlockDefinition::registryName)
                .map(id -> ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, id))
                .findFirst()
                .orElse(null);
        if (blockId == null) return ItemStack.EMPTY;

        Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
        if (block == null || block.asItem() == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(block);
    }

    /**
     * Returns every physical stack represented by one logical material part. Most parts have one
     * stack per material; BARS intentionally exposes every registered bars style so JEI can cycle
     * through them inside one recipe slot.
     */
    public static List<ItemStack> stacksFor(IndustrialSubstance material, MaterialPart part) {
        if (!exposesPart(material, part)) return List.of();
        if (material instanceof IndustrialMaterial industrial && part == MaterialPart.BARS) {
            return metalBarStacks(industrial);
        }
        ItemStack stack = stackFor(material, part);
        return stack.isEmpty() ? List.of() : List.of(stack);
    }

    private static List<StructureBlockDefinition> metalBars(IndustrialMaterial material) {
        if (!MaterialCategory.METAL.matches(material)) return List.of();
        MetalMaterial structureMaterial = MetalMaterials.ALL.stream()
                .filter(candidate -> candidate.source() == material || candidate.id().equals(material.id()))
                .findFirst()
                .orElse(null);
        if (structureMaterial == null) return List.of();
        return MetalStructureCatalog.blocks(structureMaterial).stream()
                .filter(definition -> definition.shape() == StructureBlockDefinition.Shape.BARS)
                .toList();
    }

    private static List<ItemStack> metalBarStacks(IndustrialMaterial material) {
        Map<Item, ItemStack> result = new LinkedHashMap<>();
        for (StructureBlockDefinition definition : metalBars(material)) {
            ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, definition.registryName());
            Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
            if (block == null || block.asItem() == Items.AIR) continue;
            ItemStack stack = new ItemStack(block);
            result.putIfAbsent(stack.getItem(), stack);
        }
        return List.copyOf(result.values());
    }

    public static Target find(Block block) {
        if (block == null) return null;
        if (block instanceof PebbleWorldgenBlock pebble) {
            return new Target(pebble.material(), MaterialPart.PEBBLE);
        }
        if (block.asItem() == Items.AIR) return null;
        return find(new ItemStack(block));
    }

    public static Target find(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        if (stack.getItem() instanceof StructureMaterialItem structureItem) {
            return new Target(structureItem.material(), structureItem.part());
        }

        MaterialLookup.MaterialTarget industrial = MaterialLookup.find(stack);
        if (industrial != null) {
            return new Target(industrial.material(), industrial.part());
        }

        ensureStructureCache();
        return STRUCTURE_ITEM_CACHE.get(stack.getItem());
    }

    private static List<IndustrialSubstance> buildAll() {
        Map<String, IndustrialSubstance> result = new LinkedHashMap<>();
        for (IndustrialMaterial material : MaterialCatalog.all()) {
            result.put("industrial/" + material.id(), material);
        }
        WoodMaterials.ALL.forEach(material -> result.put("wood/" + material.id(), material));
        StoneMaterials.ALL.forEach(material -> result.put("stone/" + material.id(), material));
        return List.copyOf(result.values());
    }

    private static void ensureStructureCache() {
        if (structureCacheBuilt) return;
        synchronized (STRUCTURE_ITEM_CACHE) {
            if (structureCacheBuilt) return;
            for (IndustrialSubstance substance : ALL) {
                // Normal IndustrialMaterial items are already handled by MaterialLookup. Only the
                // logical BARS bridge needs an Assembly-side cache entry for industrial metals.
                if (substance instanceof IndustrialMaterial industrial) {
                    for (ItemStack stack : stacksFor(industrial, MaterialPart.BARS)) {
                        if (!stack.isEmpty()) {
                            STRUCTURE_ITEM_CACHE.putIfAbsent(
                                    stack.getItem(),
                                    new Target(industrial, MaterialPart.BARS)
                            );
                        }
                    }
                    continue;
                }

                if (!(substance instanceof StructureMaterial structure)) continue;
                for (MaterialPart part : MaterialPart.values()) {
                    if (!exposesPart(structure, part)) continue;
                    for (ItemStack stack : stacksFor(structure, part)) {
                        if (!stack.isEmpty()) {
                            STRUCTURE_ITEM_CACHE.putIfAbsent(stack.getItem(), new Target(structure, part));
                        }
                    }
                }
            }
            structureCacheBuilt = true;
        }
    }

    public record Target(IndustrialSubstance material, MaterialPart part) {
        public Target {
            if (material == null) throw new IllegalArgumentException("Assembly material cannot be null");
            if (part == null) throw new IllegalArgumentException("Assembly material part cannot be null");
        }

        public ToolMaterialLookup.Target asToolTarget() {
            return new ToolMaterialLookup.Target(material, part);
        }
    }
}
