package net.mads.industron.material.defenitions;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.material.structure.WoodModel;

import java.util.List;

import static net.mads.industron.material.defenitions.CompoundMaterials.DULCARA;
import static net.mads.industron.material.defenitions.CompoundMaterials.LIGNARA;
import static net.mads.industron.material.defenitions.CompoundMaterials.RESYRA;
import static net.mads.industron.material.defenitions.CompoundMaterials.SYLVARA;
import static net.mads.industron.material.defenitions.IndustrialMaterials.component;

public final class WoodMaterials {
    // WoodMaterial colors are anchored to the matching plank texture, so generated wood parts
    // inherit the visual identity of the worked wood rather than the bark/log.
    public static final WoodMaterial ACACIA = wood(
            "acacia",
            "Acacia",
            0xA85A32,
            WoodModel.ACACIA,
            MachineTier.ULV
    )
            .contains(
                    component(LIGNARA, 14),
                    component(SYLVARA, 3),
                    component(RESYRA, 2),
                    component(DULCARA, 1)
            )
            .existing(MaterialPart.LOG, "minecraft:acacia_log")
            .existing(MaterialPart.STRIPPED_LOG, "minecraft:stripped_acacia_log")
            .existing(MaterialPart.WOOD, "minecraft:acacia_wood")
            .existing(MaterialPart.STRIPPED_WOOD, "minecraft:stripped_acacia_wood")
            .existing(MaterialPart.PLANKS, "minecraft:acacia_planks")
            .existing(MaterialPart.SLAB, "minecraft:acacia_slab")
            .existing(MaterialPart.STAIRS, "minecraft:acacia_stairs")
            .existing(MaterialPart.FENCE, "minecraft:acacia_fence")
            .existing(MaterialPart.FENCE_GATE, "minecraft:acacia_fence_gate")
            .existing(MaterialPart.BUTTON, "minecraft:acacia_button")
            .existing(MaterialPart.PRESSURE_PLATE, "minecraft:acacia_pressure_plate")
            .existing(MaterialPart.DOOR, "minecraft:acacia_door")
            .existing(MaterialPart.TRAPDOOR, "minecraft:acacia_trapdoor")
            .existing(MaterialPart.LEAVES, "minecraft:acacia_leaves")
            .existing(MaterialPart.SAPLING, "minecraft:acacia_sapling")
            .existing(MaterialPart.SIGN, "minecraft:acacia_sign")
            .existing(MaterialPart.WALL_SIGN, "minecraft:acacia_wall_sign")
            .existing(MaterialPart.HANGING_SIGN, "minecraft:acacia_hanging_sign")
            .existing(MaterialPart.WALL_HANGING_SIGN, "minecraft:acacia_wall_hanging_sign")
            .existing(MaterialPart.WINDOW, "create:acacia_window")
            .existing(MaterialPart.WINDOW_PANE, "create:acacia_window_pane")
            .existing(MaterialPart.BOAT, "minecraft:acacia_boat")
            .existing(MaterialPart.CHEST_BOAT, "minecraft:acacia_chest_boat");

