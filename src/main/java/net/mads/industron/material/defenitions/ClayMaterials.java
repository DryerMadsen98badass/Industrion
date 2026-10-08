package net.mads.industron.material.defenitions;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.machine.MachineTier;

import java.util.List;

import static net.mads.industron.material.defenitions.IndustrialMaterials.*;
import static net.mads.industron.material.defenitions.MineralDustMaterials.*;
import static net.mads.industron.material.defenitions.StoneMaterials.*;

/** Clay catalogue. Forms stay fixed; ceramic gameplay properties are resolved from the explicit clay tier. */
public final class ClayMaterials {

    /*
     * ULV Foundry clay
     * Max operating temperature: 578 °C
     */
    public static final IndustrialMaterial AERUM = clay("aerum", "Aerum")
            .tier(MachineTier.ULV)
            .contains(
                    component(ASURINE, 3),
                    component(VERNALITE, 2),
                    component(KAVRITE, 1)
            )
            .build();

    /*
     * LV Foundry clay
     * Max operating temperature: 1000 °C
     */
    public static final IndustrialMaterial CLAY = clay("clay", "Clay")
            .tier(MachineTier.LV)
            .contains(
                    component(ANDESITE, 1),
                    component(EVORINITE, 2),
                    component(CROVIXITE, 4)
            )
            .existing(MaterialPart.CLAY, "minecraft:clay_ball")
            .existing(MaterialPart.CLAY_BLOCK, "minecraft:clay")
            .existing(MaterialPart.BRICK, "minecraft:brick")
            .existing(MaterialPart.BRICKS, "minecraft:bricks")
            .existing(MaterialPart.BRICK_SLAB, "minecraft:brick_slab")
            .existing(MaterialPart.BRICK_STAIRS, "minecraft:brick_stairs")
            .existing(MaterialPart.BRICK_WALL, "minecraft:brick_wall")
            .build();

    /*
     * MV Foundry clay
     * Max operating temperature: 1500 °C
     */
    public static final IndustrialMaterial MADELYX = clay("madelyx", "Madelyx")
            .tier(MachineTier.MV)
            .contains(
                    component(STONE, 1),
                    component(MADSIITE, 4),
                    component(JELYXITE, 2)
            )
            .build();

    /*
     * HV Foundry clay
     * Max operating temperature: 2000 °C
     */
    public static final IndustrialMaterial RASKALT = clay("raskalt", "Raskalt")
            .tier(MachineTier.HV)
            .contains(
                    component(BASALT, 5),
                    component(RASKORITE, 1),
                    component(MADSIITE, 1)
            )
            .build();

    /*
     * EV Foundry clay
     * Max operating temperature: 2500 °C
     */
    public static final IndustrialMaterial AEVDROX = clay("aevdrox", "Aevdrox")
            .tier(MachineTier.EV)
            .contains(
                    component(SCORIA, 2),
                    component(AEVRITE, 3),
                    component(DOVREXITE, 3)
            )
            .build();


    /**
     * Vanilla Nether Brick is treated as a fired ceramic derived directly from Netherrack.
     * It deliberately does not own the normal wet-clay forms. The individual cracked brick is
     * generated; Nether Bricks / Cracked Nether Bricks blocks are assembled by private recipes.
     */
    public static final IndustrialMaterial NETHER = clay("nether", "Nether")
            .tier(MachineTier.IV)
            .contains(
                    component(NETHERRACK, 1)
            )
            .existing(MaterialPart.BRICK, "minecraft:nether_brick")
            .existing(MaterialPart.BRICK_STAIRS, "minecraft:nether_brick_stairs")
            .existing(MaterialPart.BRICK_SLAB, "minecraft:nether_brick_slab")
            .existing(MaterialPart.BRICKS, "minecraft:nether_bricks")
            .existing(MaterialPart.BRICK_WALL, "minecraft:nether_brick_wall")
            .build();

    public static final List<IndustrialMaterial> ALL = List.of(
            AERUM,
            CLAY,
            MADELYX,
            RASKALT,
            AEVDROX,
            NETHER
    );

    private ClayMaterials() {
    }

    public static void init() {
        // Touching this class initializes and registers its definitions.
    }
}