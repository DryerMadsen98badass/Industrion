package net.mads.industron.client;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import net.mads.industron.Industron;
import net.mads.industron.energy.EnergyWireBlock;
import net.mads.industron.fluid.IndustrialFluid;
import net.mads.industron.machine.MachineTierStats;
import net.mads.industron.machine.MaterialMachineCasingBlock;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.transport.FluidTransportRates;
import net.mads.industron.transport.TieredFluidTank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = Industron.MOD_ID, value = Dist.CLIENT)
public final class ClientMaterialTooltip {
    private ClientMaterialTooltip() {
    }

    @SubscribeEvent
    public static void addMaterialTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        if (player == null || !GogglesItem.isWearingGoggles(player)) {
            return;
        }

        if (event.getItemStack().getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof MaterialMachineCasingBlock casing) {
            tooltipTier(event.getToolTip(), casing);
            return;
        }

        if (event.getItemStack().getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof EnergyWireBlock wire) {
            addWireTooltipLines(event.getToolTip(), wire);
            return;
        }

        MaterialLookup.MaterialTarget target = MaterialLookup.find(event.getItemStack());
        if (target != null) {
            addMaterialTooltipLines(event.getToolTip(), target);
        }

        if (event.getItemStack().getItem() instanceof BlockItem blockItem) {
            addFluidTransportTooltipLines(event.getToolTip(), blockItem.getBlock());
        }
    }

    private static void tooltipTier(List<Component> tooltip, MaterialMachineCasingBlock casing) {
        tooltip.add(colored("Tier: ", 0xB0B0B0)
                .append(colored(casing.tier().displayName(), casing.tier().color())));
    }

    private static void addWireTooltipLines(List<Component> tooltip, EnergyWireBlock wire) {
        tooltip.add(colored("Tier: ", 0xB0B0B0).append(colored(wire.tier().displayName(), wire.tier().color())));
        tooltip.add(colored("CE: ", 0x4E8FDC).append(colored(Long.toString(MachineTierStats.ceTier(wire.tier())), 0xFFFFFF)));
        tooltip.add(colored("Amps: ", 0xE0A83A).append(colored(Integer.toString(wire.maxAmps()), 0xFFFFFF)));
    }

    private static void addFluidTransportTooltipLines(List<Component> tooltip, Block block) {
        if (block instanceof PumpBlock) {
            double rate = FluidTransportRates.pumpRate(block.defaultBlockState());
            if (rate > 0.0D) {
                tooltip.add(Component.literal("Fluid Transfer: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(formatOneDecimal(rate) + " mB/t/RPM").withStyle(ChatFormatting.AQUA)));
            }
        }

        if (block instanceof FluidPipeBlock || block instanceof GlassFluidPipeBlock || block instanceof EncasedPipeBlock) {
            int rate = FluidTransportRates.pipeRate(block.defaultBlockState());
            if (rate != Integer.MAX_VALUE) {
                tooltip.add(Component.literal("Fluid Throughput: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(rate + " mB/t").withStyle(ChatFormatting.AQUA)));
            }
            int maxTemperature = FluidTransportRates.maxFluidTemperature(block.defaultBlockState());
            if (maxTemperature != Integer.MAX_VALUE) {
                tooltip.add(Component.literal("Max Fluid Temperature: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(maxTemperature + " C").withStyle(ChatFormatting.GOLD)));
                tooltip.add(Component.literal("Chemical Range: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(
                                FluidTransportRates.minChemicalRange(block.defaultBlockState()) + " to "
                                        + FluidTransportRates.maxChemicalRange(block.defaultBlockState())
                        ).withStyle(ChatFormatting.GREEN)));
            }
        }

        if (block instanceof TieredFluidTank tank) {
            tooltip.add(Component.literal("Fluid Capacity: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(tank.transportTier().tankCapacity() + " mB").withStyle(ChatFormatting.AQUA)));
        }
    }

    private static String formatOneDecimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static String formatDecimal(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    public static void addMaterialTooltipLines(List<Component> tooltip, MaterialLookup.MaterialTarget target) {
        IndustrialMaterial material = target.material();
        Component formula = formulaLine(material);
        if (formula != null) {
            tooltip.add(formula);
        }
        MaterialProperties properties = material.properties();
        tooltip.add(colored("Tier: ", 0xB0B0B0).append(colored(material.tier().displayName(), material.tier().color())));
        if (material.atomicNumber() > 0) {
            tooltip.add(colored("Atomic Number: ", 0xB0B0B0).append(colored(Integer.toString(material.atomicNumber()), 0xFFFFFF)));
        }
        tooltip.add(colored("State: ", 0xFF66CC).append(colored(materialState(material, target.part()), 0xFF66CC)));
        tooltip.add(colored("Density: ", 0x9FD3FF).append(colored(Integer.toString(properties.density()), 0xFFFFFF)));
        tooltip.add(colored("Hardness: ", 0x2ECC40).append(colored(Integer.toString(properties.hardness()), 0xFFFFFF)));
        tooltip.add(colored("Melting Point: ", 0xFFD800).append(colored(properties.meltingPoint() + " C", 0xFF3333)));
        tooltip.add(colored("Electrical Behavior: ", 0x66D9EF).append(colored(displayName(properties.electricalBehavior().name()), 0xFFFFFF)));
        if (properties.electricallyConductive()) {
            tooltip.add(colored("Electrical Conductivity: ", 0x66D9EF).append(colored(Integer.toString(properties.electricalConductivity()), 0xFFFFFF)));
        } else {
            tooltip.add(colored("Insulation Strength: ", 0x66D9EF).append(colored(Integer.toString(properties.insulationStrength()), 0xFFFFFF)));
        }
        tooltip.add(colored("Corrosion Resistance: ", 0x9BE564).append(colored(Integer.toString(properties.corrosionResistance()), 0xFFFFFF)));
        if (material.radioactivity() > 0) {
            tooltip.add(colored("Radioactivity: ", 0xBFFF00).append(colored(Integer.toString(material.radioactivity()), 0xBFFF00)));
        }
        if (showsTemperature(target.part())) {
            tooltip.add(colored("Temperature: ", 0xFF3333).append(colored(material.temperatureFor(target.part()) + " C", 0xFF3333)));
        }
    }

    private static void addAdvancedMaterialTooltipLines(List<Component> tooltip, MaterialProperties properties) {
        tooltip.add(colored("Atomic Structure", 0xAAAAAA));
        addPropertyLine(tooltip, "Tier Multiplier", properties.tierMultiplier(), 0xFFFFFF);
        addPropertyLine(tooltip, "Protons", properties.protons(), 0xFFFFFF);
        addPropertyLine(tooltip, "Neutrons", properties.neutrons(), 0xFFFFFF);
        addPropertyLine(tooltip, "Electron Shells", properties.electronShells().toString(), 0xFFFFFF);
        addPropertyLine(tooltip, "Outer Shell", properties.outerShell(), 0xFFFFFF);
        addPropertyLine(tooltip, "Outer Shell Capacity", properties.outerShellCapacity(), 0xFFFFFF);
        addPropertyLine(tooltip, "Outer Electrons", properties.outerShellElectrons(), 0xFFFFFF);
        addPropertyLine(tooltip, "Stable Valence Target", properties.stableValenceTarget(), 0xFFFFFF);
        addPropertyLine(tooltip, "Electrons To Stable Shell", properties.electronsToStableShell(), 0xFFFFFF);
        addPropertyLine(tooltip, "Electrons From Stable Shell", properties.electronsFromStableShell(), 0xFFFFFF);
        addPropertyLine(tooltip, "Preferred Ion Charge", properties.preferredIonCharge(), 0xFFFFFF);
        addPropertyLine(tooltip, "Unpaired Electrons", properties.unpairedElectrons(), 0xC792EA);
        addPropertyLine(tooltip, "Ionization Energy", properties.ionizationEnergy(), 0x66D9EF);
        addPropertyLine(tooltip, "Electron Affinity", properties.electronAffinity(), 0x66D9EF);
        addPropertyLine(tooltip, "Electron Donation Tendency", properties.electronDonationTendency(), 0x9BE564);
        addPropertyLine(tooltip, "Electron Acceptance Tendency", properties.electronAcceptanceTendency(), 0x9BE564);
        addPropertyLine(tooltip, "Bond Strength", properties.bondStrength(), 0xFFD800);
        addPropertyLine(tooltip, "Bond Energy", properties.bondEnergy(), 0xFFD800);
        addPropertyLine(tooltip, "Atomic Stability", properties.atomicStability(), 0x9BE564);

        tooltip.add(colored("Mechanical", 0xAAAAAA));
        addPropertyLine(tooltip, "Elasticity", properties.elasticity(), 0xB0FFC8);
        addPropertyLine(tooltip, "Tensile Strength", properties.tensileStrength(), 0xB0FFC8);
        addPropertyLine(tooltip, "Yield Strength", properties.yieldStrength(), 0xB0FFC8);
        addPropertyLine(tooltip, "Fracture Toughness", properties.fractureToughness(), 0xB0FFC8);
        addPropertyLine(tooltip, "Compressive Strength", properties.compressiveStrength(), 0xB0FFC8);
        addPropertyLine(tooltip, "Ductility", properties.ductility(), 0xB0FFC8);
        addPropertyLine(tooltip, "Brittleness", properties.brittleness(), 0xB0FFC8);
        addPropertyLine(tooltip, "Wear Resistance", properties.wearResistance(), 0xB0FFC8);
        addPropertyLine(tooltip, "Fatigue Resistance", properties.fatigueResistance(), 0xB0FFC8);

        tooltip.add(colored("Thermal / Electrical", 0xAAAAAA));
        addPropertyLine(tooltip, "Boiling Point", properties.boilingPoint() + " C", 0xFFD800);
        addPropertyLine(tooltip, "Thermal Conductivity", properties.thermalConductivity(), 0xFFD800);
        addPropertyLine(tooltip, "Specific Heat Capacity", properties.specificHeatCapacity(), 0xFFD800);
        addPropertyLine(tooltip, "Thermal Expansion", properties.thermalExpansion(), 0xFFD800);
        addPropertyLine(tooltip, "Max Operating Temperature", properties.maxOperatingTemperature() + " C", 0xFFD800);
        addPropertyLine(tooltip, "Thermal Shock Resistance", properties.thermalShockResistance(), 0xFFD800);
        addPropertyLine(tooltip, "Electrical Behavior", displayName(properties.electricalBehavior().name()), 0x66D9EF);
        addPropertyLine(tooltip, "Electrical Conductivity", properties.electricalConductivity(), 0x66D9EF);
        addPropertyLine(tooltip, "Insulation Strength", properties.insulationStrength(), 0x66D9EF);

        tooltip.add(colored("Chemical / Structural", 0xAAAAAA));
        addPropertyLine(tooltip, "Chemical Stability", properties.chemicalStability(), 0x9BE564);
        addPropertyLine(tooltip, "Reactivity", properties.reactivity(), 0x9BE564);
        addPropertyLine(tooltip, "Oxidation Resistance", properties.oxidationResistance(), 0x9BE564);
        addPropertyLine(tooltip, "Acidity", properties.acidity(), 0x9BE564);
        addPropertyLine(tooltip, "Pressure Resistance", properties.pressureResistance(), 0x9FD3FF);
        addPropertyLine(tooltip, "Structural Strength", properties.structuralStrength(), 0x9FD3FF);
        addPropertyLine(tooltip, "Max Pressure", properties.maxPressure(), 0x9FD3FF);

        tooltip.add(colored("Magnetic / Processing", 0xAAAAAA));
        addPropertyLine(tooltip, "Magnetic Tendency", properties.magneticTendency(), 0xC792EA);
        addPropertyLine(tooltip, "Magnetic Strength", properties.magneticStrength(), 0xC792EA);
        addPropertyLine(tooltip, "Machinability", properties.machinability(), 0xF2C68B);
        addPropertyLine(tooltip, "Formability", properties.formability(), 0xF2C68B);
        addPropertyLine(tooltip, "Weldability", properties.weldability(), 0xF2C68B);
        addPropertyLine(tooltip, "Castability", properties.castability(), 0xF2C68B);

        tooltip.add(colored("Crystal / Color", 0xAAAAAA));
        addPropertyLine(tooltip, "Crystal Structure", displayName(properties.crystalStructure().name()), 0xD6ACFF);
        addPropertyLine(tooltip, "Crystal Stability", properties.crystalStability(), 0xD6ACFF);
        addPropertyLine(tooltip, "Transparency", properties.transparency(), 0xD6ACFF);
        addPropertyLine(tooltip, "Refractive Index", properties.refractiveIndex(), 0xD6ACFF);
        addPropertyLine(tooltip, "Luster", properties.luster(), 0xD6ACFF);
        addPropertyLine(tooltip, "Cleavage", properties.cleavage(), 0xD6ACFF);
        addPropertyLine(tooltip, "Crystal Hardness", properties.crystalHardness(), 0xD6ACFF);
        addPropertyLine(tooltip, "Fracture Behavior", displayName(properties.fractureBehavior().name()), 0xD6ACFF);
        addPropertyLine(tooltip, "Impurity Tolerance", properties.impurityTolerance(), 0xD6ACFF);
        addPropertyLine(tooltip, "Optical Purity", properties.opticalPurity(), 0xD6ACFF);
        addPropertyLine(tooltip, "Crystal Growth Difficulty", properties.crystalGrowthDifficulty(), 0xD6ACFF);
        addPropertyLine(tooltip, "Crystal Formation Temperature", properties.crystalFormationTemperature() + " C", 0xD6ACFF);
        addPropertyLine(tooltip, "Crystal Formation Pressure", properties.crystalFormationPressure(), 0xD6ACFF);
        addPropertyLine(tooltip, "Gem Quality", properties.gemQuality(), 0xD6ACFF);
        addPropertyLine(tooltip, "Highlight Color", hexColor(properties.highlightColor()), properties.highlightColor());
        addPropertyLine(tooltip, "Shadow Color", hexColor(properties.shadowColor()), properties.shadowColor());
        addPropertyLine(tooltip, "Brightness", properties.brightness(), 0xFFFFFF);
        addPropertyLine(tooltip, "Emissive Strength", properties.emissiveStrength(), 0xFFFFFF);

        tooltip.add(colored("Derived / Fuel", 0xAAAAAA));
        addPropertyLine(tooltip, "Ambient Temperature", properties.ambientTemperature() + " C", 0xFF3333);
        addPropertyLine(tooltip, "Cast Temperature", properties.castTemperature() + " C", 0xFF3333);
        addPropertyLine(tooltip, "Radioactivity", properties.radioactivity(), 0xBFFF00);
        addPropertyLine(tooltip, "Furnace Fuel Potential", properties.furnaceFuelPotential(), 0xF2C68B);
        addPropertyLine(tooltip, "Furnace Fuel", yesNo(properties.furnaceFuel()), 0xF2C68B);
        addPropertyLine(tooltip, "Furnace Burn Time", properties.furnaceBurnTimeTicks() + " ticks", 0xF2C68B);

        tooltip.add(colored("Classification", 0xAAAAAA));
        addPropertyLine(tooltip, "Metallicity", properties.metallicity(), 0xFFFFFF);
        addPropertyLine(tooltip, "Metallicity Class", displayName(properties.metallicityClass().name()), 0xFFFFFF);
        addPropertyLine(tooltip, "Metal", yesNo(properties.metal()), 0xFFFFFF);
        addPropertyLine(tooltip, "Magnetic", yesNo(properties.magnetic()), 0xFFFFFF);
        addPropertyLine(tooltip, "Crystalline", yesNo(properties.crystalline()), 0xFFFFFF);
        addPropertyLine(tooltip, "Gem Candidate", yesNo(properties.gemCandidate()), 0xFFFFFF);
        addPropertyLine(tooltip, "Electrical Behavior", displayName(properties.electricalBehavior().name()), 0xFFFFFF);
        addPropertyLine(tooltip, "Heat Resistant", yesNo(properties.heatResistant()), 0xFFFFFF);
        addPropertyLine(tooltip, "Pressure Resistant", yesNo(properties.pressureResistant()), 0xFFFFFF);
    }

    private static void addPropertyLine(List<Component> tooltip, String label, int value, int color) {
        addPropertyLine(tooltip, label, Integer.toString(value), color);
    }

    private static void addPropertyLine(List<Component> tooltip, String label, String value, int color) {
        tooltip.add(colored(label + ": ", 0x808080).append(colored(value, color)));
    }

    private static Component formulaLine(IndustrialMaterial material) {
        MutableComponent formula = formulaComponent(material, false);
        return formula == null ? null : Component.literal("Formula: ").withStyle(ChatFormatting.BLUE).append(formula);
    }

    private static MutableComponent formulaComponent(IndustrialSubstance substance, boolean nested) {
        if (substance instanceof IndustrialMaterial material) {
            if (material.elementSymbol().isPresent()) {
                return colored(material.elementSymbol().get(), material.color());
            }
            return compoundFormulaComponent(material, material.components(), nested);
        }
        if (substance instanceof IndustrialFluid fluid) {
            return compoundFormulaComponent(fluid, fluid.components(), nested);
        }
        String formula = substance.formula();
        return formula.isBlank() ? null : colored(formula, substance.color());
    }

    private static MutableComponent compoundFormulaComponent(IndustrialSubstance substance, List<MaterialComponent> components, boolean nested) {
        if (components.isEmpty()) {
            String formula = substance.formula();
            return formula.isBlank() ? null : colored(formula, substance.color());
        }
        MutableComponent result = Component.empty();
        if (nested) {
            result.append(colored("(", substance.color()));
        }
        for (MaterialComponent component : components) {
            IndustrialSubstance componentSubstance = component.substance();
            MutableComponent componentFormula = formulaComponent(componentSubstance, hasNestedComponents(componentSubstance));
            if (componentFormula == null) {
                componentFormula = colored(componentSubstance.displayName(), componentSubstance.color());
            }
            result.append(componentFormula);
            if (component.amount() > 1) {
                result.append(colored(toSubscript(component.amount()), componentSubstance.color()));
            }
        }
        if (nested) {
            result.append(colored(")", substance.color()));
        }
        return result;
    }

    private static boolean hasNestedComponents(IndustrialSubstance substance) {
        if (substance instanceof IndustrialMaterial material) {
            return material.elementSymbol().isEmpty() && !material.components().isEmpty();
        }
        if (substance instanceof IndustrialFluid fluid) {
            return !fluid.components().isEmpty();
        }
        return false;
    }

    private static String materialState(IndustrialMaterial material, MaterialPart part) {
        if (part == MaterialPart.MOLTEN_FLUID) {
            return material.properties().state() == MaterialProperties.PhysicalState.GAS ? "Gas" : "Fluid";
        }
        return switch (material.properties().state()) {
            case GAS -> "Gas";
            case LIQUID -> "Fluid";
            case SOLID -> "Solid";
        };
    }

    private static boolean showsTemperature(MaterialPart part) {
        return part == MaterialPart.MOLTEN_FLUID
                || (part.name().startsWith("CAST_") && !part.name().endsWith("_MOLD"))
                || part.name().startsWith("HOT_CAST_");
    }

    private static MutableComponent colored(String text, int color) {
        return Component.literal(text).withStyle(style -> style.withColor(TextColor.fromRgb(color)));
    }

    private static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }

    private static String hexColor(int color) {
        return String.format(java.util.Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    private static String displayName(String enumName) {
        String[] words = enumName.toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }
        return result.toString();
    }

    private static String toSubscript(int number) {
        String value = Integer.toString(number);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            result.append(switch (value.charAt(i)) {
                case '0' -> '\u2080';
                case '1' -> '\u2081';
                case '2' -> '\u2082';
                case '3' -> '\u2083';
                case '4' -> '\u2084';
                case '5' -> '\u2085';
                case '6' -> '\u2086';
                case '7' -> '\u2087';
                case '8' -> '\u2088';
                case '9' -> '\u2089';
                default -> value.charAt(i);
            });
        }
        return result.toString();
    }
}
