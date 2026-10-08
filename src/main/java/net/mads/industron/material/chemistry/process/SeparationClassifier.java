package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.chemistry.ChemicalStructure;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.MaterialClassification;

import java.util.List;
import java.util.Map;

/**
 * Deterministically selects the chemistry route for a composite material dust.
 *
 * <p>Topology is the primary rule. Calculated atomic/material properties only select the route
 * inside a chemically valid family; machine slot counts never influence this decision.</p>
 */
public final class SeparationClassifier {
    public enum Route {
        CENTRIFUGING,
        MAGNETIC_SEPARATION,
        NO_VALID_PHYSICAL_SEPARATION,
        THERMAL_REDUCTION,
        DIRECT_ELECTROLYSIS,
        MOLTEN_ELECTROLYSIS,
        MOLTEN_ELECTROREFINING,
        LEACHING,
        LEACHING_ELECTROWINNING,
        CHEMICAL_REACTION,
        MOLTEN_CHEMICAL_REACTION
    }

    public record Decision(Route route, double difficulty, String explanation) {
    }

    public Decision classify(
            MaterialAnalysis analysis,
            List<ProcessMaterial> componentOutputs,
            Map<String, MaterialAnalysis> registry
    ) {
        ChemicalStructure.Topology topology = topology(analysis);
        double bond = normalized(analysis.properties().get("bondstrength"));
        double stability = normalized(analysis.properties().get("chemicalstability"));
        double crystal = normalized(analysis.properties().get("crystalstability"));
        double reactivity = normalized(analysis.properties().get("reactivity"));
        double polarity = normalized(analysis.properties().get("polarity"));
        double atomicStability = weightedOutputProperty(componentOutputs, registry, "atomicstability");
        double componentComplexity = Math.min(100.0D, Math.max(0, componentOutputs.size() - 1) * 18.0D);
        double difficulty = clamp(
                bond * 0.30D
                        + stability * 0.25D
                        + crystal * 0.15D
                        + atomicStability * 0.10D
                        + componentComplexity * 0.20D
                        - reactivity * 0.15D
        );

        if (topology == ChemicalStructure.Topology.PHYSICAL_MIXTURE) {
            boolean magnetic = componentOutputs.stream().anyMatch(SeparationClassifier::isMagnetic);
            boolean nonMagnetic = componentOutputs.stream().anyMatch(output -> !isMagnetic(output));
            if (magnetic && nonMagnetic) {
                return new Decision(
                        Route.MAGNETIC_SEPARATION,
                        difficulty,
                        "The composition is a physical solid mixture with distinct magnetic and non-magnetic fractions."
                );
            }
            double densitySpread = outputPropertySpread(componentOutputs, registry, "density");
            if (densitySpread >= 12.0D) {
                return new Decision(
                        Route.CENTRIFUGING,
                        difficulty,
                        "The composition is a physical mixture with sufficient calculated density contrast for centrifuging."
                );
            }
            return new Decision(
                    Route.NO_VALID_PHYSICAL_SEPARATION,
                    difficulty,
                    "The composition is a physical mixture, but no typed magnetic or density separation rule is satisfied."
            );
        }

        if (topology == ChemicalStructure.Topology.METALLIC_LATTICE) {
            return new Decision(
                    Route.MOLTEN_ELECTROREFINING,
                    difficulty,
                    "A metallic lattice must first be melted, then separated by electrorefining."
            );
        }

        double electrochemicalSpread = outputPropertySpread(
                componentOutputs,
                registry,
                "electrochemicalpotential"
        );
        double thermalReductionSuitability = clamp(
                (100.0D - bond) * 0.32D
                        + (100.0D - stability) * 0.28D
                        + reactivity * 0.20D
                        + electrochemicalSpread * 0.20D
        );
        if (topology != ChemicalStructure.Topology.POLYMER_NETWORK
                && containsThermallyReducibleMetalAndNonMetal(componentOutputs, registry)
                && bond <= 72.0D
                && stability <= 78.0D
                && thermalReductionSuitability >= 55.0D) {
            return new Decision(
                    Route.THERMAL_REDUCTION,
                    difficulty,
                    "The bonded metal-bearing compound has sufficiently low calculated bond/stability resistance and sufficient electrochemical contrast for deterministic thermal reduction."
            );
        }

        if (topology == ChemicalStructure.Topology.IONIC_LATTICE) {
            double electrolysisSuitability = clamp(
                    polarity * 0.45D
                            + electrochemicalSpread * 0.35D
                            + (100.0D - stability) * 0.20D
            );
            if (difficulty < 58.0D && electrolysisSuitability >= 42.0D) {
                return new Decision(
                        Route.DIRECT_ELECTROLYSIS,
                        difficulty,
                        "The ionic structure is sufficiently mobile/reactive for direct dust electrolysis."
                );
            }
            return new Decision(
                    Route.MOLTEN_ELECTROLYSIS,
                    difficulty,
                    "The stable ionic lattice must be melted before electrolysis can separate its charged components."
            );
        }

        double leachSuitability = clamp(
                polarity * 0.45D
                        + reactivity * 0.35D
                        + (100.0D - stability) * 0.20D
        );
        if (leachSuitability >= 48.0D) {
            if (canElectrowin(componentOutputs, registry)) {
                return new Decision(
                        Route.LEACHING_ELECTROWINNING,
                        difficulty,
                        "Calculated polarity/reactivity supports solution leaching and the guaranteed products are metallic, so electrowinning is a valid recovery step."
                );
            }
            return new Decision(
                    Route.LEACHING,
                    difficulty,
                    "Calculated polarity/reactivity makes a solution-leaching route suitable; the required acidic/basic condition is expressed through the machine Chemical Balance (CB) range."
            );
        }
        if (difficulty >= 66.0D) {
            return new Decision(
                    Route.MOLTEN_CHEMICAL_REACTION,
                    difficulty,
                    "The strongly bound structure requires melting before chemical recovery."
            );
        }
        return new Decision(
                Route.CHEMICAL_REACTION,
                difficulty,
                "The components are chemically bound but can be recovered without a molten pretreatment."
        );
    }

