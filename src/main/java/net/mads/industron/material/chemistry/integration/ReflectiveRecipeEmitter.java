package net.mads.industron.material.chemistry.integration;

import net.mads.industron.fluid.IndustrialFluidLookup;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.chemistry.ChemistryDiagnostic;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.ChemistryStatus;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.process.CompatibleMaterialResolver;
import net.mads.industron.material.chemistry.process.ProcessKind;
import net.mads.industron.material.chemistry.process.ProcessMaterial;
import net.mads.industron.material.chemistry.process.ProcessPlan;
import net.mads.industron.material.chemistry.process.ProcessRequirement;
import net.mads.industron.material.chemistry.process.ProcessStep;
import net.mads.industron.material.chemistry.process.PropertyConstraint;
import net.mads.industron.material.recipes.MaterialRecipeHelper;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.mads.industron.recipe.RecipeTypeDefinition;
import net.minecraft.data.recipes.RecipeOutput;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Emits chemistry plans through Industron's existing {@link RecipeDefinition} API.
 *
 * <p>The historical class name is kept so older integration references do not break; this implementation
 * no longer uses reflection and never creates a parallel recipe type.</p>
 */
public final class ReflectiveRecipeEmitter {
    private static final int MAX_PROPERTY_VARIANTS_PER_STEP = 256;
    private static final List<MaterialPart> SOLID_PART_PREFERENCE = List.of(
            MaterialPart.DUST,
            MaterialPart.GEM,
            MaterialPart.ROUGH_GEM,
            MaterialPart.INGOT,
            MaterialPart.RAW_ORE,
            MaterialPart.CRUSHED_ORE,
            MaterialPart.BLOCK,
            MaterialPart.PLATE
    );

    public record Result(int emitted, List<ChemistryDiagnostic> diagnostics) {
        public Result {
            diagnostics = List.copyOf(diagnostics);
        }
    }

    private final CompatibleMaterialResolver resolver = new CompatibleMaterialResolver();

    public Result emit(RecipeOutput output, List<ProcessPlan> plans, Map<String, MaterialAnalysis> materials) {
        List<ChemistryDiagnostic> diagnostics = new ArrayList<>();
        Set<String> emittedIds = new HashSet<>();
        int emitted = 0;

        for (ProcessPlan plan : plans) {
            for (ProcessStep step : plan.steps()) {
                try {
                    step.requireBalanced();
                    List<RequirementVariant> variants = variantsFor(plan, step, materials, diagnostics);
                    if (variants.isEmpty()) continue;

                    for (int variantIndex = 0; variantIndex < variants.size(); variantIndex++) {
                        RequirementVariant variant = variants.get(variantIndex);
                        String recipeId = variantRecipeId(step, variant, variants.size());
                        String fullKey = step.kind().primaryRecipeTypeId() + "/" + recipeId;
                        if (!emittedIds.add(fullKey)) continue;

                        ChemistryDiagnostic problem = validateEmission(step, variant, materials);
                        if (problem != null) {
                            diagnostics.add(problem);
                            continue;
                        }
                        emitOne(output, step, recipeId, variant);
                        emitted++;
                    }
                } catch (RuntimeException e) {
                    diagnostics.add(skip(plan, step, e.getClass().getSimpleName() + ": " + safeMessage(e)));
                }
            }
        }
        return new Result(emitted, diagnostics);
    }

