package net.mads.industron.progression;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialCatalog;
import net.mads.industron.material.MaterialItem;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

/** Canonical replacement policy for newly produced external metals, not player inventories. */
public final class ProgressionMaterials {
    public static final String COPPER = "vernium";
    public static final String IRON = "caskel";
    public static final String GOLD = "raskel";
    public static final String ZINC = "uskara";
    public static final String BRASS = "jubrex";

    public record Replacement(String material, MaterialPart part, int count) {}
    private static final Map<ResourceLocation, Replacement> REPLACEMENTS = replacements();
    private static final Map<ResourceLocation, String> STRUCTURE_FORMS = structureForms();
    private static final Map<ResourceLocation, String> NON_METAL_ITEMS = Map.of(
            ResourceLocation.withDefaultNamespace("golden_apple"), "apple",
            ResourceLocation.withDefaultNamespace("enchanted_golden_apple"), "apple",
            ResourceLocation.withDefaultNamespace("golden_carrot"), "carrot",
            ResourceLocation.withDefaultNamespace("iron_horse_armor"), "leather_horse_armor",
            ResourceLocation.withDefaultNamespace("golden_horse_armor"), "leather_horse_armor");

    private ProgressionMaterials() {}

    public static IndustrialMaterial material(String id) {
        IndustrialMaterial material = MaterialCatalog.find(id);
        if (material == null || !material.properties().metal()) {
            throw new IllegalStateException("Progression requires a registered metal: " + id);
        }
        return material;
    }

    public static boolean isNether(Level level) {
        return level.dimension().equals(Level.NETHER);
    }

    /** Gold found outside Nether becomes early copper metal, preventing ruined-portal shortcuts. */
    public static Replacement replacement(ResourceLocation source, boolean netherSource) {
        Replacement replacement = REPLACEMENTS.get(source);
        if (replacement == null && (source.getNamespace().equals("minecraft") || source.getNamespace().equals("create"))) {
            String path = source.getPath();
            // Equipment has its own composed-part replacement; do not reduce it to raw salvage.
            boolean equipment = java.util.Arrays.stream(new String[]{"sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots"})
                    .anyMatch(type -> path.endsWith("_" + type));
            // These functional transports are intentionally kept by the existing material API.
            boolean functionalTransport = source.getNamespace().equals("create")
                    && (path.equals("brass_funnel") || path.equals("brass_tunnel"));
            if (!equipment && !functionalTransport) {
                for (String token : path.split("_")) {
                    String metal = switch (token) {
                        case "copper" -> COPPER; case "iron" -> IRON;
                        case "gold", "golden" -> GOLD; case "zinc" -> ZINC; case "brass" -> BRASS;
                        default -> null;
                    };
                    if (metal != null) {
                        replacement = new Replacement(metal, MaterialPart.NUGGET, 1);
                        break;
                    }
                }
            }
        }
        if (replacement != null && replacement.material().equals(GOLD) && !netherSource) {
            return new Replacement(COPPER, replacement.part(), replacement.count());
        }
        return replacement;
    }