    public static final WoodMaterial BAMBOO = wood(
            "bamboo",
            "Bamboo",
            0xC1AD50,
            WoodModel.BAMBOO,
            MachineTier.ULV
    )
            .contains(
                    component(LIGNARA, 14),
                    component(SYLVARA, 3),
                    component(RESYRA, 2),
                    component(DULCARA, 1)
            )
            .existing(MaterialPart.LOG, "minecraft:bamboo_block")
            .existing(MaterialPart.STRIPPED_LOG, "minecraft:stripped_bamboo_block")
            .existing(MaterialPart.PLANKS, "minecraft:bamboo_planks")
            .existing(MaterialPart.SLAB, "minecraft:bamboo_slab")
            .existing(MaterialPart.STAIRS, "minecraft:bamboo_stairs")
            .existing(MaterialPart.FENCE, "minecraft:bamboo_fence")
            .existing(MaterialPart.FENCE_GATE, "minecraft:bamboo_fence_gate")
            .existing(MaterialPart.BUTTON, "minecraft:bamboo_button")
            .existing(MaterialPart.PRESSURE_PLATE, "minecraft:bamboo_pressure_plate")
            .existing(MaterialPart.DOOR, "minecraft:bamboo_door")
            .existing(MaterialPart.TRAPDOOR, "minecraft:bamboo_trapdoor")
            .existing(MaterialPart.SAPLING, "minecraft:bamboo_sapling")
            .existing(MaterialPart.SIGN, "minecraft:bamboo_sign")
            .existing(MaterialPart.WALL_SIGN, "minecraft:bamboo_wall_sign")
            .existing(MaterialPart.HANGING_SIGN, "minecraft:bamboo_hanging_sign")
            .existing(MaterialPart.WALL_HANGING_SIGN, "minecraft:bamboo_wall_hanging_sign")
            .existing(MaterialPart.MOSAIC, "minecraft:bamboo_mosaic")
            .existing(MaterialPart.MOSAIC_SLAB, "minecraft:bamboo_mosaic_slab")
            .existing(MaterialPart.MOSAIC_STAIRS, "minecraft:bamboo_mosaic_stairs")
            .existing(MaterialPart.WINDOW, "create:bamboo_window")
            .existing(MaterialPart.WINDOW_PANE, "create:bamboo_window_pane")
            .existing(MaterialPart.BOAT, "minecraft:bamboo_raft")
            .existing(MaterialPart.CHEST_BOAT, "minecraft:bamboo_chest_raft");

    public static final WoodMaterial BIRCH = wood(
            "birch",
            "Birch",
            0xC0AF79,
            WoodModel.BIRCH,
            MachineTier.ULV
    )
            .contains(
                    component(LIGNARA, 14),
                    component(SYLVARA, 3),
                    component(RESYRA, 2),
                    component(DULCARA, 1)
            )
            .existing(MaterialPart.LOG, "minecraft:birch_log")
            .existing(MaterialPart.STRIPPED_LOG, "minecraft:stripped_birch_log")
            .existing(MaterialPart.WOOD, "minecraft:birch_wood")
            .existing(MaterialPart.STRIPPED_WOOD, "minecraft:stripped_birch_wood")
            .existing(MaterialPart.PLANKS, "minecraft:birch_planks")
            .existing(MaterialPart.SLAB, "minecraft:birch_slab")
            .existing(MaterialPart.STAIRS, "minecraft:birch_stairs")
            .existing(MaterialPart.FENCE, "minecraft:birch_fence")
            .existing(MaterialPart.FENCE_GATE, "minecraft:birch_fence_gate")
            .existing(MaterialPart.BUTTON, "minecraft:birch_button")
            .existing(MaterialPart.PRESSURE_PLATE, "minecraft:birch_pressure_plate")
            .existing(MaterialPart.DOOR, "minecraft:birch_door")
            .existing(MaterialPart.TRAPDOOR, "minecraft:birch_trapdoor")
            .existing(MaterialPart.LEAVES, "minecraft:birch_leaves")
            .existing(MaterialPart.SAPLING, "minecraft:birch_sapling")
            .existing(MaterialPart.SIGN, "minecraft:birch_sign")
            .existing(MaterialPart.WALL_SIGN, "minecraft:birch_wall_sign")
            .existing(MaterialPart.HANGING_SIGN, "minecraft:birch_hanging_sign")
            .existing(MaterialPart.WALL_HANGING_SIGN, "minecraft:birch_wall_hanging_sign")
            .existing(MaterialPart.WINDOW, "create:birch_window")
            .existing(MaterialPart.WINDOW_PANE, "create:birch_window_pane")
            .existing(MaterialPart.BOAT, "minecraft:birch_boat")
            .existing(MaterialPart.CHEST_BOAT, "minecraft:birch_chest_boat");