    private List<RequirementVariant> variantsFor(
            ProcessPlan plan,
            ProcessStep step,
            Map<String, MaterialAnalysis> materials,
            List<ChemistryDiagnostic> diagnostics
    ) {
        List<ProcessRequirement> concreteRequirements = step.requirements().stream()
                .filter(requirement -> requirement.role() != ProcessRequirement.Role.ACID_BASE_ENVIRONMENT)
                .toList();

        for (ProcessRequirement requirement : concreteRequirements) {
            if (requirement.consumed()) {
                diagnostics.add(new ChemistryDiagnostic(
                        ChemistryStatus.CHANGE_REQUIRED,
                        plan.targetMaterialId(),
                        "CONSUMED_REQUIREMENT_NOT_BALANCED",
                        "Property-selected consumed requirement " + requirement.role()
                                + " cannot be added unless its material also appears in the balanced outputs/recovery stream.",
                        List.of("Model consumed reagents as explicit ProcessMaterial inputs and include every consumed unit in guaranteed outputs/recovery."),
                        ""
                ));
                return List.of();
            }
        }

        List<List<MaterialAnalysis>> choices = new ArrayList<>();
        for (ProcessRequirement requirement : concreteRequirements) {
            List<MaterialAnalysis> candidates = resolver.resolve(requirement, materials);
            if (candidates.isEmpty()) {
                diagnostics.add(new ChemistryDiagnostic(
                        ChemistryStatus.MISSING_MATERIAL,
                        plan.targetMaterialId(),
                        "NO_COMPATIBLE_" + requirement.role(),
                        "No registered material satisfies " + describe(requirement) + ".",
                        List.of("Create a fictional material whose calculated properties satisfy the requirement, then rerun runData."),
                        suggestedRequirementMaterial(requirement)
                ));
                return List.of();
            }
            choices.add(candidates);
        }

        List<RequirementVariant> variants = new ArrayList<>();
        buildCartesianVariants(concreteRequirements, choices, 0, new LinkedHashMap<>(), variants);
        if (variants.size() > MAX_PROPERTY_VARIANTS_PER_STEP) {
            diagnostics.add(new ChemistryDiagnostic(
                    ChemistryStatus.OK_WITH_REQUIREMENTS,
                    plan.targetMaterialId(),
                    "TOO_MANY_PROPERTY_VARIANTS",
                    "Property-based requirements produce " + variants.size() + " concrete recipe variants; capped at "
                            + MAX_PROPERTY_VARIANTS_PER_STEP + ".",
                    List.of("Narrow the requirement range or add a future property-aware ingredient type if all variants must remain dynamic."),
                    ""
            ));
            return List.copyOf(variants.subList(0, MAX_PROPERTY_VARIANTS_PER_STEP));
        }
        if (variants.isEmpty()) variants.add(new RequirementVariant(Map.of()));
        return List.copyOf(variants);
    }

    private static void buildCartesianVariants(
            List<ProcessRequirement> requirements,
            List<List<MaterialAnalysis>> choices,
            int index,
            Map<ProcessRequirement, MaterialAnalysis> current,
            List<RequirementVariant> result
    ) {
        if (index >= requirements.size()) {
            result.add(new RequirementVariant(Map.copyOf(current)));
            return;
        }
        if (result.size() > MAX_PROPERTY_VARIANTS_PER_STEP) return;
        ProcessRequirement requirement = requirements.get(index);
        for (MaterialAnalysis candidate : choices.get(index)) {
            current.put(requirement, candidate);
            buildCartesianVariants(requirements, choices, index + 1, current, result);
            current.remove(requirement);
            if (result.size() > MAX_PROPERTY_VARIANTS_PER_STEP) return;
        }
    }

    private ChemistryDiagnostic validateEmission(
            ProcessStep step,
            RequirementVariant variant,
            Map<String, MaterialAnalysis> materials
    ) {
        RecipeTypeDefinition type = typeFor(step.kind());
        if (type == null) {
            return emissionProblem(step, "No existing RecipeTypeDefinition maps to " + step.kind() + ".");
        }

        int itemInputs = countSolid(step.inputs());
        int fluidInputs = countFluid(step.inputs());
        int itemOutputs = countSolid(step.outputs());
        int fluidOutputs = countFluid(step.outputs());

        for (Map.Entry<ProcessRequirement, MaterialAnalysis> entry : variant.materials().entrySet()) {
            if (entry.getValue().phase().isFluidLike()) fluidInputs++;
            else itemInputs++;
        }

        if (itemInputs > type.maxItemInputs() || itemOutputs > type.maxItemOutputs()
                || fluidInputs > type.maxFluidInputs() || fluidOutputs > type.maxFluidOutputs()) {
            return emissionProblem(
                    step,
                    "Planned IO exceeds existing " + type.id() + " limits: items " + itemInputs + "->" + itemOutputs
                            + ", fluids " + fluidInputs + "->" + fluidOutputs + "; max "
                            + type.maxItemInputs() + "->" + type.maxItemOutputs() + ", "
                            + type.maxFluidInputs() + "->" + type.maxFluidOutputs() + "."
            );
        }

        for (ProcessMaterial material : concat(step.inputs(), step.outputs())) {
            if (!(material.backingMaterial() instanceof IndustrialMaterial industrial)) {
                return emissionProblem(step, "Material " + material.materialId() + " has no IndustrialMaterial backing object.");
            }
            if (resourceId(industrial, material.phase(), material.part()) == null) {
                return emissionProblem(step, "Material " + industrial.id() + " has no registered form for phase " + material.phase() + ".");
            }
            if (!material.phase().isFluidLike() && material.milliUnits() % 1000 != 0) {
                return emissionProblem(step, "Solid material " + industrial.id() + " requires a fractional item amount (" + material.units() + ").");
            }
        }
        return null;
    }

    private void emitOne(
            RecipeOutput output,
            ProcessStep step,
            String recipeId,
            RequirementVariant variant
    ) {
        RecipeTypeDefinition type = typeFor(step.kind());
        RecipeDefinition recipe = RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(recipeId))
                .recipeDefinition(RecipeDefinition.Option.recipeType(type));