    public static ItemStack replace(ItemStack original, boolean netherSource) {
        if (original.isEmpty()) return original;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(original.getItem());
        String shape = STRUCTURE_FORMS.get(id);
        if (shape != null) return copyName(original, new ItemStack(structureBlock(shape).asItem(), original.getCount()));
        String nonMetal = NON_METAL_ITEMS.get(id);
        if (nonMetal != null) return copyName(original, new ItemStack(
                BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(nonMetal)), original.getCount()));
        Replacement replacement = replacement(id, netherSource);
        if (replacement == null) return original;
        var holder = ItemRegistry.getMaterialItem(material(replacement.material()), replacement.part());
        if (holder == null) throw new IllegalStateException("Missing progression form: " + replacement);
        ItemStack result = new ItemStack(holder.get(), Math.multiplyExact(original.getCount(), replacement.count()));
        // Do not copy foreign custom-data/material components onto a different substance.
        if (original.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            result.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    original.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME));
        }
        return result;
    }

    public static boolean isCurrency(ItemStack stack) {
        return stack.getItem() instanceof MaterialItem item && !item.magnetic()
                && item.part() == MaterialPart.INGOT && item.material().id().equals(GOLD);
    }

    /** Structure shapes use the existing metal library; shared properties preserve native behaviour. */
    public static BlockState replaceStructureState(BlockState state, boolean netherSource) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String shape = STRUCTURE_FORMS.get(id);
        if (shape != null) return structureBlock(shape).withPropertiesOf(state);
        String path = id.getPath();
        if ((id.getNamespace().equals("minecraft") || id.getNamespace().equals("create"))
                && (path.endsWith("_ore") && replacement(id, netherSource) != null
                    || path.equals("gilded_blackstone"))) {
            if (path.equals("gilded_blackstone")) return net.minecraft.world.level.block.Blocks.BLACKSTONE.defaultBlockState();
            if (path.equals("nether_gold_ore")) return net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState();
            return (path.startsWith("deepslate_") ? net.minecraft.world.level.block.Blocks.DEEPSLATE
                    : net.minecraft.world.level.block.Blocks.STONE).defaultBlockState();
        }
        Replacement replacement = replacement(id, netherSource);
        if (replacement == null) return state;
        if (!replacement.part().isBlock() || replacement.count() != 1) {
            // No corresponding functional block exists yet: do not spawn a legacy metal block.
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        var holder = ItemRegistry.getMaterialItem(material(replacement.material()), replacement.part());
        if (holder == null || !(holder.get() instanceof net.minecraft.world.item.BlockItem blockItem))
            throw new IllegalStateException("Missing progression block: " + replacement);
        return blockItem.getBlock().withPropertiesOf(state);
    }

    public static boolean isLegacyOre(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getPath().endsWith("_ore") && replacement(id, true) != null;
    }

    private static Block structureBlock(String path) {
        ResourceLocation target = ResourceLocation.fromNamespaceAndPath("industron", path);
        if (!BuiltInRegistries.BLOCK.containsKey(target))
            throw new IllegalStateException("Missing progression structure block: " + target);
        return BuiltInRegistries.BLOCK.get(target);
    }

    private static ItemStack copyName(ItemStack original, ItemStack result) {
        if (original.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME))
            result.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    original.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME));
        return result;
    }

    public static Map<ResourceLocation, String> structureMappings() { return STRUCTURE_FORMS; }

    private static Map<ResourceLocation, String> structureForms() {
        Map<ResourceLocation, String> map = new LinkedHashMap<>();
        for (String age : new String[]{"", "exposed_", "weathered_", "oxidized_"}) {
            for (String wax : new String[]{"", "waxed_"}) {
                for (String[] form : new String[][]{
                        {"copper", "plated_block"}, {"cut_copper", "cut_block"},
                        {"chiseled_copper", "chiseled_block"}, {"cut_copper_slab", "cut_slab"},
                        {"cut_copper_stairs", "cut_stairs"}, {"copper_grate", "grate"},
                        {"copper_bulb", "bulb"}, {"copper_door", "crossbar_door"},
                        {"copper_trapdoor", "crossbar_trapdoor"}}) {
                    String name = age.isEmpty() && form[0].equals("copper") ? "copper_block" : age + form[0];
                    map.put(ResourceLocation.withDefaultNamespace(wax + name), COPPER + "_" + form[1]);
                }
            }
        }
        map.put(ResourceLocation.withDefaultNamespace("iron_bars"), IRON + "_classic_bars");
        map.put(ResourceLocation.withDefaultNamespace("iron_door"), IRON + "_secure_door");
        map.put(ResourceLocation.withDefaultNamespace("iron_trapdoor"), IRON + "_secure_trapdoor");
        map.put(ResourceLocation.withDefaultNamespace("chain"), IRON + "_classic_bars");
        for (String metal : new String[]{"copper", "iron", "zinc", "brass"}) {
            String target = switch (metal) { case "copper" -> COPPER; case "iron" -> IRON; case "zinc" -> ZINC; default -> BRASS; };
            for (String[] form : new String[][]{{"bars", "framed_bars"}, {"door", "framed_door"},
                    {"ladder", "ladder"}, {"scaffolding", "framed_scaffolding"}, {"casing", "plated_block"},
                    {"shingles", "shingles"}, {"shingle_slab", "shingle_slab"}, {"shingle_stairs", "shingle_stairs"},
                    {"tiles", "tiles"}, {"tile_slab", "tile_slab"}, {"tile_stairs", "tile_stairs"}})
                map.put(ResourceLocation.fromNamespaceAndPath("create", metal + "_" + form[0]), target + "_" + form[1]);
        }
        return Map.copyOf(map);
    }

    public static Map<ResourceLocation, Replacement> mappings() { return REPLACEMENTS; }

    private static Map<ResourceLocation, Replacement> replacements() {
        Map<ResourceLocation, Replacement> map = new LinkedHashMap<>();
        family(map, "minecraft", "copper", COPPER);
        family(map, "minecraft", "iron", IRON);
        family(map, "minecraft", "gold", GOLD);
        family(map, "create", "zinc", ZINC);
        family(map, "create", "brass", BRASS);
        put(map, "create", "crushed_raw_copper", COPPER, MaterialPart.CRUSHED_ORE, 1);
        put(map, "create", "crushed_raw_iron", IRON, MaterialPart.CRUSHED_ORE, 1);
        put(map, "create", "crushed_raw_gold", GOLD, MaterialPart.CRUSHED_ORE, 1);
        put(map, "create", "crushed_raw_zinc", ZINC, MaterialPart.CRUSHED_ORE, 1);
        put(map, "create", "copper_sheet", COPPER, MaterialPart.PLATE, 1);
        put(map, "create", "iron_sheet", IRON, MaterialPart.PLATE, 1);
        put(map, "create", "golden_sheet", GOLD, MaterialPart.PLATE, 1);
        put(map, "create", "brass_sheet", BRASS, MaterialPart.PLATE, 1);
        for (String age : new String[]{"", "exposed_", "weathered_", "oxidized_"}) {
            for (String wax : new String[]{"", "waxed_"}) {
                for (String block : new String[]{"copper", "cut_copper", "chiseled_copper"}) {
                    String name = age.isEmpty() && block.equals("copper") ? "copper_block" : age + block;
                    put(map, "minecraft", wax + name, COPPER, MaterialPart.BLOCK, 1);
                }
                // Conservative salvage: rounded down, never yields more metal than crafting consumes.
                for (String shape : new String[]{"cut_copper_slab", "cut_copper_stairs"}) {
                    put(map, "minecraft", wax + age + shape, COPPER, MaterialPart.NUGGET, 40);
                }
                put(map, "minecraft", wax + age + "copper_door", COPPER, MaterialPart.NUGGET, 18);
                put(map, "minecraft", wax + age + "copper_trapdoor", COPPER, MaterialPart.NUGGET, 36);
                put(map, "minecraft", wax + age + "copper_grate", COPPER, MaterialPart.NUGGET, 20);
                put(map, "minecraft", wax + age + "copper_bulb", COPPER, MaterialPart.NUGGET, 6);
            }
        }
        put(map, "minecraft", "gilded_blackstone", GOLD, MaterialPart.NUGGET, 1);
        put(map, "minecraft", "lightning_rod", COPPER, MaterialPart.INGOT, 3);
        put(map, "minecraft", "bell", BRASS, MaterialPart.INGOT, 3);
        for (String rail : new String[]{"rail", "detector_rail", "activator_rail"})
            put(map, "minecraft", rail, IRON, MaterialPart.NUGGET, 3);
        put(map, "minecraft", "powered_rail", GOLD, MaterialPart.NUGGET, 9);
        for (String cart : new String[]{"minecart", "chest_minecart", "hopper_minecart", "furnace_minecart", "tnt_minecart"})
            put(map, "minecraft", cart, IRON, MaterialPart.INGOT, 5);
        put(map, "minecraft", "iron_bars", IRON, MaterialPart.NUGGET, 3);
        put(map, "minecraft", "iron_door", IRON, MaterialPart.NUGGET, 18);
        put(map, "minecraft", "iron_trapdoor", IRON, MaterialPart.NUGGET, 36);
        put(map, "minecraft", "chain", IRON, MaterialPart.NUGGET, 11);
        put(map, "minecraft", "heavy_weighted_pressure_plate", IRON, MaterialPart.NUGGET, 18);
        put(map, "minecraft", "light_weighted_pressure_plate", GOLD, MaterialPart.NUGGET, 18);
        return Map.copyOf(map);
    }

    private static void family(Map<ResourceLocation, Replacement> map, String namespace, String name, String material) {
        put(map, namespace, name + "_ingot", material, MaterialPart.INGOT, 1);
        put(map, namespace, name + "_nugget", material, MaterialPart.NUGGET, 1);
        put(map, namespace, name + "_block", material, MaterialPart.BLOCK, 1);
        put(map, namespace, "raw_" + name, material, MaterialPart.RAW_ORE, 1);
        put(map, namespace, "raw_" + name + "_block", material, MaterialPart.RAW_BLOCK, 1);
        // Silk Touch/structure ore items become raw mineral; no new elemental ore blocks are invented.
        put(map, namespace, name + "_ore", material, MaterialPart.RAW_ORE, 1);
        put(map, namespace, "deepslate_" + name + "_ore", material, MaterialPart.RAW_ORE, 1);
        if (name.equals("gold")) put(map, namespace, "nether_gold_ore", material, MaterialPart.RAW_ORE, 1);
    }

    private static void put(Map<ResourceLocation, Replacement> map, String namespace, String path,
                            String material, MaterialPart part, int count) {
        map.put(ResourceLocation.fromNamespaceAndPath(namespace, path), new Replacement(material, part, count));
    }
}