    public static final WoodMaterial CHERRY = wood(
            "cherry",
            "Cherry",
            0xE3B3AD,
            WoodModel.CHERRY,
            MachineTier.ULV
    )
            .contains(
                    component(LIGNARA, 14),
                    component(SYLVARA, 3),
                    component(RESYRA, 2),
                    component(DULCARA, 1)
            )
            .existing(MaterialPart.LOG, "minecraft:cherry_log")
            .existing(MaterialPart.STRIPPED_LOG, "minecraft:stripped_cherry_log")
            .existing(MaterialPart.WOOD, "minecraft:cherry_wood")
            .existing(MaterialPart.STRIPPED_WOOD, "minecraft:stripped_cherry_wood")
            .existing(MaterialPart.PLANKS, "minecraft:cherry_planks")
            .existing(MaterialPart.SLAB, "minecraft:cherry_slab")
            .existing(MaterialPart.STAIRS, "minecraft:cherry_stairs")
            .existing(MaterialPart.FENCE, "minecraft:cherry_fence")
            .existing(MaterialPart.FENCE_GATE, "minecraft:cherry_fence_gate")
            .existing(MaterialPart.BUTTON, "minecraft:cherry_button")
            .existing(MaterialPart.PRESSURE_PLATE, "minecraft:cherry_pressure_plate")
            .existing(MaterialPart.DOOR, "minecraft:cherry_door")
            .existing(MaterialPart.TRAPDOOR, "minecraft:cherry_trapdoor")
            .existing(MaterialPart.LEAVES, "minecraft:cherry_leaves")
            .existing(MaterialPart.SAPLING, "minecraft:cherry_sapling")
            .existing(MaterialPart.SIGN, "minecraft:cherry_sign")
            .existing(MaterialPart.WALL_SIGN, "minecraft:cherry_wall_sign")
            .existing(MaterialPart.HANGING_SIGN, "minecraft:cherry_hanging_sign")
            .existing(MaterialPart.WALL_HANGING_SIGN, "minecraft:cherry_wall_hanging_sign")
            .existing(MaterialPart.WINDOW, "create:cherry_window")
            .existing(MaterialPart.WINDOW_PANE, "create:cherry_window_pane")
            .existing(MaterialPart.BOAT, "minecraft:cherry_boat")
            .existing(MaterialPart.CHEST_BOAT, "minecraft:cherry_chest_boat");

    public static final WoodMaterial CRIMSON = wood(
            "crimson",
            "Crimson",
            0x653147,
            WoodModel.CRIMSON,
            MachineTier.ULV
    )
            .contains(
                    component(LIGNARA, 14),
                    component(SYLVARA, 3),
                    component(RESYRA, 2),
                    component(DULCARA, 1)
            )
            .existing(MaterialPart.LOG, "minecraft:crimson_stem")
            .existing(MaterialPart.STRIPPED_LOG, "minecraft:stripped_crimson_stem")
            .existing(MaterialPart.WOOD, "minecraft:crimson_hyphae")
            .existing(MaterialPart.STRIPPED_WOOD, "minecraft:stripped_crimson_hyphae")
            .existing(MaterialPart.PLANKS, "minecraft:crimson_planks")
            .existing(MaterialPart.SLAB, "minecraft:crimson_slab")
            .existing(MaterialPart.STAIRS, "minecraft:crimson_stairs")
            .existing(MaterialPart.FENCE, "minecraft:crimson_fence")
            .existing(MaterialPart.FENCE_GATE, "minecraft:crimson_fence_gate")
            .existing(MaterialPart.BUTTON, "minecraft:crimson_button")
            .existing(MaterialPart.PRESSURE_PLATE, "minecraft:crimson_pressure_plate")
            .existing(MaterialPart.DOOR, "minecraft:crimson_door")
            .existing(MaterialPart.TRAPDOOR, "minecraft:crimson_trapdoor")
            .existing(MaterialPart.SIGN, "minecraft:crimson_sign")
            .existing(MaterialPart.WALL_SIGN, "minecraft:crimson_wall_sign")
            .existing(MaterialPart.HANGING_SIGN, "minecraft:crimson_hanging_sign")
            .existing(MaterialPart.WALL_HANGING_SIGN, "minecraft:crimson_wall_hanging_sign")
            .existing(MaterialPart.WINDOW, "create:crimson_window")
            .existing(MaterialPart.WINDOW_PANE, "create:crimson_window_pane");

