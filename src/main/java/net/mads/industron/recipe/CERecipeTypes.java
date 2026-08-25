package net.mads.industron.recipe;

import net.mads.industron.recipe.recipetypes.TestProcessingRecipeType;
import net.mads.industron.recipe.recipetypes.PulverizingRecipeType;
import net.mads.industron.recipe.recipetypes.CrushingRecipeType;
import net.mads.industron.recipe.recipetypes.GrindingRecipeType;
import net.mads.industron.recipe.recipetypes.SiftingRecipeType;
import net.mads.industron.recipe.recipetypes.WashingRecipeType;
import net.mads.industron.recipe.recipetypes.MixingRecipeType;
import net.mads.industron.recipe.recipetypes.HeatingRecipeType;
import net.mads.industron.recipe.recipetypes.CoolingRecipeType;
import net.mads.industron.recipe.recipetypes.DryingRecipeType;
import net.mads.industron.recipe.recipetypes.EvaporationRecipeType;
import net.mads.industron.recipe.recipetypes.VaporizationRecipeType;
import net.mads.industron.recipe.recipetypes.CondensationRecipeType;
import net.mads.industron.recipe.recipetypes.LiquefactionRecipeType;
import net.mads.industron.recipe.recipetypes.FreezingRecipeType;
import net.mads.industron.recipe.recipetypes.MeltingRecipeType;
import net.mads.industron.recipe.recipetypes.SmeltingRecipeType;
import net.mads.industron.recipe.recipetypes.AlloyingRecipeType;
import net.mads.industron.recipe.recipetypes.CastingRecipeType;
import net.mads.industron.recipe.recipetypes.RoastingRecipeType;
import net.mads.industron.recipe.recipetypes.CalcinationRecipeType;
import net.mads.industron.recipe.recipetypes.SinteringRecipeType;
import net.mads.industron.recipe.recipetypes.AnnealingRecipeType;
import net.mads.industron.recipe.recipetypes.HeatTreatingRecipeType;
import net.mads.industron.recipe.recipetypes.QuenchingRecipeType;
import net.mads.industron.recipe.recipetypes.ChemicalReactionRecipeType;
import net.mads.industron.recipe.recipetypes.DissolutionRecipeType;
import net.mads.industron.recipe.recipetypes.NeutralizationRecipeType;
import net.mads.industron.recipe.recipetypes.PrecipitationRecipeType;
import net.mads.industron.recipe.recipetypes.CrystallizationRecipeType;
import net.mads.industron.recipe.recipetypes.LeachingRecipeType;
import net.mads.industron.recipe.recipetypes.SolventExtractionRecipeType;
import net.mads.industron.recipe.recipetypes.PyrolysisRecipeType;
import net.mads.industron.recipe.recipetypes.CrackingRecipeType;
import net.mads.industron.recipe.recipetypes.ReformingRecipeType;
import net.mads.industron.recipe.recipetypes.PolymerizationRecipeType;
import net.mads.industron.recipe.recipetypes.FermentationRecipeType;
import net.mads.industron.recipe.recipetypes.ElectrolysisRecipeType;
import net.mads.industron.recipe.recipetypes.ElectrorefiningRecipeType;
import net.mads.industron.recipe.recipetypes.ElectrowinningRecipeType;
import net.mads.industron.recipe.recipetypes.CentrifugingRecipeType;
import net.mads.industron.recipe.recipetypes.FiltrationRecipeType;
import net.mads.industron.recipe.recipetypes.PhaseSeparationRecipeType;
import net.mads.industron.recipe.recipetypes.GasSeparationRecipeType;
import net.mads.industron.recipe.recipetypes.AbsorptionRecipeType;
import net.mads.industron.recipe.recipetypes.AdsorptionRecipeType;
import net.mads.industron.recipe.recipetypes.DistillationRecipeType;
import net.mads.industron.recipe.recipetypes.FractionationRecipeType;
import net.mads.industron.recipe.recipetypes.CompactingRecipeType;
import net.mads.industron.recipe.recipetypes.CompressingRecipeType;
import net.mads.industron.recipe.recipetypes.ExtrudingRecipeType;
import net.mads.industron.recipe.recipetypes.RollingRecipeType;
import net.mads.industron.recipe.recipetypes.HammeringRecipeType;
import net.mads.industron.recipe.recipetypes.CuttingRecipeType;
import net.mads.industron.recipe.recipetypes.TurningRecipeType;
import net.mads.industron.recipe.recipetypes.BendingRecipeType;
import net.mads.industron.recipe.recipetypes.WireDrawingRecipeType;
import net.mads.industron.recipe.recipetypes.WindingRecipeType;
import net.mads.industron.recipe.recipetypes.PrecisionMachiningRecipeType;
import net.mads.industron.recipe.recipetypes.AssemblingRecipeType;
import net.mads.industron.recipe.recipetypes.MagnetizingRecipeType;
import net.mads.industron.recipe.recipetypes.PolishingRecipeType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CERecipeTypes {
    public static final RecipeTypeDefinition TEST_PROCESSING = TestProcessingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition PULVERIZING = PulverizingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition CRUSHING = CrushingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition GRINDING = GrindingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition SIFTING = SiftingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition WASHING = WashingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition MIXING = MixingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition HEATING = HeatingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition COOLING = CoolingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition DRYING = DryingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition EVAPORATION = EvaporationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition VAPORIZATION = VaporizationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition CONDENSATION = CondensationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition LIQUEFACTION = LiquefactionRecipeType.DEFINITION;
    public static final RecipeTypeDefinition FREEZING = FreezingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition MELTING = MeltingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition SMELTING = SmeltingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition ALLOYING = AlloyingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition CASTING = CastingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition ROASTING = RoastingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition CALCINATION = CalcinationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition SINTERING = SinteringRecipeType.DEFINITION;
    public static final RecipeTypeDefinition ANNEALING = AnnealingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition HEAT_TREATING = HeatTreatingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition QUENCHING = QuenchingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition CHEMICAL_REACTION = ChemicalReactionRecipeType.DEFINITION;
    public static final RecipeTypeDefinition DISSOLUTION = DissolutionRecipeType.DEFINITION;
    public static final RecipeTypeDefinition NEUTRALIZATION = NeutralizationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition PRECIPITATION = PrecipitationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition CRYSTALLIZATION = CrystallizationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition LEACHING = LeachingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition SOLVENT_EXTRACTION = SolventExtractionRecipeType.DEFINITION;
    public static final RecipeTypeDefinition PYROLYSIS = PyrolysisRecipeType.DEFINITION;
    public static final RecipeTypeDefinition CRACKING = CrackingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition REFORMING = ReformingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition POLYMERIZATION = PolymerizationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition FERMENTATION = FermentationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition ELECTROLYSIS = ElectrolysisRecipeType.DEFINITION;
    public static final RecipeTypeDefinition ELECTROREFINING = ElectrorefiningRecipeType.DEFINITION;
    public static final RecipeTypeDefinition ELECTROWINNING = ElectrowinningRecipeType.DEFINITION;
    public static final RecipeTypeDefinition CENTRIFUGING = CentrifugingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition FILTRATION = FiltrationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition PHASE_SEPARATION = PhaseSeparationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition GAS_SEPARATION = GasSeparationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition ABSORPTION = AbsorptionRecipeType.DEFINITION;
    public static final RecipeTypeDefinition ADSORPTION = AdsorptionRecipeType.DEFINITION;
    public static final RecipeTypeDefinition DISTILLATION = DistillationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition FRACTIONATION = FractionationRecipeType.DEFINITION;
    public static final RecipeTypeDefinition COMPACTING = CompactingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition COMPRESSING = CompressingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition EXTRUDING = ExtrudingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition ROLLING = RollingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition HAMMERING = HammeringRecipeType.DEFINITION;
    public static final RecipeTypeDefinition CUTTING = CuttingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition TURNING = TurningRecipeType.DEFINITION;
    public static final RecipeTypeDefinition BENDING = BendingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition WIRE_DRAWING = WireDrawingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition WINDING = WindingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition PRECISION_MACHINING = PrecisionMachiningRecipeType.DEFINITION;
    public static final RecipeTypeDefinition ASSEMBLING = AssemblingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition MAGNETIZING = MagnetizingRecipeType.DEFINITION;
    public static final RecipeTypeDefinition POLISHING = PolishingRecipeType.DEFINITION;

    public static final List<RecipeTypeDefinition> ALL = List.of(
            TEST_PROCESSING,
            PULVERIZING,
            CRUSHING,
            GRINDING,
            SIFTING,
            WASHING,
            MIXING,
            HEATING,
            COOLING,
            DRYING,
            EVAPORATION,
            VAPORIZATION,
            CONDENSATION,
            LIQUEFACTION,
            FREEZING,
            MELTING,
            SMELTING,
            ALLOYING,
            CASTING,
            ROASTING,
            CALCINATION,
            SINTERING,
            ANNEALING,
            HEAT_TREATING,
            QUENCHING,
            CHEMICAL_REACTION,
            DISSOLUTION,
            NEUTRALIZATION,
            PRECIPITATION,
            CRYSTALLIZATION,
            LEACHING,
            SOLVENT_EXTRACTION,
            PYROLYSIS,
            CRACKING,
            REFORMING,
            POLYMERIZATION,
            FERMENTATION,
            ELECTROLYSIS,
            ELECTROREFINING,
            ELECTROWINNING,
            CENTRIFUGING,
            FILTRATION,
            PHASE_SEPARATION,
            GAS_SEPARATION,
            ABSORPTION,
            ADSORPTION,
            DISTILLATION,
            FRACTIONATION,
            COMPACTING,
            COMPRESSING,
            EXTRUDING,
            ROLLING,
            HAMMERING,
            CUTTING,
            TURNING,
            BENDING,
            WIRE_DRAWING,
            WINDING,
            PRECISION_MACHINING,
            ASSEMBLING,
            MAGNETIZING,
            POLISHING
    );

    private static final Map<ResourceLocation, RecipeTypeDefinition> BY_ID = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(RecipeTypeDefinition::id, Function.identity()));

    private CERecipeTypes() {
    }

    public static RecipeTypeDefinition byId(ResourceLocation id) {
        return BY_ID.get(id);
    }
}