    private static ChemicalStructure.Topology topology(MaterialAnalysis analysis) {
        if (analysis.source().structure().isPresent()) {
            return analysis.source().structure().orElseThrow().topology();
        }
        if (analysis.classifications().contains(MaterialClassification.PHYSICAL_MIXTURE)) {
            return ChemicalStructure.Topology.PHYSICAL_MIXTURE;
        }
        if (analysis.classifications().contains(MaterialClassification.ALLOY)) {
            return ChemicalStructure.Topology.METALLIC_LATTICE;
        }
        if (analysis.classifications().contains(MaterialClassification.IONIC_COMPOUND)) {
            return ChemicalStructure.Topology.IONIC_LATTICE;
        }
        if (analysis.classifications().contains(MaterialClassification.POLYMER)) {
            return ChemicalStructure.Topology.POLYMER_NETWORK;
        }
        return ChemicalStructure.Topology.NETWORK;
    }

    private static boolean containsThermallyReducibleMetalAndNonMetal(
            List<ProcessMaterial> outputs,
            Map<String, MaterialAnalysis> registry
    ) {
        boolean metal = false;
        boolean nonMetal = false;
        for (ProcessMaterial output : outputs) {
            MaterialAnalysis component = registry.get(output.materialId());
            if (component == null) continue;
            if (component.source().backingMaterial() instanceof IndustrialMaterial material
                    && net.mads.industron.material.MaterialCategory.of(material)
                    == net.mads.industron.material.MaterialCategory.METAL) {
                // Thermal reduction is only selected when the hot metal product has a real molten
                // representation. Otherwise another chemistry route must recover a cold DUST form.
                if (!material.has(net.mads.industron.material.MaterialPart.MOLTEN_FLUID)) return false;
                metal = true;
            } else {
                nonMetal = true;
            }
        }
        return metal && nonMetal;
    }

    private static double weightedOutputProperty(
            List<ProcessMaterial> outputs,
            Map<String, MaterialAnalysis> registry,
            String property
    ) {
        double weighted = 0.0D;
        long units = 0L;
        for (ProcessMaterial output : outputs) {
            MaterialAnalysis component = registry.get(output.materialId());
            if (component == null) continue;
            weighted += normalized(component.properties().get(property)) * output.milliUnits();
            units += output.milliUnits();
        }
        return units == 0L ? 0.0D : weighted / units;
    }

    private static double outputPropertySpread(
            List<ProcessMaterial> outputs,
            Map<String, MaterialAnalysis> registry,
            String property
    ) {
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (ProcessMaterial output : outputs) {
            MaterialAnalysis component = registry.get(output.materialId());
            if (component == null) continue;
            double value = normalized(component.properties().get(property));
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }
        return Double.isFinite(minimum) && Double.isFinite(maximum) ? maximum - minimum : 0.0D;
    }


    private static boolean canElectrowin(
            List<ProcessMaterial> outputs,
            Map<String, MaterialAnalysis> registry
    ) {
        if (outputs.isEmpty()) return false;
        for (ProcessMaterial output : outputs) {
            MaterialAnalysis analysis = registry.get(output.materialId());
            if (analysis == null || output.phase() != net.mads.industron.material.chemistry.ChemistryPhase.SOLID) {
                return false;
            }
            if (!(analysis.source().backingMaterial() instanceof IndustrialMaterial material)
                    || !material.properties().metal()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMagnetic(ProcessMaterial material) {
        double magneticStrength = material.processProperty("magneticstrength");
        if (Double.isFinite(magneticStrength)) return magneticStrength > 0.0D;
        return material.backingMaterial() instanceof IndustrialMaterial industrial
                && industrial.properties().magnetic()
                && industrial.properties().magneticStrength() > 0;
    }

    private static double normalized(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return clamp(value);
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(100.0D, value));
    }
}
