package net.mads.industron.material;

import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.MaterialClassification;
import net.mads.industron.material.chemistry.MaterialSource;

import java.util.EnumSet;
import java.util.Set;

/** Content-form eligibility for compound definitions. Chemistry and content ownership are separate. */
public final class CompoundMaterialFormGenerator {
    private static final EnumSet<MaterialPart> ORE_PROCESSING = oreProcessing();

    private static EnumSet<MaterialPart> oreProcessing() {
        EnumSet<MaterialPart> parts = EnumSet.noneOf(MaterialPart.class);
        // ORE/SMALL_ORE are logical ore forms. The actual registry block is host-specific and is
        // generated from MaterialOreHost (for example <id>_granite_ore).
        parts.add(MaterialPart.ORE);
        parts.add(MaterialPart.SMALL_ORE);
        parts.add(MaterialPart.RAW_ORE);
        parts.add(MaterialPart.RAW_BLOCK);
        parts.add(MaterialPart.CRUSHED_ORE);
        parts.add(MaterialPart.WASHED_CRUSHED_ORE);
        parts.add(MaterialPart.REFINED_ORE);
        parts.add(MaterialPart.IMPURE_DUST);
        parts.add(MaterialPart.PURIFIED_DUST);
        parts.add(MaterialPart.TINY_DUST);
        parts.add(MaterialPart.SMALL_DUST);
        parts.add(MaterialPart.DUST);
        return parts;
    }

    private CompoundMaterialFormGenerator() {
    }

    public static Set<MaterialPart> partsFor(
            MaterialContentProfile profile,
            MaterialProperties properties,
            ChemistryPhase phase,
            Set<MaterialClassification> classifications,
            Set<MaterialSource> sources,
            Set<MaterialPart> explicitParts
    ) {
        if (profile == MaterialContentProfile.MINERAL_DUST) {
            // Deliberately strict. Trace minerals are real chemistry, but own exactly one game form.
            return Set.of(MaterialPart.DUST);
        }
        if (profile == MaterialContentProfile.ORE) {
            // OreMaterials owns ore/pre-processing forms. It never turns into ingots/plates merely
            // because the calculated chemistry is metallic.
            EnumSet<MaterialPart> result = EnumSet.copyOf(ORE_PROCESSING);
            result.addAll(explicitParts);
            return Set.copyOf(result);
        }

        EnumSet<MaterialPart> parts = EnumSet.noneOf(MaterialPart.class);

        if (phase == ChemistryPhase.GAS) {
            parts.add(MaterialPart.GAS);
            parts.addAll(explicitParts);
            return Set.copyOf(parts);
        }
        if (phase == ChemistryPhase.LIQUID) {
            parts.add(MaterialPart.LIQUID);
            parts.addAll(explicitParts);
            return Set.copyOf(parts);
        }

        // Normal compound forms come from calculated bulk bonding, not the amount of metallic
        // elements. A true metallic lattice (ALLOY) gets normal metal forms.
        if (classifications.contains(MaterialClassification.ALLOY)) {
            parts.addAll(MaterialFormGenerator.partsFor(properties));
            parts.removeAll(ORE_PROCESSING);
        } else {
            parts.add(MaterialPart.MOLTEN_FLUID);
        }
        parts.add(MaterialPart.TINY_DUST);
        parts.add(MaterialPart.SMALL_DUST);
        parts.add(MaterialPart.DUST);

        if (classifications.contains(MaterialClassification.POLYMER)) {
            parts.add(MaterialPart.BLOCK);
            parts.add(MaterialPart.FOIL);
            parts.add(MaterialPart.RING);
            if (classifications.contains(MaterialClassification.THERMOSET)) {
                parts.remove(MaterialPart.MOLTEN_FLUID);
            }
        }

        parts.addAll(explicitParts);
        return Set.copyOf(parts);
    }
}
