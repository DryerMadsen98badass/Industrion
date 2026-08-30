package net.mads.industron.material.defenitions;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialOrePolicy;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.StoneModel;

import java.util.List;

import static net.mads.industron.material.defenitions.IndustrialMaterials.component;
import static net.mads.industron.material.defenitions.MineralDustMaterials.*;

public final class StoneMaterials {
    // Colors are tint anchors for the grayscale structure templates, not flat texture averages.
    public static final StoneMaterial STONE = stone(
            "stone",
            "Stone",
            0x767676,
            StoneModel.STONE
    )
            .contains(component(VERNALITE, 2), component(DRAXITE, 1), component(ELNARITE, 1), component(FYRALITE, 1), component(JORVITE, 1), component(KAVRITE, 1), component(RELYXITE, 1), component(SENVRAITE, 1), component(TALYXITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.STONE, "minecraft:stone")
            .existing(MaterialPart.SLAB, "minecraft:stone_slab")
            .existing(MaterialPart.STAIRS, "minecraft:stone_stairs")
            .existing(MaterialPart.BUTTON, "minecraft:stone_button")
            .existing(MaterialPart.PRESSURE_PLATE, "minecraft:stone_pressure_plate")
            .existing(MaterialPart.COBBLED_STONE, "minecraft:cobblestone")
            .existing(MaterialPart.COBBLED_SLAB, "minecraft:cobblestone_slab")
            .existing(MaterialPart.COBBLED_STAIRS, "minecraft:cobblestone_stairs")
            .existing(MaterialPart.COBBLED_WALL, "minecraft:cobblestone_wall")
            .existing(MaterialPart.STONE_BRICKS, "minecraft:stone_bricks")
            .existing(MaterialPart.STONE_BRICK_SLAB, "minecraft:stone_brick_slab")
            .existing(MaterialPart.STONE_BRICK_STAIRS, "minecraft:stone_brick_stairs")
            .existing(MaterialPart.STONE_BRICK_WALL, "minecraft:stone_brick_wall")
            .existing(MaterialPart.CRACKED_STONE_BRICKS, "minecraft:cracked_stone_bricks")
            .existing(MaterialPart.CHISELED_STONE_BRICKS, "minecraft:chiseled_stone_bricks")
            .existing(MaterialPart.SMOOTH_STONE, "minecraft:smooth_stone")
            .existing(MaterialPart.SMOOTH_STONE_SLAB, "minecraft:smooth_stone_slab");

    public static final StoneMaterial ANDESITE = stone(
            "andesite",
            "Andesite",
            0x808080,
            StoneModel.ANDESITE
    )
            .contains(component(KAVRITE, 2), component(LORYXITE, 2), component(VASKYRITE, 1), component(USKARITE, 1), component(DRAXITE, 1), component(JORVITE, 1), component(FYRALITE, 1), component(TALYXITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.COBBLED_STONE, "minecraft:andesite")
            .existing(MaterialPart.COBBLED_SLAB, "minecraft:andesite_slab")
            .existing(MaterialPart.COBBLED_STAIRS, "minecraft:andesite_stairs")
            .existing(MaterialPart.COBBLED_WALL, "minecraft:andesite_wall")
            .existing(MaterialPart.POLISHED_STONE, "minecraft:polished_andesite")
            .existing(MaterialPart.POLISHED_SLAB, "minecraft:polished_andesite_slab")
            .existing(MaterialPart.POLISHED_STAIRS, "minecraft:polished_andesite_stairs")
            .existing(MaterialPart.STONE, "create:cut_andesite")
            .existing(MaterialPart.SLAB, "create:cut_andesite_slab")
            .existing(MaterialPart.STAIRS, "create:cut_andesite_stairs")
            .existing(MaterialPart.WALL, "create:cut_andesite_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_andesite")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_andesite_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_andesite_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_andesite_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_andesite_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_andesite_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_andesite_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_andesite_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_andesite_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_andesite_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_andesite_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_andesite_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_andesite")
            .existing(MaterialPart.PILLAR, "create:andesite_pillar")
            .without(MaterialPart.CUT_STONE);

    public static final StoneMaterial ASURINE = stone(
            "asurine",
            "Asurine",
            0x668EA5,
            StoneModel.ASURINE
    )
            .contains(component(WELYRITE, 2), component(XAVRITE, 1), component(CYVERITE, 1), component(GORVIXITE, 1), component(DRAXITE, 1), component(SENVRAITE, 1), component(AULVENITE, 1), component(HORYXITE, 1), component(KELYRITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.COBBLED_STONE, "create:asurine")
            .existing(MaterialPart.STONE, "create:cut_asurine")
            .existing(MaterialPart.SLAB, "create:cut_asurine_slab")
            .existing(MaterialPart.STAIRS, "create:cut_asurine_stairs")
            .existing(MaterialPart.WALL, "create:cut_asurine_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_asurine")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_asurine_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_asurine_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_asurine_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_asurine_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_asurine_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_asurine_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_asurine_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_asurine_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_asurine_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_asurine_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_asurine_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_asurine")
            .existing(MaterialPart.PILLAR, "create:asurine_pillar")
            .without(MaterialPart.CUT_STONE);

    public static final StoneMaterial BASALT = stone(
            "basalt",
            "Basalt",
            0x7F7D83,
            StoneModel.BASALT
    )
            .contains(component(SORYNITE, 2), component(AEVRITE, 1), component(BRALYXITE, 1), component(MORYXITE, 1), component(NAXIRITE, 1), component(HAVORITE, 1), component(KORVENITE, 1), component(PRYVENITE, 1), component(DOVREXITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.NETHER)
            .existing(MaterialPart.COBBLED_STONE, "minecraft:basalt")
            .existing(MaterialPart.POLISHED_STONE, "minecraft:polished_basalt")
            .existing(MaterialPart.STONE, "minecraft:smooth_basalt")
            .without(MaterialPart.SLAB)
            .without(MaterialPart.STAIRS)
            .without(MaterialPart.WALL)
            .without(MaterialPart.SMOOTH_STONE);

    public static final StoneMaterial BLACKSTONE = stone(
            "blackstone",
            "Blackstone",
            0x8D798A,
            StoneModel.BLACKSTONE
    )
            .contains(component(RASKORITE, 2), component(CIRYNITE, 1), component(DOVREXITE, 1), component(MORYXITE, 1), component(NAXIRITE, 1), component(OVELYNITE, 1), component(JELYXITE, 1), component(TAVORITE, 1), component(PRYVENITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.NETHER)
            .existing(MaterialPart.COBBLED_STONE, "minecraft:blackstone")
            .existing(MaterialPart.COBBLED_SLAB, "minecraft:blackstone_slab")
            .existing(MaterialPart.COBBLED_STAIRS, "minecraft:blackstone_stairs")
            .existing(MaterialPart.COBBLED_WALL, "minecraft:blackstone_wall")
            .existing(MaterialPart.POLISHED_STONE, "minecraft:polished_blackstone")
            .existing(MaterialPart.POLISHED_SLAB, "minecraft:polished_blackstone_slab")
            .existing(MaterialPart.POLISHED_STAIRS, "minecraft:polished_blackstone_stairs")
            .existing(MaterialPart.POLISHED_WALL, "minecraft:polished_blackstone_wall")
            .existing(MaterialPart.BUTTON, "minecraft:polished_blackstone_button")
            .existing(MaterialPart.PRESSURE_PLATE, "minecraft:polished_blackstone_pressure_plate")
            .existing(MaterialPart.POLISHED_STONE_BRICKS, "minecraft:polished_blackstone_bricks")
            .existing(MaterialPart.POLISHED_STONE_BRICK_SLAB, "minecraft:polished_blackstone_brick_slab")
            .existing(MaterialPart.POLISHED_STONE_BRICK_STAIRS, "minecraft:polished_blackstone_brick_stairs")
            .existing(MaterialPart.POLISHED_STONE_BRICK_WALL, "minecraft:polished_blackstone_brick_wall")
            .existing(MaterialPart.CRACKED_POLISHED_STONE_BRICKS, "minecraft:cracked_polished_blackstone_bricks")
            .existing(MaterialPart.CHISELED_POLISHED_STONE, "minecraft:chiseled_polished_blackstone")
            .existing(MaterialPart.GILDED_STONE, "minecraft:gilded_blackstone")
            .without(MaterialPart.STONE);

    public static final StoneMaterial CALCITE = stone(
            "calcite",
            "Calcite",
            0x80817E,
            StoneModel.CALCITE
    )
            .contains(component(RELYXITE, 2), component(VERNALITE, 2), component(WELYRITE, 1), component(EVORINITE, 1), component(DASKENITE, 1), component(ELNARITE, 1), component(VASKYRITE, 1), component(HORYXITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.COBBLED_STONE, "minecraft:calcite")
            .existing(MaterialPart.STONE, "create:cut_calcite")
            .existing(MaterialPart.SLAB, "create:cut_calcite_slab")
            .existing(MaterialPart.STAIRS, "create:cut_calcite_stairs")
            .existing(MaterialPart.WALL, "create:cut_calcite_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_calcite")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_calcite_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_calcite_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_calcite_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_calcite_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_calcite_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_calcite_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_calcite_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_calcite_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_calcite_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_calcite_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_calcite_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_calcite")
            .existing(MaterialPart.PILLAR, "create:calcite_pillar")
            .without(MaterialPart.CUT_STONE);

    public static final StoneMaterial CRIMSITE = stone(
            "crimsite",
            "Crimsite",
            0xC76C68,
            StoneModel.CRIMSITE
    )
            .contains(component(GORVIXITE, 2), component(KELYRITE, 1), component(XAVRITE, 1), component(ZORIXITE, 1), component(DASKENITE, 1), component(FYRALITE, 1), component(USKARITE, 1), component(HORYXITE, 1), component(CYVERITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.COBBLED_STONE, "create:crimsite")
            .existing(MaterialPart.STONE, "create:cut_crimsite")
            .existing(MaterialPart.SLAB, "create:cut_crimsite_slab")
            .existing(MaterialPart.STAIRS, "create:cut_crimsite_stairs")
            .existing(MaterialPart.WALL, "create:cut_crimsite_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_crimsite")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_crimsite_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_crimsite_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_crimsite_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_crimsite_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_crimsite_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_crimsite_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_crimsite_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_crimsite_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_crimsite_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_crimsite_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_crimsite_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_crimsite")
            .existing(MaterialPart.PILLAR, "create:crimsite_pillar")
            .without(MaterialPart.CUT_STONE);

    public static final StoneMaterial DEEPSLATE = stone(
            "deepslate",
            "Deepslate",
            0x7F7F82,
            StoneModel.DEEPSLATE
    )
            .contains(component(DASKENITE, 2), component(GORVIXITE, 2), component(EVORINITE, 1), component(FALYXITE, 1), component(GRAVENITE, 1), component(KELYRITE, 1), component(WELYRITE, 1), component(AULVENITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.STONE, "minecraft:deepslate")
            .existing(MaterialPart.COBBLED_STONE, "minecraft:cobbled_deepslate")
            .existing(MaterialPart.COBBLED_SLAB, "minecraft:cobbled_deepslate_slab")
            .existing(MaterialPart.COBBLED_STAIRS, "minecraft:cobbled_deepslate_stairs")
            .existing(MaterialPart.COBBLED_WALL, "minecraft:cobbled_deepslate_wall")
            .existing(MaterialPart.POLISHED_STONE, "minecraft:polished_deepslate")
            .existing(MaterialPart.POLISHED_SLAB, "minecraft:polished_deepslate_slab")
            .existing(MaterialPart.POLISHED_STAIRS, "minecraft:polished_deepslate_stairs")
            .existing(MaterialPart.POLISHED_WALL, "minecraft:polished_deepslate_wall")
            .existing(MaterialPart.STONE_BRICKS, "minecraft:deepslate_bricks")
            .existing(MaterialPart.STONE_BRICK_SLAB, "minecraft:deepslate_brick_slab")
            .existing(MaterialPart.STONE_BRICK_STAIRS, "minecraft:deepslate_brick_stairs")
            .existing(MaterialPart.STONE_BRICK_WALL, "minecraft:deepslate_brick_wall")
            .existing(MaterialPart.CRACKED_STONE_BRICKS, "minecraft:cracked_deepslate_bricks")
            .existing(MaterialPart.STONE_TILES, "minecraft:deepslate_tiles")
            .existing(MaterialPart.STONE_TILE_SLAB, "minecraft:deepslate_tile_slab")
            .existing(MaterialPart.STONE_TILE_STAIRS, "minecraft:deepslate_tile_stairs")
            .existing(MaterialPart.STONE_TILE_WALL, "minecraft:deepslate_tile_wall")
            .existing(MaterialPart.CRACKED_STONE_TILES, "minecraft:cracked_deepslate_tiles")
            .existing(MaterialPart.CHISELED_STONE, "minecraft:chiseled_deepslate")
            .existing(MaterialPart.CUT_STONE, "create:cut_deepslate")
            .existing(MaterialPart.CUT_STONE_SLAB, "create:cut_deepslate_slab")
            .existing(MaterialPart.CUT_STONE_STAIRS, "create:cut_deepslate_stairs")
            .existing(MaterialPart.CUT_STONE_WALL, "create:cut_deepslate_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_deepslate")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_deepslate_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_deepslate_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_deepslate_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_deepslate_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_deepslate_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_deepslate_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_deepslate_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_deepslate_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_deepslate_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_deepslate_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_deepslate_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_deepslate")
            .existing(MaterialPart.PILLAR, "create:deepslate_pillar");

    public static final StoneMaterial DIORITE = stone(
            "diorite",
            "Diorite",
            0x808080,
            StoneModel.DIORITE
    )
            .contains(component(KELYRITE, 2), component(GORVIXITE, 1), component(CYVERITE, 1), component(HORYXITE, 1), component(EVORINITE, 1), component(WELYRITE, 1), component(JORVITE, 1), component(RELYXITE, 1), component(AULVENITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.COBBLED_STONE, "minecraft:diorite")
            .existing(MaterialPart.COBBLED_SLAB, "minecraft:diorite_slab")
            .existing(MaterialPart.COBBLED_STAIRS, "minecraft:diorite_stairs")
            .existing(MaterialPart.COBBLED_WALL, "minecraft:diorite_wall")
            .existing(MaterialPart.POLISHED_STONE, "minecraft:polished_diorite")
            .existing(MaterialPart.POLISHED_SLAB, "minecraft:polished_diorite_slab")
            .existing(MaterialPart.POLISHED_STAIRS, "minecraft:polished_diorite_stairs")
            .existing(MaterialPart.STONE, "create:cut_diorite")
            .existing(MaterialPart.SLAB, "create:cut_diorite_slab")
            .existing(MaterialPart.STAIRS, "create:cut_diorite_stairs")
            .existing(MaterialPart.WALL, "create:cut_diorite_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_diorite")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_diorite_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_diorite_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_diorite_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_diorite_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_diorite_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_diorite_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_diorite_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_diorite_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_diorite_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_diorite_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_diorite_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_diorite")
            .existing(MaterialPart.PILLAR, "create:diorite_pillar")
            .without(MaterialPart.CUT_STONE);

    public static final StoneMaterial DRIPSTONE = stone(
            "dripstone",
            "Dripstone",
            0x97786A,
            StoneModel.DRIPSTONE
    )
            .contains(component(WELYRITE, 2), component(DASKENITE, 1), component(EVORINITE, 1), component(HORYXITE, 1), component(GRAVENITE, 1), component(RELYXITE, 1), component(VERNALITE, 1), component(ELNARITE, 1), component(VASKYRITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.COBBLED_STONE, "minecraft:dripstone_block")
            .existing(MaterialPart.STONE, "create:cut_dripstone")
            .existing(MaterialPart.SLAB, "create:cut_dripstone_slab")
            .existing(MaterialPart.STAIRS, "create:cut_dripstone_stairs")
            .existing(MaterialPart.WALL, "create:cut_dripstone_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_dripstone")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_dripstone_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_dripstone_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_dripstone_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_dripstone_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_dripstone_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_dripstone_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_dripstone_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_dripstone_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_dripstone_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_dripstone_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_dripstone_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_dripstone")
            .existing(MaterialPart.PILLAR, "create:dripstone_pillar")
            .without(MaterialPart.CUT_STONE);

    public static final StoneMaterial END_STONE = stone(
            "end_stone",
            "End Stone",
            0x83845E,
            StoneModel.END_STONE
    )
            .contains(component(YRYXITE, 2), component(ZORVANITE, 1), component(AXYRITE, 1), component(BELVIXITE, 1), component(GALYTHITE, 1), component(MYRITHITE, 1), component(QEVORITE, 1), component(NUVEXITE, 1), component(XYTHERITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.END)
            .existing(MaterialPart.COBBLED_STONE, "minecraft:end_stone")
            .existing(MaterialPart.STONE_BRICKS, "minecraft:end_stone_bricks")
            .existing(MaterialPart.STONE_BRICK_SLAB, "minecraft:end_stone_brick_slab")
            .existing(MaterialPart.STONE_BRICK_STAIRS, "minecraft:end_stone_brick_stairs")
            .existing(MaterialPart.STONE_BRICK_WALL, "minecraft:end_stone_brick_wall")
            .without(MaterialPart.STONE);

    public static final StoneMaterial GRANITE = stone(
            "granite",
            "Granite",
            0xA6735F,
            StoneModel.GRANITE
    )
            .contains(component(GORVIXITE, 2), component(KELYRITE, 2), component(WELYRITE, 1), component(XAVRITE, 1), component(YSKELITE, 1), component(ZORIXITE, 1), component(AULVENITE, 1), component(CYVERITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.COBBLED_STONE, "minecraft:granite")
            .existing(MaterialPart.COBBLED_SLAB, "minecraft:granite_slab")
            .existing(MaterialPart.COBBLED_STAIRS, "minecraft:granite_stairs")
            .existing(MaterialPart.COBBLED_WALL, "minecraft:granite_wall")
            .existing(MaterialPart.POLISHED_STONE, "minecraft:polished_granite")
            .existing(MaterialPart.POLISHED_SLAB, "minecraft:polished_granite_slab")
            .existing(MaterialPart.POLISHED_STAIRS, "minecraft:polished_granite_stairs")
            .existing(MaterialPart.STONE, "create:cut_granite")
            .existing(MaterialPart.SLAB, "create:cut_granite_slab")
            .existing(MaterialPart.STAIRS, "create:cut_granite_stairs")
            .existing(MaterialPart.WALL, "create:cut_granite_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_granite")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_granite_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_granite_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_granite_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_granite_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_granite_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_granite_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_granite_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_granite_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_granite_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_granite_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_granite_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_granite")
            .existing(MaterialPart.PILLAR, "create:granite_pillar")
            .without(MaterialPart.CUT_STONE);

    public static final StoneMaterial LIMESTONE = stone(
            "limestone",
            "Limestone",
            0x948B6B,
            StoneModel.LIMESTONE
    )
            .contains(component(RELYXITE, 2), component(VERNALITE, 1), component(WELYRITE, 1), component(DASKENITE, 1), component(EVORINITE, 1), component(FYRALITE, 1), component(SENVRAITE, 1), component(VASKYRITE, 1), component(AULVENITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.COBBLED_STONE, "create:limestone")
            .existing(MaterialPart.STONE, "create:cut_limestone")
            .existing(MaterialPart.SLAB, "create:cut_limestone_slab")
            .existing(MaterialPart.STAIRS, "create:cut_limestone_stairs")
            .existing(MaterialPart.WALL, "create:cut_limestone_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_limestone")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_limestone_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_limestone_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_limestone_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_limestone_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_limestone_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_limestone_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_limestone_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_limestone_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_limestone_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_limestone_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_limestone_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_limestone")
            .existing(MaterialPart.PILLAR, "create:limestone_pillar")
            .without(MaterialPart.CUT_STONE);

    public static final StoneMaterial NETHERRACK = stone(
            "netherrack",
            "Netherrack",
            0xDE5757,
            StoneModel.NETHERRACK
    )
            .contains(component(SORYXITE, 2), component(SORYNITE, 1), component(RASKORITE, 1), component(NERYNITE, 1), component(AEVRITE, 1), component(BRALYXITE, 1), component(HAVORITE, 1), component(MORYXITE, 1), component(OVELYNITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.NETHER)
            .existing(MaterialPart.COBBLED_STONE, "minecraft:netherrack")
            .without(MaterialPart.STONE);

    public static final StoneMaterial OCHRUM = stone(
            "ochrum",
            "Ochrum",
            0xB88737,
            StoneModel.OCHRUM
    )
            .contains(component(CYVERITE, 2), component(GORVIXITE, 1), component(WELYRITE, 1), component(HORYXITE, 1), component(GRAVENITE, 1), component(FYRALITE, 1), component(TALYXITE, 1), component(KELYRITE, 1), component(XAVRITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.COBBLED_STONE, "create:ochrum")
            .existing(MaterialPart.STONE, "create:cut_ochrum")
            .existing(MaterialPart.SLAB, "create:cut_ochrum_slab")
            .existing(MaterialPart.STAIRS, "create:cut_ochrum_stairs")
            .existing(MaterialPart.WALL, "create:cut_ochrum_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_ochrum")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_ochrum_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_ochrum_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_ochrum_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_ochrum_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_ochrum_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_ochrum_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_ochrum_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_ochrum_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_ochrum_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_ochrum_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_ochrum_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_ochrum")
            .existing(MaterialPart.PILLAR, "create:ochrum_pillar")
            .without(MaterialPart.CUT_STONE);

    public static final StoneMaterial RED_SANDSTONE = stone(
            "red_sandstone",
            "Red Sandstone",
            0xCB6C20,
            StoneModel.RED_SANDSTONE
    )
            .contains(component(TALYXITE, 2), component(FYRALITE, 2), component(VERNALITE, 1), component(AULVENITE, 1), component(WELYRITE, 1), component(DASKENITE, 1), component(USKARITE, 1), component(SENVRAITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.STONE, "minecraft:red_sandstone")
            .existing(MaterialPart.SLAB, "minecraft:red_sandstone_slab")
            .existing(MaterialPart.STAIRS, "minecraft:red_sandstone_stairs")
            .existing(MaterialPart.WALL, "minecraft:red_sandstone_wall")
            .existing(MaterialPart.CUT_STONE, "minecraft:cut_red_sandstone")
            .existing(MaterialPart.CUT_STONE_SLAB, "minecraft:cut_red_sandstone_slab")
            .existing(MaterialPart.CHISELED_STONE, "minecraft:chiseled_red_sandstone")
            .existing(MaterialPart.SMOOTH_STONE, "minecraft:smooth_red_sandstone")
            .existing(MaterialPart.SMOOTH_STONE_SLAB, "minecraft:smooth_red_sandstone_slab")
            .existing(MaterialPart.SMOOTH_STONE_STAIRS, "minecraft:smooth_red_sandstone_stairs")
            .without(MaterialPart.COBBLED_STONE);

    public static final StoneMaterial SANDSTONE = stone(
            "sandstone",
            "Sandstone",
            0x898162,
            StoneModel.SANDSTONE
    )
            .contains(component(VERNALITE, 2), component(JORVITE, 1), component(KAVRITE, 1), component(LORYXITE, 1), component(RELYXITE, 1), component(SENVRAITE, 1), component(TALYXITE, 1), component(USKARITE, 1), component(VASKYRITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.STONE, "minecraft:sandstone")
            .existing(MaterialPart.SLAB, "minecraft:sandstone_slab")
            .existing(MaterialPart.STAIRS, "minecraft:sandstone_stairs")
            .existing(MaterialPart.WALL, "minecraft:sandstone_wall")
            .existing(MaterialPart.CUT_STONE, "minecraft:cut_sandstone")
            .existing(MaterialPart.CUT_STONE_SLAB, "minecraft:cut_sandstone_slab")
            .existing(MaterialPart.CHISELED_STONE, "minecraft:chiseled_sandstone")
            .existing(MaterialPart.SMOOTH_STONE, "minecraft:smooth_sandstone")
            .existing(MaterialPart.SMOOTH_STONE_SLAB, "minecraft:smooth_sandstone_slab")
            .existing(MaterialPart.SMOOTH_STONE_STAIRS, "minecraft:smooth_sandstone_stairs")
            .without(MaterialPart.COBBLED_STONE);

    public static final StoneMaterial SCORCHIA = stone(
            "scorchia",
            "Scorchia",
            0x897F7C,
            StoneModel.SCORCHIA
    )
            .contains(component(SORYXITE, 2), component(MORYXITE, 2), component(RHELIXITE, 1), component(SYVRENITE, 1), component(TYRAXITE, 1), component(IXRANITE, 1), component(TAVORITE, 1), component(PRYVENITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.NETHER)
            .existing(MaterialPart.COBBLED_STONE, "create:scorchia")
            .existing(MaterialPart.STONE, "create:cut_scorchia")
            .existing(MaterialPart.SLAB, "create:cut_scorchia_slab")
            .existing(MaterialPart.STAIRS, "create:cut_scorchia_stairs")
            .existing(MaterialPart.WALL, "create:cut_scorchia_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_scorchia")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_scorchia_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_scorchia_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_scorchia_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_scorchia_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_scorchia_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_scorchia_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_scorchia_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_scorchia_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_scorchia_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_scorchia_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_scorchia_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_scorchia")
            .existing(MaterialPart.PILLAR, "create:scorchia_pillar")
            .without(MaterialPart.CUT_STONE);

    public static final StoneMaterial SCORIA = stone(
            "scoria",
            "Scoria",
            0xA46E5C,
            StoneModel.SCORIA
    )
            .contains(component(NERYNITE, 2), component(AEVRITE, 1), component(CIRYNITE, 1), component(QYXARITE, 1), component(RHELIXITE, 1), component(SYVRENITE, 1), component(HAVORITE, 1), component(JELYXITE, 1), component(NAXIRITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.NETHER)
            .existing(MaterialPart.COBBLED_STONE, "create:scoria")
            .existing(MaterialPart.STONE, "create:cut_scoria")
            .existing(MaterialPart.SLAB, "create:cut_scoria_slab")
            .existing(MaterialPart.STAIRS, "create:cut_scoria_stairs")
            .existing(MaterialPart.WALL, "create:cut_scoria_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_scoria")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_scoria_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_scoria_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_scoria_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_scoria_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_scoria_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_scoria_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_scoria_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_scoria_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_scoria_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_scoria_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_scoria_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_scoria")
            .existing(MaterialPart.PILLAR, "create:scoria_pillar")
            .without(MaterialPart.CUT_STONE);

    public static final StoneMaterial TUFF = stone(
            "tuff",
            "Tuff",
            0x808179,
            StoneModel.TUFF
    )
            .contains(component(MADSIITE, 2), component(HAVORITE, 2), component(IXRANITE, 1), component(GORVIXITE, 1), component(KELYRITE, 1), component(WELYRITE, 1), component(DASKENITE, 1), component(CYVERITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.COBBLED_STONE, "minecraft:tuff")
            .existing(MaterialPart.COBBLED_SLAB, "minecraft:tuff_slab")
            .existing(MaterialPart.COBBLED_STAIRS, "minecraft:tuff_stairs")
            .existing(MaterialPart.COBBLED_WALL, "minecraft:tuff_wall")
            .existing(MaterialPart.POLISHED_STONE, "minecraft:polished_tuff")
            .existing(MaterialPart.POLISHED_SLAB, "minecraft:polished_tuff_slab")
            .existing(MaterialPart.POLISHED_STAIRS, "minecraft:polished_tuff_stairs")
            .existing(MaterialPart.POLISHED_WALL, "minecraft:polished_tuff_wall")
            .existing(MaterialPart.STONE_BRICKS, "minecraft:tuff_bricks")
            .existing(MaterialPart.STONE_BRICK_SLAB, "minecraft:tuff_brick_slab")
            .existing(MaterialPart.STONE_BRICK_STAIRS, "minecraft:tuff_brick_stairs")
            .existing(MaterialPart.STONE_BRICK_WALL, "minecraft:tuff_brick_wall")
            .existing(MaterialPart.CHISELED_STONE, "minecraft:chiseled_tuff")
            .existing(MaterialPart.CHISELED_STONE_BRICKS, "minecraft:chiseled_tuff_bricks")
            .existing(MaterialPart.STONE, "create:cut_tuff")
            .existing(MaterialPart.SLAB, "create:cut_tuff_slab")
            .existing(MaterialPart.STAIRS, "create:cut_tuff_stairs")
            .existing(MaterialPart.WALL, "create:cut_tuff_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_tuff")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_tuff_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_tuff_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_tuff_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_tuff_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_tuff_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_tuff_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_tuff_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_tuff_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_tuff_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_tuff_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_tuff_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_tuff")
            .existing(MaterialPart.PILLAR, "create:tuff_pillar")
            .without(MaterialPart.CUT_STONE);

    public static final StoneMaterial VERIDIUM = stone(
            "veridium",
            "Veridium",
            0x679681,
            StoneModel.VERIDIUM
    )
            .contains(component(WELYRITE, 2), component(XAVRITE, 1), component(ZORIXITE, 1), component(CYVERITE, 1), component(GORVIXITE, 1), component(KELYRITE, 1), component(AULVENITE, 1), component(EVORINITE, 1), component(HORYXITE, 1))
            .dimension(MaterialOrePolicy.DimensionBand.OVERWORLD)
            .existing(MaterialPart.COBBLED_STONE, "create:veridium")
            .existing(MaterialPart.STONE, "create:cut_veridium")
            .existing(MaterialPart.SLAB, "create:cut_veridium_slab")
            .existing(MaterialPart.STAIRS, "create:cut_veridium_stairs")
            .existing(MaterialPart.WALL, "create:cut_veridium_wall")
            .existing(MaterialPart.POLISHED_CUT_STONE, "create:polished_cut_veridium")
            .existing(MaterialPart.POLISHED_CUT_STONE_SLAB, "create:polished_cut_veridium_slab")
            .existing(MaterialPart.POLISHED_CUT_STONE_STAIRS, "create:polished_cut_veridium_stairs")
            .existing(MaterialPart.POLISHED_CUT_STONE_WALL, "create:polished_cut_veridium_wall")
            .existing(MaterialPart.CUT_STONE_BRICKS, "create:cut_veridium_bricks")
            .existing(MaterialPart.CUT_STONE_BRICK_SLAB, "create:cut_veridium_brick_slab")
            .existing(MaterialPart.CUT_STONE_BRICK_STAIRS, "create:cut_veridium_brick_stairs")
            .existing(MaterialPart.CUT_STONE_BRICK_WALL, "create:cut_veridium_brick_wall")
            .existing(MaterialPart.SMALL_STONE_BRICKS, "create:small_veridium_bricks")
            .existing(MaterialPart.SMALL_STONE_BRICK_SLAB, "create:small_veridium_brick_slab")
            .existing(MaterialPart.SMALL_STONE_BRICK_STAIRS, "create:small_veridium_brick_stairs")
            .existing(MaterialPart.SMALL_STONE_BRICK_WALL, "create:small_veridium_brick_wall")
            .existing(MaterialPart.LAYERED_STONE, "create:layered_veridium")
            .existing(MaterialPart.PILLAR, "create:veridium_pillar")
            .without(MaterialPart.CUT_STONE);
    public static final List<StoneMaterial> ALL = List.of(
            STONE,
            ANDESITE,
            ASURINE,
            BASALT,
            BLACKSTONE,
            CALCITE,
            CRIMSITE,
            DEEPSLATE,
            DIORITE,
            DRIPSTONE,
            END_STONE,
            GRANITE,
            LIMESTONE,
            NETHERRACK,
            OCHRUM,
            RED_SANDSTONE,
            SANDSTONE,
            SCORCHIA,
            SCORIA,
            TUFF,
            VERIDIUM
    );

    private StoneMaterials() {
    }

    public static StoneMaterial stone(String id, String displayName, int color, StoneModel model) {
        return new StoneMaterial(id, displayName, color, model);
    }
}
