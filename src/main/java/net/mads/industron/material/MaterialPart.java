package net.mads.industron.material;

import net.mads.industron.material.structure.StructureMaterial;

public enum MaterialPart {
    ORE("ore", "Ore", Kind.BLOCK, "ore", "normal", "ore"),
    SMALL_ORE("small_ore", "Small Ore", Kind.BLOCK, "ore", "small", "ore_small"),
    DEEPSLATE_ORE("deepslate_ore", "Deepslate Ore", Kind.BLOCK, "ore", "normal", "ore"),
    SMALL_DEEPSLATE_ORE("small_deepslate_ore", "Small Deepslate Ore", Kind.BLOCK, "ore", "small", "ore_small"),
    NETHERRACK_ORE("netherrack_ore", "Netherrack Ore", Kind.BLOCK, "ore", "normal", "ore"),
    SMALL_NETHERRACK_ORE("small_netherrack_ore", "Small Netherrack Ore", Kind.BLOCK, "ore", "small", "ore_small"),
    BLACKSTONE_ORE("blackstone_ore", "Blackstone Ore", Kind.BLOCK, "ore", "normal", "ore"),
    SMALL_BLACKSTONE_ORE("small_blackstone_ore", "Small Blackstone Ore", Kind.BLOCK, "ore", "small", "ore_small"),
    BASALT_ORE("basalt_ore", "Basalt Ore", Kind.BLOCK, "ore", "normal", "ore"),
    SMALL_BASALT_ORE("small_basalt_ore", "Small Basalt Ore", Kind.BLOCK, "ore", "small", "ore_small"),
    END_STONE_ORE("end_stone_ore", "End Stone Ore", Kind.BLOCK, "ore", "normal", "ore"),
    SMALL_END_STONE_ORE("small_end_stone_ore", "Small End Stone Ore", Kind.BLOCK, "ore", "small", "ore_small"),
    // Shared structure-material roles. These are logical registry forms, not texture names.
    STONE("stone", "Stone", Kind.BLOCK, null, null, null),
    COBBLED_STONE("cobbled_stone", "Cobbled Stone", Kind.BLOCK, null, null, null),
    POLISHED_STONE("polished_stone", "Polished Stone", Kind.BLOCK, null, null, null),
    STONE_BRICKS("stone_bricks", "Stone Bricks", Kind.BLOCK, null, null, null),
    STONE_TILES("stone_tiles", "Stone Tiles", Kind.BLOCK, null, null, null),
    CRACKED_STONE_BRICKS("cracked_stone_bricks", "Cracked Stone Bricks", Kind.BLOCK, null, null, null),
    CRACKED_STONE_TILES("cracked_stone_tiles", "Cracked Stone Tiles", Kind.BLOCK, null, null, null),
    CHISELED_STONE("chiseled_stone", "Chiseled Stone", Kind.BLOCK, null, null, null),
    CHISELED_STONE_BRICKS("chiseled_stone_bricks", "Chiseled Stone Bricks", Kind.BLOCK, null, null, null),
    GILDED_STONE("gilded_stone", "Gilded Stone", Kind.BLOCK, null, null, null),
    SMOOTH_STONE("smooth_stone", "Smooth Stone", Kind.BLOCK, null, null, null),
    CUT_STONE("cut_stone", "Cut Stone", Kind.BLOCK, null, null, null),
    POLISHED_CUT_STONE("polished_cut_stone", "Polished Cut Stone", Kind.BLOCK, null, null, null),
    CUT_STONE_BRICKS("cut_stone_bricks", "Cut Stone Bricks", Kind.BLOCK, null, null, null),
    SMALL_STONE_BRICKS("small_stone_bricks", "Small Stone Bricks", Kind.BLOCK, null, null, null),
    LAYERED_STONE("layered_stone", "Layered Stone", Kind.BLOCK, null, null, null),

    LOG("log", "Log", Kind.BLOCK, null, null, null),
    STRIPPED_LOG("stripped_log", "Stripped Log", Kind.BLOCK, null, null, null),
    WOOD("wood", "Wood", Kind.BLOCK, null, null, null),
    STRIPPED_WOOD("stripped_wood", "Stripped Wood", Kind.BLOCK, null, null, null),
    PLANKS("planks", "Planks", Kind.BLOCK, null, null, null),