    public static final WoodMaterial DARK_OAK = wood(
            "dark_oak",
            "Dark Oak",
            0x432B14,
            WoodModel.DARK_OAK,
            MachineTier.ULV
    )
            .contains(
                    component(LIGNARA, 14),
                    component(SYLVARA, 3),
                    component(RESYRA, 2),
                    component(DULCARA, 1)
            )
            .existing(MaterialPart.LOG, "minecraft:dark_oak_log")
            .existing(MaterialPart.STRIPPED_LOG, "minecraft:stripped_dark_oak_log")
            .existing(MaterialPart.WOOD, "minecraft:dark_oak_wood")
            .existing(MaterialPart.STRIPPED_WOOD, "minecraft:stripped_dark_oak_wood")
            .existing(MaterialPart.PLANKS, "minecraft:dark_oak_planks")
            .existing(MaterialPart.SLAB, "minecraft:dark_oak_slab")
            .existing(MaterialPart.STAIRS, "minecraft:dark_oak_stairs")
            .existing(MaterialPart.FENCE, "minecraft:dark_oak_fence")
            .existing(MaterialPart.FENCE_GATE, "minecraft:dark_oak_fence_gate")
            .existing(MaterialPart.BUTTON, "minecraft:dark_oak_button")
            .existing(MaterialPart.PRESSURE_PLATE, "minecraft:dark_oak_pressure_plate")
            .existing(MaterialPart.DOOR, "minecraft:dark_oak_door")
            .existing(MaterialPart.TRAPDOOR, "minecraft:dark_oak_trapdoor")
            .existing(MaterialPart.LEAVES, "minecraft:dark_oak_leaves")
            .existing(MaterialPart.SAPLING, "minecraft:dark_oak_sapling")
            .existing(MaterialPart.SIGN, "minecraft:dark_oak_sign")
            .existing(MaterialPart.WALL_SIGN, "minecraft:dark_oak_wall_sign")
            .existing(MaterialPart.HANGING_SIGN, "minecraft:dark_oak_hanging_sign")
            .existing(MaterialPart.WALL_HANGING_SIGN, "minecraft:dark_oak_wall_hanging_sign")
            .existing(MaterialPart.WINDOW, "create:dark_oak_window")
            .existing(MaterialPart.WINDOW_PANE, "create:dark_oak_window_pane")
            .existing(MaterialPart.BOAT, "minecraft:dark_oak_boat")
            .existing(MaterialPart.CHEST_BOAT, "minecraft:dark_oak_chest_boat");

