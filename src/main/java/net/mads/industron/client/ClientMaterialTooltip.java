package net.mads.industron.client;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import net.mads.industron.Industron;
import net.mads.industron.energy.EnergyWireBlock;
import net.mads.industron.item.CreativeGogglesItem;
import net.mads.industron.fluid.IndustrialFluid;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.mads.industron.machine.MaterialMachineCasingBlock;
import net.mads.industron.machine.foundry.CastingBlock;
import net.mads.industron.machine.foundry.CastingMoldItem;
import net.mads.industron.material.CompoundMaterialPropertyCalculator;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialComponentWeights;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.MaterialVariantResolver;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.material.structure.StructureMaterialItem;
import net.mads.industron.recipe.CEChancedItemOutput;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.primitive.PrimitiveSiftingRules;
import net.mads.industron.transport.FluidTransportRates;
import net.mads.industron.transport.TieredFluidTank;
import net.mads.industron.tool.ToolMaterialLookup;
import net.mads.industron.tool.ToolMaterialRules;
import net.mads.industron.tool.ToolMaterialStatCalculator;
import net.mads.industron.tool.ToolPartStats;
import net.mads.industron.tool.EquipmentStats;
import net.mads.industron.tool.EquipmentTooltip;
import net.mads.industron.tool.ToolStatPresentation;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Industron.MOD_ID, value = Dist.CLIENT)
public final class ClientMaterialTooltip {
    private static final Map<WoodMaterial, MaterialProperties> WOOD_PROPERTIES =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private ClientMaterialTooltip() {
    }