    SLAB("slab", "Slab", Kind.BLOCK, null, null, null),
    STAIRS("stairs", "Stairs", Kind.BLOCK, null, null, null),
    WALL("wall", "Wall", Kind.BLOCK, null, null, null),
    COBBLED_SLAB("cobbled_slab", "Cobbled Slab", Kind.BLOCK, null, null, null),
    COBBLED_STAIRS("cobbled_stairs", "Cobbled Stairs", Kind.BLOCK, null, null, null),
    COBBLED_WALL("cobbled_wall", "Cobbled Wall", Kind.BLOCK, null, null, null),
    POLISHED_SLAB("polished_slab", "Polished Slab", Kind.BLOCK, null, null, null),
    POLISHED_STAIRS("polished_stairs", "Polished Stairs", Kind.BLOCK, null, null, null),
    POLISHED_WALL("polished_wall", "Polished Wall", Kind.BLOCK, null, null, null),
    POLISHED_STONE_BRICKS("polished_stone_bricks", "Polished Stone Bricks", Kind.BLOCK, null, null, null),
    POLISHED_STONE_BRICK_SLAB("polished_stone_brick_slab", "Polished Stone Brick Slab", Kind.BLOCK, null, null, null),
    POLISHED_STONE_BRICK_STAIRS("polished_stone_brick_stairs", "Polished Stone Brick Stairs", Kind.BLOCK, null, null, null),
    POLISHED_STONE_BRICK_WALL("polished_stone_brick_wall", "Polished Stone Brick Wall", Kind.BLOCK, null, null, null),
    CRACKED_POLISHED_STONE_BRICKS("cracked_polished_stone_bricks", "Cracked Polished Stone Bricks", Kind.BLOCK, null, null, null),
    CHISELED_POLISHED_STONE("chiseled_polished_stone", "Chiseled Polished Stone", Kind.BLOCK, null, null, null),
    STONE_BRICK_SLAB("stone_brick_slab", "Stone Brick Slab", Kind.BLOCK, null, null, null),
    STONE_BRICK_STAIRS("stone_brick_stairs", "Stone Brick Stairs", Kind.BLOCK, null, null, null),
    STONE_BRICK_WALL("stone_brick_wall", "Stone Brick Wall", Kind.BLOCK, null, null, null),
    STONE_TILE_SLAB("stone_tile_slab", "Stone Tile Slab", Kind.BLOCK, null, null, null),
    STONE_TILE_STAIRS("stone_tile_stairs", "Stone Tile Stairs", Kind.BLOCK, null, null, null),
    STONE_TILE_WALL("stone_tile_wall", "Stone Tile Wall", Kind.BLOCK, null, null, null),
    SMOOTH_STONE_SLAB("smooth_stone_slab", "Smooth Stone Slab", Kind.BLOCK, null, null, null),
    SMOOTH_STONE_STAIRS("smooth_stone_stairs", "Smooth Stone Stairs", Kind.BLOCK, null, null, null),
    CUT_STONE_SLAB("cut_stone_slab", "Cut Stone Slab", Kind.BLOCK, null, null, null),
    CUT_STONE_STAIRS("cut_stone_stairs", "Cut Stone Stairs", Kind.BLOCK, null, null, null),
    CUT_STONE_WALL("cut_stone_wall", "Cut Stone Wall", Kind.BLOCK, null, null, null),
    POLISHED_CUT_STONE_SLAB("polished_cut_stone_slab", "Polished Cut Stone Slab", Kind.BLOCK, null, null, null),
    POLISHED_CUT_STONE_STAIRS("polished_cut_stone_stairs", "Polished Cut Stone Stairs", Kind.BLOCK, null, null, null),
    POLISHED_CUT_STONE_WALL("polished_cut_stone_wall", "Polished Cut Stone Wall", Kind.BLOCK, null, null, null),
    CUT_STONE_BRICK_SLAB("cut_stone_brick_slab", "Cut Stone Brick Slab", Kind.BLOCK, null, null, null),
    CUT_STONE_BRICK_STAIRS("cut_stone_brick_stairs", "Cut Stone Brick Stairs", Kind.BLOCK, null, null, null),
    CUT_STONE_BRICK_WALL("cut_stone_brick_wall", "Cut Stone Brick Wall", Kind.BLOCK, null, null, null),
    SMALL_STONE_BRICK_SLAB("small_stone_brick_slab", "Small Stone Brick Slab", Kind.BLOCK, null, null, null),
    SMALL_STONE_BRICK_STAIRS("small_stone_brick_stairs", "Small Stone Brick Stairs", Kind.BLOCK, null, null, null),
    SMALL_STONE_BRICK_WALL("small_stone_brick_wall", "Small Stone Brick Wall", Kind.BLOCK, null, null, null),

    FENCE("fence", "Fence", Kind.BLOCK, null, null, null),
    FENCE_GATE("fence_gate", "Fence Gate", Kind.BLOCK, null, null, null),
    BUTTON("button", "Button", Kind.BLOCK, null, null, null),
    PRESSURE_PLATE("pressure_plate", "Pressure Plate", Kind.BLOCK, null, null, null),
    DOOR("door", "Door", Kind.BLOCK, null, null, null),
    TRAPDOOR("trapdoor", "Trapdoor", Kind.BLOCK, null, null, null),
    LEAVES("leaves", "Leaves", Kind.BLOCK, null, null, null),
    SAPLING("sapling", "Sapling", Kind.BLOCK, null, null, null),
    SIGN("sign", "Sign", Kind.BLOCK, null, null, null),
    WALL_SIGN("wall_sign", "Wall Sign", Kind.BLOCK, null, null, null),
    HANGING_SIGN("hanging_sign", "Hanging Sign", Kind.BLOCK, null, null, null),
    WALL_HANGING_SIGN("wall_hanging_sign", "Wall Hanging Sign", Kind.BLOCK, null, null, null),
    WINDOW("window", "Window", Kind.BLOCK, null, null, null),
    WINDOW_PANE("window_pane", "Window Pane", Kind.BLOCK, null, null, null),
    MOSAIC("mosaic", "Mosaic", Kind.BLOCK, null, null, null),
    MOSAIC_SLAB("mosaic_slab", "Mosaic Slab", Kind.BLOCK, null, null, null),
    MOSAIC_STAIRS("mosaic_stairs", "Mosaic Stairs", Kind.BLOCK, null, null, null),

    PILLAR("pillar", "Pillar", Kind.BLOCK, null, null, null),
    CHISELED_BLOCK("chiseled_block", "Chiseled Block", Kind.BLOCK, null, null, null),
    BRICKS("bricks", "Bricks", Kind.BLOCK, null, null, null),
    SMOOTH_BLOCK("smooth_block", "Smooth Block", Kind.BLOCK, null, null, null),
    SMOOTH_SLAB("smooth_slab", "Smooth Slab", Kind.BLOCK, null, null, null),
    SMOOTH_STAIRS("smooth_stairs", "Smooth Stairs", Kind.BLOCK, null, null, null),

