package net.mads.industron.material.defenitions;

import net.mads.industron.material.IndustrialMaterial;

import java.util.List;

import static net.mads.industron.material.defenitions.IndustrialMaterials.DRAXIL;
import static net.mads.industron.material.defenitions.IndustrialMaterials.ELNARA;
import static net.mads.industron.material.defenitions.IndustrialMaterials.ORLUNE;
import static net.mads.industron.material.defenitions.IndustrialMaterials.VERNIUM;
import static net.mads.industron.material.defenitions.IndustrialMaterials.WEXARA;
import static net.mads.industron.material.defenitions.IndustrialMaterials.component;
import static net.mads.industron.material.defenitions.IndustrialMaterials.material;

/**
 * Fictional compound/material test definitions.
 *
 * <p>These intentionally use only .contains(...). Bond topology, bulk properties, phase and tier
 * are inferred by the chemistry framework. Explicit .structure(...) remains available only as an
 * advanced override when a future material truly needs a topology that composition cannot imply.</p>
 */
public final class CompoundMaterials {
    public static final IndustrialMaterial TEST_VERNIUM_ALLOY =
            material("test_vernium_alloy", "Test Vernium Alloy", 0x8D929B)
                    .contains(component(VERNIUM, 1), component(ORLUNE, 3))
                    .build();

    public static final IndustrialMaterial TEST_VERNIUM_DRAXIL =
            material("test_vernium_draxil", "Test Vernium-Draxil", 0x91B8C4)
                    .contains(component(VERNIUM, 2), component(DRAXIL, 1))
                    .build();

    public static final IndustrialMaterial TEST_DRAXIL_ELNARA =
            material("test_draxil_elnara", "Test Draxil-Elnara", 0xB5D5DF)
                    .contains(component(DRAXIL, 1), component(ELNARA, 1))
                    .build();

    /** Four fluid-like units plus one solid unit should infer a physical mixture. */
    public static final IndustrialMaterial TEST_WEXARA_VERNIUM_MIXTURE =
            material("test_wexara_vernium_mixture", "Test Wexara-Vernium Mixture", 0x768A8E)
                    .contains(component(WEXARA, 4), component(VERNIUM, 1))
                    .build();

    public static final List<IndustrialMaterial> ALL = List.of(
            TEST_VERNIUM_ALLOY,
            TEST_VERNIUM_DRAXIL,
            TEST_DRAXIL_ELNARA,
            TEST_WEXARA_VERNIUM_MIXTURE
    );

    private CompoundMaterials() {
    }

    public static void init() {
        // Intentionally empty: touching this class initializes its static material definitions.
    }
}
