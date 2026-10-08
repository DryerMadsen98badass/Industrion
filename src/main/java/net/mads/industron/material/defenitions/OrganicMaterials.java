package net.mads.industron.material.defenitions;

import net.mads.industron.material.organic.OrganicMaterial;

import java.util.List;
import net.mads.industron.material.organism.BiologicalMaterial;
import net.mads.industron.material.organism.OrganicRole;
import static net.mads.industron.material.organism.BiologicalMaterial.biological;
import static net.mads.industron.material.defenitions.IndustrialMaterials.*;

/** Existing organic materials that are not themselves plants but require explicit composition. */
public final class OrganicMaterials {
    /**
     * Carbon-rich primitive fuel. Its future production route must conserve the source wood/plant
     * material by emitting the non-charcoal fractions as balanced byproducts; this definition only
     * states what one charcoal material unit is, not how it is manufactured.
     */
    public static final OrganicMaterial CHARCOAL = OrganicMaterial.organic("charcoal", "Charcoal")
            .existing("minecraft:charcoal")
            .contains(component(CompoundMaterials.RESYRA, 1))
            .build();

    public static final List<OrganicMaterial> ALL = List.of(CHARCOAL);


    // Shared biological definitions. BiologicalItemCatalog registers their permanent item forms.
    public static final BiologicalMaterial MUSCLE_PROTEIN = biological("muscle_protein", "Muscle Protein")
            .color(0xD8C2AA)
            .roles(OrganicRole.PROTEIN)
            .contains(component(REDSTONE, 5), component(DRAXIL, 4), component(CEVORA, 1))
            .build();

    public static final BiologicalMaterial COLLAGEN = biological("collagen", "Collagen")
            .color(0xE4D6BB)
            .roles(OrganicRole.PROTEIN)
            .contains(component(REDSTONE, 7), component(DRAXIL, 5), component(CEVORA, 2))
            .build();

    public static final BiologicalMaterial KERATIN = biological("keratin", "Keratin")
            .color(0xDFD2B7)
            .roles(OrganicRole.PROTEIN)
            .contains(component(REDSTONE, 5), component(DRAXIL, 3), component(CEVORA, 2))
            .build();

    public static final BiologicalMaterial SILK_PROTEIN = biological("silk_protein", "Silk Protein")
            .color(0xE6E2D6)
            .roles(OrganicRole.PROTEIN)
            .contains(component(REDSTONE, 6), component(DRAXIL, 5), component(CEVORA, 2))
            .build();

    public static final BiologicalMaterial MILK_PROTEIN = biological("milk_protein", "Milk Protein")
            .color(0xF2EDDC)
            .roles(OrganicRole.PROTEIN)
            .contains(component(REDSTONE, 4), component(DRAXIL, 4), component(CEVORA, 1))
            .build();

    public static final BiologicalMaterial EGG_PROTEIN = biological("egg_protein", "Egg Protein")
            .color(0xEEE2BD)
            .roles(OrganicRole.PROTEIN)
            .contains(component(REDSTONE, 5), component(DRAXIL, 5), component(CEVORA, 2))
            .build();

    public static final BiologicalMaterial ANIMAL_FAT = biological("animal_fat", "Animal Fat")
            .color(0xEEE0B5)
            .roles(OrganicRole.FAT)
            .contains(component(REDSTONE, 3), component(DRAXIL, 1))
            .build();

    public static final BiologicalMaterial MILK_FAT = biological("milk_fat", "Milk Fat")
            .color(0xF1DE9D)
            .roles(OrganicRole.FAT)
            .contains(component(REDSTONE, 5), component(DRAXIL, 2))
            .build();

    public static final BiologicalMaterial WOOL_FAT = biological("wool_fat", "Wool Fat")
            .color(0xC9B674)
            .roles(OrganicRole.FAT)
            .contains(component(REDSTONE, 7), component(DRAXIL, 2))
            .build();

    public static final BiologicalMaterial WAX = biological("wax", "Wax")
            .color(0xE3BA63)
            .roles(OrganicRole.WAX)
            .contains(component(REDSTONE, 9), component(DRAXIL, 2))
            .build();

    public static final BiologicalMaterial SLIME_COMPOUND = biological("slime_compound", "Slime Compound")
            .color(0x77A45E)
            .contains(component(REDSTONE, 3), component(DRAXIL, 4), component(CEVORA, 1))
            .build();

    public static final BiologicalMaterial INK_PIGMENT = biological("ink_pigment", "Ink Pigment")
            .color(0x272735)
            .roles(OrganicRole.PIGMENT)
            .contains(component(REDSTONE, 8), component(DRAXIL, 3), component(CEVORA, 1))
            .build();

    public static final BiologicalMaterial TOXIN = biological("toxin", "Toxin")
            .color(0x779944)
            .roles(OrganicRole.TOXIN)
            .contains(component(REDSTONE, 4), component(DRAXIL, 3), component(CEVORA, 2))
            .build();

    public static final BiologicalMaterial FUR_FIBER = biological("fur_fiber", "Fur Fiber")
            .color(0xDCD4C1)
            .roles(OrganicRole.FIBER)
            .contains(component(KERATIN, 1))
            .build();

    public static final BiologicalMaterial WOOL_FIBER = biological("wool_fiber", "Wool Fiber")
            .color(0xDCD4C1)
            .roles(OrganicRole.FIBER)
            .contains(component(KERATIN, 1))
            .build();

    public static final BiologicalMaterial FEATHER_FIBER = biological("feather_fiber", "Feather Fiber")
            .color(0xDCD4C1)
            .roles(OrganicRole.FIBER)
            .contains(component(KERATIN, 1))
            .build();

    public static final BiologicalMaterial SILK_FIBER = biological("silk_fiber", "Silk Fiber")
            .color(0xDCD4C1)
            .roles(OrganicRole.FIBER)
            .contains(component(SILK_PROTEIN, 1))
            .build();

    public static final List<BiologicalMaterial> BIOLOGICAL = List.of(
            MUSCLE_PROTEIN, COLLAGEN, KERATIN, SILK_PROTEIN, MILK_PROTEIN, EGG_PROTEIN, ANIMAL_FAT, MILK_FAT, WOOL_FAT, WAX, SLIME_COMPOUND, INK_PIGMENT, TOXIN,
            FUR_FIBER, WOOL_FIBER, FEATHER_FIBER, SILK_FIBER);

    private OrganicMaterials() {
    }
}