    TINY_WOOD_PULP("tiny_wood_pulp", "Tiny Wood Pulp", Kind.ITEM, "dust", "tiny", null),
    SMALL_WOOD_PULP("small_wood_pulp", "Small Wood Pulp", Kind.ITEM, "dust", "small", null),
    WOOD_PULP("wood_pulp", "Wood Pulp", Kind.ITEM, "dust", "normal", null),

    RAW_ORE("raw_ore", "Raw Ore", Kind.ITEM, "raw_ore", "normal", "raw_ore"),
    RAW_BLOCK("raw_block", "Raw Block", Kind.BLOCK, "raw_ore_block", "normal", "raw_ore_block"),
    CRUSHED_ORE("crushed_ore", "Crushed Ore", Kind.ITEM, "crushed_ore", "normal", "crushed"),
    WASHED_CRUSHED_ORE("washed_crushed_ore", "Washed Crushed Ore", Kind.ITEM, "crushed_ore", "washed", "crushed_purified"),
    REFINED_ORE("refined_ore", "Refined Ore", Kind.ITEM, "refined_ore", "normal", "crushed_refined"),
    INGOT("ingot", "Ingot", Kind.ITEM, "ingot", "normal", "ingot"),
    DOUBLE_INGOT("double_ingot", "Double Ingot", Kind.ITEM, "ingot", "double", "ingot_double"),
    HOT_INGOT("hot_ingot", "Hot Ingot", Kind.ITEM, "ingot", "hot", "ingot_hot"),
    NUGGET("nugget", "Nugget", Kind.ITEM, "nugget", "normal", "nugget"),
    HOT_NUGGET("hot_nugget", "Hot Nugget", Kind.ITEM, "nugget", "hot", "nugget"),
    BLOCK("block", "Block", Kind.BLOCK, "block", "normal", "block"),

    TINY_DUST("tiny_dust", "Tiny Dust", Kind.ITEM, "dust", "tiny", "dust_tiny"),
    SMALL_DUST("small_dust", "Small Dust", Kind.ITEM, "dust", "small", "dust_small"),
    DUST("dust", "Dust", Kind.ITEM, "dust", "normal", "dust"),
    IMPURE_DUST("impure_dust", "Impure Dust", Kind.ITEM, "dust", "impure", "impure_dust"),
    PURIFIED_DUST("purified_dust", "Purified Dust", Kind.ITEM, "dust", "purified", "purified_dust"),

    TINY_GEM("tiny_gem", "Tiny Gem", Kind.ITEM, "gem", "tiny", null),
    SMALL_GEM("small_gem", "Small Gem", Kind.ITEM, "gem", "small", null),
    GEM("gem", "Gem", Kind.ITEM, "gem", "normal", "gem"),
    FLAWLESS_GEM("flawless_gem", "Flawless Gem", Kind.ITEM, "gem", "large", "gem_flawless"),
    EXQUISITE_GEM("exquisite_gem", "Exquisite Gem", Kind.ITEM, "gem", "huge", "gem_exquisite"),
    ROUGH_TINY_GEM("rough_tiny_gem", "Rough Tiny Gem", Kind.ITEM, "gem_rough", "tiny", null),
    ROUGH_SMALL_GEM("rough_small_gem", "Rough Small Gem", Kind.ITEM, "gem_rough", "small", null),
    ROUGH_GEM("rough_gem", "Rough Gem", Kind.ITEM, "gem_rough", "normal", "gem_rough"),
    ROUGH_FLAWLESS_GEM("rough_flawless_gem", "Rough Flawless Gem", Kind.ITEM, "gem_rough", "large", null),
    ROUGH_EXQUISITE_GEM("rough_exquisite_gem", "Rough Exquisite Gem", Kind.ITEM, "gem_rough", "huge", null),

    TINY_BALL("tiny_ball", "Tiny Ball", Kind.ITEM, "ball", "tiny", null),
    SMALL_BALL("small_ball", "Small Ball", Kind.ITEM, "ball", "small", null),
    BALL("ball", "Ball", Kind.ITEM, "ball", "normal", "bearing_ball"),
    LARGE_BALL("large_ball", "Large Ball", Kind.ITEM, "ball", "large", null),
    HUGE_BALL("huge_ball", "Huge Ball", Kind.ITEM, "ball", "huge", null),

    TINY_BEARING("tiny_bearing", "Tiny Bearing", Kind.ITEM, "bearing", "tiny", null),
    SMALL_BEARING("small_bearing", "Small Bearing", Kind.ITEM, "bearing", "small", null),
    BEARING("bearing", "Bearing", Kind.ITEM, "bearing", "normal", "bearing"),
    LARGE_BEARING("large_bearing", "Large Bearing", Kind.ITEM, "bearing", "large", null),
    HUGE_BEARING("huge_bearing", "Huge Bearing", Kind.ITEM, "bearing", "huge", null),

    TINY_BOLT("tiny_bolt", "Tiny Bolt", Kind.ITEM, "bolt", "tiny", null),
    SMALL_BOLT("small_bolt", "Small Bolt", Kind.ITEM, "bolt", "small", null),
    BOLT("bolt", "Bolt", Kind.ITEM, "bolt", "normal", "bolt"),
    LARGE_BOLT("large_bolt", "Large Bolt", Kind.ITEM, "bolt", "large", null),
    HUGE_BOLT("huge_bolt", "Huge Bolt", Kind.ITEM, "bolt", "huge", null),

    TINY_SCREW("tiny_screw", "Tiny Screw", Kind.ITEM, "screw", "tiny", null),
    SMALL_SCREW("small_screw", "Small Screw", Kind.ITEM, "screw", "small", null),
    SCREW("screw", "Screw", Kind.ITEM, "screw", "normal", "screw"),
    LARGE_SCREW("large_screw", "Large Screw", Kind.ITEM, "screw", "large", null),
    HUGE_SCREW("huge_screw", "Huge Screw", Kind.ITEM, "screw", "huge", null),

