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
    GRAVEL("gravel", "Gravel", Kind.BLOCK, null, null, null),
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
    BRICKS("bricks", "Bricks", Kind.BLOCK, "bricks", "normal", null),
    FIREBOX("firebox", "Firebox", Kind.BLOCK, null, null, null),
    BRICK_SLAB("brick_slab", "Brick Slab", Kind.BLOCK, "bricks", "normal", null),
    BRICK_STAIRS("brick_stairs", "Brick Stairs", Kind.BLOCK, "bricks", "normal", null),
    BRICK_WALL("brick_wall", "Brick Wall", Kind.BLOCK, "bricks", "normal", null),
    SMOOTH_BLOCK("smooth_block", "Smooth Block", Kind.BLOCK, null, null, null),
    SMOOTH_SLAB("smooth_slab", "Smooth Slab", Kind.BLOCK, null, null, null),
    SMOOTH_STAIRS("smooth_stairs", "Smooth Stairs", Kind.BLOCK, null, null, null),

    TINY_WOOD_PULP("tiny_wood_pulp", "Tiny Wood Pulp", Kind.ITEM, "dust", "tiny", null),
    SMALL_WOOD_PULP("small_wood_pulp", "Small Wood Pulp", Kind.ITEM, "dust", "small", null),
    WOOD_PULP("wood_pulp", "Wood Pulp", Kind.ITEM, "dust", "normal", null),
    BARK("bark", "Bark", Kind.ITEM, null, null, null),

    RAW_ORE("raw_ore", "Raw Ore", Kind.ITEM, "raw_ore", "normal", "raw_ore"),
    RAW_BLOCK("raw_block", "Raw Block", Kind.BLOCK, "raw_ore_block", "normal", "raw_ore_block"),
    CRUSHED_ORE("crushed_ore", "Crushed Ore", Kind.ITEM, "crushed_ore", "normal", "crushed"),
    WASHED_CRUSHED_ORE("washed_crushed_ore", "Washed Crushed Ore", Kind.ITEM, "washed_crushed_ore", "normal", "crushed_purified"),
    REFINED_ORE("refined_ore", "Refined Ore", Kind.ITEM, "refined_ore", "normal", "crushed_refined"),
    INGOT("ingot", "Ingot", Kind.ITEM, "ingot", "normal", "ingot", 144, 317),
    DOUBLE_INGOT("double_ingot", "Double Ingot", Kind.ITEM, "ingot", "double", "ingot_double", 288, 216),
    HOT_INGOT(INGOT, "hot"),
    NUGGET("nugget", "Nugget", Kind.ITEM, "nugget", "normal", "nugget", 16, 89),
    HOT_NUGGET(NUGGET, "hot"),
    BLOCK("block", "Block", Kind.BLOCK, "block", "normal", "block", 1296, 94),

    TINY_DUST("tiny_dust", "Tiny Dust", Kind.ITEM, "dust", "tiny", "dust_tiny"),
    SMALL_DUST("small_dust", "Small Dust", Kind.ITEM, "dust", "small", "dust_small"),
    DUST("dust", "Dust", Kind.ITEM, "dust", "normal", "dust"),
    // Existing biological compound item; not a powder and never an inferred manufactured form.
    BIOLOGICAL_FEED("biological_feed", "Biological Feed", Kind.ITEM, null, null, null),
    CLAY("clay_ball", "Clay Ball", Kind.ITEM, "clay", "normal", null),
    CLAY_BLOCK("clay", "Clay", Kind.BLOCK, "clay", "block", null),
    UNFIRED_BRICK("unfired_brick", "Unfired Brick", Kind.ITEM, "brick", "unfired", null),
    DRIED_UNFIRED_BRICK("dried_unfired_brick", "Dried Unfired Brick", Kind.ITEM, "brick", "dried", null),
    BRICK("brick", "Brick", Kind.ITEM, "brick", "normal", null),
    CRACKED_BRICK("cracked_brick", "Cracked Brick", Kind.ITEM, "brick", "cracked", null),
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

    TINY_BALL("tiny_ball", "Tiny Ball", Kind.ITEM, "ball", "tiny", null, 8, 136),
    SMALL_BALL("small_ball", "Small Ball", Kind.ITEM, "ball", "small", null, 16, 567),
    BALL("ball", "Ball", Kind.ITEM, "ball", "normal", "bearing_ball", 32, 186),
    LARGE_BALL("large_ball", "Large Ball", Kind.ITEM, "ball", "large", null, 64, 886),
    HUGE_BALL("huge_ball", "Huge Ball", Kind.ITEM, "ball", "huge", null, 128, 244),

    TINY_BEARING("tiny_bearing", "Tiny Bearing", Kind.ITEM, "bearing", "tiny", null, 96, 666),
    SMALL_BEARING("small_bearing", "Small Bearing", Kind.ITEM, "bearing", "small", null, 192, 593),
    BEARING("bearing", "Bearing", Kind.ITEM, "bearing", "normal", "bearing", 384, 931),
    LARGE_BEARING("large_bearing", "Large Bearing", Kind.ITEM, "bearing", "large", null, 768, 738),
    HUGE_BEARING("huge_bearing", "Huge Bearing", Kind.ITEM, "bearing", "huge", null, 1536, 61),

    TINY_BOLT("tiny_bolt", "Tiny Bolt", Kind.ITEM, "bolt", "tiny", null, 2, 183),
    SMALL_BOLT("small_bolt", "Small Bolt", Kind.ITEM, "bolt", "small", null, 4, 868),
    BOLT("bolt", "Bolt", Kind.ITEM, "bolt", "normal", "bolt", 8, 943),
    LARGE_BOLT("large_bolt", "Large Bolt", Kind.ITEM, "bolt", "large", null, 16, 133),
    HUGE_BOLT("huge_bolt", "Huge Bolt", Kind.ITEM, "bolt", "huge", null, 32, 581),

    TINY_SCREW("tiny_screw", "Tiny Screw", Kind.ITEM, "screw", "tiny", null, 2, 903),
    SMALL_SCREW("small_screw", "Small Screw", Kind.ITEM, "screw", "small", null, 4, 949),
    SCREW("screw", "Screw", Kind.ITEM, "screw", "normal", "screw", 8, 884),
    LARGE_SCREW("large_screw", "Large Screw", Kind.ITEM, "screw", "large", null, 16, 517),
    HUGE_SCREW("huge_screw", "Huge Screw", Kind.ITEM, "screw", "huge", null, 32, 642),

    TINY_RIVET("tiny_rivet", "Tiny Rivet", Kind.ITEM, "rivet", "tiny", null, 1, 31),
    SMALL_RIVET("small_rivet", "Small Rivet", Kind.ITEM, "rivet", "small", null, 2, 766),
    RIVET("rivet", "Rivet", Kind.ITEM, "rivet", "normal", "rivet", 4, 522),
    LARGE_RIVET("large_rivet", "Large Rivet", Kind.ITEM, "rivet", "large", null, 8, 277),
    HUGE_RIVET("huge_rivet", "Huge Rivet", Kind.ITEM, "rivet", "huge", null, 16, 711),

    TINY_GEAR("tiny_gear", "Tiny Gear", Kind.ITEM, "gear", "tiny", null, 144, 861),
    SMALL_GEAR("small_gear", "Small Gear", Kind.ITEM, "gear", "small", "gear_small", 288, 362),
    GEAR("gear", "Gear", Kind.ITEM, "gear", "normal", "gear", 288, 565),
    LARGE_GEAR("large_gear", "Large Gear", Kind.ITEM, "gear", "large", "gear_large", 576, 438),
    HUGE_GEAR("huge_gear", "Huge Gear", Kind.ITEM, "gear", "huge", null, 1152, 654),

    TINY_RING("tiny_ring", "Tiny Ring", Kind.ITEM, "ring", "tiny", null, 16, 338),
    SMALL_RING("small_ring", "Small Ring", Kind.ITEM, "ring", "small", "ring_small", 32, 802),
    RING("ring", "Ring", Kind.ITEM, "ring", "normal", "ring", 64, 739),
    LARGE_RING("large_ring", "Large Ring", Kind.ITEM, "ring", "large", "ring_large", 128, 329),
    HUGE_RING("huge_ring", "Huge Ring", Kind.ITEM, "ring", "huge", null, 256, 205),

    VERY_SHORT_ROD("very_short_rod", "Very Short Rod", Kind.ITEM, "rod", "very_short", null, 16, 637),
    SHORT_ROD("short_rod", "Short Rod", Kind.ITEM, "rod", "short", null, 32, 47),
    ROD("rod", "Rod", Kind.ITEM, "rod", "normal", "rod", 64, 123),
    LONG_ROD("long_rod", "Long Rod", Kind.ITEM, "rod", "long", "rod_long", 128, 461),
    VERY_LONG_ROD("very_long_rod", "Very Long Rod", Kind.ITEM, "rod", "very_long", null, 256, 274),

    TINY_SPRING("tiny_spring", "Tiny Spring", Kind.ITEM, "spring", "tiny", null, 16, 118),
    SMALL_SPRING("small_spring", "Small Spring", Kind.ITEM, "spring", "small", "small_spring", 16, 206),
    SPRING("spring", "Spring", Kind.ITEM, "spring", "normal", "spring", 32, 941),
    LARGE_SPRING("large_spring", "Large Spring", Kind.ITEM, "spring", "large", "large_spring", 64, 806),
    HUGE_SPRING("huge_spring", "Huge Spring", Kind.ITEM, "spring", "huge", null, 128, 769),

    TINY_ROTOR("tiny_rotor", "Tiny Rotor", Kind.ITEM, "rotor", "tiny", null, 144, 225),
    SMALL_ROTOR("small_rotor", "Small Rotor", Kind.ITEM, "rotor", "small", null, 288, 762),
    ROTOR("rotor", "Rotor", Kind.ITEM, "rotor", "normal", "rotor", 288, 259),
    LARGE_ROTOR("large_rotor", "Large Rotor", Kind.ITEM, "rotor", "large", null, 576, 98),
    HUGE_ROTOR("huge_rotor", "Huge Rotor", Kind.ITEM, "rotor", "huge", null, 1152, 385),

    PLATE("plate", "Plate", Kind.ITEM, "plate", "normal", "plate", 144, 293),
    LARGE_PLATE("large_plate", "Large Plate", Kind.ITEM, "plate", "large", null, 576, 787),
    DOUBLE_PLATE("double_plate", "Double Plate", Kind.ITEM, "plate_double", "normal", "plate_double", 288, 373),
    LARGE_DOUBLE_PLATE("large_double_plate", "Large Double Plate", Kind.ITEM, "plate_double", "large", null, 1152, 453),
    DENSE_PLATE("dense_plate", "Dense Plate", Kind.ITEM, "plate_dense", "normal", "plate_dense", 1296, 38),
    LARGE_DENSE_PLATE("large_dense_plate", "Large Dense Plate", Kind.ITEM, "plate_dense", "large", null, 5184, 57),
    REINFORCED_PLATE("reinforced_plate", "Reinforced Plate", Kind.ITEM, "plate_reinforced", "normal", "plate_reinforced"),
    HEAT_EXCHANGER_PLATE("heat_exchanger_plate", "Heat Exchanger Plate", Kind.ITEM, "plate_heat_exchanger", "normal", "plate_heat_exchanger"),
    FOIL("foil", "Foil", Kind.ITEM, "foil", "normal", "foil", 36, 59),
    FINE_WIRE("fine_wire", "Fine Wire", Kind.ITEM, "fine_wire", "normal", "wire_fine", 8, 238),
    WIRE_1X("wire_1x", "1x Wire", Kind.ITEM, "wire", "1x", null, 8, 355),
    WIRE_2X("wire_2x", "2x Wire", Kind.ITEM, "wire", "2x", null, 16, 531),
    WIRE_4X("wire_4x", "4x Wire", Kind.ITEM, "wire", "4x", null, 32, 105),
    WIRE_8X("wire_8x", "8x Wire", Kind.ITEM, "wire", "8x", null, 64, 334),
    WIRE_16X("wire_16x", "16x Wire", Kind.ITEM, "wire", "16x", null, 128, 681),
    WIRE("wire", "Wire", Kind.ITEM, "wire", "normal", "wire"),
    COIL("coil", "Coil", Kind.ITEM, "coil", "normal", "coil", 64, 3),
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
    BUZZ_SAW("buzz_saw", "Buzz Saw", Kind.ITEM, "buzz_saw", null, null, 288, 906),
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
    HOT_CAST_SCREW_MOLD("hot_cast_screw_mold", "Hot Cast Screw Mold", Kind.ITEM, "cast_screw_mold", "normal", "cast_screw_mold"),
    SHEARS_HEAD("shears_head", "Shears Head", Kind.ITEM, "shears", "head", null, 288, 712),
    SHEARS_HANDLE("shears_handle", "Shears Handle", Kind.ITEM, "shears", "handle", null, 144, 713),
    CROSSBOW_LIMBS("crossbow_limbs", "Crossbow Limbs", Kind.ITEM, "crossbow", "limbs", null, 432, 714),
    CROSSBOW_TRIGGER("crossbow_trigger", "Crossbow Trigger", Kind.ITEM, "crossbow", "trigger", null, 16, 715),
    FISHING_HOOK("fishing_hook", "Fishing Hook", Kind.ITEM, "fishing_rod", "hook", null, 16, 716),
    SHIELD_BODY("shield_body", "Shield Body", Kind.ITEM, "shield", "body", null, 864, 717),
    SHIELD_HANDLE("shield_handle", "Shield Handle", Kind.ITEM, "shield", "handle", null, 144, 718),
    HELMET_SHELL("helmet_shell", "Helmet Shell", Kind.ITEM, "armour", "helmet", null, 576, 719),
    CHESTPLATE_SHELL("chestplate_shell", "Chestplate Shell", Kind.ITEM, "armour", "chestplate", null, 1152, 720),
    LEGGINGS_SHELL("leggings_shell", "Leggings Shell", Kind.ITEM, "armour", "leggings", null, 1008, 721),
    BOOTS_SHELL("boots_shell", "Boots Shell", Kind.ITEM, "armour", "boots", null, 576, 722),
    SNAP_RING_PLIERS_HEAD("snap_ring_pliers_head", "Snap Ring Pliers Head", Kind.ITEM, "snap_ring_pliers", "head", null, 144, 731),
    SNAP_RING_PLIERS_HANDLE("snap_ring_pliers_handle", "Snap Ring Pliers Handle", Kind.ITEM, "snap_ring_pliers", "handle", null, 144, 732),
    BEARING_PRESS_HEAD("bearing_press_head", "Bearing Press Head", Kind.ITEM, "bearing_press", "head", null, 576, 733),
    BEARING_PRESS_HANDLE("bearing_press_handle", "Bearing Press Handle", Kind.ITEM, "bearing_press", "handle", null, 144, 734),
    CLAMP_HEAD("clamp_head", "Clamp Head", Kind.ITEM, "clamp", "head", null, 288, 735),
    CLAMP_HANDLE("clamp_handle", "Clamp Handle", Kind.ITEM, "clamp", "handle", null, 72, 736),
    CRIMPING_TOOL_HEAD("crimping_tool_head", "Crimping Tool Head", Kind.ITEM, "crimping_tool", "head", null, 288, 737),
    CRIMPING_TOOL_HANDLE("crimping_tool_handle", "Crimping Tool Handle", Kind.ITEM, "crimping_tool", "handle", null, 144, 738),
    GEAR_CUTTER_HEAD("gear_cutter_head", "Gear Cutter Head", Kind.ITEM, "gear_cutter", "head", null, 144, 739),
    GEAR_CUTTER_HANDLE("gear_cutter_handle", "Gear Cutter Handle", Kind.ITEM, "gear_cutter", "handle", null, 144, 740),
    BOW_BODY("bow_body", "Bow Body", Kind.ITEM, "bow", "body", null),
    CROSSBOW_STOCK("crossbow_stock", "Crossbow Stock", Kind.ITEM, "crossbow", "stock", null),
    FISHING_ROD_BODY("fishing_rod_body", "Fishing Rod Body", Kind.ITEM, "fishing_rod", "body", null),
    HOT_SHEARS_HEAD(SHEARS_HEAD),
    CAST_SHEARS_HEAD_MOLD("cast_shears_head_mold", "Cast Shears Head Mold", Kind.ITEM, "cast_shears_head_mold", "normal", null),
    HOT_SHEARS_HANDLE(SHEARS_HANDLE),
    CAST_SHEARS_HANDLE_MOLD("cast_shears_handle_mold", "Cast Shears Handle Mold", Kind.ITEM, "cast_shears_handle_mold", "normal", null),
    HOT_CROSSBOW_LIMBS(CROSSBOW_LIMBS),
    CAST_CROSSBOW_LIMBS_MOLD("cast_crossbow_limbs_mold", "Cast Crossbow Limbs Mold", Kind.ITEM, "cast_crossbow_limbs_mold", "normal", null),
    HOT_CROSSBOW_TRIGGER(CROSSBOW_TRIGGER),
    CAST_CROSSBOW_TRIGGER_MOLD("cast_crossbow_trigger_mold", "Cast Crossbow Trigger Mold", Kind.ITEM, "cast_crossbow_trigger_mold", "normal", null),
    HOT_FISHING_HOOK(FISHING_HOOK),
    CAST_FISHING_HOOK_MOLD("cast_fishing_hook_mold", "Cast Fishing Hook Mold", Kind.ITEM, "cast_fishing_hook_mold", "normal", null),
    HOT_SHIELD_BODY(SHIELD_BODY),
    CAST_SHIELD_BODY_MOLD("cast_shield_body_mold", "Cast Shield Body Mold", Kind.ITEM, "cast_shield_body_mold", "normal", null),
    HOT_SHIELD_HANDLE(SHIELD_HANDLE),
    CAST_SHIELD_HANDLE_MOLD("cast_shield_handle_mold", "Cast Shield Handle Mold", Kind.ITEM, "cast_shield_handle_mold", "normal", null),
    HOT_HELMET_SHELL(HELMET_SHELL),
    CAST_HELMET_SHELL_MOLD("cast_helmet_shell_mold", "Cast Helmet Shell Mold", Kind.ITEM, "cast_helmet_shell_mold", "normal", null),
    HOT_CHESTPLATE_SHELL(CHESTPLATE_SHELL),
    CAST_CHESTPLATE_SHELL_MOLD("cast_chestplate_shell_mold", "Cast Chestplate Shell Mold", Kind.ITEM, "cast_chestplate_shell_mold", "normal", null),
    HOT_LEGGINGS_SHELL(LEGGINGS_SHELL),
    CAST_LEGGINGS_SHELL_MOLD("cast_leggings_shell_mold", "Cast Leggings Shell Mold", Kind.ITEM, "cast_leggings_shell_mold", "normal", null),
    HOT_BOOTS_SHELL(BOOTS_SHELL),
    CAST_BOOTS_SHELL_MOLD("cast_boots_shell_mold", "Cast Boots Shell Mold", Kind.ITEM, "cast_boots_shell_mold", "normal", null),
    HOT_SNAP_RING_PLIERS_HEAD(SNAP_RING_PLIERS_HEAD),
    CAST_SNAP_RING_PLIERS_HEAD_MOLD("cast_snap_ring_pliers_head_mold", "Cast Snap Ring Pliers Head Mold", Kind.ITEM, "cast_snap_ring_pliers_head_mold", "normal", null),
    HOT_SNAP_RING_PLIERS_HANDLE(SNAP_RING_PLIERS_HANDLE),
    CAST_SNAP_RING_PLIERS_HANDLE_MOLD("cast_snap_ring_pliers_handle_mold", "Cast Snap Ring Pliers Handle Mold", Kind.ITEM, "cast_snap_ring_pliers_handle_mold", "normal", null),
    HOT_BEARING_PRESS_HEAD(BEARING_PRESS_HEAD),
    CAST_BEARING_PRESS_HEAD_MOLD("cast_bearing_press_head_mold", "Cast Bearing Press Head Mold", Kind.ITEM, "cast_bearing_press_head_mold", "normal", null),
    HOT_BEARING_PRESS_HANDLE(BEARING_PRESS_HANDLE),
    CAST_BEARING_PRESS_HANDLE_MOLD("cast_bearing_press_handle_mold", "Cast Bearing Press Handle Mold", Kind.ITEM, "cast_bearing_press_handle_mold", "normal", null),
    HOT_CLAMP_HEAD(CLAMP_HEAD),
    CAST_CLAMP_HEAD_MOLD("cast_clamp_head_mold", "Cast Clamp Head Mold", Kind.ITEM, "cast_clamp_head_mold", "normal", null),
    HOT_CLAMP_HANDLE(CLAMP_HANDLE),
    CAST_CLAMP_HANDLE_MOLD("cast_clamp_handle_mold", "Cast Clamp Handle Mold", Kind.ITEM, "cast_clamp_handle_mold", "normal", null),
    HOT_CRIMPING_TOOL_HEAD(CRIMPING_TOOL_HEAD),
    CAST_CRIMPING_TOOL_HEAD_MOLD("cast_crimping_tool_head_mold", "Cast Crimping Tool Head Mold", Kind.ITEM, "cast_crimping_tool_head_mold", "normal", null),
    HOT_CRIMPING_TOOL_HANDLE(CRIMPING_TOOL_HANDLE),
    CAST_CRIMPING_TOOL_HANDLE_MOLD("cast_crimping_tool_handle_mold", "Cast Crimping Tool Handle Mold", Kind.ITEM, "cast_crimping_tool_handle_mold", "normal", null),
    HOT_GEAR_CUTTER_HEAD(GEAR_CUTTER_HEAD),
    CAST_GEAR_CUTTER_HEAD_MOLD("cast_gear_cutter_head_mold", "Cast Gear Cutter Head Mold", Kind.ITEM, "cast_gear_cutter_head_mold", "normal", null),
    HOT_GEAR_CUTTER_HANDLE(GEAR_CUTTER_HANDLE),
    CAST_GEAR_CUTTER_HANDLE_MOLD("cast_gear_cutter_handle_mold", "Cast Gear Cutter Handle Mold", Kind.ITEM, "cast_gear_cutter_handle_mold", "normal", null),
    TOOL_HEAD_AXE("tool_head_axe", "Tool Head Axe", Kind.ITEM, "axe", "head", null, 432, 66),
    TOOL_HEAD_CHAINSAW("tool_head_chainsaw", "Tool Head Chainsaw", Kind.ITEM, "chainsaw", "head", null, 288, 551),
    TOOL_HEAD_CHISEL("tool_head_chisel", "Tool Head Chisel", Kind.ITEM, "chisel", "head", null, 144, 978),
    TOOL_HEAD_CROWBAR("tool_head_crowbar", "Tool Head Crowbar", Kind.ITEM, "crowbar", "head", null, 144, 408),
    TOOL_HEAD_DRILL("tool_head_drill", "Tool Head Drill", Kind.ITEM, "drill", "head", null, 576, 398),
    TOOL_HEAD_FILE("tool_head_file", "Tool Head File", Kind.ITEM, "file", "head", null, 288, 405),
    TOOL_HEAD_HAMMER("tool_head_hammer", "Tool Head Hammer", Kind.ITEM, "hammer", "head", null, 864, 952),
    TOOL_HEAD_HOE("tool_head_hoe", "Tool Head Hoe", Kind.ITEM, "hoe", "head", null, 288, 191),
    TOOL_HEAD_MALLET("tool_head_mallet", "Tool Head Mallet", Kind.ITEM, "mallet", "head", null),
    TOOL_HEAD_PESTLE("tool_head_pestle", "Pestle Head", Kind.ITEM, "pestle", "head", null),
    TOOL_HEAD_PICKAXE("tool_head_pickaxe", "Tool Head Pickaxe", Kind.ITEM, "pickaxe", "head", null, 432, 185),
    TOOL_HEAD_SCREWDRIVER("tool_head_screwdriver", "Tool Head Screwdriver", Kind.ITEM, "screwdriver", "head", null, 72, 798),
    TOOL_HEAD_SHOVEL("tool_head_shovel", "Tool Head Shovel", Kind.ITEM, "shovel", "head", null, 144, 685),
    TOOL_HEAD_WIRE_CUTTER("tool_head_wire_cutter", "Tool Head Wire Cutter", Kind.ITEM, "wirecutter", "head", null, 288, 662),
    WRENCH("wrench", "Wrench", Kind.ITEM, "wrench", null, null, 576, 569),
    KNIFE_BLADE("knife_blade", "Knife Blade", Kind.ITEM, "knife", "head", null, 72, 327),
    SAW_BLADE("saw_blade", "Saw Blade", Kind.ITEM, "saw", "head", null, 288, 786),
    TOOL_HANDLE("tool_handle", "Tool Handle", Kind.ITEM, "tool_handle", null, null, 144, 62),
    DRILL("drill", "Drill", Kind.ITEM, "drill", null, null),
    SAW_HANDLE("saw_handle", "Saw Handle", Kind.ITEM, "saw", "handle", null, 144, 995),
    SIFTER_FRAME("sifter_frame", "Sifter Frame", Kind.ITEM, "sifter", "frame", null),
    WIRE_CUTTER_BODY("wire_cutter_body", "Wire Cutter Body", Kind.ITEM, "wirecutter", "body", null, 288, 159),
    DRILL_BODY("drill_body", "Drill Body", Kind.ITEM, "drill", "body", null),
    CHAINSAW_BODY("chainsaw_body", "Chainsaw Body", Kind.ITEM, "chainsaw", "body", null),
    HOT_TINY_BALL(TINY_BALL),
    HOT_SMALL_BALL(SMALL_BALL),
    HOT_BALL(BALL),
    HOT_LARGE_BALL(LARGE_BALL),
    HOT_HUGE_BALL(HUGE_BALL),
    HOT_TINY_GEAR(TINY_GEAR),
    HOT_SMALL_GEAR(SMALL_GEAR),
    HOT_GEAR(GEAR),
    HOT_LARGE_GEAR(LARGE_GEAR),
    HOT_HUGE_GEAR(HUGE_GEAR),
    HOT_TINY_RING(TINY_RING),
    HOT_SMALL_RING(SMALL_RING),
    HOT_RING(RING),
    HOT_LARGE_RING(LARGE_RING),
    HOT_HUGE_RING(HUGE_RING),
    HOT_TINY_RIVET(TINY_RIVET),
    HOT_SMALL_RIVET(SMALL_RIVET),
    HOT_RIVET(RIVET),
    HOT_LARGE_RIVET(LARGE_RIVET),
    HOT_HUGE_RIVET(HUGE_RIVET),
    HOT_TINY_ROTOR(TINY_ROTOR),
    HOT_SMALL_ROTOR(SMALL_ROTOR),
    HOT_ROTOR(ROTOR),
    HOT_LARGE_ROTOR(LARGE_ROTOR),
    HOT_HUGE_ROTOR(HUGE_ROTOR),
    HOT_PLATE(PLATE),
    HOT_LARGE_PLATE(LARGE_PLATE),
    HOT_VERY_SHORT_ROD(VERY_SHORT_ROD),
    HOT_SHORT_ROD(SHORT_ROD),
    HOT_ROD(ROD),
    HOT_LONG_ROD(LONG_ROD),
    HOT_VERY_LONG_ROD(VERY_LONG_ROD),
    HOT_TOOL_HEAD_AXE(TOOL_HEAD_AXE),
    HOT_BUZZ_SAW(BUZZ_SAW),
    HOT_TOOL_HEAD_CHAINSAW(TOOL_HEAD_CHAINSAW),
    HOT_TOOL_HEAD_CHISEL(TOOL_HEAD_CHISEL),
    HOT_TOOL_HEAD_CROWBAR(TOOL_HEAD_CROWBAR),
    HOT_TOOL_HEAD_DRILL(TOOL_HEAD_DRILL),
    HOT_TOOL_HEAD_FILE(TOOL_HEAD_FILE),
    HOT_TOOL_HEAD_HAMMER(TOOL_HEAD_HAMMER),
    HOT_TOOL_HEAD_HOE(TOOL_HEAD_HOE),
    HOT_TOOL_HEAD_MALLET(TOOL_HEAD_MALLET),
    HOT_TOOL_HEAD_PICKAXE(TOOL_HEAD_PICKAXE),
    HOT_TOOL_HEAD_SCREWDRIVER(TOOL_HEAD_SCREWDRIVER),
    HOT_TOOL_HEAD_SHOVEL(TOOL_HEAD_SHOVEL),
    HOT_TOOL_HEAD_WIRE_CUTTER(TOOL_HEAD_WIRE_CUTTER),
    HOT_WRENCH(WRENCH),
    HOT_KNIFE_BLADE(KNIFE_BLADE),
    HOT_SAW_BLADE(SAW_BLADE),
    HOT_TOOL_HANDLE(TOOL_HANDLE),
    HOT_DRILL(DRILL),
    HOT_SAW_HANDLE(SAW_HANDLE),
    HOT_WIRE_CUTTER_BODY(WIRE_CUTTER_BODY),
    CAST_TINY_BALL_MOLD("cast_tiny_ball_mold", "Cast Tiny Ball Mold", Kind.ITEM, "cast_tiny_ball_mold", "normal", null),
    CAST_SMALL_BALL_MOLD("cast_small_ball_mold", "Cast Small Ball Mold", Kind.ITEM, "cast_small_ball_mold", "normal", null),
    CAST_LARGE_BALL_MOLD("cast_large_ball_mold", "Cast Large Ball Mold", Kind.ITEM, "cast_large_ball_mold", "normal", null),
    CAST_HUGE_BALL_MOLD("cast_huge_ball_mold", "Cast Huge Ball Mold", Kind.ITEM, "cast_huge_ball_mold", "normal", null),
    CAST_TINY_GEAR_MOLD("cast_tiny_gear_mold", "Cast Tiny Gear Mold", Kind.ITEM, "cast_tiny_gear_mold", "normal", null),
    CAST_LARGE_GEAR_MOLD("cast_large_gear_mold", "Cast Large Gear Mold", Kind.ITEM, "cast_large_gear_mold", "normal", null),
    CAST_HUGE_GEAR_MOLD("cast_huge_gear_mold", "Cast Huge Gear Mold", Kind.ITEM, "cast_huge_gear_mold", "normal", null),
    CAST_TINY_RING_MOLD("cast_tiny_ring_mold", "Cast Tiny Ring Mold", Kind.ITEM, "cast_tiny_ring_mold", "normal", null),
    CAST_HUGE_RING_MOLD("cast_huge_ring_mold", "Cast Huge Ring Mold", Kind.ITEM, "cast_huge_ring_mold", "normal", null),
    CAST_TINY_RIVET_MOLD("cast_tiny_rivet_mold", "Cast Tiny Rivet Mold", Kind.ITEM, "cast_tiny_rivet_mold", "normal", null),
    CAST_SMALL_RIVET_MOLD("cast_small_rivet_mold", "Cast Small Rivet Mold", Kind.ITEM, "cast_small_rivet_mold", "normal", null),
    CAST_RIVET_MOLD("cast_rivet_mold", "Cast Rivet Mold", Kind.ITEM, "cast_rivet_mold", "normal", null),
    CAST_LARGE_RIVET_MOLD("cast_large_rivet_mold", "Cast Large Rivet Mold", Kind.ITEM, "cast_large_rivet_mold", "normal", null),
    CAST_HUGE_RIVET_MOLD("cast_huge_rivet_mold", "Cast Huge Rivet Mold", Kind.ITEM, "cast_huge_rivet_mold", "normal", null),
    CAST_TINY_ROTOR_MOLD("cast_tiny_rotor_mold", "Cast Tiny Rotor Mold", Kind.ITEM, "cast_tiny_rotor_mold", "normal", null),
    CAST_SMALL_ROTOR_MOLD("cast_small_rotor_mold", "Cast Small Rotor Mold", Kind.ITEM, "cast_small_rotor_mold", "normal", null),
    CAST_LARGE_ROTOR_MOLD("cast_large_rotor_mold", "Cast Large Rotor Mold", Kind.ITEM, "cast_large_rotor_mold", "normal", null),
    CAST_HUGE_ROTOR_MOLD("cast_huge_rotor_mold", "Cast Huge Rotor Mold", Kind.ITEM, "cast_huge_rotor_mold", "normal", null),
    CAST_LARGE_PLATE_MOLD("cast_large_plate_mold", "Cast Large Plate Mold", Kind.ITEM, "cast_large_plate_mold", "normal", null),
    CAST_VERY_SHORT_ROD_MOLD("cast_very_short_rod_mold", "Cast Very Short Rod Mold", Kind.ITEM, "cast_very_short_rod_mold", "normal", null),
    CAST_SHORT_ROD_MOLD("cast_short_rod_mold", "Cast Short Rod Mold", Kind.ITEM, "cast_short_rod_mold", "normal", null),
    CAST_VERY_LONG_ROD_MOLD("cast_very_long_rod_mold", "Cast Very Long Rod Mold", Kind.ITEM, "cast_very_long_rod_mold", "normal", null),
    CAST_TOOL_HEAD_AXE_MOLD("cast_tool_head_axe_mold", "Cast Tool Head Axe Mold", Kind.ITEM, "cast_tool_head_axe_mold", "normal", null),
    CAST_BUZZ_SAW_MOLD("cast_buzz_saw_mold", "Cast Buzz Saw Mold", Kind.ITEM, "cast_buzz_saw_mold", "normal", null),
    CAST_TOOL_HEAD_CHAINSAW_MOLD("cast_tool_head_chainsaw_mold", "Cast Tool Head Chainsaw Mold", Kind.ITEM, "cast_tool_head_chainsaw_mold", "normal", null),
    CAST_TOOL_HEAD_CHISEL_MOLD("cast_tool_head_chisel_mold", "Cast Tool Head Chisel Mold", Kind.ITEM, "cast_tool_head_chisel_mold", "normal", null),
    CAST_TOOL_HEAD_CROWBAR_MOLD("cast_tool_head_crowbar_mold", "Cast Tool Head Crowbar Mold", Kind.ITEM, "cast_tool_head_crowbar_mold", "normal", null),
    CAST_TOOL_HEAD_DRILL_MOLD("cast_tool_head_drill_mold", "Cast Tool Head Drill Mold", Kind.ITEM, "cast_tool_head_drill_mold", "normal", null),
    CAST_TOOL_HEAD_FILE_MOLD("cast_tool_head_file_mold", "Cast Tool Head File Mold", Kind.ITEM, "cast_tool_head_file_mold", "normal", null),
    CAST_TOOL_HEAD_HAMMER_MOLD("cast_tool_head_hammer_mold", "Cast Tool Head Hammer Mold", Kind.ITEM, "cast_tool_head_hammer_mold", "normal", null),
    CAST_TOOL_HEAD_HOE_MOLD("cast_tool_head_hoe_mold", "Cast Tool Head Hoe Mold", Kind.ITEM, "cast_tool_head_hoe_mold", "normal", null),
    CAST_TOOL_HEAD_MALLET_MOLD("cast_tool_head_mallet_mold", "Cast Tool Head Mallet Mold", Kind.ITEM, "cast_tool_head_mallet_mold", "normal", null),
    CAST_TOOL_HEAD_PICKAXE_MOLD("cast_tool_head_pickaxe_mold", "Cast Tool Head Pickaxe Mold", Kind.ITEM, "cast_tool_head_pickaxe_mold", "normal", null),
    CAST_TOOL_HEAD_SCREWDRIVER_MOLD("cast_tool_head_screwdriver_mold", "Cast Tool Head Screwdriver Mold", Kind.ITEM, "cast_tool_head_screwdriver_mold", "normal", null),
    CAST_TOOL_HEAD_SHOVEL_MOLD("cast_tool_head_shovel_mold", "Cast Tool Head Shovel Mold", Kind.ITEM, "cast_tool_head_shovel_mold", "normal", null),
    CAST_TOOL_HEAD_WIRE_CUTTER_MOLD("cast_tool_head_wire_cutter_mold", "Cast Tool Head Wire Cutter Mold", Kind.ITEM, "cast_tool_head_wire_cutter_mold", "normal", null),
    CAST_WRENCH_MOLD("cast_wrench_mold", "Cast Wrench Mold", Kind.ITEM, "cast_wrench_mold", "normal", null),
    CAST_KNIFE_BLADE_MOLD("cast_knife_blade_mold", "Cast Knife Blade Mold", Kind.ITEM, "cast_knife_blade_mold", "normal", null),
    CAST_SAW_BLADE_MOLD("cast_saw_blade_mold", "Cast Saw Blade Mold", Kind.ITEM, "cast_saw_blade_mold", "normal", null),
    CAST_TOOL_HANDLE_MOLD("cast_tool_handle_mold", "Cast Tool Handle Mold", Kind.ITEM, "cast_tool_handle_mold", "normal", null),
    CAST_DRILL_MOLD("cast_drill_mold", "Cast Drill Mold", Kind.ITEM, "cast_drill_mold", "normal", null),
    CAST_SAW_HANDLE_MOLD("cast_saw_handle_mold", "Cast Saw Handle Mold", Kind.ITEM, "cast_saw_handle_mold", "normal", null),
    CAST_WIRE_CUTTER_BODY_MOLD("cast_wire_cutter_body_mold", "Cast Wire Cutter Body Mold", Kind.ITEM, "cast_wire_cutter_body_mold", "normal", null),

    // Appended to keep every pre-existing MaterialPart ordinal stable for deterministic texture variants.
    PEBBLE("pebble", "Pebble", Kind.ITEM, null, null, null),
    STICK("stick", "Stick", Kind.ITEM, "stick", "normal", null),

    // Shared utility wood forms. Appended so every existing ordinal above stays stable.
    CHEST("chest", "Chest", Kind.BLOCK, null, null, null),
    BARREL("barrel", "Barrel", Kind.BLOCK, null, null, null),
    BOOKSHELF("bookshelf", "Bookshelf", Kind.BLOCK, null, null, null),
    CHISELED_BOOKSHELF("chiseled_bookshelf", "Chiseled Bookshelf", Kind.BLOCK, null, null, null),
    LADDER("ladder", "Ladder", Kind.BLOCK, null, null, null),
    BOWL("bowl", "Bowl", Kind.ITEM, null, null, null),
    BOAT("boat", "Boat", Kind.ITEM, null, null, null),
    CHEST_BOAT("chest_boat", "Chest Boat", Kind.ITEM, null, null, null),

    // Logical metal-structure role. Appended to preserve every existing MaterialPart ordinal.
    // One BARS input may match any registered bars style for the selected metal.
    BARS("bars", "Bars", Kind.BLOCK, null, null, null),

    // Kinetic transmission form. Appended so every pre-existing ordinal remains stable.
    SHAFT("shaft", "Shaft", Kind.BLOCK, null, null, null),

    // Wooden fastening form. Appended so every pre-existing MaterialPart ordinal remains stable.
    WOOD_PEG("wood_peg", "Wood Peg", Kind.ITEM, "peg", "normal", null),
    // Visual machine-component forms; recipes and constituent amounts are not assigned yet.
    TINY_MOTOR_ARMATURE("tiny_motor_armature", "Tiny Motor Armature", Kind.ITEM, "motor_armature", "tiny", null),
    SMALL_MOTOR_ARMATURE("small_motor_armature", "Small Motor Armature", Kind.ITEM, "motor_armature", "small", null),
    MOTOR_ARMATURE("motor_armature", "Motor Armature", Kind.ITEM, "motor_armature", "normal", null),
    LARGE_MOTOR_ARMATURE("large_motor_armature", "Large Motor Armature", Kind.ITEM, "motor_armature", "large", null),
    HUGE_MOTOR_ARMATURE("huge_motor_armature", "Huge Motor Armature", Kind.ITEM, "motor_armature", "huge", null),
    TINY_MOTOR_COIL("tiny_motor_coil", "Tiny Motor Coil", Kind.ITEM, "motor_coil", "tiny", null),
    SMALL_MOTOR_COIL("small_motor_coil", "Small Motor Coil", Kind.ITEM, "motor_coil", "small", null),
    MOTOR_COIL("motor_coil", "Motor Coil", Kind.ITEM, "motor_coil", "normal", null),
    LARGE_MOTOR_COIL("large_motor_coil", "Large Motor Coil", Kind.ITEM, "motor_coil", "large", null),
    HUGE_MOTOR_COIL("huge_motor_coil", "Huge Motor Coil", Kind.ITEM, "motor_coil", "huge", null),
    TINY_MOTOR_HOUSING("tiny_motor_housing", "Tiny Motor Housing", Kind.ITEM, "motor_housing", "tiny", null),
    SMALL_MOTOR_HOUSING("small_motor_housing", "Small Motor Housing", Kind.ITEM, "motor_housing", "small", null),
    MOTOR_HOUSING("motor_housing", "Motor Housing", Kind.ITEM, "motor_housing", "normal", null),
    LARGE_MOTOR_HOUSING("large_motor_housing", "Large Motor Housing", Kind.ITEM, "motor_housing", "large", null),
    HUGE_MOTOR_HOUSING("huge_motor_housing", "Huge Motor Housing", Kind.ITEM, "motor_housing", "huge", null),
    TINY_MOTOR_SHAFT("tiny_motor_shaft", "Tiny Motor Shaft", Kind.ITEM, "motor_shaft", "tiny", null),
    SMALL_MOTOR_SHAFT("small_motor_shaft", "Small Motor Shaft", Kind.ITEM, "motor_shaft", "small", null),
    MOTOR_SHAFT("motor_shaft", "Motor Shaft", Kind.ITEM, "motor_shaft", "normal", null),
    LARGE_MOTOR_SHAFT("large_motor_shaft", "Large Motor Shaft", Kind.ITEM, "motor_shaft", "large", null),
    HUGE_MOTOR_SHAFT("huge_motor_shaft", "Huge Motor Shaft", Kind.ITEM, "motor_shaft", "huge", null),
    TINY_PISTON_ROD("tiny_piston_rod", "Tiny Piston Rod", Kind.ITEM, "piston_rod", "tiny", null),
    SMALL_PISTON_ROD("small_piston_rod", "Small Piston Rod", Kind.ITEM, "piston_rod", "small", null),
    PISTON_ROD("piston_rod", "Piston Rod", Kind.ITEM, "piston_rod", "normal", null),
    LARGE_PISTON_ROD("large_piston_rod", "Large Piston Rod", Kind.ITEM, "piston_rod", "large", null),
    HUGE_PISTON_ROD("huge_piston_rod", "Huge Piston Rod", Kind.ITEM, "piston_rod", "huge", null),
    TINY_PUMP_IMPELLER("tiny_pump_impeller", "Tiny Pump Impeller", Kind.ITEM, "pump_impeller", "tiny", null),
    SMALL_PUMP_IMPELLER("small_pump_impeller", "Small Pump Impeller", Kind.ITEM, "pump_impeller", "small", null),
    PUMP_IMPELLER("pump_impeller", "Pump Impeller", Kind.ITEM, "pump_impeller", "normal", null),
    LARGE_PUMP_IMPELLER("large_pump_impeller", "Large Pump Impeller", Kind.ITEM, "pump_impeller", "large", null),
    HUGE_PUMP_IMPELLER("huge_pump_impeller", "Huge Pump Impeller", Kind.ITEM, "pump_impeller", "huge", null),
    // Sword parts are appended to preserve all existing material-form ordinals.
    SWORD_BLADE("sword_blade", "Sword Blade", Kind.ITEM, "sword", "head", null, 288, 728),
    HOT_SWORD_BLADE(SWORD_BLADE),
    CAST_SWORD_BLADE_MOLD("cast_sword_blade_mold", "Cast Sword Blade Mold", Kind.ITEM, "cast_sword_blade_mold", "normal", null),
    // Physical workshop forms; append to preserve existing ordinals.
    MIXING_BLADE("mixing_blade", "Mixing Blade", Kind.ITEM, "machine_parts", "mixing_blade", null, 72, 730),
    HOT_MIXING_BLADE(MIXING_BLADE),
    CAST_MIXING_BLADE_MOLD("cast_mixing_blade_mold", "Cast Mixing Blade Mold", Kind.ITEM, "cast_mixing_blade_mold", "normal", null),
    IMPELLER("impeller", "Impeller", Kind.ITEM, "machine_parts", "impeller", null, 288, 741),
    HOT_IMPELLER(IMPELLER),
    CAST_IMPELLER_MOLD("cast_impeller_mold", "Cast Impeller Mold", Kind.ITEM, "cast_impeller_mold", "normal", null),
    CRANK("crank", "Crank", Kind.ITEM, "machine_parts", "crank", null, 144, 742),
    HOT_CRANK(CRANK),
    CAST_CRANK_MOLD("cast_crank_mold", "Cast Crank Mold", Kind.ITEM, "cast_crank_mold", "normal", null),
    CONNECTING_ROD("connecting_rod", "Connecting Rod", Kind.ITEM, "machine_parts", "connecting_rod", null, 144, 743),
    HOT_CONNECTING_ROD(CONNECTING_ROD),
    CAST_CONNECTING_ROD_MOLD("cast_connecting_rod_mold", "Cast Connecting Rod Mold", Kind.ITEM, "cast_connecting_rod_mold", "normal", null),
    PRESS_HEAD("press_head", "Press Head", Kind.ITEM, "machine_parts", "press_head", null, 576, 744),
    HOT_PRESS_HEAD(PRESS_HEAD),
    CAST_PRESS_HEAD_MOLD("cast_press_head_mold", "Cast Press Head Mold", Kind.ITEM, "cast_press_head_mold", "normal", null),
    GUIDE_RAIL("guide_rail", "Guide Rail", Kind.ITEM, "machine_parts", "guide_rail", null, 288, 745),
    HOT_GUIDE_RAIL(GUIDE_RAIL),
    CAST_GUIDE_RAIL_MOLD("cast_guide_rail_mold", "Cast Guide Rail Mold", Kind.ITEM, "cast_guide_rail_mold", "normal", null),
    CHUCK_JAW("chuck_jaw", "Chuck Jaw", Kind.ITEM, "machine_parts", "chuck_jaw", null, 72, 746),
    HOT_CHUCK_JAW(CHUCK_JAW),
    CAST_CHUCK_JAW_MOLD("cast_chuck_jaw_mold", "Cast Chuck Jaw Mold", Kind.ITEM, "cast_chuck_jaw_mold", "normal", null),
    CUTTING_INSERT("cutting_insert", "Cutting Insert", Kind.ITEM, "machine_parts", "cutting_insert", null, 36, 747),
    HOT_CUTTING_INSERT(CUTTING_INSERT),
    CAST_CUTTING_INSERT_MOLD("cast_cutting_insert_mold", "Cast Cutting Insert Mold", Kind.ITEM, "cast_cutting_insert_mold", "normal", null),
    CRUSHING_SEGMENT("crushing_segment", "Crushing Segment", Kind.ITEM, "machine_parts", "crushing_segment", null, 288, 748),
    HOT_CRUSHING_SEGMENT(CRUSHING_SEGMENT),
    CAST_CRUSHING_SEGMENT_MOLD("cast_crushing_segment_mold", "Cast Crushing Segment Mold", Kind.ITEM, "cast_crushing_segment_mold", "normal", null),
    CYLINDER("cylinder", "Cylinder", Kind.ITEM, "machine_parts", "cylinder", null, 576, 749),
    HOT_CYLINDER(CYLINDER),
    CAST_CYLINDER_MOLD("cast_cylinder_mold", "Cast Cylinder Mold", Kind.ITEM, "cast_cylinder_mold", "normal", null),
    PISTON("piston", "Piston", Kind.ITEM, "machine_parts", "piston", null, 288, 750),
    HOT_PISTON(PISTON),
    CAST_PISTON_MOLD("cast_piston_mold", "Cast Piston Mold", Kind.ITEM, "cast_piston_mold", "normal", null),
    PISTON_RING("piston_ring", "Piston Ring", Kind.ITEM, "machine_parts", "piston_ring", null, 36, 751),
    HOT_PISTON_RING(PISTON_RING),
    CAST_PISTON_RING_MOLD("cast_piston_ring_mold", "Cast Piston Ring Mold", Kind.ITEM, "cast_piston_ring_mold", "normal", null),
    VALVE_BODY("valve_body", "Valve Body", Kind.ITEM, "machine_parts", "valve_body", null, 144, 752),
    HOT_VALVE_BODY(VALVE_BODY),
    CAST_VALVE_BODY_MOLD("cast_valve_body_mold", "Cast Valve Body Mold", Kind.ITEM, "cast_valve_body_mold", "normal", null),
    VALVE_STEM("valve_stem", "Valve Stem", Kind.ITEM, "machine_parts", "valve_stem", null, 36, 753),
    HOT_VALVE_STEM(VALVE_STEM),
    CAST_VALVE_STEM_MOLD("cast_valve_stem_mold", "Cast Valve Stem Mold", Kind.ITEM, "cast_valve_stem_mold", "normal", null),
    VALVE_SEAT("valve_seat", "Valve Seat", Kind.ITEM, "machine_parts", "valve_seat", null, 72, 754),
    HOT_VALVE_SEAT(VALVE_SEAT),
    CAST_VALVE_SEAT_MOLD("cast_valve_seat_mold", "Cast Valve Seat Mold", Kind.ITEM, "cast_valve_seat_mold", "normal", null),
    HOT_SPRING(SPRING),
    CAST_SPRING_MOLD("cast_spring_mold", "Cast Spring Mold", Kind.ITEM, "cast_spring_mold", "normal", null),
    FLANGE("flange", "Flange", Kind.ITEM, "machine_parts", "flange", null, 144, 756),
    HOT_FLANGE(FLANGE),
    CAST_FLANGE_MOLD("cast_flange_mold", "Cast Flange Mold", Kind.ITEM, "cast_flange_mold", "normal", null),
    EXTRUSION_DIE("extrusion_die", "Extrusion Die", Kind.ITEM, "machine_parts", "extrusion_die", null, 288, 757),
    HOT_EXTRUSION_DIE(EXTRUSION_DIE),
    CAST_EXTRUSION_DIE_MOLD("cast_extrusion_die_mold", "Cast Extrusion Die Mold", Kind.ITEM, "cast_extrusion_die_mold", "normal", null),
    ;

    private final String id;
    private final String displayName;
    private final Kind kind;
    private final String textureFamily;
    private final String textureSize;
    private final String legacyTextureFamily;
    /** Exact physical material amount carried by this form. 0 means no explicit form amount. */
    private final int materialAmountMb;
    /** Hidden anvil-shape state. Negative means this part is not defined for forging. */
    private final int forgeValue;
    /** Non-null only for hot variants; points at the canonical cold form. */
    private final MaterialPart coldForgePart;

    static {
        // A mass + hidden shape pair must resolve to exactly one cold form. Otherwise the anvil
        // could not deterministically decide which single MaterialPart the player forged.
        java.util.Map<Long, MaterialPart> forgeShapes = new java.util.HashMap<>();
        for (MaterialPart part : values()) {
            if (part.isHotForgePart() || !part.isForgeableForm()) continue;
            long key = ((long) part.materialAmountMb << 32) ^ Integer.toUnsignedLong(part.forgeValue);
            MaterialPart previous = forgeShapes.putIfAbsent(key, part);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate forge shape for " + part.materialAmountMb + " mB and value " + part.forgeValue
                                + ": " + previous.name() + " and " + part.name()
                );
            }
        }
    }

    MaterialPart(String id, String displayName, Kind kind, String textureFamily, String textureSize, String legacyTextureFamily) {
        this(id, displayName, kind, textureFamily, textureSize, legacyTextureFamily, 0, -1, null);
    }

    MaterialPart(String id, String displayName, Kind kind, String textureFamily, String textureSize, String legacyTextureFamily,
                 int materialAmountMb, int forgeValue) {
        this(id, displayName, kind, textureFamily, textureSize, legacyTextureFamily, materialAmountMb, forgeValue, null);
    }

    private MaterialPart(String id, String displayName, Kind kind, String textureFamily, String textureSize, String legacyTextureFamily,
                         int materialAmountMb, int forgeValue, MaterialPart coldForgePart) {
        if (materialAmountMb < 0) throw new IllegalArgumentException("Material amount cannot be negative: " + id);
        if (forgeValue > 1000) throw new IllegalArgumentException("MaterialPart forge value must be <= 1000: " + id);
        this.id = id;
        this.displayName = displayName;
        this.kind = kind;
        this.textureFamily = textureFamily;
        this.textureSize = textureSize;
        this.legacyTextureFamily = legacyTextureFamily;
        this.materialAmountMb = materialAmountMb;
        this.forgeValue = forgeValue;
        this.coldForgePart = coldForgePart;
    }

    MaterialPart(MaterialPart cold) {
        this(cold, cold.textureSize);
    }

    MaterialPart(MaterialPart cold, String textureSizeOverride) {
        this("hot_" + cold.id, "Hot " + cold.displayName, Kind.ITEM,
                cold.textureFamily, textureSizeOverride, cold.legacyTextureFamily,
                cold.materialAmountMb, cold.forgeValue, cold);
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
    public int materialAmountMb() { return materialAmountMb; }
    public int forgeValue() { return forgeValue; }
    public boolean hasExplicitMaterialAmount() { return materialAmountMb > 0; }
    public boolean hasForgeValue() { return forgeValue >= 0; }
    public boolean isHotForgePart() { return coldForgePart != null; }
    public MaterialPart coldForgePart() { return coldForgePart == null ? this : coldForgePart; }

    /** Returns the generated hot counterpart for this cold form, or null when none exists. */
    public MaterialPart hotForgePart() {
        if (coldForgePart != null) return this;
        for (MaterialPart candidate : values()) {
            if (candidate.coldForgePart == this) return candidate;
        }
        return null;
    }

    /** A part participates in manual anvil forging only when amount, shape number and hot form all exist. */
    public boolean isForgeableForm() {
        return hasExplicitMaterialAmount() && hasForgeValue() && hotForgePart() != null;
    }

    /** Components awaiting explicit assembly bills have no generic pure-material processing. */
    public boolean isMachineComponent() {
        return switch (this) {
            case TINY_MOTOR_ARMATURE, SMALL_MOTOR_ARMATURE, MOTOR_ARMATURE, LARGE_MOTOR_ARMATURE, HUGE_MOTOR_ARMATURE, TINY_MOTOR_COIL, SMALL_MOTOR_COIL, MOTOR_COIL, LARGE_MOTOR_COIL, HUGE_MOTOR_COIL, TINY_MOTOR_HOUSING, SMALL_MOTOR_HOUSING, MOTOR_HOUSING, LARGE_MOTOR_HOUSING, HUGE_MOTOR_HOUSING, TINY_MOTOR_SHAFT, SMALL_MOTOR_SHAFT, MOTOR_SHAFT, LARGE_MOTOR_SHAFT, HUGE_MOTOR_SHAFT, TINY_PISTON_ROD, SMALL_PISTON_ROD, PISTON_ROD, LARGE_PISTON_ROD, HUGE_PISTON_ROD, TINY_PUMP_IMPELLER, SMALL_PUMP_IMPELLER, PUMP_IMPELLER, LARGE_PUMP_IMPELLER, HUGE_PUMP_IMPELLER -> true;
            default -> false;
        };
    }

    public String registryName(IndustrialMaterial material) {
        return switch (this) {
            case UNFIRED_BRICK -> "unfired_" + material.id() + "_brick";
            case DRIED_UNFIRED_BRICK -> "dried_unfired_" + material.id() + "_brick";
            case CRACKED_BRICK -> "cracked_" + material.id() + "_brick";
            default -> material.id() + "_" + id;
        };
    }

    public String registryName(StructureMaterial material) {
        if (material == null) {
            throw new IllegalArgumentException("Structure material cannot be null");
        }
        return switch (this) {
            case STONE, BLOCK -> material.id();
            case COBBLED_STONE -> "cobbled_" + material.id();
            case GRAVEL -> material.id() + "_gravel";
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
        if (this == UNFIRED_BRICK) return "Unfired " + material.displayName() + " Brick";
        if (this == DRIED_UNFIRED_BRICK) return "Dried Unfired " + material.displayName() + " Brick";
        if (this == CRACKED_BRICK) return "Cracked " + material.displayName() + " Brick";
        return material.displayName() + " " + displayName;
    }

    public String readableName(StructureMaterial material) {
        if (material == null) {
            throw new IllegalArgumentException("Structure material cannot be null");
        }
        return switch (this) {
            case STONE, BLOCK -> material.displayName();
            case COBBLED_STONE -> "Cobbled " + material.displayName();
            case GRAVEL -> material.displayName() + " Gravel";
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
