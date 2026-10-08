package net.mads.industron.data;

import com.simibubi.create.AllTags.AllItemTags;
import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.defenitions.PlantMaterials;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.plant.PlantMaterialGenerator;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.plant.PlantProcessingPlanner;
import net.mads.industron.material.plant.PlantStringCatalog;
import net.mads.industron.material.recipes.MaterialCasingGenerator;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.registry.ItemRegistry;
import net.mads.industron.recipe.recipes.assembly.ToolDefinitions;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.mads.industron.tool.ToolMaterialLookup;
import net.mads.industron.tool.ToolMaterialRules;
import net.mads.industron.transport.color.ColoredFluidPipeRegistrations;
import net.mads.industron.transport.color.PipeColorDefinitions;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.mads.industron.material.plant.PlantTags.FERTILIZERS;

public class MaterialItemTagProvider extends ItemTagsProvider {
    private static final TagKey<Item> MOLDS = createExpansionTag("molds");
    private static final TagKey<Item> COLD_MOLDS = createExpansionTag("cold_molds");
    private static final TagKey<Item> HOT_MOLDS = createExpansionTag("hot_molds");
    private static final TagKey<Item> C_CASINGS = cTag("casings");
    private static final TagKey<Item> C_MACHINE_CASINGS = cTag("machine_casings");
    private static final TagKey<Item> MATERIAL_MACHINE_CASINGS = createExpansionTag("material_machine_casings");
    private static final TagKey<Item> FINISHED_TOOLS = createExpansionTag("tools");
    private static final TagKey<Item> STRINGS = TagKey.create(Registries.ITEM, PlantStringCatalog.TAG_ID);
    private static final TagKey<Item> PLANT_FIBERS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "plant_fibers")
    );
    private static final TagKey<Item> DURABILITY_ENCHANTABLE = minecraftTag("enchantable/durability");
    private static final TagKey<Item> MINING_ENCHANTABLE = minecraftTag("enchantable/mining");
    private static final TagKey<Item> MINING_LOOT_ENCHANTABLE = minecraftTag("enchantable/mining_loot");

    private static final Map<MaterialPart, TagKey<Item>> COMMON_MATERIAL_TAGS = Map.ofEntries(
            Map.entry(MaterialPart.ORE, cTag("ores")),
            Map.entry(MaterialPart.SMALL_ORE, cTag("ores")),
            Map.entry(MaterialPart.DEEPSLATE_ORE, cTag("ores")),
            Map.entry(MaterialPart.SMALL_DEEPSLATE_ORE, cTag("ores")),
            Map.entry(MaterialPart.NETHERRACK_ORE, cTag("ores")),
            Map.entry(MaterialPart.SMALL_NETHERRACK_ORE, cTag("ores")),
            Map.entry(MaterialPart.BLACKSTONE_ORE, cTag("ores")),
            Map.entry(MaterialPart.SMALL_BLACKSTONE_ORE, cTag("ores")),
            Map.entry(MaterialPart.BASALT_ORE, cTag("ores")),
            Map.entry(MaterialPart.SMALL_BASALT_ORE, cTag("ores")),
            Map.entry(MaterialPart.END_STONE_ORE, cTag("ores")),
            Map.entry(MaterialPart.SMALL_END_STONE_ORE, cTag("ores")),
            Map.entry(MaterialPart.RAW_ORE, cTag("raw_materials")),
            Map.entry(MaterialPart.RAW_BLOCK, cTag("raw_material_blocks")),
            Map.entry(MaterialPart.INGOT, cTag("ingots")),
            Map.entry(MaterialPart.NUGGET, cTag("nuggets")),
            Map.entry(MaterialPart.BLOCK, cTag("storage_blocks")),
            Map.entry(MaterialPart.DUST, cTag("dusts")),
            Map.entry(MaterialPart.TINY_DUST, cTag("tiny_dusts")),
            Map.entry(MaterialPart.SMALL_DUST, cTag("small_dusts")),
            Map.entry(MaterialPart.CLAY, cTag("clays")),
            Map.entry(MaterialPart.CLAY_BLOCK, cTag("clay_blocks")),
            Map.entry(MaterialPart.UNFIRED_BRICK, cTag("unfired_bricks")),
            Map.entry(MaterialPart.BRICK, cTag("bricks")),
            Map.entry(MaterialPart.BRICKS, cTag("brick_blocks")),
            Map.entry(MaterialPart.IMPURE_DUST, cTag("impure_dusts")),
            Map.entry(MaterialPart.PURIFIED_DUST, cTag("purified_dusts")),
            Map.entry(MaterialPart.CRUSHED_ORE, cTag("crushed_ores")),
            Map.entry(MaterialPart.WASHED_CRUSHED_ORE, cTag("washed_crushed_ores")),
            Map.entry(MaterialPart.REFINED_ORE, cTag("refined_ores")),
            Map.entry(MaterialPart.GEM, cTag("gems")),
            Map.entry(MaterialPart.FLAWLESS_GEM, cTag("flawless_gems")),
            Map.entry(MaterialPart.EXQUISITE_GEM, cTag("exquisite_gems")),
            Map.entry(MaterialPart.PLATE, cTag("plates")),
            Map.entry(MaterialPart.DOUBLE_PLATE, cTag("double_plates")),
            Map.entry(MaterialPart.DENSE_PLATE, cTag("dense_plates")),
            Map.entry(MaterialPart.REINFORCED_PLATE, cTag("reinforced_plates")),
            Map.entry(MaterialPart.HEAT_EXCHANGER_PLATE, cTag("heat_exchanger_plates")),
            Map.entry(MaterialPart.FOIL, cTag("foils")),
            Map.entry(MaterialPart.ROD, cTag("rods")),
            Map.entry(MaterialPart.LONG_ROD, cTag("long_rods")),
            Map.entry(MaterialPart.BOLT, cTag("bolts")),
            Map.entry(MaterialPart.SCREW, cTag("screws")),
            Map.entry(MaterialPart.RIVET, cTag("rivets")),
            Map.entry(MaterialPart.WIRE, cTag("wires")),
            Map.entry(MaterialPart.FINE_WIRE, cTag("fine_wires")),
            Map.entry(MaterialPart.RING, cTag("rings")),
            Map.entry(MaterialPart.SMALL_RING, cTag("small_rings")),
            Map.entry(MaterialPart.LARGE_RING, cTag("large_rings")),
            Map.entry(MaterialPart.GEAR, cTag("gears")),
            Map.entry(MaterialPart.SMALL_GEAR, cTag("small_gears")),
            Map.entry(MaterialPart.LARGE_GEAR, cTag("large_gears")),
            Map.entry(MaterialPart.BEARING, cTag("bearings")),
            Map.entry(MaterialPart.SPRING, cTag("springs")),
            Map.entry(MaterialPart.COIL, cTag("coils")),
            Map.entry(MaterialPart.ROTOR, cTag("rotors")),
            Map.entry(MaterialPart.FRAME, cTag("frames")),
            Map.entry(MaterialPart.CASING, cTag("casings"))
    );

    public MaterialItemTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
            ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, blockTags, Industron.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            for (MaterialPart part : material.parts()) {
                TagKey<Item> commonTag = COMMON_MATERIAL_TAGS.get(part);
                if (commonTag != null) {
                    addItem(material, part, commonTag);
                }

                if (isColdMold(part) || isHotMold(part)) {
                    addMoldTags(material, part);
                }
            }

            if (MaterialOreHost.hasNaturalOre(material)) {
                for (MaterialOreHost host : MaterialOreHost.compatibleHosts(material)) {
                    for (boolean small : new boolean[]{false, true}) {
                        var item = ItemRegistry.getMaterialOreHostItem(material, host, small);
                        if (item == null) continue;
                        tag(cTag("ores")).add(item.get());
                        tag(cTag("ores/" + material.id())).add(item.get());
                    }
                }
            }
        }

        addColoredPipeVariantTags();
        addStructureMaterialTags();
        addMaterialMachineCasingTags();
        addPlantStringTags();
        addPlantFertilizerTags();
        addFinishedToolTags();
        addToolEnchantmentTags();
    }


    /** Generated plant fibre gets its own tag; all usable string variants share one string tag. */
    private void addPlantStringTags() {
        tag(STRINGS).add(Items.STRING);
        for (var material : PlantMaterials.ALL) {
            if (PlantMaterialGenerator.generates(material, PlantPart.FIBER)) {
                var fiber = ItemRegistry.getPlantMaterialItem(material, PlantPart.FIBER);
                if (fiber == null) {
                    throw new IllegalStateException(
                            "Missing generated plant fibre item for tag " + PlantPart.FIBER.registryName(material)
                    );
                }
                tag(PLANT_FIBERS).add(fiber.get());
            }

            if (!PlantMaterialGenerator.generates(material, PlantPart.STRING)) continue;
            var string = ItemRegistry.getPlantMaterialItem(material, PlantPart.STRING);
            if (string == null) {
                throw new IllegalStateException(
                        "Missing generated plant string item for tag " + PlantPart.STRING.registryName(material)
                );
            }
            tag(STRINGS).add(string.get());
        }
    }

    private void addPlantFertilizerTags() {
        for (var material : PlantMaterials.ALL) {
            if (!PlantMaterialGenerator.hasProcessSource(material)) continue;
            var fertilizer = PlantProcessingPlanner.requireIntermediate(
                    material, PlantProcessingPlanner.FERTILIZER
            );
            var item = ItemRegistry.getPlantProcessIntermediateItem(fertilizer.id());
            if (item == null) {
                throw new IllegalStateException("Missing plant fertilizer item for tag " + fertilizer.id());
            }
            tag(FERTILIZERS).add(item.get());
        }
    }

    /**
     * Finished dynamic tools are real items outside Assembly too. Give every family an
     * Industron item tag, plus the normal vanilla mining-tool tags where Minecraft has one.
     * This makes tag-based recipes/integrations see the registered finished tool item rather
     * than only the Assembly-specific ToolDefinition.
     */
    private void addFinishedToolTags() {
        for (var definition : ToolDefinitions.ALL) {
            if (!definition.isFinishedToolEnabled()) continue;
            TagKey<Item> familyTag = createExpansionTag("tools/" + definition.id());

            if (definition.isAssembledTool()) {
                var holder = ItemRegistry.getComposedTool(definition.id());
                if (holder == null) continue;
                Item item = holder.get();
                tag(FINISHED_TOOLS).add(item);
                tag(familyTag).add(item);
                switch (definition.id()) {
                    case "bow" -> tag(minecraftTag("enchantable/bow")).add(item);
                    case "crossbow" -> tag(minecraftTag("enchantable/crossbow")).add(item);
                    case "fishing_rod" -> tag(minecraftTag("enchantable/fishing")).add(item);
                    case "helmet", "chestplate", "leggings", "boots" -> {
                        tag(minecraftTag("enchantable/armor")).add(item);
                        String slot = switch(definition.id()) {case "helmet" -> "head";case "chestplate" -> "chest";case "leggings" -> "leg";default -> "foot";};
                        tag(minecraftTag("enchantable/"+slot+"_armor")).add(item);
                        tag(minecraftTag(definition.id().equals("helmet")?"head_armor":definition.id().equals("chestplate")?"chest_armor":definition.id().equals("leggings")?"leg_armor":"foot_armor")).add(item);
                    }
                    default -> {}
                }
                continue;
            }

            // Direct-part tools (currently Wrench) have one registered item per material.
            ToolDefinition.PartSlot slot = definition.parts().getFirst();
            for (var material : ToolMaterialRules.candidates(slot.part())) {
                ItemStack stack = ToolMaterialLookup.stackFor(material, slot.part());
                if (stack.isEmpty()) continue;
                tag(FINISHED_TOOLS).add(stack.getItem());
                tag(familyTag).add(stack.getItem());
            }
        }

        addVanillaToolTag(ToolDefinitions.PICKAXE, ItemTags.PICKAXES);
        addVanillaToolTag(ToolDefinitions.AXE, ItemTags.AXES);
        addVanillaToolTag(ToolDefinitions.SHOVEL, ItemTags.SHOVELS);
        addVanillaToolTag(ToolDefinitions.HOE, ItemTags.HOES);
        // Vanilla weapon enchantment tags inherit from minecraft:swords.
        addVanillaToolTag(ToolDefinitions.SWORD, ItemTags.SWORDS);
    }

    private void addVanillaToolTag(ToolDefinition definition, TagKey<Item> vanillaTag) {
        var holder = ItemRegistry.getComposedTool(definition.id());
        if (holder != null) tag(vanillaTag).add(holder.get());
    }

    private void addToolEnchantmentTags() {
        // Every finished dynamic tool uses vanilla durability, so Unbreaking/Mending remain valid.
        for (var holder : ItemRegistry.getAllComposedTools()) {
            tag(DURABILITY_ENCHANTABLE).add(holder.get());
        }

        // Only the normal mining families receive the mining-specific enchantment pools.
        for (var definition : java.util.List.of(
                ToolDefinitions.PICKAXE,
                ToolDefinitions.AXE,
                ToolDefinitions.SHOVEL,
                ToolDefinitions.HOE
        )) {
            var holder = ItemRegistry.getComposedTool(definition.id());
            if (holder == null) continue;
            tag(MINING_ENCHANTABLE).add(holder.get());
            tag(MINING_LOOT_ENCHANTABLE).add(holder.get());
        }

        // Wrench is a direct cast MaterialPart rather than a composed item, but it is still
        // a finished damageable tool and therefore belongs in the durability enchantment pool.
        for (var material : ToolMaterialRules.candidates(MaterialPart.WRENCH)) {
            var stack = ToolMaterialLookup.stackFor(material, MaterialPart.WRENCH);
            if (!stack.isEmpty()) tag(DURABILITY_ENCHANTABLE).add(stack.getItem());
        }
    }

    private void addMaterialMachineCasingTags() {
        for (MaterialCasingGenerator.GeneratedCasing generated : MaterialCasingGenerator.ALL) {
            var holder = ItemRegistry.MATERIAL_MACHINE_CASINGS.get(generated.registryName());
            if (holder == null) {
                throw new IllegalStateException("Missing registered material casing item: " + generated.registryName());
            }
            Item item = holder.get();
            tag(C_CASINGS).add(item);
            tag(C_MACHINE_CASINGS).add(item);
            tag(MATERIAL_MACHINE_CASINGS).add(item);
            tag(createExpansionTag("material_machine_casings/" + generated.definition().id())).add(item);
            tag(createExpansionTag("material_machine_casings/material/" + generated.material().id())).add(item);
        }
    }



    private void addStructureMaterialTags() {
        for (StructureMaterial material : StructureMaterials.ALL) {
            for (StructureBlockDefinition definition
                    : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                var holder = ItemRegistry.getStructureMaterialBlockItem(definition.registryName());
                if (holder == null) {
                    continue;
                }
                Item item = holder.get();
                switch (definition.shape()) {
                    case DOOR -> {
                        tag(ItemTags.DOORS).add(item);
                        if (definition.modelKind() == StructureBlockDefinition.ModelKind.CREATE_DOOR) {
                            tag(AllItemTags.CONTRAPTION_CONTROLLED.tag).add(item);
                        }
                    }
                    case TRAPDOOR -> tag(ItemTags.TRAPDOORS).add(item);
                    default -> {
                    }
                }
            }
        }
    }

    private void addColoredPipeVariantTags() {
        for (PipeColorDefinitions.PipeFamily family : PipeColorDefinitions.allFamilies()) {
            tag(family.allVariantsTag()).addOptional(family.basePipeId());
            for (DyeColor color : DyeColor.values()) {
                var item = ColoredFluidPipeRegistrations.items(family, color).pipe().get();
                tag(family.allVariantsTag()).add(item);
                tag(family.coloredVariantsTag()).add(item);
            }
        }
    }

    private void addMoldTags(IndustrialMaterial material, MaterialPart part) {
        TagKey<Item> temperatureTag = isHotMold(part) ? HOT_MOLDS : COLD_MOLDS;
        String shape = moldShape(part);

        addItem(material, part, MOLDS);
        addItem(material, part, temperatureTag);
        addItem(material, part, createExpansionTag("molds/" + shape));
        addItem(material, part, createExpansionTag((isHotMold(part) ? "hot_molds/" : "cold_molds/") + shape));
        addItem(material, part, createExpansionTag("molds/" + material.id()));
        addItem(material, part, createExpansionTag((isHotMold(part) ? "hot_molds/" : "cold_molds/") + material.id()));
    }

    private void addItem(IndustrialMaterial material, MaterialPart part, TagKey<Item> tag) {
        if (material.hasExistingPart(part)) {
            tag(tag).addOptional(material.existingPart(part));
            return;
        }

        var item = ItemRegistry.getMaterialItem(material, part);
        if (item != null) {
            tag(tag).add(item.get());
        }
        if (net.mads.industron.material.MaterialFormGenerator.hasMagneticVariant(material, part)) {
            var magneticItem = ItemRegistry.getMagneticMaterialItem(material, part);
            if (magneticItem != null) {
                tag(tag).add(magneticItem.get());
            }
        }
    }

    private static boolean isColdMold(MaterialPart part) {
        return part.name().startsWith("CAST_") && part.name().endsWith("_MOLD");
    }

    private static boolean isHotMold(MaterialPart part) {
        return part.name().startsWith("HOT_CAST_") && part.name().endsWith("_MOLD");
    }

    private static String moldShape(MaterialPart part) {
        String id = part.id();
        if (id.startsWith("hot_cast_")) {
            id = id.substring("hot_cast_".length());
        } else if (id.startsWith("cast_")) {
            id = id.substring("cast_".length());
        }
        if (id.endsWith("_mold")) {
            id = id.substring(0, id.length() - "_mold".length());
        }
        return id;
    }

    private static TagKey<Item> minecraftTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace(path));
    }

    private static TagKey<Item> cTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static TagKey<Item> createExpansionTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, path));
    }
}