    TINY_RIVET("tiny_rivet", "Tiny Rivet", Kind.ITEM, "rivet", "tiny", null),
    SMALL_RIVET("small_rivet", "Small Rivet", Kind.ITEM, "rivet", "small", null),
    RIVET("rivet", "Rivet", Kind.ITEM, "rivet", "normal", "rivet"),
    LARGE_RIVET("large_rivet", "Large Rivet", Kind.ITEM, "rivet", "large", null),
    HUGE_RIVET("huge_rivet", "Huge Rivet", Kind.ITEM, "rivet", "huge", null),

    TINY_GEAR("tiny_gear", "Tiny Gear", Kind.ITEM, "gear", "tiny", null),
    SMALL_GEAR("small_gear", "Small Gear", Kind.ITEM, "gear", "small", "gear_small"),
    GEAR("gear", "Gear", Kind.ITEM, "gear", "normal", "gear"),
    LARGE_GEAR("large_gear", "Large Gear", Kind.ITEM, "gear", "large", "gear_large"),
    HUGE_GEAR("huge_gear", "Huge Gear", Kind.ITEM, "gear", "huge", null),

    TINY_RING("tiny_ring", "Tiny Ring", Kind.ITEM, "ring", "tiny", null),
    SMALL_RING("small_ring", "Small Ring", Kind.ITEM, "ring", "small", "ring_small"),
    RING("ring", "Ring", Kind.ITEM, "ring", "normal", "ring"),
    LARGE_RING("large_ring", "Large Ring", Kind.ITEM, "ring", "large", "ring_large"),
    HUGE_RING("huge_ring", "Huge Ring", Kind.ITEM, "ring", "huge", null),

    VERY_SHORT_ROD("very_short_rod", "Very Short Rod", Kind.ITEM, "rod", "very_short", null),
    SHORT_ROD("short_rod", "Short Rod", Kind.ITEM, "rod", "short", null),
    ROD("rod", "Rod", Kind.ITEM, "rod", "normal", "rod"),
    LONG_ROD("long_rod", "Long Rod", Kind.ITEM, "rod", "long", "rod_long"),
    VERY_LONG_ROD("very_long_rod", "Very Long Rod", Kind.ITEM, "rod", "very_long", null),

    TINY_SPRING("tiny_spring", "Tiny Spring", Kind.ITEM, "spring", "tiny", null),
    SMALL_SPRING("small_spring", "Small Spring", Kind.ITEM, "spring", "small", "small_spring"),
    SPRING("spring", "Spring", Kind.ITEM, "spring", "normal", "spring"),
    LARGE_SPRING("large_spring", "Large Spring", Kind.ITEM, "spring", "large", "large_spring"),
    HUGE_SPRING("huge_spring", "Huge Spring", Kind.ITEM, "spring", "huge", null),

    TINY_ROTOR("tiny_rotor", "Tiny Rotor", Kind.ITEM, "rotor", "tiny", null),
    SMALL_ROTOR("small_rotor", "Small Rotor", Kind.ITEM, "rotor", "small", null),
    ROTOR("rotor", "Rotor", Kind.ITEM, "rotor", "normal", "rotor"),
    LARGE_ROTOR("large_rotor", "Large Rotor", Kind.ITEM, "rotor", "large", null),
    HUGE_ROTOR("huge_rotor", "Huge Rotor", Kind.ITEM, "rotor", "huge", null),

    PLATE("plate", "Plate", Kind.ITEM, "plate", "normal", "plate"),
    LARGE_PLATE("large_plate", "Large Plate", Kind.ITEM, "plate", "large", null),
    DOUBLE_PLATE("double_plate", "Double Plate", Kind.ITEM, "plate_double", "normal", "plate_double"),
    LARGE_DOUBLE_PLATE("large_double_plate", "Large Double Plate", Kind.ITEM, "plate_double", "large", null),
    DENSE_PLATE("dense_plate", "Dense Plate", Kind.ITEM, "plate_dense", "normal", "plate_dense"),
    LARGE_DENSE_PLATE("large_dense_plate", "Large Dense Plate", Kind.ITEM, "plate_dense", "large", null),
    REINFORCED_PLATE("reinforced_plate", "Reinforced Plate", Kind.ITEM, "plate_reinforced", "normal", "plate_reinforced"),
    HEAT_EXCHANGER_PLATE("heat_exchanger_plate", "Heat Exchanger Plate", Kind.ITEM, "plate_heat_exchanger", "normal", "plate_heat_exchanger"),
    FOIL("foil", "Foil", Kind.ITEM, "foil", "normal", "foil"),
    FINE_WIRE("fine_wire", "Fine Wire", Kind.ITEM, "fine_wire", "normal", "wire_fine"),
    WIRE_1X("wire_1x", "1x Wire", Kind.ITEM, "wire", "1x", null),
    WIRE_2X("wire_2x", "2x Wire", Kind.ITEM, "wire", "2x", null),
    WIRE_4X("wire_4x", "4x Wire", Kind.ITEM, "wire", "4x", null),
    WIRE_8X("wire_8x", "8x Wire", Kind.ITEM, "wire", "8x", null),
    WIRE_16X("wire_16x", "16x Wire", Kind.ITEM, "wire", "16x", null),
    WIRE("wire", "Wire", Kind.ITEM, "wire", "normal", "wire"),
    COIL("coil", "Coil", Kind.ITEM, "coil", "normal", "coil"),
    LENS("lens", "Lens", Kind.ITEM, "lens", "normal", "lens"),
    TURBINE_BLADE("turbine_blade", "Turbine Blade", Kind.ITEM, "turbine_blade", "normal", "turbine_blade"),
    FRAME("frame", "Frame", Kind.BLOCK, "frame", "normal", "frame_gt"),

