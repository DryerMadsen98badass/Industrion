package net.mads.industron.material;

import java.util.EnumSet;
import java.util.Set;

/**
 * Central element-form eligibility. This class decides what content an element gets;
 * texture availability never decides whether a form exists.
 */
public final class MaterialFormGenerator {
    private static final EnumSet<MaterialPart> ORE_PROCESSING = EnumSet.of(
            MaterialPart.ORE, MaterialPart.SMALL_ORE,
            MaterialPart.DEEPSLATE_ORE, MaterialPart.SMALL_DEEPSLATE_ORE,
            MaterialPart.DIORITE_ORE, MaterialPart.SMALL_DIORITE_ORE,
            MaterialPart.ANDESITE_ORE, MaterialPart.SMALL_ANDESITE_ORE,
            MaterialPart.GRANITE_ORE, MaterialPart.SMALL_GRANITE_ORE,
            MaterialPart.TUFF_ORE, MaterialPart.SMALL_TUFF_ORE,
            MaterialPart.NETHERRACK_ORE, MaterialPart.SMALL_NETHERRACK_ORE,
            MaterialPart.BLACKSTONE_ORE, MaterialPart.SMALL_BLACKSTONE_ORE,
            MaterialPart.END_STONE_ORE, MaterialPart.SMALL_END_STONE_ORE,
            MaterialPart.RAW_ORE, MaterialPart.RAW_BLOCK, MaterialPart.CRUSHED_ORE,
            MaterialPart.WASHED_CRUSHED_ORE, MaterialPart.REFINED_ORE,
            MaterialPart.TINY_DUST, MaterialPart.SMALL_DUST, MaterialPart.DUST,
            MaterialPart.IMPURE_DUST, MaterialPart.PURIFIED_DUST
    );

    private static final EnumSet<MaterialPart> METAL_FORMS = EnumSet.of(
            MaterialPart.INGOT, MaterialPart.DOUBLE_INGOT, MaterialPart.HOT_INGOT,
            MaterialPart.NUGGET, MaterialPart.HOT_NUGGET, MaterialPart.BLOCK,
            MaterialPart.TINY_BALL, MaterialPart.SMALL_BALL, MaterialPart.BALL, MaterialPart.LARGE_BALL, MaterialPart.HUGE_BALL,
            MaterialPart.TINY_BEARING, MaterialPart.SMALL_BEARING, MaterialPart.BEARING, MaterialPart.LARGE_BEARING, MaterialPart.HUGE_BEARING,
            MaterialPart.TINY_BOLT, MaterialPart.SMALL_BOLT, MaterialPart.BOLT, MaterialPart.LARGE_BOLT, MaterialPart.HUGE_BOLT,
            MaterialPart.TINY_SCREW, MaterialPart.SMALL_SCREW, MaterialPart.SCREW, MaterialPart.LARGE_SCREW, MaterialPart.HUGE_SCREW,
            MaterialPart.TINY_RIVET, MaterialPart.SMALL_RIVET, MaterialPart.RIVET, MaterialPart.LARGE_RIVET, MaterialPart.HUGE_RIVET,
            MaterialPart.TINY_GEAR, MaterialPart.SMALL_GEAR, MaterialPart.GEAR, MaterialPart.LARGE_GEAR, MaterialPart.HUGE_GEAR,
            MaterialPart.TINY_RING, MaterialPart.SMALL_RING, MaterialPart.RING, MaterialPart.LARGE_RING, MaterialPart.HUGE_RING,
            MaterialPart.VERY_SHORT_ROD, MaterialPart.SHORT_ROD, MaterialPart.ROD, MaterialPart.LONG_ROD, MaterialPart.VERY_LONG_ROD,
            MaterialPart.TINY_SPRING, MaterialPart.SMALL_SPRING, MaterialPart.SPRING, MaterialPart.LARGE_SPRING, MaterialPart.HUGE_SPRING,
            MaterialPart.TINY_ROTOR, MaterialPart.SMALL_ROTOR, MaterialPart.ROTOR, MaterialPart.LARGE_ROTOR, MaterialPart.HUGE_ROTOR,
            MaterialPart.PLATE, MaterialPart.LARGE_PLATE,
            MaterialPart.DOUBLE_PLATE, MaterialPart.LARGE_DOUBLE_PLATE,
            MaterialPart.DENSE_PLATE, MaterialPart.LARGE_DENSE_PLATE,
            MaterialPart.REINFORCED_PLATE, MaterialPart.HEAT_EXCHANGER_PLATE,
            MaterialPart.FOIL, MaterialPart.FINE_WIRE,
            MaterialPart.WIRE_1X, MaterialPart.WIRE_2X, MaterialPart.WIRE_4X, MaterialPart.WIRE_8X, MaterialPart.WIRE_16X,
            MaterialPart.COIL, MaterialPart.TURBINE_BLADE, MaterialPart.FRAME
    );