    public static final WoodMaterial JUNGLE = wood(
            "jungle",
            "Jungle",
            0xA07351,
            WoodModel.JUNGLE,
            MachineTier.ULV
    )
            .contains(
                    component(LIGNARA, 14),
                    component(SYLVARA, 3),
                    component(RESYRA, 2),
                    component(DULCARA, 1)
            )
            .existing(MaterialPart.LOG, "minecraft:jungle_log")
            .existing(MaterialPart.STRIPPED_LOG, "minecraft:stripped_jungle_log")
            .existing(MaterialPart.WOOD, "minecraft:jungle_wood")
            .existing(MaterialPart.STRIPPED_WOOD, "minecraft:stripped_jungle_wood")
            .existing(MaterialPart.PLANKS, "minecraft:jungle_planks")
            .existing(MaterialPart.SLAB, "minecraft:jungle_slab")
            .existing(MaterialPart.STAIRS, "minecraft:jungle_stairs")
            .existing(MaterialPart.FENCE, "minecraft:jungle_fence")
            .existing(MaterialPart.FENCE_GATE, "minecraft:jungle_fence_gate")
            .existing(MaterialPart.BUTTON, "minecraft:jungle_button")
            .existing(MaterialPart.PRESSURE_PLATE, "minecraft:jungle_pressure_plate")
            .existing(MaterialPart.DOOR, "minecraft:jungle_door")
            .existing(MaterialPart.TRAPDOOR, "minecraft:jungle_trapdoor")
            .existing(MaterialPart.LEAVES, "minecraft:jungle_leaves")
            .existing(MaterialPart.SAPLING, "minecraft:jungle_sapling")
            .existing(MaterialPart.SIGN, "minecraft:jungle_sign")
            .existing(MaterialPart.WALL_SIGN, "minecraft:jungle_wall_sign")
            .existing(MaterialPart.HANGING_SIGN, "minecraft:jungle_hanging_sign")
            .existing(MaterialPart.WALL_HANGING_SIGN, "minecraft:jungle_wall_hanging_sign")
            .existing(MaterialPart.WINDOW, "create:jungle_window")
            .existing(MaterialPart.WINDOW_PANE, "create:jungle_window_pane")
            .existing(MaterialPart.BOAT, "minecraft:jungle_boat")
            .existing(MaterialPart.CHEST_BOAT, "minecraft:jungle_chest_boat");

    public static final WoodMaterial MANGROVE = wood(
            "mangrove",
            "Mangrove",
            0x763631,
            WoodModel.MANGROVE,
            MachineTier.ULV
    )
            .contains(
                    component(LIGNARA, 14),
                    component(SYLVARA, 3),
                    component(RESYRA, 2),
                    component(DULCARA, 1)
            )
            .existing(MaterialPart.LOG, "minecraft:mangrove_log")
            .existing(MaterialPart.STRIPPED_LOG, "minecraft:stripped_mangrove_log")
            .existing(MaterialPart.WOOD, "minecraft:mangrove_wood")
            .existing(MaterialPart.STRIPPED_WOOD, "minecraft:stripped_mangrove_wood")
            .existing(MaterialPart.PLANKS, "minecraft:mangrove_planks")
            .existing(MaterialPart.SLAB, "minecraft:mangrove_slab")
            .existing(MaterialPart.STAIRS, "minecraft:mangrove_stairs")
            .existing(MaterialPart.FENCE, "minecraft:mangrove_fence")
            .existing(MaterialPart.FENCE_GATE, "minecraft:mangrove_fence_gate")
            .existing(MaterialPart.BUTTON, "minecraft:mangrove_button")
            .existing(MaterialPart.PRESSURE_PLATE, "minecraft:mangrove_pressure_plate")
            .existing(MaterialPart.DOOR, "minecraft:mangrove_door")
            .existing(MaterialPart.TRAPDOOR, "minecraft:mangrove_trapdoor")
            .existing(MaterialPart.LEAVES, "minecraft:mangrove_leaves")
            .existing(MaterialPart.SAPLING, "minecraft:mangrove_propagule")
            .existing(MaterialPart.SIGN, "minecraft:mangrove_sign")
            .existing(MaterialPart.WALL_SIGN, "minecraft:mangrove_wall_sign")
            .existing(MaterialPart.HANGING_SIGN, "minecraft:mangrove_hanging_sign")
            .existing(MaterialPart.WALL_HANGING_SIGN, "minecraft:mangrove_wall_hanging_sign")
            .existing(MaterialPart.WINDOW, "create:mangrove_window")
            .existing(MaterialPart.WINDOW_PANE, "create:mangrove_window_pane")
            .existing(MaterialPart.BOAT, "minecraft:mangrove_boat")
            .existing(MaterialPart.CHEST_BOAT, "minecraft:mangrove_chest_boat");