    LIQUID("liquid", "Liquid", Kind.FLUID, "liquid", "normal", "liquid"),
    GAS("gas", "Gas", Kind.FLUID, "gas", "normal", "gas"),
    MOLTEN_FLUID("molten_fluid", "Molten Fluid", Kind.FLUID, "molten", "normal", "molten"),

    // Legacy production/mold parts retained for existing recipes. They are not auto-generated for elements.
    CAST_INGOT("cast_ingot", "Cast Ingot", Kind.ITEM, "ingot", "hot", "ingot_hot"),
    CAST_NUGGET("cast_nugget", "Cast Nugget", Kind.ITEM, "nugget", "normal", "nugget"),
    CAST_BLOCK("cast_block", "Cast Block", Kind.ITEM, "block", "normal", "block"),
    CAST_PLATE("cast_plate", "Cast Plate", Kind.ITEM, "plate", "normal", "plate"),
    CAST_ROD("cast_rod", "Cast Rod", Kind.ITEM, "rod", "normal", "rod"),
    CAST_LONG_ROD("cast_long_rod", "Cast Long Rod", Kind.ITEM, "rod", "long", "rod_long"),
    CAST_BOLT("cast_bolt", "Cast Bolt", Kind.ITEM, "bolt", "normal", "bolt"),
    CAST_SCREW("cast_screw", "Cast Screw", Kind.ITEM, "screw", "normal", "screw"),
    CAST_RING("cast_ring", "Cast Ring", Kind.ITEM, "ring", "normal", "ring"),
    CAST_SMALL_RING("cast_small_ring", "Cast Small Ring", Kind.ITEM, "ring", "small", "ring_small"),
    CAST_LARGE_RING("cast_large_ring", "Cast Large Ring", Kind.ITEM, "ring", "large", "ring_large"),
    CAST_GEAR("cast_gear", "Cast Gear", Kind.ITEM, "gear", "normal", "gear"),
    CAST_SMALL_GEAR("cast_small_gear", "Cast Small Gear", Kind.ITEM, "gear", "small", "gear_small"),
    CAST_BEARING_BALL("cast_bearing_ball", "Cast Bearing Ball", Kind.ITEM, "ball", "normal", "bearing_ball"),
    CAST_BEARING("cast_bearing", "Cast Bearing", Kind.ITEM, "bearing", "normal", "bearing"),
    CAST_ROTOR("cast_rotor", "Cast Rotor", Kind.ITEM, "rotor", "normal", "rotor"),
    TOOL_HEAD_BUZZ_SAW("tool_head_buzz_saw", "Buzz Saw Tool Head", Kind.ITEM, "tool_head_buzz_saw", "normal", "tool_head_buzz_saw"),
    CASING("casing", "Casing", Kind.BLOCK, null, null, null),
    MACHINE_HULL("machine_hull", "Machine Hull", Kind.BLOCK, null, null, null),

