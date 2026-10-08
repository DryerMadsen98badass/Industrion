package net.mads.industron.material.defenitions;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialMaterialBuilder;
import net.mads.industron.material.MaterialCategory;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.machine.MachineTier;
import java.util.ArrayList;

import java.util.List;

import static net.mads.industron.material.defenitions.IndustrialMaterials.BRELIX;
import static net.mads.industron.material.defenitions.IndustrialMaterials.DASKEN;
import static net.mads.industron.material.defenitions.IndustrialMaterials.DRAXIL;
import static net.mads.industron.material.defenitions.IndustrialMaterials.ELNARA;
import static net.mads.industron.material.defenitions.IndustrialMaterials.GRESKA;
import static net.mads.industron.material.defenitions.IndustrialMaterials.HAAKONIUM;
import static net.mads.industron.material.defenitions.IndustrialMaterials.HESKOR;
import static net.mads.industron.material.defenitions.IndustrialMaterials.HORYX;
import static net.mads.industron.material.defenitions.IndustrialMaterials.ISKARA;
import static net.mads.industron.material.defenitions.IndustrialMaterials.JUVREN;
import static net.mads.industron.material.defenitions.IndustrialMaterials.LOXEN;
import static net.mads.industron.material.defenitions.IndustrialMaterials.MAVRIX;
import static net.mads.industron.material.defenitions.IndustrialMaterials.NERYN;
import static net.mads.industron.material.defenitions.IndustrialMaterials.ORLUNE;
import static net.mads.industron.material.defenitions.IndustrialMaterials.OSKARA;
import static net.mads.industron.material.defenitions.IndustrialMaterials.USKARA;
import static net.mads.industron.material.defenitions.IndustrialMaterials.VERNIUM;
import static net.mads.industron.material.defenitions.IndustrialMaterials.WELYRA;
import static net.mads.industron.material.defenitions.IndustrialMaterials.WEXARA;
import static net.mads.industron.material.defenitions.IndustrialMaterials.CEVORA;
import static net.mads.industron.material.defenitions.IndustrialMaterials.REDSTONE;
import static net.mads.industron.material.defenitions.IndustrialMaterials.component;
import static net.mads.industron.material.defenitions.IndustrialMaterials.material;

/**
 * Fictional compound/material definitions.
 *
 * <p>These intentionally use only .contains(...). Bond topology, bulk properties, phase and tier
 * are inferred by the chemistry framework. Explicit .structure(...) remains available only as an
 * advanced override when a future material truly needs a topology that composition cannot imply.</p>
 */
public final class CompoundMaterials {
    /** Carbon/oxygen-rich structural fraction that makes up most worked wood. */
    public static final IndustrialMaterial LIGNARA =
            material("lignara", "Lignara", 0xB28A5A)
                    .contains(component(REDSTONE, 6), component(DRAXIL, 5))
                    .build();

    /** Nitrogen-bearing plant-fiber fraction used by primitive biological materials. */
    public static final IndustrialMaterial SYLVARA =
            material("sylvara", "Sylvara", 0x91A86B)
                    .contains(component(REDSTONE, 6), component(DRAXIL, 5), component(CEVORA, 1))
                    .build();

    /** Sugar-like crystalline plant compound. */
    public static final IndustrialMaterial DULCARA =
            material("dulcara", "Dulcara", 0xD7C99B)
                    .contains(component(REDSTONE, 1), component(DRAXIL, 1))
                    .build();

    /** Carbon-rich natural resin fraction found in wood. */
    public static final IndustrialMaterial RESYRA =
            material("resyra", "Resyra", 0x9B6C3D)
                    .contains(component(REDSTONE, 8), component(DRAXIL, 2), component(CEVORA, 1))
                    .build();

    /** ULV resistance-heating alloy used by the 500 C heating coil. */
    public static final IndustrialMaterial ISKARIUM =
            material("iskarium", "Iskarium", 0xB8734A)
                    .contains(component(ISKARA, 4), component(USKARA, 1))
                    .build();

    /** LV resistance-heating alloy used by the 1000 C heating coil. */
    public static final IndustrialMaterial JUBREX =
            material("jubrex", "Jubrex", 0x9A836F)
                    .contains(component(JUVREN, 6), component(HESKOR, 3), component(BRELIX, 1))
                    .build();

    /** MV resistance-heating alloy used by the 1500 C heating coil. */
    public static final IndustrialMaterial HORDELYRA =
            material("hordelyra", "Hordelyra", 0x6F7783)
                    .contains(component(HORYX, 5), component(DASKEN, 3), component(WELYRA, 2))
                    .build();

    /** HV resistance-heating alloy used by the 2000 C heating coil. */
    public static final IndustrialMaterial MAVLOX =
            material("mavlox", "Mavlox", 0x555B66)
                    .contains(component(MAVRIX, 5), component(LOXEN, 3), component(GRESKA, 2))
                    .build();

    /** EV resistance-heating alloy used by the 2500 C heating coil. */
    public static final IndustrialMaterial NERYKON =
            material("nerykon", "Nerykon", 0x343942)
                    .contains(component(NERYN, 7), component(HAAKONIUM, 2), component(OSKARA, 1))
                    .build();


    public static final List<IndustrialMaterial> ALL = List.of(
            LIGNARA,
            SYLVARA,
            DULCARA,
            RESYRA,
            ISKARIUM,
            JUBREX,
            HORDELYRA,
            MAVLOX,
            NERYKON
    );

    // Created after stone/mineral definitions are complete, avoiding a static initialization cycle.
    // Distinct metals keep distinct chemistry and registry identities; only the Vernium variant
    // owns Create's existing item, whose material lookup must remain unambiguous.
    private static List<IndustrialMaterial> andesiteAlloys = List.of();

    public static List<IndustrialMaterial> andesiteAlloys() {
        return andesiteAlloys;
    }

    public static void initAndesiteAlloys() {
        if (!andesiteAlloys.isEmpty()) return;
        List<IndustrialMaterial> result = new ArrayList<>();
        // Snapshot before registering variants: never derive alloys from the alloys being added.
        for (IndustrialMaterial metal : List.copyOf(IndustrialMaterials.ALL)) {
            if (metal.tier() != MachineTier.ULV || MaterialCategory.of(metal) != MaterialCategory.METAL
                    || !metal.has(MaterialPart.NUGGET)) continue;
            boolean canonical = metal.id().equals(VERNIUM.id());
            IndustrialMaterialBuilder definition = material(
                    canonical ? "andesite_alloy" : "andesite_alloy_" + metal.id(),
                    canonical ? "Andesite Alloy" : "Andesite " + metal.displayName() + " Alloy",
                    -1
            )
                    // One 144 mB stone dust + eight 16 mB nuggets have the exact ratio 9:8.
                    .contains(component(StoneMaterials.ANDESITE, 9), component(metal, 8))
                    .parts(MaterialPart.INGOT)
                    .existingRecipe(MaterialPart.DUST, MaterialPart.INGOT);
            if (canonical) definition.existing(MaterialPart.INGOT, "create:andesite_alloy");
            result.add(definition.build());
        }
        andesiteAlloys = List.copyOf(result);
    }

    private CompoundMaterials() {
    }

    public static void init() {
        // Intentionally empty: touching this class initializes its static material definitions.
    }
}