    public static final WoodMaterial OAK = wood(
            "oak",
            "Oak",
            0xA2834F,
            WoodModel.OAK,
            MachineTier.ULV
    )
            .contains(
                    component(LIGNARA, 14),
                    component(SYLVARA, 3),
                    component(RESYRA, 2),
                    component(DULCARA, 1)
            )
            .existing(MaterialPart.LOG, "minecraft:oak_log")
            .existing(MaterialPart.STRIPPED_LOG, "minecraft:stripped_oak_log")
            .existing(MaterialPart.WOOD, "minecraft:oak_wood")
            .existing(MaterialPart.STRIPPED_WOOD, "minecraft:stripped_oak_wood")
            .existing(MaterialPart.PLANKS, "minecraft:oak_planks")
            .existing(MaterialPart.SLAB, "minecraft:oak_slab")
            .existing(MaterialPart.STAIRS, "minecraft:oak_stairs")
            .existing(MaterialPart.FENCE, "minecraft:oak_fence")
            .existing(MaterialPart.FENCE_GATE, "minecraft:oak_fence_gate")
            .existing(MaterialPart.BUTTON, "minecraft:oak_button")
            .existing(MaterialPart.PRESSURE_PLATE, "minecraft:oak_pressure_plate")
            .existing(MaterialPart.DOOR, "minecraft:oak_door")
            .existing(MaterialPart.TRAPDOOR, "minecraft:oak_trapdoor")
            .existing(MaterialPart.LEAVES, "minecraft:oak_leaves")
            .existing(MaterialPart.SAPLING, "minecraft:oak_sapling")
            .existing(MaterialPart.SIGN, "minecraft:oak_sign")
            .existing(MaterialPart.WALL_SIGN, "minecraft:oak_wall_sign")
            .existing(MaterialPart.HANGING_SIGN, "minecraft:oak_hanging_sign")
            .existing(MaterialPart.WALL_HANGING_SIGN, "minecraft:oak_wall_hanging_sign")
            .existing(MaterialPart.WINDOW, "create:oak_window")
            .existing(MaterialPart.WINDOW_PANE, "create:oak_window_pane")
            .existing(MaterialPart.STICK, "minecraft:stick")
            .existing(MaterialPart.CHEST, "minecraft:chest")
            .existing(MaterialPart.BOOKSHELF, "minecraft:bookshelf")
            .existing(MaterialPart.CHISELED_BOOKSHELF, "minecraft:chiseled_bookshelf")
            .existing(MaterialPart.LADDER, "minecraft:ladder")
            .existing(MaterialPart.BOWL, "minecraft:bowl")
            .existing(MaterialPart.BOAT, "minecraft:oak_boat")
            .existing(MaterialPart.CHEST_BOAT, "minecraft:oak_chest_boat");