    CAST_NUGGET_MOLD("cast_nugget_mold", "Cast Nugget Mold", Kind.ITEM, "cast_nugget_mold", "normal", "cast_nugget_mold"),
    CAST_BEARING_BALL_MOLD("cast_bearing_ball_mold", "Cast Bearing Ball Mold", Kind.ITEM, "cast_bearing_ball_mold", "normal", "cast_bearing_ball_mold"),
    CAST_ROTOR_MOLD("cast_rotor_mold", "Cast Rotor Mold", Kind.ITEM, "cast_rotor_mold", "normal", "cast_rotor_mold"),
    CAST_INGOT_MOLD("cast_ingot_mold", "Cast Ingot Mold", Kind.ITEM, "cast_ingot_mold", "normal", "cast_ingot_mold"),
    CAST_PLATE_MOLD("cast_plate_mold", "Cast Plate Mold", Kind.ITEM, "cast_plate_mold", "normal", "cast_plate_mold"),
    CAST_ROD_MOLD("cast_rod_mold", "Cast Rod Mold", Kind.ITEM, "cast_rod_mold", "normal", "cast_rod_mold"),
    CAST_LONG_ROD_MOLD("cast_long_rod_mold", "Cast Long Rod Mold", Kind.ITEM, "cast_long_rod_mold", "normal", "cast_long_rod_mold"),
    CAST_BOLT_MOLD("cast_bolt_mold", "Cast Bolt Mold", Kind.ITEM, "cast_bolt_mold", "normal", "cast_bolt_mold"),
    CAST_RING_MOLD("cast_ring_mold", "Cast Ring Mold", Kind.ITEM, "cast_ring_mold", "normal", "cast_ring_mold"),
    CAST_SMALL_RING_MOLD("cast_small_ring_mold", "Cast Small Ring Mold", Kind.ITEM, "cast_small_ring_mold", "normal", "cast_small_ring_mold"),
    CAST_LARGE_RING_MOLD("cast_large_ring_mold", "Cast Large Ring Mold", Kind.ITEM, "cast_large_ring_mold", "normal", "cast_large_ring_mold"),
    CAST_GEAR_MOLD("cast_gear_mold", "Cast Gear Mold", Kind.ITEM, "cast_gear_mold", "normal", "cast_gear_mold"),
    CAST_SMALL_GEAR_MOLD("cast_small_gear_mold", "Cast Small Gear Mold", Kind.ITEM, "cast_small_gear_mold", "normal", "cast_small_gear_mold"),
    CAST_BEARING_MOLD("cast_bearing_mold", "Cast Bearing Mold", Kind.ITEM, "cast_bearing_mold", "normal", "cast_bearing_mold"),
    CAST_SCREW_MOLD("cast_screw_mold", "Cast Screw Mold", Kind.ITEM, "cast_screw_mold", "normal", "cast_screw_mold"),
    HOT_CAST_NUGGET_MOLD("hot_cast_nugget_mold", "Hot Cast Nugget Mold", Kind.ITEM, "cast_nugget_mold", "normal", "cast_nugget_mold"),
    HOT_CAST_BEARING_BALL_MOLD("hot_cast_bearing_ball_mold", "Hot Cast Bearing Ball Mold", Kind.ITEM, "cast_bearing_ball_mold", "normal", "cast_bearing_ball_mold"),
    HOT_CAST_ROTOR_MOLD("hot_cast_rotor_mold", "Hot Cast Rotor Mold", Kind.ITEM, "cast_rotor_mold", "normal", "cast_rotor_mold"),
    HOT_CAST_INGOT_MOLD("hot_cast_ingot_mold", "Hot Cast Ingot Mold", Kind.ITEM, "cast_ingot_mold", "normal", "cast_ingot_mold"),
    HOT_CAST_PLATE_MOLD("hot_cast_plate_mold", "Hot Cast Plate Mold", Kind.ITEM, "cast_plate_mold", "normal", "cast_plate_mold"),
    HOT_CAST_ROD_MOLD("hot_cast_rod_mold", "Hot Cast Rod Mold", Kind.ITEM, "cast_rod_mold", "normal", "cast_rod_mold"),
    HOT_CAST_LONG_ROD_MOLD("hot_cast_long_rod_mold", "Hot Cast Long Rod Mold", Kind.ITEM, "cast_long_rod_mold", "normal", "cast_long_rod_mold"),
    HOT_CAST_BOLT_MOLD("hot_cast_bolt_mold", "Hot Cast Bolt Mold", Kind.ITEM, "cast_bolt_mold", "normal", "cast_bolt_mold"),
    HOT_CAST_RING_MOLD("hot_cast_ring_mold", "Hot Cast Ring Mold", Kind.ITEM, "cast_ring_mold", "normal", "cast_ring_mold"),
    HOT_CAST_SMALL_RING_MOLD("hot_cast_small_ring_mold", "Hot Cast Small Ring Mold", Kind.ITEM, "cast_small_ring_mold", "normal", "cast_small_ring_mold"),
    HOT_CAST_LARGE_RING_MOLD("hot_cast_large_ring_mold", "Hot Cast Large Ring Mold", Kind.ITEM, "cast_large_ring_mold", "normal", "cast_large_ring_mold"),
    HOT_CAST_GEAR_MOLD("hot_cast_gear_mold", "Hot Cast Gear Mold", Kind.ITEM, "cast_gear_mold", "normal", "cast_gear_mold"),
    HOT_CAST_SMALL_GEAR_MOLD("hot_cast_small_gear_mold", "Hot Cast Small Gear Mold", Kind.ITEM, "cast_small_gear_mold", "normal", "cast_small_gear_mold"),
    HOT_CAST_BEARING_MOLD("hot_cast_bearing_mold", "Hot Cast Bearing Mold", Kind.ITEM, "cast_bearing_mold", "normal", "cast_bearing_mold"),
    HOT_CAST_SCREW_MOLD("hot_cast_screw_mold", "Hot Cast Screw Mold", Kind.ITEM, "cast_screw_mold", "normal", "cast_screw_mold");

    private final String id;
    private final String displayName;
    private final Kind kind;
    private final String textureFamily;
    private final String textureSize;
    private final String legacyTextureFamily;

