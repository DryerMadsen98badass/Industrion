package net.mads.industron.material.chemistry.process;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.chemistry.ChemicalStructure;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.MaterialClassification;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic complexity/profile selection for generated dust-processing chains.
 *
 * <p>Tier controls how much chemistry an automatic route may use. Structure and calculated
 * properties select the physical/chemical route inside that budget. Recipe IO/slot counts never
 * participate in this decision.</p>
 */
public record ProcessChainProfile(
        boolean enabled,
        int targetSteps,
        double difficulty,
        LeachEnvironment leachEnvironment,
        double chemicalBalanceMinimum,
        double chemicalBalanceMaximum,
        boolean requiresActivator,
        List<Stage> stages,
        String explanation
) {
    public enum Stage {
        ROASTED_DUST,
        SLURRY,
        SOLUTION,
        REACTION_MIXTURE
    }

    public enum LeachEnvironment {
        NONE,
        ACIDIC,
        BASIC
    }

    private static final int LUV_INDEX = MachineTier.ALL.indexOf(MachineTier.LUV);

    public ProcessChainProfile {
        stages = List.copyOf(stages == null ? List.of() : stages);
        explanation = explanation == null ? "" : explanation;
        if (targetSteps < 0) throw new IllegalArgumentException("targetSteps cannot be negative");
        if (!Double.isFinite(chemicalBalanceMinimum) || !Double.isFinite(chemicalBalanceMaximum)) {
            throw new IllegalArgumentException("Chemical Balance bounds must be finite");
        }
        if (leachEnvironment == LeachEnvironment.NONE) {
            if (chemicalBalanceMinimum != 0.0D || chemicalBalanceMaximum != 0.0D) {
                throw new IllegalArgumentException("A non-leaching profile may not carry a CB range");
            }
        } else {
            if (chemicalBalanceMinimum < -100.0D || chemicalBalanceMaximum > 100.0D
                    || chemicalBalanceMinimum > chemicalBalanceMaximum) {
                throw new IllegalArgumentException("Invalid CB range " + chemicalBalanceMinimum + ".." + chemicalBalanceMaximum);
            }
            if (leachEnvironment == LeachEnvironment.ACIDIC
                    && chemicalBalanceMinimum <= 0.0D) {
                throw new IllegalArgumentException("Acidic CB must stay positive");
            }
            if (leachEnvironment == LeachEnvironment.BASIC
                    && chemicalBalanceMaximum >= 0.0D) {
                throw new IllegalArgumentException("Basic CB must stay negative");
            }
        }
    }

    /** Base tier/chemistry profile, before a concrete separation family is known. */
    public static ProcessChainProfile forAnalysis(MaterialAnalysis analysis) {
        int tier = Math.max(0, analysis.calculatedTierIndex());
        double difficulty = difficulty(analysis);
        if (tier > LUV_INDEX) {
            return disabled(
                    difficulty,
                    "Automatic compound-dust chemistry is intentionally capped at LuV; ZPM and higher tiers are reserved for later progression."
            );
        }

        ChemicalStructure.Topology topology = ProcessSubstanceState.topologyOf(analysis);
        if (topology == ChemicalStructure.Topology.PHYSICAL_MIXTURE
                || analysis.classifications().contains(MaterialClassification.PHYSICAL_MIXTURE)) {
            return direct(
                    difficulty,
                    1,
                    "The registered substance is already a physical mixture; only a typed physical separator may expose its fractions."
            );
        }

        int minimum = minimumSteps(tier);
        int maximum = maximumSteps(tier);
        int target = minimum;
        if (maximum > minimum) {
            double threshold = switch (tier) {
                case 2 -> 48.0D; // MV: 1..2
                case 3 -> 52.0D; // HV: 1..2
                case 4 -> 56.0D; // EV: 2..3
                case 5 -> 50.0D; // IV: 3..4
                default -> 48.0D; // LuV: 4..5
            };
            if (difficulty >= threshold) target = maximum;
        }

        return withStages(
                analysis,
                difficulty,
                target,
                "Tier " + analysis.calculatedTierName() + " permits " + minimum + ".." + maximum
                        + " chemistry step(s); calculated separation difficulty " + Math.round(difficulty)
                        + " selected " + target + " step(s)."
        );
    }

    /**
     * Route-aware profile used by both registry-time intermediate generation and runtime/datagen
     * planning. This is the single source of truth for whether slurry/solution/etc. may exist.
     */
    public static ProcessChainProfile forAnalysis(
            MaterialAnalysis analysis,
            SeparationClassifier.Route route
    ) {
        ProcessChainProfile base = forAnalysis(analysis);
        if (!base.enabled()) return base;
        if (route == null) return base;

        int processingTier = processingTierIndex(analysis, route);
        int maximum = maximumSteps(processingTier);
        String promotion = processingTier > analysis.calculatedTierIndex()
                ? " The selected route requires at least " + processingTierName(processingTier)
                + " processing, so the generated recipe tier is raised from " + analysis.calculatedTierName()
                + " to " + processingTierName(processingTier) + " without changing the material identity."
                : "";
        return switch (route) {
            case NO_VALID_PHYSICAL_SEPARATION -> disabled(
                    base.difficulty(),
                    "No chemically valid automatic separation family exists for this physical mixture."
            );
            case CENTRIFUGING, MAGNETIC_SEPARATION -> direct(
                    base.difficulty(),
                    1,
                    base.explanation() + promotion + " The selected physical separation family needs no synthetic chemistry intermediate."
            );
            case THERMAL_REDUCTION -> direct(
                    base.difficulty(),
                    1,
                    base.explanation() + promotion + " The metal-bearing compound supports a direct temperature-gated thermal-reduction step, so no synthetic liquid intermediate is inserted."
            );
            case DIRECT_ELECTROLYSIS -> direct(
                    base.difficulty(),
                    1,
                    base.explanation() + promotion + " The ionic structure supports direct electrolysis, so no artificial slurry/solution stage is inserted."
            );
            case MOLTEN_ELECTROLYSIS, MOLTEN_ELECTROREFINING, MOLTEN_CHEMICAL_REACTION -> {
                yield direct(
                        base.difficulty(),
                        2,
                        base.explanation() + promotion + " The selected molten family uses exactly a registered melting step and a molten separation step; no fake liquid intermediates are added."
                );
            }
            case LEACHING, LEACHING_ELECTROWINNING -> {
                int target = Math.max(2, base.targetSteps());
                // processingTier is promoted to the first tier whose chain budget can represent
                // the selected chemistry; tier never changes the chemistry family itself.
                if (maximum < target) {
                    target = maximum;
                }
                yield withStages(
                        analysis,
                        base.difficulty(),
                        target,
                        base.explanation() + promotion + " The selected leaching family requires a registry-backed solution path."
                );
            }
            case CHEMICAL_REACTION -> base;
        };
    }

    /**
     * Effective machine-processing tier for an already selected chemistry route. Material identity
     * tier remains unchanged; this only prevents a chemically necessary multi-step family from being
     * discarded because its source material happened to calculate below the first tier that can
     * represent that many automatic steps.
     */
    public static int processingTierIndex(MaterialAnalysis analysis, SeparationClassifier.Route route) {
        int materialTier = Math.max(0, analysis.calculatedTierIndex());
        if (materialTier > LUV_INDEX || route == null) return materialTier;
        int minimumSteps = switch (route) {
            case MOLTEN_ELECTROLYSIS, MOLTEN_ELECTROREFINING, MOLTEN_CHEMICAL_REACTION,
                    LEACHING, LEACHING_ELECTROWINNING -> 2;
            default -> 1;
        };
        return Math.max(materialTier, minimumTierForSteps(minimumSteps));
    }

    public static String processingTierName(int tierIndex) {
        int safe = Math.max(0, Math.min(tierIndex, MachineTier.ALL.size() - 1));
        return MachineTier.ALL.get(safe).displayName();
    }

    private static int minimumTierForSteps(int steps) {
        if (steps <= 1) return 0; // ULV/LV both support one step; choose the earliest tier.
        if (steps == 2) return 2; // MV is the first 1..2-step tier.
        if (steps == 3) return 4; // EV is the first 2..3-step tier.
        if (steps == 4) return 5; // IV is the first 3..4-step tier.
        return 6;                // LuV is the first 4..5-step tier.
    }

    private static ProcessChainProfile withStages(
            MaterialAnalysis analysis,
            double difficulty,
            int target,
            String explanation
    ) {
        List<Stage> stages = stagesFor(target);
        LeachEnvironment environment = (stages.contains(Stage.SLURRY) || stages.contains(Stage.SOLUTION))
                ? leachEnvironment(analysis)
                : LeachEnvironment.NONE;
        double[] chemicalBalance = chemicalBalanceRange(analysis, environment);

        double reactivity = normalized(analysis.properties().get("reactivity"));
        double stability = normalized(analysis.properties().get("chemicalstability"));
        boolean requiresActivator = stages.contains(Stage.REACTION_MIXTURE)
                && (reactivity < 42.0D || stability > 62.0D);

        return new ProcessChainProfile(
                true,
                target,
                difficulty,
                environment,
                chemicalBalance[0],
                chemicalBalance[1],
                requiresActivator,
                stages,
                explanation
        );
    }

    private static ProcessChainProfile direct(double difficulty, int steps, String explanation) {
        return new ProcessChainProfile(
                true,
                steps,
                difficulty,
                LeachEnvironment.NONE,
                0.0D,
                0.0D,
                false,
                List.of(),
                explanation
        );
    }

    private static ProcessChainProfile disabled(double difficulty, String explanation) {
        return new ProcessChainProfile(
                false,
                0,
                difficulty,
                LeachEnvironment.NONE,
                0.0D,
                0.0D,
                false,
                List.of(),
                explanation
        );
    }

    private static int minimumSteps(int tier) {
        return switch (tier) {
            case 0, 1, 2, 3 -> 1; // ULV/LV = 1; MV/HV = 1..2
            case 4 -> 2;          // EV = 2..3
            case 5 -> 3;          // IV = 3..4
            default -> 4;         // LuV = 4..5
        };
    }

    private static int maximumSteps(int tier) {
        return switch (tier) {
            case 0, 1 -> 1;       // ULV/LV
            case 2, 3 -> 2;       // MV/HV
            case 4 -> 3;          // EV
            case 5 -> 4;          // IV
            default -> 5;         // LuV
        };
    }

    private static List<Stage> stagesFor(int steps) {
        ArrayList<Stage> result = new ArrayList<>();
        if (steps >= 4) result.add(Stage.ROASTED_DUST);
        if (steps >= 3) result.add(Stage.SLURRY);
        if (steps >= 2) result.add(Stage.SOLUTION);
        if (steps >= 5) result.add(Stage.REACTION_MIXTURE);
        return List.copyOf(result);
    }

    private static double[] chemicalBalanceRange(MaterialAnalysis analysis, LeachEnvironment environment) {
        if (environment == LeachEnvironment.NONE) return new double[]{0.0D, 0.0D};

        double acidity = signed(analysis.properties().get("acidity"));
        double magnitude = Math.max(20.0D, Math.abs(acidity));
        double center = environment == LeachEnvironment.ACIDIC ? magnitude : -magnitude;
        double minimum = Math.max(-100.0D, center - 15.0D);
        double maximum = Math.min(100.0D, center + 15.0D);

        // Never allow a leaching recipe's accepted CB window to cross neutral.
        if (environment == LeachEnvironment.ACIDIC) minimum = Math.max(1.0D, minimum);
        else maximum = Math.min(-1.0D, maximum);
        return new double[]{minimum, maximum};
    }

    private static LeachEnvironment leachEnvironment(MaterialAnalysis analysis) {
        double acidity = signed(analysis.properties().get("acidity"));
        // The material property describes the compound's own acid/base tendency. Processing uses
        // the complementary CB environment: acid-like/electron-accepting compounds are treated in
        // basic conditions, while base-like/electron-donating compounds are treated in acidic
        // conditions. CB itself keeps the project convention positive=acidic, negative=basic.
        if (acidity >= 15.0D) return LeachEnvironment.BASIC;
        if (acidity <= -15.0D) return LeachEnvironment.ACIDIC;

        double acceptance = normalized(analysis.properties().get("electronacceptancetendency"));
        double donation = normalized(analysis.properties().get("electrondonationtendency"));
        if (acceptance > donation + 5.0D) return LeachEnvironment.BASIC;
        if (donation > acceptance + 5.0D) return LeachEnvironment.ACIDIC;

        // Exact neutral is deterministic rather than random.
        return acidity >= 0.0D ? LeachEnvironment.BASIC : LeachEnvironment.ACIDIC;
    }

    private static double difficulty(MaterialAnalysis analysis) {
        double bond = normalized(analysis.properties().get("bondstrength"));
        double stability = normalized(analysis.properties().get("chemicalstability"));
        double crystal = normalized(analysis.properties().get("crystalstability"));
        double reactivity = normalized(analysis.properties().get("reactivity"));
        double polarity = normalized(analysis.properties().get("polarity"));
        int componentCount = Math.max(1, analysis.source().composition().size());
        double complexity = Math.min(100.0D, Math.max(0, componentCount - 1) * 18.0D);
        return clamp(
                bond * 0.28D
                        + stability * 0.24D
                        + crystal * 0.14D
                        + complexity * 0.18D
                        + (100.0D - polarity) * 0.08D
                        - reactivity * 0.12D
        );
    }

    private static double normalized(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return clamp(value);
    }

    private static double signed(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(-100.0D, Math.min(100.0D, value));
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(100.0D, value));
    }
}