    public static final WoodMaterial SPRUCE = wood(
            "spruce",
            "Spruce",
            0x735531,
            WoodModel.SPRUCE,
            MachineTier.ULV
    )
            .contains(
                    component(LIGNARA, 14),
                    component(SYLVARA, 3),
                    component(RESYRA, 2),
                    component(DULCARA, 1)
            )
            .existing(MaterialPart.LOG, "minecraft:spruce_log")
            .existing(MaterialPart.STRIPPED_LOG, "minecraft:stripped_spruce_log")
            .existing(MaterialPart.WOOD, "minecraft:spruce_wood")
            .existing(MaterialPart.STRIPPED_WOOD, "minecraft:stripped_spruce_wood")
            .existing(MaterialPart.PLANKS, "minecraft:spruce_planks")
            .existing(MaterialPart.SLAB, "minecraft:spruce_slab")
            .existing(MaterialPart.STAIRS, "minecraft:spruce_stairs")
            .existing(MaterialPart.FENCE, "minecraft:spruce_fence")
            .existing(MaterialPart.FENCE_GATE, "minecraft:spruce_fence_gate")
            .existing(MaterialPart.BUTTON, "minecraft:spruce_button")
            .existing(MaterialPart.PRESSURE_PLATE, "minecraft:spruce_pressure_plate")
            .existing(MaterialPart.DOOR, "minecraft:spruce_door")
            .existing(MaterialPart.TRAPDOOR, "minecraft:spruce_trapdoor")
            .existing(MaterialPart.LEAVES, "minecraft:spruce_leaves")
            .existing(MaterialPart.SAPLING, "minecraft:spruce_sapling")
            .existing(MaterialPart.SIGN, "minecraft:spruce_sign")
            .existing(MaterialPart.WALL_SIGN, "minecraft:spruce_wall_sign")
            .existing(MaterialPart.HANGING_SIGN, "minecraft:spruce_hanging_sign")
            .existing(MaterialPart.WALL_HANGING_SIGN, "minecraft:spruce_wall_hanging_sign")
            .existing(MaterialPart.WINDOW, "create:spruce_window")
            .existing(MaterialPart.WINDOW_PANE, "create:spruce_window_pane")
            .existing(MaterialPart.BARREL, "minecraft:barrel")
            .existing(MaterialPart.BOAT, "minecraft:spruce_boat")
            .existing(MaterialPart.CHEST_BOAT, "minecraft:spruce_chest_boat");

    public static final WoodMaterial WARPED = wood(
            "warped",
            "Warped",
            0x2B6963,
            WoodModel.WARPED,
            MachineTier.ULV
    )
            .contains(
                    component(LIGNARA, 14),
                    component(SYLVARA, 3),
                    component(RESYRA, 2),
                    component(DULCARA, 1)
            )
            .existing(MaterialPart.LOG, "minecraft:warped_stem")
            .existing(MaterialPart.STRIPPED_LOG, "minecraft:stripped_warped_stem")
            .existing(MaterialPart.WOOD, "minecraft:warped_hyphae")
            .existing(MaterialPart.STRIPPED_WOOD, "minecraft:stripped_warped_hyphae")
            .existing(MaterialPart.PLANKS, "minecraft:warped_planks")
            .existing(MaterialPart.SLAB, "minecraft:warped_slab")
            .existing(MaterialPart.STAIRS, "minecraft:warped_stairs")
            .existing(MaterialPart.FENCE, "minecraft:warped_fence")
            .existing(MaterialPart.FENCE_GATE, "minecraft:warped_fence_gate")
            .existing(MaterialPart.BUTTON, "minecraft:warped_button")
            .existing(MaterialPart.PRESSURE_PLATE, "minecraft:warped_pressure_plate")
            .existing(MaterialPart.DOOR, "minecraft:warped_door")
            .existing(MaterialPart.TRAPDOOR, "minecraft:warped_trapdoor")
            .existing(MaterialPart.SIGN, "minecraft:warped_sign")
            .existing(MaterialPart.WALL_SIGN, "minecraft:warped_wall_sign")
            .existing(MaterialPart.HANGING_SIGN, "minecraft:warped_hanging_sign")
            .existing(MaterialPart.WALL_HANGING_SIGN, "minecraft:warped_wall_hanging_sign")
            .existing(MaterialPart.WINDOW, "create:warped_window")
            .existing(MaterialPart.WINDOW_PANE, "create:warped_window_pane");

    public static final List<WoodMaterial> ALL = List.of(
            ACACIA,
            BAMBOO,
            BIRCH,
            CHERRY,
            CRIMSON,
            DARK_OAK,
            JUNGLE,
            MANGROVE,
            OAK,
            SPRUCE,
            WARPED
    );

    private WoodMaterials() {
    }

    public static WoodMaterial wood(String id, String displayName, int color, WoodModel model, MachineTier tier) {
        // Composition belongs on each definition so individual woods can add or change components
        // without silently changing every other wood species.
        return new WoodMaterial(id, displayName, color, model, tier);
    }
}