        for (ProcessMaterial input : step.inputs()) addMaterial(recipe, input, true, false);
        for (Map.Entry<ProcessRequirement, MaterialAnalysis> entry : variant.materials().entrySet()) {
            ProcessRequirement requirement = entry.getKey();
            MaterialAnalysis candidate = entry.getValue();
            ProcessMaterial material = new ProcessMaterial(
                    candidate.source().id(),
                    candidate.phase(),
                    Math.max(1, requirement.milliUnits()),
                    candidate.calculatedTierIndex(),
                    candidate.calculatedTierName(),
                    true,
                    candidate.source().backingMaterial()
            );
            addMaterial(recipe, material, true, !requirement.consumed());
        }

        for (ProcessRequirement requirement : step.requirements()) {
            if (requirement.role() == ProcessRequirement.Role.ACID_BASE_ENVIRONMENT) {
                PropertyConstraint acidity = requirement.constraints().stream()
                        .filter(constraint -> constraint.property().equals("acidity"))
                        .findFirst()
                        .orElse(null);
                if (acidity != null) {
                    double min = Math.max(-100, acidity.minimum());
                    double max = Math.min(100, acidity.maximum());
                    recipe.recipeDefinition(RecipeDefinition.Option.chemicalBalanceRange(min, max));
                }
            }
        }

