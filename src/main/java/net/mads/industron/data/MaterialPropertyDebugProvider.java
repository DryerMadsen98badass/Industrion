package net.mads.industron.data;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.atomic.AtomicModel;
import net.mads.industron.material.atomic.IonState;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class MaterialPropertyDebugProvider implements DataProvider {
    private final PackOutput.PathProvider materials;

    public MaterialPropertyDebugProvider(PackOutput output) {
        this.materials = output.createPathProvider(
                PackOutput.Target.DATA_PACK,
                "material_debug"
        );
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        if (IndustrialMaterials.ALL.isEmpty()) {
            throw new IllegalStateException("No industrial materials registered for material property debug output");
        }
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            Path path = materials.file(
                    ResourceLocation.fromNamespaceAndPath(
                            Industron.MOD_ID,
                            material.id()
                    ),
                    "txt"
            );
            write(output, path, material);
        }
        return CompletableFuture.completedFuture(null);
    }

    private static void write(CachedOutput output, Path path, IndustrialMaterial material) {
        byte[] bytes = text(material).getBytes(StandardCharsets.UTF_8);
        HashCode hash = Hashing.sha1().hashBytes(bytes);
        try {
            output.writeIfNeeded(path, bytes, hash);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write material debug file: " + path, exception);
        }
    }

    private static String text(IndustrialMaterial material) {
        MaterialProperties properties = material.properties();
        StringBuilder builder = new StringBuilder();

        line(builder, "Material", material.displayName());
        line(builder, "ID", material.id());
        line(builder, "Symbol", material.elementSymbol().orElse(""));
        line(builder, "Atomic Number", material.atomicNumber());
        line(builder, "Tier", material.tier().id());
        builder.append('\n');

        section(builder, "Atomic Structure");
        line(builder, "Tier Multiplier", properties.tierMultiplier());
        line(builder, "Protons", properties.protons());
        line(builder, "Neutrons", properties.neutrons());
        line(builder, "Electrons", properties.electrons());
        line(builder, "Electron Shells", properties.electronShells());
        line(builder, "Outer Shell", properties.outerShell());
        line(builder, "Outer Shell Capacity", properties.outerShellCapacity());
        line(builder, "Outer Shell Electrons", properties.outerShellElectrons());
        line(builder, "Stable Valence Target", properties.stableValenceTarget());
        line(builder, "Electrons To Stable Shell", properties.electronsToStableShell());
        line(builder, "Electrons From Stable Shell", properties.electronsFromStableShell());
        line(builder, "Preferred Ion Charge", properties.preferredIonCharge());
        line(builder, "Unpaired Electrons", properties.unpairedElectrons());
        line(builder, "Ionization Energy", properties.ionizationEnergy());
        line(builder, "Electron Affinity", properties.electronAffinity());
        line(builder, "Electron Donation Tendency", properties.electronDonationTendency());
        line(builder, "Electron Acceptance Tendency", properties.electronAcceptanceTendency());
        line(builder, "Bond Strength", properties.bondStrength());
        line(builder, "Bond Energy", properties.bondEnergy());
        line(builder, "Atomic Stability", properties.atomicStability());
        line(builder, "Effective Nuclear Charge", properties.effectiveNuclearCharge());
        line(builder, "Atomic Radius", properties.atomicRadius());
        line(builder, "Valence s Electrons", properties.valenceSElectrons());
        line(builder, "Valence p Electrons", properties.valencePElectrons());
        line(builder, "Active d Electrons", properties.activeDElectrons());
        line(builder, "Active f Electrons", properties.activeFElectrons());
        line(builder, "Electronic Family", properties.electronicFamily());
        builder.append('\n');

        if (material.elementSymbol().isPresent()) {
            section(builder, "Ion States");
            java.util.List<IonState> ions = AtomicModel.allowedIonStates(material.atomicNumber());
            line(builder, "Allowed Charges", ions.stream().map(IonState::charge).toList());
            line(builder, "Preferred Charge", AtomicModel.preferredIonState(material.atomicNumber())
                    .map(IonState::charge)
                    .orElse(0));
            for (IonState ion : ions) {
                line(builder,
                        "Ion " + (ion.charge() > 0 ? "+" : "") + ion.charge(),
                        "electrons=" + ion.electronCount()
                                + ", stability=" + ion.electronicStability()
                                + ", formationCost=" + ion.formationCost()
                                + ", viability=" + ion.viabilityScore()
                                + ", family=" + ion.electronicFamily());
            }
            builder.append('\n');
        }

        section(builder, "Classification");
        line(builder, "State", properties.state());
        line(builder, "Metallicity", properties.metallicity());
        line(builder, "Metallicity Class", properties.metallicityClass());
        line(builder, "Metal", properties.metal());
        line(builder, "Magnetic", properties.magnetic());
        line(builder, "Crystalline", properties.crystalline());
        line(builder, "Gem Candidate", properties.gemCandidate());
        if (properties.hasProperty("electricalBehavior")) line(builder, "Electrical Behavior", properties.electricalBehavior());
        line(builder, "Heat Resistant", properties.heatResistant());
        line(builder, "Pressure Resistant", properties.pressureResistant());
        builder.append('\n');

        section(builder, "Mechanical");
        if (properties.hasProperty("density")) line(builder, "Density", properties.density());
        if (properties.hasProperty("hardness")) line(builder, "Hardness", properties.hardness());
        line(builder, "Elasticity", properties.elasticity());
        line(builder, "Tensile Strength", properties.tensileStrength());
        line(builder, "Yield Strength", properties.yieldStrength());
        line(builder, "Fracture Toughness", properties.fractureToughness());
        line(builder, "Compressive Strength", properties.compressiveStrength());
        line(builder, "Ductility", properties.ductility());
        line(builder, "Brittleness", properties.brittleness());
        line(builder, "Wear Resistance", properties.wearResistance());
        line(builder, "Fatigue Resistance", properties.fatigueResistance());
        builder.append('\n');

        section(builder, "Thermal");
        if (properties.hasProperty("meltingPoint")) line(builder, "Melting Point", properties.meltingPoint());
        line(builder, "Boiling Point", properties.boilingPoint());
        line(builder, "Thermal Conductivity", properties.thermalConductivity());
        line(builder, "Specific Heat Capacity", properties.specificHeatCapacity());
        line(builder, "Thermal Expansion", properties.thermalExpansion());
        line(builder, "Max Operating Temperature", properties.maxOperatingTemperature());
        line(builder, "Thermal Shock Resistance", properties.thermalShockResistance());
        builder.append('\n');

        section(builder, "Electrical");
        if (properties.hasProperty("electricalConductivity")) line(builder, "Electrical Conductivity", properties.electricalConductivity());
        line(builder, "Insulation Strength", properties.insulationStrength());
        line(builder, "Electrochemical Potential", properties.electrochemicalPotential());
        line(builder, "Charge Storage Potential", properties.chargeStoragePotential());
        line(builder, "Battery Potential", properties.batteryPotential());
        builder.append('\n');

        section(builder, "Chemical");
        if (properties.hasProperty("corrosionResistance")) line(builder, "Corrosion Resistance", properties.corrosionResistance());
        line(builder, "Chemical Stability", properties.chemicalStability());
        if (properties.hasProperty("reactivity")) line(builder, "Reactivity", properties.reactivity());
        line(builder, "Oxidation Resistance", properties.oxidationResistance());
        line(builder, "Acidity", properties.acidity());
        builder.append('\n');

        section(builder, "Pressure");
        line(builder, "Pressure Resistance", properties.pressureResistance());
        line(builder, "Structural Strength", properties.structuralStrength());
        line(builder, "Max Pressure", properties.maxPressure());
        builder.append('\n');

        section(builder, "Magnetic");
        line(builder, "Magnetic Tendency", properties.magneticTendency());
        line(builder, "Magnetic Strength", properties.magneticStrength());
        builder.append('\n');

        section(builder, "Processing");
        line(builder, "Machinability", properties.machinability());
        line(builder, "Formability", properties.formability());
        line(builder, "Weldability", properties.weldability());
        line(builder, "Castability", properties.castability());
        builder.append('\n');

        section(builder, "Crystal And Gem");
        line(builder, "Crystal Structure", properties.crystalStructure());
        line(builder, "Crystal Stability", properties.crystalStability());
        line(builder, "Transparency", properties.transparency());
        line(builder, "Refractive Index", properties.refractiveIndex());
        line(builder, "Luster", properties.luster());
        line(builder, "Cleavage", properties.cleavage());
        line(builder, "Crystal Hardness", properties.crystalHardness());
        line(builder, "Fracture Behavior", properties.fractureBehavior());
        line(builder, "Impurity Tolerance", properties.impurityTolerance());
        line(builder, "Optical Purity", properties.opticalPurity());
        line(builder, "Crystal Growth Difficulty", properties.crystalGrowthDifficulty());
        line(builder, "Crystal Formation Temperature", properties.crystalFormationTemperature());
        line(builder, "Crystal Formation Pressure", properties.crystalFormationPressure());
        line(builder, "Gem Quality", properties.gemQuality());
        builder.append('\n');

        section(builder, "Derived And Fuel");
        line(builder, "Ambient Temperature", properties.ambientTemperature());
        line(builder, "Cast Temperature", properties.castTemperature());
        line(builder, "Radioactivity", properties.radioactivity());
        line(builder, "Furnace Fuel Potential", properties.furnaceFuelPotential());
        line(builder, "Furnace Fuel", properties.furnaceFuel());
        line(builder, "Furnace Burn Time Ticks", properties.furnaceBurnTimeTicks());
        builder.append('\n');

        section(builder, "Color Data");
        line(builder, "Base Color", color(properties.baseColor()));
        line(builder, "Highlight Color", color(properties.highlightColor()));
        line(builder, "Shadow Color", color(properties.shadowColor()));
        line(builder, "Brightness", properties.brightness());
        line(builder, "Emissive Strength", properties.emissiveStrength());

        return builder.toString();
    }

    private static void section(StringBuilder builder, String name) {
        builder.append("[").append(name).append("]").append('\n');
    }

    private static void line(StringBuilder builder, String name, Object value) {
        builder.append(name).append(": ").append(value).append('\n');
    }

    private static String color(int color) {
        return String.format("#%06X", color);
    }

    @Override
    public String getName() {
        return "Industron Material Property Debug";
    }
}
