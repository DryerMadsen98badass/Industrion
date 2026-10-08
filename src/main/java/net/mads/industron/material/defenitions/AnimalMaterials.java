package net.mads.industron.material.defenitions;

import net.mads.industron.material.organism.*;
import net.mads.industron.fluid.IndustrialFluid;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import static net.mads.industron.material.defenitions.OrganicMaterials.*;
import static net.mads.industron.material.defenitions.IndustrialMaterials.*;
import static net.mads.industron.material.organism.BiologicalMaterial.biological;

/** Canonical shared source materials. Existing items never acquire a different identity per mob. */
public final class AnimalMaterials {
    private AnimalMaterials() {}
    // Existing vanilla water, no second water fluid or bucket is registered.
    public static final IndustrialFluid WATER = new IndustrialFluid("water", "Water", 0x3F76E4,
            IndustrialFluid.Kind.LIQUID, 300, 1000, 1000, 0, Optional.empty(), 0,
            List.of(component(VERNIUM,2),component(DRAXIL,1)),
            Optional.of(ResourceLocation.withDefaultNamespace("water")));
    // Draxite is the existing low-tier mineral (Draxil + Cevora), not a rare metal source.
    public static final BiologicalMaterial BONE_MINERAL = biological("bone_mineral", "Bone Mineral")
            .color(0xDDD8C7)
            .roles(OrganicRole.MINERAL)
            .contains(component(MineralDustMaterials.DRAXITE,1))
            .build();
    public static final BiologicalMaterial FLESH = biological("flesh", "Flesh")
            .color(0xA76558)
            .contains(component(MUSCLE_PROTEIN,5),component(COLLAGEN,1),component(ANIMAL_FAT,2),component(WATER,8))
            .build();
    public static final BiologicalMaterial BONE = biological("bone", "Bone")
            .color(0xE5DDC5)
            .contains(component(BONE_MINERAL,3),component(COLLAGEN,1))
            .build();
    public static final BiologicalMaterial HIDE = biological("hide", "Hide")
            .color(0x967454)
            .contains(component(COLLAGEN,6),component(FUR_FIBER,2),component(ANIMAL_FAT,1),component(WATER,3))
            .build();
    public static final BiologicalMaterial LEATHER = biological("leather", "Leather")
            .color(0x986B43)
            .contains(component(COLLAGEN,6),component(ANIMAL_FAT,1))
            .build();
    public static final BiologicalMaterial WOOL = biological("wool", "Wool")
            .color(0xEEEADD)
            .roles(OrganicRole.FIBER)
            .contains(component(KERATIN,9),component(WOOL_FAT,1))
            .build();
    public static final BiologicalMaterial EYE = biological("eye", "Eye")
            .color(0x9E4C4C)
            .contains(component(MUSCLE_PROTEIN,3),component(COLLAGEN,1),component(WATER,5),component(TOXIN,1))
            .build();
    public static final BiologicalMaterial INK_SAC = biological("ink_sac", "Ink Sac")
            .color(0x353440)
            .contains(component(INK_PIGMENT,2),component(COLLAGEN,1),component(WATER,5))
            .build();
    public static final BiologicalMaterial MEMBRANE = biological("membrane", "Membrane")
            .color(0xB6B69D)
            .contains(component(COLLAGEN,3),component(ANIMAL_FAT,1),component(WATER,4))
            .build();
    public static final BiologicalMaterial SLIME = biological("slime", "Slime")
            .color(0x78A75B)
            .contains(component(SLIME_COMPOUND,3),component(WATER,1))
            .build();
    public static final List<BiologicalMaterial> ALL = List.of(BONE_MINERAL,FLESH,BONE,HIDE,LEATHER,WOOL,EYE,INK_SAC,MEMBRANE,SLIME);
}