    private static final EnumSet<MaterialPart> MACHINED_METAL_FORMS = EnumSet.of(
            MaterialPart.TOOL_HEAD_BUZZ_SAW
    );

    private static final EnumSet<MaterialPart> INSULATOR_FORMS = EnumSet.of(
            MaterialPart.TINY_RING, MaterialPart.SMALL_RING, MaterialPart.RING,
            MaterialPart.LARGE_RING, MaterialPart.HUGE_RING
    );

    private static final EnumSet<MaterialPart> GEM_FORMS = EnumSet.of(
            MaterialPart.BLOCK,
            MaterialPart.TINY_GEM, MaterialPart.SMALL_GEM, MaterialPart.GEM,
            MaterialPart.FLAWLESS_GEM, MaterialPart.EXQUISITE_GEM,
            MaterialPart.ROUGH_TINY_GEM, MaterialPart.ROUGH_SMALL_GEM, MaterialPart.ROUGH_GEM,
            MaterialPart.ROUGH_FLAWLESS_GEM, MaterialPart.ROUGH_EXQUISITE_GEM,
            MaterialPart.LENS
    );

    private static final EnumSet<MaterialPart> MAGNETIC_ITEM_FORMS = magneticItemForms();


    private static EnumSet<MaterialPart> magneticItemForms() {
        EnumSet<MaterialPart> forms = EnumSet.copyOf(METAL_FORMS);
        for (MaterialPart part : ORE_PROCESSING) {
            if (part.isItem()) forms.add(part);
        }
        return forms;
    }

    private MaterialFormGenerator() {
    }

    public static Set<MaterialPart> partsFor(MaterialProperties properties) {
        EnumSet<MaterialPart> parts = EnumSet.noneOf(MaterialPart.class);

        if (properties.state() == MaterialProperties.PhysicalState.GAS) {
            parts.add(MaterialPart.GAS);
            return Set.copyOf(parts);
        }

        if (properties.state() == MaterialProperties.PhysicalState.LIQUID) {
            parts.add(MaterialPart.LIQUID);
            return Set.copyOf(parts);
        }

        parts.addAll(ORE_PROCESSING);
        parts.add(MaterialPart.MOLTEN_FLUID);

        if (properties.metal()) {
            parts.addAll(METAL_FORMS);
            parts.addAll(MACHINED_METAL_FORMS);
        } else {
            // Electrically insulating solids can be formed into insulating rings.
            // This is capability-driven content generation, not a hard-coded material name list.
            if (properties.electricallyInsulating()) {
                parts.addAll(INSULATOR_FORMS);
            }
            if (properties.gemCandidate()) {
                parts.addAll(GEM_FORMS);
            }
        }

        return Set.copyOf(parts);
    }

    public static boolean hasMagneticVariant(IndustrialMaterial material, MaterialPart part) {
        return material.properties().metal()
                && material.properties().magnetic()
                && part.isItem()
                && MAGNETIC_ITEM_FORMS.contains(part);
    }
}