        for (ProcessMaterial value : step.outputs()) addMaterial(recipe, value, false, false);
        recipe.recipeDefinition(RecipeDefinition.Option.duration(step.durationTicks()));
        recipe.recipeDefinition(RecipeDefinition.Option.tier(MachineTier.ALL.get(
                Math.min(step.recipeTierIndex(), MachineTier.ALL.size() - 1)
        )));
        if (step.requiredTemperature() > 0) {
            recipe.recipeDefinition(RecipeDefinition.Option.temperature(step.requiredTemperature()));
        }
        recipe.save(output);
    }

    private static void addMaterial(RecipeDefinition recipe, ProcessMaterial value, boolean input, boolean notConsumed) {
        IndustrialMaterial material = (IndustrialMaterial) value.backingMaterial();
        String resource = resourceId(material, value.phase(), value.part());
        if (value.phase().isFluidLike()) {
            int amount = value.milliBucketsExact();
            if (notConsumed) recipe.recipeDefinition(RecipeDefinition.Option.notConsumableFluid(resource, amount));
            else if (input) recipe.recipeDefinition(RecipeDefinition.Option.inputFluid(resource, amount));
            else recipe.recipeDefinition(RecipeDefinition.Option.outputFluid(resource, amount));
        } else {
            int amount = value.itemAmountExact();
            if (notConsumed) recipe.recipeDefinition(RecipeDefinition.Option.notConsumableItem(resource, amount));
            else if (input) recipe.recipeDefinition(RecipeDefinition.Option.inputItem(resource, amount));
            else recipe.recipeDefinition(RecipeDefinition.Option.outputItem(resource, amount));
        }
    }

    private static String resourceId(IndustrialMaterial material, ChemistryPhase phase, MaterialPart preferredPart) {
        if (preferredPart != null) {
            if (!material.has(preferredPart)) return null;
            if (preferredPart.isFluid()) {
                return IndustrialFluidLookup.fluidId(material, preferredPart).toString();
            }
            return MaterialRecipeHelper.itemId(material, preferredPart);
        }
        if (phase.isFluidLike()) {
            MaterialPart part = switch (phase) {
                case GAS, PLASMA -> MaterialPart.GAS;
                case MOLTEN -> MaterialPart.MOLTEN_FLUID;
                case LIQUID -> MaterialPart.LIQUID;
                default -> null;
            };
            if (part == null || !material.has(part)) return null;
            return IndustrialFluidLookup.fluidId(material, part).toString();
        }
        for (MaterialPart part : SOLID_PART_PREFERENCE) {
            if (material.has(part) && !part.isFluid()) return MaterialRecipeHelper.itemId(material, part);
        }
        return null;
    }

    private static RecipeTypeDefinition typeFor(ProcessKind kind) {
        return switch (kind) {
            case MIXING -> CERecipeTypes.MIXING;
            case ALLOYING -> CERecipeTypes.ALLOYING;
            case SMELTING -> CERecipeTypes.SMELTING;
            case ROASTING -> CERecipeTypes.ROASTING;
            case CALCINATION -> CERecipeTypes.CALCINATION;
            case CHEMICAL_REACTION -> CERecipeTypes.CHEMICAL_REACTION;
            case DISSOLUTION -> CERecipeTypes.DISSOLUTION;
            case NEUTRALIZATION -> CERecipeTypes.NEUTRALIZATION;
            case PRECIPITATION -> CERecipeTypes.PRECIPITATION;
            case CRYSTALLIZATION -> CERecipeTypes.CRYSTALLIZATION;
            case LEACHING -> CERecipeTypes.LEACHING;
            case SOLVENT_EXTRACTION -> CERecipeTypes.SOLVENT_EXTRACTION;
            case ELECTROLYSIS -> CERecipeTypes.ELECTROLYSIS;
            case ELECTROREFINING -> CERecipeTypes.ELECTROREFINING;
            case ELECTROWINNING -> CERecipeTypes.ELECTROWINNING;
            case CENTRIFUGING -> CERecipeTypes.CENTRIFUGING;
            case MAGNETIC_SEPARATION -> CERecipeTypes.MAGNETIC_SEPARATION;
            case FILTRATION -> CERecipeTypes.FILTRATION;
            case PHASE_SEPARATION -> CERecipeTypes.PHASE_SEPARATION;
            case GAS_SEPARATION -> CERecipeTypes.GAS_SEPARATION;
            case DISTILLATION -> CERecipeTypes.DISTILLATION;
            case FRACTIONATION -> CERecipeTypes.FRACTIONATION;
            case CRACKING -> CERecipeTypes.CRACKING;
            case REFORMING -> CERecipeTypes.REFORMING;
            case POLYMERIZATION -> CERecipeTypes.POLYMERIZATION;
            case PYROLYSIS -> CERecipeTypes.PYROLYSIS;
            case ABSORPTION -> CERecipeTypes.ABSORPTION;
            case ADSORPTION -> CERecipeTypes.ADSORPTION;
            case DRYING -> CERecipeTypes.DRYING;
            case EVAPORATION -> CERecipeTypes.EVAPORATION;
            case CONDENSATION -> CERecipeTypes.CONDENSATION;
            case LIQUEFACTION -> CERecipeTypes.LIQUEFACTION;
            case FREEZING -> CERecipeTypes.FREEZING;
            case MELTING -> CERecipeTypes.MELTING;
        };
    }

    private static String variantRecipeId(ProcessStep step, RequirementVariant variant, int totalVariants) {
        if (totalVariants <= 1 || variant.materials().isEmpty()) return step.id();
        StringBuilder id = new StringBuilder(step.id());
        variant.materials().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(value -> value.role().name())))
                .forEach(entry -> id.append("__")
                        .append(entry.getKey().role().name().toLowerCase(Locale.ROOT))
                        .append("_")
                        .append(entry.getValue().source().id()));
        return id.toString();
    }

    private static int countSolid(List<ProcessMaterial> materials) {
        return (int) materials.stream().filter(value -> !value.phase().isFluidLike()).count();
    }

    private static int countFluid(List<ProcessMaterial> materials) {
        return (int) materials.stream().filter(value -> value.phase().isFluidLike()).count();
    }

    private static List<ProcessMaterial> concat(List<ProcessMaterial> first, List<ProcessMaterial> second) {
        List<ProcessMaterial> values = new ArrayList<>(first.size() + second.size());
        values.addAll(first);
        values.addAll(second);
        return values;
    }

    private static ChemistryDiagnostic emissionProblem(ProcessStep step, String reason) {
        return new ChemistryDiagnostic(
                ChemistryStatus.OK_WITH_REQUIREMENTS,
                step.id(),
                "RECIPE_EMISSION_SKIPPED",
                reason,
                List.of("The process remains visible in build/reports/industron/chemistry/process-plans.txt.",
                        "Fix the material form/IO definition; do not create a new recipe type."),
                ""
        );
    }

    private static ChemistryDiagnostic skip(ProcessPlan plan, ProcessStep step, String reason) {
        return new ChemistryDiagnostic(
                ChemistryStatus.OK_WITH_REQUIREMENTS,
                plan.targetMaterialId(),
                "RECIPE_EMISSION_SKIPPED",
                "Could not emit " + step.id() + ": " + reason,
                List.of("The process plan remains in build/reports/industron/chemistry/process-plans.txt."),
                ""
        );
    }

    private static String describe(ProcessRequirement requirement) {
        return requirement.role() + " in phase " + requirement.requiredPhase() + " with " + requirement.constraints();
    }

    private static String suggestedRequirementMaterial(ProcessRequirement requirement) {
        return "// Required " + requirement.role() + "\n"
                + "public static final IndustrialMaterial CHANGE_ME = material(\"change_me\", \"Change Me\", 0x808080)\n"
                + "        .contains(/* component(...) */)\n"
                + "        // Must satisfy: " + requirement.constraints() + "\n"
                + "        .build();";
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null ? "<no message>" : error.getMessage();
    }

    private record RequirementVariant(Map<ProcessRequirement, MaterialAnalysis> materials) {
        private RequirementVariant {
            materials = Map.copyOf(materials);
        }
    }
}