    MaterialPart(String id, String displayName, Kind kind, String textureFamily, String textureSize, String legacyTextureFamily) {
        this.id = id;
        this.displayName = displayName;
        this.kind = kind;
        this.textureFamily = textureFamily;
        this.textureSize = textureSize;
        this.legacyTextureFamily = legacyTextureFamily;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public Kind kind() { return kind; }
    public boolean isItem() { return kind == Kind.ITEM; }
    public boolean isBlock() { return kind == Kind.BLOCK; }
    public boolean isFluid() { return kind == Kind.FLUID; }
    public String textureFamily() { return textureFamily; }
    public String textureSize() { return textureSize; }
    public String legacyTextureFamily() { return legacyTextureFamily; }

    public String registryName(IndustrialMaterial material) { return material.id() + "_" + id; }

    public String registryName(StructureMaterial material) {
        if (material == null) {
            throw new IllegalArgumentException("Structure material cannot be null");
        }
        return switch (this) {
            case STONE, BLOCK -> material.id();
            case COBBLED_STONE -> "cobbled_" + material.id();
            case COBBLED_SLAB -> "cobbled_" + material.id() + "_slab";
            case COBBLED_STAIRS -> "cobbled_" + material.id() + "_stairs";
            case COBBLED_WALL -> "cobbled_" + material.id() + "_wall";
            case POLISHED_STONE -> "polished_" + material.id();
            case POLISHED_SLAB -> "polished_" + material.id() + "_slab";
            case POLISHED_STAIRS -> "polished_" + material.id() + "_stairs";
            case POLISHED_WALL -> "polished_" + material.id() + "_wall";
            case POLISHED_STONE_BRICKS -> "polished_" + material.id() + "_bricks";
            case POLISHED_STONE_BRICK_SLAB -> "polished_" + material.id() + "_brick_slab";
            case POLISHED_STONE_BRICK_STAIRS -> "polished_" + material.id() + "_brick_stairs";
            case POLISHED_STONE_BRICK_WALL -> "polished_" + material.id() + "_brick_wall";
            case CRACKED_POLISHED_STONE_BRICKS -> "cracked_polished_" + material.id() + "_bricks";
            case CHISELED_POLISHED_STONE -> "chiseled_polished_" + material.id();
            case STONE_BRICKS -> material.id() + "_bricks";
            case STONE_BRICK_SLAB -> material.id() + "_brick_slab";
            case STONE_BRICK_STAIRS -> material.id() + "_brick_stairs";
            case STONE_BRICK_WALL -> material.id() + "_brick_wall";
            case STONE_TILES -> material.id() + "_tiles";
            case STONE_TILE_SLAB -> material.id() + "_tile_slab";
            case STONE_TILE_STAIRS -> material.id() + "_tile_stairs";
            case STONE_TILE_WALL -> material.id() + "_tile_wall";
            case CRACKED_STONE_BRICKS -> "cracked_" + material.id() + "_bricks";
            case CRACKED_STONE_TILES -> "cracked_" + material.id() + "_tiles";
            case CHISELED_STONE -> "chiseled_" + material.id();
            case CHISELED_STONE_BRICKS -> "chiseled_" + material.id() + "_bricks";
            case GILDED_STONE -> "gilded_" + material.id();
            case SMOOTH_STONE -> "smooth_" + material.id();
            case SMOOTH_STONE_SLAB -> "smooth_" + material.id() + "_slab";
            case SMOOTH_STONE_STAIRS -> "smooth_" + material.id() + "_stairs";
            case CUT_STONE -> "cut_" + material.id();
            case CUT_STONE_SLAB -> "cut_" + material.id() + "_slab";
            case CUT_STONE_STAIRS -> "cut_" + material.id() + "_stairs";
            case CUT_STONE_WALL -> "cut_" + material.id() + "_wall";
            case POLISHED_CUT_STONE -> "polished_cut_" + material.id();
            case POLISHED_CUT_STONE_SLAB -> "polished_cut_" + material.id() + "_slab";
            case POLISHED_CUT_STONE_STAIRS -> "polished_cut_" + material.id() + "_stairs";
            case POLISHED_CUT_STONE_WALL -> "polished_cut_" + material.id() + "_wall";
            case CUT_STONE_BRICKS -> "cut_" + material.id() + "_bricks";
            case CUT_STONE_BRICK_SLAB -> "cut_" + material.id() + "_brick_slab";
            case CUT_STONE_BRICK_STAIRS -> "cut_" + material.id() + "_brick_stairs";
            case CUT_STONE_BRICK_WALL -> "cut_" + material.id() + "_brick_wall";
            case SMALL_STONE_BRICKS -> "small_" + material.id() + "_bricks";
            case SMALL_STONE_BRICK_SLAB -> "small_" + material.id() + "_brick_slab";
            case SMALL_STONE_BRICK_STAIRS -> "small_" + material.id() + "_brick_stairs";
            case SMALL_STONE_BRICK_WALL -> "small_" + material.id() + "_brick_wall";
            case LAYERED_STONE -> "layered_" + material.id();
            case CHISELED_BLOCK -> "chiseled_" + material.id() + "_block";
            case SMOOTH_BLOCK -> "smooth_" + material.id() + "_block";
            case SMOOTH_SLAB -> "smooth_" + material.id() + "_slab";
            case SMOOTH_STAIRS -> "smooth_" + material.id() + "_stairs";
            default -> material.id() + "_" + id;
        };
    }

    public String magneticRegistryName(IndustrialMaterial material) { return "magnetic_" + registryName(material); }

    public String readableName(IndustrialMaterial material) {
        if (isOre()) {
            String stonePrefix = oreStonePrefix();
            String sizePrefix = isSmallOre() ? "Small " : "";
            return sizePrefix + (stonePrefix == null ? "" : stonePrefix + " ") + material.displayName() + " Ore";
        }
        if (this == RAW_ORE) return "Raw " + material.displayName();
        if (this == RAW_BLOCK) return "Block of Raw " + material.displayName();
        if (this == BLOCK) return "Block of " + material.displayName();
        if (this == LIQUID) return material.displayName();
        if (this == GAS) return material.displayName() + " Gas";
        if (this == MOLTEN_FLUID) return "Molten " + material.displayName();
        return material.displayName() + " " + displayName;
    }

    public String readableName(StructureMaterial material) {
        if (material == null) {
            throw new IllegalArgumentException("Structure material cannot be null");
        }
        return switch (this) {
            case STONE, BLOCK -> material.displayName();
            case COBBLED_STONE -> "Cobbled " + material.displayName();
            case COBBLED_SLAB -> "Cobbled " + material.displayName() + " Slab";
            case COBBLED_STAIRS -> "Cobbled " + material.displayName() + " Stairs";
            case COBBLED_WALL -> "Cobbled " + material.displayName() + " Wall";
            case POLISHED_STONE -> "Polished " + material.displayName();
            case POLISHED_SLAB -> "Polished " + material.displayName() + " Slab";
            case POLISHED_STAIRS -> "Polished " + material.displayName() + " Stairs";
            case POLISHED_WALL -> "Polished " + material.displayName() + " Wall";
            case POLISHED_STONE_BRICKS -> "Polished " + material.displayName() + " Bricks";
            case POLISHED_STONE_BRICK_SLAB -> "Polished " + material.displayName() + " Brick Slab";
            case POLISHED_STONE_BRICK_STAIRS -> "Polished " + material.displayName() + " Brick Stairs";
            case POLISHED_STONE_BRICK_WALL -> "Polished " + material.displayName() + " Brick Wall";
            case CRACKED_POLISHED_STONE_BRICKS -> "Cracked Polished " + material.displayName() + " Bricks";
            case CHISELED_POLISHED_STONE -> "Chiseled Polished " + material.displayName();
            case STONE_BRICKS -> material.displayName() + " Bricks";
            case STONE_BRICK_SLAB -> material.displayName() + " Brick Slab";
            case STONE_BRICK_STAIRS -> material.displayName() + " Brick Stairs";
            case STONE_BRICK_WALL -> material.displayName() + " Brick Wall";
            case STONE_TILES -> material.displayName() + " Tiles";
            case STONE_TILE_SLAB -> material.displayName() + " Tile Slab";
            case STONE_TILE_STAIRS -> material.displayName() + " Tile Stairs";
            case STONE_TILE_WALL -> material.displayName() + " Tile Wall";
            case CRACKED_STONE_BRICKS -> "Cracked " + material.displayName() + " Bricks";
            case CRACKED_STONE_TILES -> "Cracked " + material.displayName() + " Tiles";
            case CHISELED_STONE -> "Chiseled " + material.displayName();
            case CHISELED_STONE_BRICKS -> "Chiseled " + material.displayName() + " Bricks";
            case GILDED_STONE -> "Gilded " + material.displayName();
            case SMOOTH_STONE -> "Smooth " + material.displayName();
            case SMOOTH_STONE_SLAB -> "Smooth " + material.displayName() + " Slab";
            case SMOOTH_STONE_STAIRS -> "Smooth " + material.displayName() + " Stairs";
            case CUT_STONE -> "Cut " + material.displayName();
            case CUT_STONE_SLAB -> "Cut " + material.displayName() + " Slab";
            case CUT_STONE_STAIRS -> "Cut " + material.displayName() + " Stairs";
            case CUT_STONE_WALL -> "Cut " + material.displayName() + " Wall";
            case POLISHED_CUT_STONE -> "Polished Cut " + material.displayName();
            case POLISHED_CUT_STONE_SLAB -> "Polished Cut " + material.displayName() + " Slab";
            case POLISHED_CUT_STONE_STAIRS -> "Polished Cut " + material.displayName() + " Stairs";
            case POLISHED_CUT_STONE_WALL -> "Polished Cut " + material.displayName() + " Wall";
            case CUT_STONE_BRICKS -> "Cut " + material.displayName() + " Bricks";
            case CUT_STONE_BRICK_SLAB -> "Cut " + material.displayName() + " Brick Slab";
            case CUT_STONE_BRICK_STAIRS -> "Cut " + material.displayName() + " Brick Stairs";
            case CUT_STONE_BRICK_WALL -> "Cut " + material.displayName() + " Brick Wall";
            case SMALL_STONE_BRICKS -> "Small " + material.displayName() + " Bricks";
            case SMALL_STONE_BRICK_SLAB -> "Small " + material.displayName() + " Brick Slab";
            case SMALL_STONE_BRICK_STAIRS -> "Small " + material.displayName() + " Brick Stairs";
            case SMALL_STONE_BRICK_WALL -> "Small " + material.displayName() + " Brick Wall";
            case LAYERED_STONE -> "Layered " + material.displayName();
            case TINY_DUST -> "Tiny " + material.displayName() + " Dust";
            case SMALL_DUST -> "Small " + material.displayName() + " Dust";
            case DUST -> material.displayName() + " Dust";
            case TINY_WOOD_PULP -> "Tiny " + material.displayName() + " Wood Pulp";
            case SMALL_WOOD_PULP -> "Small " + material.displayName() + " Wood Pulp";
            case WOOD_PULP -> material.displayName() + " Wood Pulp";
            case CHISELED_BLOCK -> "Chiseled " + material.displayName() + " Block";
            case SMOOTH_BLOCK -> "Smooth " + material.displayName() + " Block";
            case SMOOTH_SLAB -> "Smooth " + material.displayName() + " Slab";
            case SMOOTH_STAIRS -> "Smooth " + material.displayName() + " Stairs";
            default -> material.displayName() + " " + displayName;
        };
    }

    public String magneticReadableName(IndustrialMaterial material) {
        return "Magnetic " + readableName(material);
    }

    public boolean isOre() {
        return switch (this) {
            case ORE, SMALL_ORE,
                    DEEPSLATE_ORE, SMALL_DEEPSLATE_ORE,
                    NETHERRACK_ORE, SMALL_NETHERRACK_ORE,
                    BLACKSTONE_ORE, SMALL_BLACKSTONE_ORE,
                    BASALT_ORE, SMALL_BASALT_ORE,
                    END_STONE_ORE, SMALL_END_STONE_ORE -> true;
            default -> false;
        };
    }

    public boolean isSmallOre() {
        return switch (this) {
            case SMALL_ORE, SMALL_DEEPSLATE_ORE, SMALL_NETHERRACK_ORE,
                    SMALL_BLACKSTONE_ORE, SMALL_BASALT_ORE, SMALL_END_STONE_ORE -> true;
            default -> false;
        };
    }

    private String oreStonePrefix() {
        return switch (this) {
            case DEEPSLATE_ORE, SMALL_DEEPSLATE_ORE -> "Deepslate";
            case NETHERRACK_ORE, SMALL_NETHERRACK_ORE -> "Netherrack";
            case BLACKSTONE_ORE, SMALL_BLACKSTONE_ORE -> "Blackstone";
            case BASALT_ORE, SMALL_BASALT_ORE -> "Basalt";
            case END_STONE_ORE, SMALL_END_STONE_ORE -> "End Stone";
            default -> null;
        };
    }

    public enum Kind { ITEM, BLOCK, FLUID }
}