    @SubscribeEvent
    public static void addMaterialTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(Items.CAMPFIRE)) {
            event.getToolTip().add(Component.literal("Requires fuel.").withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.literal("Light with Flint and Steel or Flint and Pebble.")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        Player player = event.getEntity();
        if (CreativeGogglesItem.isWearing(player)) {
            addCreativeForgingDebug(event);
        }

        ToolMaterialLookup.Target toolPart = ToolMaterialLookup.find(event.getItemStack());
        if (toolPart != null && ToolMaterialRules.isToolPartForm(toolPart.part())) {
            MaterialPart statsPart = MaterialVariantResolver.coldTexturePart(toolPart.part());
            if (!ToolMaterialRules.isDirectFinishedToolPart(toolPart.part())
                    && ToolMaterialRules.allows(toolPart.material(), statsPart)) {
                if (EquipmentStats.isEquipmentPart(statsPart)) {
                    EquipmentTooltip.appendPart(event.getToolTip(), toolPart.material(), statsPart);
                } else {
                    addToolPartTooltipLines(event.getToolTip(), statsPart,
                            ToolMaterialStatCalculator.calculate(toolPart.material(), statsPart));
                }
                if (ToolMaterialRules.isHotToolPart(toolPart.part())) {
                    event.getToolTip().add(Component.literal("Cool before assembly.").withStyle(ChatFormatting.RED));
                }
            }
            // Tool-shaped parts never inherit the generic material-property tooltip. Direct
            // finished tools append their live durability/tool stats from their own Item class.
            return;
        }

        if (player == null || !GogglesItem.isWearingGoggles(player)) {
            return;
        }

        if (event.getItemStack().getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof MaterialMachineCasingBlock casing) {
            tooltipTier(event.getToolTip(), casing);
            return;
        }

        if (event.getItemStack().getItem() instanceof CastingMoldItem mold) {
            addCeramicOperatingTooltipLines(event.getToolTip(), mold.clay());
            return;
        }

        if (event.getItemStack().getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof CastingBlock castingBlock) {
            addCeramicOperatingTooltipLines(event.getToolTip(), castingBlock.clay());
            return;
        }

        if (event.getItemStack().getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof EnergyWireBlock wire) {
            addWireTooltipLines(event.getToolTip(), wire);
            return;
        }

        if (event.getItemStack().getItem() instanceof StructureMaterialItem structureItem) {
            if (structureItem.material() instanceof StoneMaterial stone
                    && isStoneDust(structureItem.part())) {
                addStoneContentsTooltip(event.getToolTip(), stone);
                return;
            }
            if (structureItem.material() instanceof WoodMaterial wood
                    && isWoodPulp(structureItem.part())) {
                addWoodMaterialTooltipLines(event.getToolTip(), wood);
                return;
            }
        }

        MaterialLookup.MaterialTarget target = MaterialLookup.find(event.getItemStack());
        if (target != null) {
            addMaterialTooltipLines(event.getToolTip(), target);
        }

        if (event.getItemStack().getItem() instanceof BlockItem blockItem) {
            addFluidTransportTooltipLines(event.getToolTip(), blockItem.getBlock());
        }
    }


    private static void addCreativeForgingDebug(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        var tool = AssemblyTools.findAny(stack);
        if (tool != null) {
            var definition = AssemblyTools.definition(tool.type());
            if (definition != null && definition.canForgeOnAnvil()) {
                event.getToolTip().add(colored("Forge formula: ", 0x55AAFF)
                        .append(colored(definition.forgeFormula(), 0xFFFFFF)));
            }
        }

        MaterialLookup.MaterialTarget target = MaterialLookup.find(stack);
        if (target != null) {
            MaterialPart part = target.part().coldForgePart();
            if (part.hasForgeValue()) {
                event.getToolTip().add(colored("Forge value: ", 0x55AAFF)
                        .append(colored(Integer.toString(part.forgeValue()), 0xFFFFFF)));
            }
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

    private static void addToolPartTooltipLines(List<Component> tooltip, MaterialPart part, ToolPartStats stats) {
        tooltip.add(colored("Tier: ", 0xB0B0B0)
                .append(colored(stats.tier().displayName(), stats.tier().color())));
        tooltip.add(colored("Durability: ", 0x55FF55)
                .append(colored(Integer.toString(stats.durability()), 0x55FF55)));
        if (ToolStatPresentation.showsEfficiency(part)) tooltip.add(colored("Efficiency: ", 0x55FFFF)
                .append(colored(formatDecimal(stats.efficiencySeconds()), 0x55FFFF)));
        if (ToolStatPresentation.showsDamage(part)) tooltip.add(colored("Damage: ", 0xFF5555)
                .append(colored(formatDecimal(stats.damage()), 0xFF5555)));
    }

    private static boolean isStoneDust(MaterialPart part) {
        return part == MaterialPart.DUST
                || part == MaterialPart.SMALL_DUST
                || part == MaterialPart.TINY_DUST;
    }

    private static boolean isWoodPulp(MaterialPart part) {
        return part == MaterialPart.WOOD_PULP
                || part == MaterialPart.SMALL_WOOD_PULP
                || part == MaterialPart.TINY_WOOD_PULP;
    }

    private static void addStoneContentsTooltip(List<Component> tooltip, StoneMaterial stone) {
        if (stone.components().isEmpty()) {
            return;
        }

        MutableComponent contents = colored("Contains (on find): ", 0xB0B0B0);
        for (int i = 0; i < stone.components().size(); i++) {
            IndustrialSubstance substance = stone.components().get(i).substance();
            if (i > 0) {
                contents.append(colored(", ", 0x808080));
            }
            MaterialComponent component = stone.components().get(i);
            contents.append(colored(substance.displayName(), substance.color() & 0x00FFFFFF));
            contents.append(colored(" x" + component.amount(), substance.color() & 0x00FFFFFF));
            contents.append(colored(
                    " (" + formatDecimal(MaterialComponentWeights.percentage(component, stone.components())) + "% of finds)",
                    0xB0B0B0
            ));
        }
        tooltip.add(contents);
        tooltip.add(colored("Primitive sieve find chance: ", 0xB0B0B0)
                .append(colored(
                        formatDecimal(PrimitiveSiftingRules.FIND_CHANCE * 100.0D
                                / CEChancedItemOutput.MAX_CHANCE) + "%",
                        0xFFFFFF
                )));
    }

    private static void addWoodContentsTooltip(List<Component> tooltip, WoodMaterial wood) {
        if (wood.components().isEmpty()) {
            return;
        }

        MutableComponent contents = colored("Contains: ", 0xB0B0B0);
        for (int i = 0; i < wood.components().size(); i++) {
            MaterialComponent component = wood.components().get(i);
            IndustrialSubstance substance = component.substance();
            if (i > 0) {
                contents.append(colored(", ", 0x808080));
            }
            contents.append(colored(substance.displayName(), substance.color() & 0x00FFFFFF));
            contents.append(colored(" x" + component.amount(), substance.color() & 0x00FFFFFF));
            contents.append(colored(
                    " (" + formatDecimal(MaterialComponentWeights.percentage(component, wood.components())) + "%)",
                    0xB0B0B0
            ));
        }
        tooltip.add(contents);
    }

    private static void addWoodMaterialTooltipLines(
            List<Component> tooltip,
            WoodMaterial wood
    ) {
        Component formula = formulaLine(wood);
        if (formula != null) tooltip.add(formula);

        MaterialProperties properties = WOOD_PROPERTIES.computeIfAbsent(wood, CompoundMaterialPropertyCalculator::propertiesFor);
        int tierIndex = Math.max(0, Math.min(MachineTier.ALL.size() - 1, properties.tierMultiplier() - 1));
        MachineTier tier = MachineTier.ALL.get(tierIndex);
        tooltip.add(colored("Tier: ", 0xB0B0B0).append(colored(tier.displayName(), tier.color())));
        tooltip.add(colored("State: ", 0xFF66CC).append(colored("Solid", 0xFF66CC)));
        if (properties.hasProperty("density")) {
            tooltip.add(colored("Density: ", 0x9FD3FF).append(colored(Integer.toString(properties.density()), 0xFFFFFF)));
        }
        if (properties.hasProperty("hardness")) {
            tooltip.add(colored("Hardness: ", 0x2ECC40).append(colored(Integer.toString(properties.hardness()), 0xFFFFFF)));
        }
        if (properties.hasProperty("meltingPoint")) {
            tooltip.add(colored("Melting Point: ", 0xFFD800).append(colored(properties.meltingPoint() + " C", 0xFF3333)));
        }
        if (properties.hasProperty("electricalBehavior")) {
            tooltip.add(colored("Electrical Behavior: ", 0x66D9EF)
                    .append(colored(displayName(properties.electricalBehavior().name()), 0xFFFFFF)));
            if (properties.electricallyConductive() && properties.hasProperty("electricalConductivity")) {
                tooltip.add(colored("Electrical Conductivity: ", 0x66D9EF)
                        .append(colored(Integer.toString(properties.electricalConductivity()), 0xFFFFFF)));
            } else {
                tooltip.add(colored("Insulation Strength: ", 0x66D9EF)
                        .append(colored(Integer.toString(properties.insulationStrength()), 0xFFFFFF)));
            }
        }
        if (properties.hasProperty("corrosionResistance")) {
            tooltip.add(colored("Corrosion Resistance: ", 0x9BE564)
                    .append(colored(Integer.toString(properties.corrosionResistance()), 0xFFFFFF)));
        }
        addWoodContentsTooltip(tooltip, wood);
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
        MaterialProperties properties = material.properties();

        if (material.isClayMaterial()) {
            if (target.part() == MaterialPart.BRICK || target.part() == MaterialPart.BRICKS) {
                addCeramicOperatingTooltipLines(tooltip, material);
            }
            return;
        }

        Component formula = formulaLine(material);
        if (formula != null) tooltip.add(formula);
        tooltip.add(colored("Tier: ", 0xB0B0B0).append(colored(material.tier().displayName(), material.tier().color())));
        if (material.atomicNumber() > 0) {
            tooltip.add(colored("Atomic Number: ", 0xB0B0B0).append(colored(Integer.toString(material.atomicNumber()), 0xFFFFFF)));
        }
        tooltip.add(colored("State: ", 0xFF66CC).append(colored(materialState(material, target.part()), 0xFF66CC)));
        if (properties.hasProperty("density")) {
            tooltip.add(colored("Density: ", 0x9FD3FF).append(colored(Integer.toString(properties.density()), 0xFFFFFF)));
        }
        if (properties.hasProperty("hardness")) {
            tooltip.add(colored("Hardness: ", 0x2ECC40).append(colored(Integer.toString(properties.hardness()), 0xFFFFFF)));
        }
        if (properties.hasProperty("meltingPoint")) {
            tooltip.add(colored("Melting Point: ", 0xFFD800).append(colored(properties.meltingPoint() + " C", 0xFF3333)));
        }
        if (properties.hasProperty("electricalBehavior")) {
            tooltip.add(colored("Electrical Behavior: ", 0x66D9EF).append(colored(displayName(properties.electricalBehavior().name()), 0xFFFFFF)));
            if (properties.electricallyConductive() && properties.hasProperty("electricalConductivity")) {
                tooltip.add(colored("Electrical Conductivity: ", 0x66D9EF).append(colored(Integer.toString(properties.electricalConductivity()), 0xFFFFFF)));
            } else {
                tooltip.add(colored("Insulation Strength: ", 0x66D9EF).append(colored(Integer.toString(properties.insulationStrength()), 0xFFFFFF)));
            }
        }
        if (properties.hasProperty("corrosionResistance")) {
            tooltip.add(colored("Corrosion Resistance: ", 0x9BE564).append(colored(Integer.toString(properties.corrosionResistance()), 0xFFFFFF)));
        }
        if (material.radioactivity() > 0) {
            tooltip.add(colored("Radioactivity: ", 0xBFFF00).append(colored(Integer.toString(material.radioactivity()), 0xBFFF00)));
        }
        if (showsTemperature(target.part())) {
            tooltip.add(colored("Temperature: ", 0xFF3333).append(colored(material.temperatureFor(target.part()) + " C", 0xFF3333)));
        }
    }

    private static void addCeramicOperatingTooltipLines(List<Component> tooltip, IndustrialMaterial material) {
        Component formula = formulaLine(material);
        if (formula != null) tooltip.add(formula);
        tooltip.add(colored("Tier: ", 0xB0B0B0).append(colored(material.tier().displayName(), material.tier().color())));
        tooltip.add(colored("State: ", 0xFF66CC).append(colored("Fired Solid", 0xFF66CC)));
        tooltip.add(colored("Maximum Operating Temperature: ", 0xFF9F43)
                .append(colored(material.properties().maxOperatingTemperature() + " C", 0xFFFFFF)));
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
        if (properties.hasProperty("electricalBehavior")) addPropertyLine(tooltip, "Electrical Behavior", displayName(properties.electricalBehavior().name()), 0x66D9EF);
        if (properties.hasProperty("electricalConductivity")) addPropertyLine(tooltip, "Electrical Conductivity", properties.electricalConductivity(), 0x66D9EF);
        addPropertyLine(tooltip, "Insulation Strength", properties.insulationStrength(), 0x66D9EF);

        tooltip.add(colored("Chemical / Structural", 0xAAAAAA));
        addPropertyLine(tooltip, "Chemical Stability", properties.chemicalStability(), 0x9BE564);
        if (properties.hasProperty("reactivity")) addPropertyLine(tooltip, "Reactivity", properties.reactivity(), 0x9BE564);
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
        if (properties.hasProperty("electricalBehavior")) addPropertyLine(tooltip, "Electrical Behavior", displayName(properties.electricalBehavior().name()), 0xFFFFFF);
        addPropertyLine(tooltip, "Heat Resistant", yesNo(properties.heatResistant()), 0xFFFFFF);
        addPropertyLine(tooltip, "Pressure Resistant", yesNo(properties.pressureResistant()), 0xFFFFFF);
    }

    private static void addPropertyLine(List<Component> tooltip, String label, int value, int color) {
        addPropertyLine(tooltip, label, Integer.toString(value), color);
    }

    private static void addPropertyLine(List<Component> tooltip, String label, String value, int color) {
        tooltip.add(colored(label + ": ", 0x808080).append(colored(value, color)));
    }

    private static Component formulaLine(IndustrialSubstance material) {
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
        if (material.isClayMaterial()) {
            return switch (part) {
                case CLAY, CLAY_BLOCK -> "Wet Clay";
                case UNFIRED_BRICK -> "Wet Unfired Solid";
                case DRIED_UNFIRED_BRICK -> "Dried Unfired Solid";
                case BRICK, BRICKS, FIREBOX, BRICK_SLAB, BRICK_STAIRS, BRICK_WALL -> "Fired Solid";
                case CRACKED_BRICK -> "Overfired Solid";
                case TINY_DUST, SMALL_DUST, DUST -> "Dust";
                default -> "Solid";
            };
        }
        return switch (material.properties().state()) {
            case GAS -> "Gas";
            case LIQUID -> "Fluid";
            case SOLID -> "Solid";
        };
    }

    private static boolean showsTemperature(MaterialPart part) {
        return part.isHotForgePart()
                || part == MaterialPart.MOLTEN_FLUID
                || (part.name().startsWith("CAST_") && !part.name().endsWith("_MOLD"))
                || part.name().startsWith("HOT_CAST_");
    }

    private static MutableComponent colored(String text, int color) {
        return Component.literal(text).withStyle(style -> style.withColor(TextColor.fromRgb(color & 0x00FFFFFF)));
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
