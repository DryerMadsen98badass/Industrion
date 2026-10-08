package net.mads.industron.material;

import java.util.EnumSet;
import java.util.Set;

/**
 * Central element-form eligibility. This class decides what content an element gets;
 * texture availability never decides whether a form exists.
 */
public final class MaterialFormGenerator {
    /** Processing forms stay available for elemental outputs, but elements no longer
     * receive generated natural ore blocks merely because they are solid. */
    private static final EnumSet<MaterialPart> ORE_PROCESSING = EnumSet.of(
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
            MaterialPart.COIL, MaterialPart.TURBINE_BLADE, MaterialPart.FRAME,
            MaterialPart.SHAFT
    );

    // Loose material parts only; completed machines are standalone tier items.
    // Kept separate from METAL_FORMS to avoid automatic magnetic recipes.
    private static final EnumSet<MaterialPart> MACHINE_COMPONENT_FORMS = EnumSet.of(
            MaterialPart.TINY_MOTOR_ARMATURE, MaterialPart.SMALL_MOTOR_ARMATURE, MaterialPart.MOTOR_ARMATURE, MaterialPart.LARGE_MOTOR_ARMATURE, MaterialPart.HUGE_MOTOR_ARMATURE,
            MaterialPart.TINY_MOTOR_COIL, MaterialPart.SMALL_MOTOR_COIL, MaterialPart.MOTOR_COIL, MaterialPart.LARGE_MOTOR_COIL, MaterialPart.HUGE_MOTOR_COIL,
            MaterialPart.TINY_MOTOR_HOUSING, MaterialPart.SMALL_MOTOR_HOUSING, MaterialPart.MOTOR_HOUSING, MaterialPart.LARGE_MOTOR_HOUSING, MaterialPart.HUGE_MOTOR_HOUSING,
            MaterialPart.TINY_MOTOR_SHAFT, MaterialPart.SMALL_MOTOR_SHAFT, MaterialPart.MOTOR_SHAFT, MaterialPart.LARGE_MOTOR_SHAFT, MaterialPart.HUGE_MOTOR_SHAFT,
            MaterialPart.TINY_PISTON_ROD, MaterialPart.SMALL_PISTON_ROD, MaterialPart.PISTON_ROD, MaterialPart.LARGE_PISTON_ROD, MaterialPart.HUGE_PISTON_ROD,
            MaterialPart.TINY_PUMP_IMPELLER, MaterialPart.SMALL_PUMP_IMPELLER, MaterialPart.PUMP_IMPELLER, MaterialPart.LARGE_PUMP_IMPELLER, MaterialPart.HUGE_PUMP_IMPELLER
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
            net.mads.industron.machine.foundry.casting.CastingDefinitions.addMetalForms(parts);
            parts.addAll(METAL_FORMS);
            parts.addAll(MACHINE_COMPONENT_FORMS);
            // Manual forging is definition-driven: adding a new MaterialPart with explicit mass,
            // hidden forge value and a HOT counterpart automatically exposes both forms to every
            // metal unless that material definition removes them later. No anvil recipe whitelist.
            for (MaterialPart part : MaterialPart.values()) {
                if (part.isHotForgePart() || !part.isForgeableForm()) continue;
                parts.add(part);
                MaterialPart hot = part.hotForgePart();
                if (hot != null) parts.add(hot);
            }
        } else {
            // Electrically insulating solids can be formed into insulating rings.
            // This is capability-driven content generation, not a hard-coded material name list.
            if (properties.electricallyInsulating()) {
                parts.addAll(INSULATOR_FORMS);
            }
            if (properties.gemCandidate()) {
                parts.addAll(GEM_FORMS);
                parts.add(MaterialPart.HELMET_SHELL); parts.add(MaterialPart.CHESTPLATE_SHELL);
                parts.add(MaterialPart.LEGGINGS_SHELL); parts.add(MaterialPart.BOOTS_SHELL);
            }
        }

        return Set.copyOf(parts);
    }

    public static boolean hasMagneticVariant(IndustrialMaterial material, MaterialPart part) {
        return MaterialCategory.METAL.matches(material)
                && material.properties().magnetic()
                && part.isItem()
                && MAGNETIC_ITEM_FORMS.contains(part);
    }
}
