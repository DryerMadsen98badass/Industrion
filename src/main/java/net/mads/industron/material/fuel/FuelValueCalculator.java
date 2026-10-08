package net.mads.industron.material.fuel;

import net.mads.industron.material.ElementDefinition;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.MaterialPropertyCalculator;
import net.mads.industron.material.organic.OrganicMaterial;
import net.mads.industron.material.plant.PlantDerivedSubstance;
import net.mads.industron.material.plant.PlantMaterial;
import net.mads.industron.material.plant.PlantProcessIntermediate;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.WoodMaterial;

import static net.mads.industron.material.defenitions.CompoundMaterials.DULCARA;
import static net.mads.industron.material.defenitions.CompoundMaterials.LIGNARA;
import static net.mads.industron.material.defenitions.CompoundMaterials.RESYRA;
import static net.mads.industron.material.defenitions.CompoundMaterials.SYLVARA;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntSupplier;

/**
 * Chemistry-derived combustion model.
 *
 * <p>Combustibility, ignition difficulty and stored chemical energy are deliberately separate.
 * A substance only becomes a FUEL recipe when its chemistry looks like a sustained combustible
 * energy carrier. Merely being reactive/easy to oxidize is not enough; this prevents ordinary
 * reactive metals from becoming furnace fuel while still allowing coal/charcoal-like fictional
 * non-metallic chemistry to emerge naturally from calculated properties.</p>
 */
public final class FuelValueCalculator {
    private static final double MIN_COMBUSTIBILITY = 50.0D;
    private static final double FUEL_UNITS_PER_ENERGY_POINT = 30.0D / 40.0D;

    private FuelValueCalculator() {
    }

    public record FuelProfile(
            double combustibility,
            double chemicalEnergy,
            int ignitionTemperature
    ) {
        public FuelProfile {
            combustibility = clamp(combustibility);
            chemicalEnergy = clamp(chemicalEnergy);
            ignitionTemperature = Math.max(20, Math.min(2000, ignitionTemperature));
        }

        public boolean combustible() {
            return combustibility >= MIN_COMBUSTIBILITY && chemicalEnergy > 0.0D;
        }
    }

    /** Fuel Units for one full material unit before physical-form scaling. */
    public static double baseFuelUnits(IndustrialSubstance substance) {
        FuelProfile profile = profile(substance);
        if (!profile.combustible()) return 0.0D;

        // Energy is the source of Fuel Units; combustibility only discounts energy that cannot be
        // released efficiently as sustained combustion. Technology tier is intentionally absent.
        double usableFraction = 0.55D + 0.45D * (profile.combustibility() / 100.0D);
        return profile.chemicalEnergy() * usableFraction * FUEL_UNITS_PER_ENERGY_POINT;
    }

    public static double fuelUnits(IndustrialSubstance substance, FuelFormFactor factor) {
        double base = baseFuelUnits(substance);
        return base <= 0.0D ? 0.0D : factor.apply(base);
    }

    public static FuelProfile profile(IndustrialSubstance substance) {
        if (substance == null) return new FuelProfile(0.0D, 0.0D, 2000);
        return recursiveProfile(substance, new HashSet<>());
    }

    /** Human-readable Fuel Units for JEI/tooltips. */
    public static String displayFuelUnits(double fuelUnits) {
        if (!Double.isFinite(fuelUnits) || fuelUnits <= 0.0D) return "0";
        java.math.BigDecimal value = java.math.BigDecimal.valueOf(fuelUnits)
                .setScale(4, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return value.toPlainString();
    }

    private static FuelProfile recursiveProfile(IndustrialSubstance substance, Set<String> visiting) {
        String key = substance.getClass().getName() + ":" + substance.id();
        if (!visiting.add(key)) {
            throw new IllegalStateException("Cyclic .contains(...) graph while calculating fuel: " + substance.id());
        }
        try {
            FuelProfile intrinsic = intrinsicProfile(substance);
            List<MaterialComponent> components = componentsOf(substance);
            FuelProfile composition = weightedComposition(components, visiting);

            FuelProfile resolved;
            if (intrinsic != null && composition != null) {
                // Parent bulk properties describe the actual bonded substance; recursively resolved
                // components preserve composition identity and prevent one local score dominating.
                resolved = blend(intrinsic, composition, 0.65D);
            } else if (intrinsic != null) {
                resolved = intrinsic;
            } else if (composition != null) {
                resolved = composition;
            } else {
                resolved = new FuelProfile(0.0D, 0.0D, 2000);
            }

            OrganicFuelSignature organic = organicFuelSignature(substance, new HashSet<>());
            if (organic.organicFraction() > 0.0D) {
                resolved = applyOrganicFuelSignature(resolved, organic);
            }
            return resolved;
        } finally {
            visiting.remove(key);
        }
    }

    private static FuelProfile intrinsicProfile(IndustrialSubstance substance) {
        if (substance instanceof IndustrialMaterial material) {
            return fromProperties(material.properties());
        }
        if (substance instanceof ElementDefinition element) {
            return fromProperties(MaterialPropertyCalculator.calculate(element));
        }
        return null;
    }

    private static FuelProfile fromProperties(MaterialProperties p) {
        if (p == null || p.electronicFamily() == MaterialProperties.ElectronicFamily.NOBLE_GAS_LIKE) {
            return new FuelProfile(0.0D, 0.0D, 2000);
        }

        double reactivity = property(p, "reactivity", p::reactivity, 0);
        double donor = property(p, "electronDonationTendency", p::electronDonationTendency, 0);
        double electrochemical = property(p, "electrochemicalPotential", p::electrochemicalPotential, 0);
        double oxidationResistance = property(p, "oxidationResistance", p::oxidationResistance, 100);
        double oxidation = clamp(100.0D - oxidationResistance);
        double stability = property(p, "chemicalStability", p::chemicalStability, 70);
        double bond = property(p, "bondStrength", p::bondStrength, 50);
        double metallicity = property(p, "metallicity", p::metallicity, p.metal() ? 100 : 0);
        double heatCapacity = property(p, "specificHeatCapacity", p::specificHeatCapacity, 50);
        double volatility = volatility(p);
        double nonMetal = clamp(100.0D - metallicity);
        double stabilityWindow = clamp(100.0D - Math.abs(stability - 55.0D) * 1.6D);

        double combustibility = weighted(
                donor, 0.20D,
                electrochemical, 0.18D,
                oxidation, 0.18D,
                reactivity, 0.12D,
                volatility, 0.10D,
                nonMetal, 0.14D,
                stabilityWindow, 0.08D
        );

        double energy = weighted(
                donor, 0.28D,
                electrochemical, 0.26D,
                oxidation, 0.20D,
                reactivity, 0.10D,
                nonMetal, 0.10D,
                stabilityWindow, 0.06D
        );

        // Oxidizable metal is not synonymous with usable furnace fuel. Metallic lattices heavily
        // suppress sustained combustion even when electron donation/reactivity are high.
        if (p.metal() || metallicity >= 55.0D) {
            double latticeFactor = Math.max(0.06D, 1.0D - metallicity / 105.0D);
            combustibility *= latticeFactor * 0.30D;
            energy *= latticeFactor * 0.35D;
        }
        if (oxidation < 12.0D) combustibility *= 0.55D;
        if (stability > 90.0D && bond > 80.0D) combustibility *= 0.70D;

        int ignition = (int) Math.round(clampRange(
                120.0D
                        + stability * 6.0D
                        + bond * 4.0D
                        + oxidationResistance * 3.0D
                        + heatCapacity * 1.5D
                        - reactivity * 4.0D
                        - volatility * 3.5D,
                40.0D,
                2000.0D
        ));
        return new FuelProfile(combustibility, energy, ignition);
    }

    private static double volatility(MaterialProperties p) {
        if (p.state() == MaterialProperties.PhysicalState.GAS) return 100.0D;
        if (p.state() == MaterialProperties.PhysicalState.LIQUID) return 80.0D;
        double boiling = property(p, "boilingPoint", p::boilingPoint, 1200);
        return clamp(100.0D - Math.max(0.0D, boiling - 100.0D) / 11.0D);
    }

    private static FuelProfile weightedComposition(List<MaterialComponent> components, Set<String> visiting) {
        if (components == null || components.isEmpty()) return null;
        long total = 0L;
        double combustibility = 0.0D;
        double energy = 0.0D;
        double ignition = 0.0D;
        for (MaterialComponent component : components) {
            if (component == null || component.amount() <= 0) continue;
            FuelProfile child = recursiveProfile(component.substance(), visiting);
            int amount = component.amount();
            total += amount;
            combustibility += child.combustibility() * amount;
            energy += child.chemicalEnergy() * amount;
            ignition += child.ignitionTemperature() * (double) amount;
        }
        if (total <= 0L) return null;
        return new FuelProfile(combustibility / total, energy / total, (int) Math.round(ignition / total));
    }

    private static FuelProfile blend(FuelProfile parent, FuelProfile composition, double parentWeight) {
        double childWeight = 1.0D - parentWeight;
        return new FuelProfile(
                parent.combustibility() * parentWeight + composition.combustibility() * childWeight,
                parent.chemicalEnergy() * parentWeight + composition.chemicalEnergy() * childWeight,
                (int) Math.round(parent.ignitionTemperature() * parentWeight
                        + composition.ignitionTemperature() * childWeight)
        );
    }

    /**
     * Fictional organic combustion signature.  These are chemical roles, not item/name checks:
     * wood, plants, charcoal and future organics inherit the result recursively through .contains(...).
     *
     * <p>Resyra is the carbon-rich/charcoal-like energy carrier.  Lignara and Sylvara are the
     * structural plant fractions, while Dulcara is a sugar-like fraction that ignites easily.
     * The signature deliberately lives above elemental redox scoring: an oxidizable metal is not
     * fuel, but a material built from these organic compounds is.</p>
     */
    private record OrganicFuelSignature(
            double organicFraction,
            double energyDensity,
            int ignitionTemperature
    ) {
        private OrganicFuelSignature {
            organicFraction = clampRange(organicFraction, 0.0D, 1.0D);
            energyDensity = clamp(energyDensity);
            ignitionTemperature = Math.max(40, Math.min(2000, ignitionTemperature));
        }

        private static OrganicFuelSignature none() {
            return new OrganicFuelSignature(0.0D, 0.0D, 2000);
        }
    }

    private static FuelProfile applyOrganicFuelSignature(
            FuelProfile chemistry,
            OrganicFuelSignature organic
    ) {
        double fraction = organic.organicFraction();
        if (fraction <= 0.0D) return chemistry;

        // Fully organic materials are governed by the fictional organic chemistry itself.
        // This guarantees, for example, that Resyra-rich charcoal stores more usable chemical
        // energy than ordinary wood rather than letting an unrelated atomic proxy reverse them.
        if (fraction >= 0.999D) {
            return new FuelProfile(95.0D, organic.energyDensity(), organic.ignitionTemperature());
        }

        // Partly-organic compounds are naturally diluted by their inert/non-organic fraction.
        // Below roughly one third organic content the combustion score falls under the generic
        // fuel threshold unless the remaining chemistry is independently combustible.
        double organicCombustibility = clamp(30.0D + 65.0D * fraction);
        double organicEnergy = clamp(organic.energyDensity() * fraction);
        int dilutedIgnition = (int) Math.round(clampRange(
                organic.ignitionTemperature() + (1.0D - fraction) * 350.0D,
                40.0D,
                2000.0D
        ));

        return new FuelProfile(
                Math.max(chemistry.combustibility(), organicCombustibility),
                Math.max(chemistry.chemicalEnergy(), organicEnergy),
                Math.min(chemistry.ignitionTemperature(), dilutedIgnition)
        );
    }

    private static OrganicFuelSignature organicFuelSignature(
            IndustrialSubstance substance,
            Set<String> visiting
    ) {
        if (substance == null) return OrganicFuelSignature.none();

        // Canonical fictional organic fractions.  Values describe relative chemical-energy density
        // and ignition difficulty, not Minecraft burn time.  Resyra is intentionally the strongest
        // fuel and harder to ignite than ordinary wood-like structural compounds.
        if (substance == RESYRA) return new OrganicFuelSignature(1.0D, 100.0D, 420);
        if (substance == DULCARA) return new OrganicFuelSignature(1.0D, 82.0D, 180);
        if (substance == LIGNARA) return new OrganicFuelSignature(1.0D, 70.0D, 300);
        if (substance == SYLVARA) return new OrganicFuelSignature(1.0D, 60.0D, 280);

        String key = substance.getClass().getName() + ":" + substance.id();
        if (!visiting.add(key)) {
            throw new IllegalStateException(
                    "Cyclic .contains(...) graph while calculating organic fuel signature: " + substance.id()
            );
        }
        try {
            List<MaterialComponent> components = componentsOf(substance);
            if (components == null || components.isEmpty()) return OrganicFuelSignature.none();

            double totalAmount = 0.0D;
            double organicAmount = 0.0D;
            double energy = 0.0D;
            double ignition = 0.0D;
            for (MaterialComponent component : components) {
                if (component == null || component.amount() <= 0) continue;
                double amount = component.amount();
                totalAmount += amount;

                OrganicFuelSignature child = organicFuelSignature(component.substance(), visiting);
                double childOrganic = amount * child.organicFraction();
                if (childOrganic <= 0.0D) continue;
                organicAmount += childOrganic;
                energy += child.energyDensity() * childOrganic;
                ignition += child.ignitionTemperature() * childOrganic;
            }
            if (totalAmount <= 0.0D || organicAmount <= 0.0D) return OrganicFuelSignature.none();
            return new OrganicFuelSignature(
                    organicAmount / totalAmount,
                    energy / organicAmount,
                    (int) Math.round(ignition / organicAmount)
            );
        } finally {
            visiting.remove(key);
        }
    }

    private static List<MaterialComponent> componentsOf(IndustrialSubstance substance) {
        if (substance instanceof net.mads.industron.material.organism.BiologicalMaterial biological) return biological.components();
        if (substance instanceof OrganicMaterial organic) return organic.components();
        if (substance instanceof PlantDerivedSubstance derived) return derived.components();
        if (substance instanceof PlantProcessIntermediate intermediate) return intermediate.components();
        if (substance instanceof PlantMaterial plant) return plant.components();
        if (substance instanceof StructureMaterial structure) return structure.components();
        if (substance instanceof IndustrialMaterial material) return material.components();
        return List.of();
    }

    private static double property(MaterialProperties properties, String name, IntSupplier getter, double fallback) {
        if (!properties.hasProperty(name)) return clamp(fallback);
        try {
            return clamp(getter.getAsInt());
        } catch (IllegalStateException unavailable) {
            return clamp(fallback);
        }
    }

    private static double weighted(double... valuesAndWeights) {
        double weighted = 0.0D;
        double weights = 0.0D;
        for (int i = 0; i + 1 < valuesAndWeights.length; i += 2) {
            weighted += valuesAndWeights[i] * valuesAndWeights[i + 1];
            weights += valuesAndWeights[i + 1];
        }
        return weights <= 0.0D ? 0.0D : clamp(weighted / weights);
    }

    private static double clamp(double value) {
        return clampRange(value, 0.0D, 100.0D);
    }

    private static double clampRange(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) return minimum;
        return Math.max(minimum, Math.min(maximum, value));
    }
}
