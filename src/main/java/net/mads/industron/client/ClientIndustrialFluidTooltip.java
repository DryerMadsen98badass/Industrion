package net.mads.industron.client;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.mads.industron.Industron;
import net.mads.industron.fluid.IndustrialFluid;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialLookup;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = Industron.MOD_ID, value = Dist.CLIENT)
public final class ClientIndustrialFluidTooltip {
    private ClientIndustrialFluidTooltip() {
    }

    @SubscribeEvent
    public static void addFluidTooltip(ItemTooltipEvent event) {
        IndustrialFluid fluid = MaterialLookup.findIndustrialFluid(event.getItemStack());
        if (fluid == null) {
            return;
        }

        addChemicalBalanceLine(event.getToolTip(), fluid);
        Player player = event.getEntity();
        if (player != null && GogglesItem.isWearingGoggles(player)) {
            addLines(event.getToolTip(), fluid);
        }
    }


    private static void addChemicalBalanceLine(List<Component> tooltip, IndustrialFluid fluid) {
        if (!fluid.hasChemicalBalance()) {
            return;
        }
        double cb = fluid.chemicalBalance();
        String classification = cb > 0.0D ? "Acidic" : cb < 0.0D ? "Basic" : "Neutral";
        ChatFormatting formatting = cb > 0.0D
                ? ChatFormatting.RED
                : cb < 0.0D ? ChatFormatting.AQUA : ChatFormatting.GREEN;
        String value = cb > 0.0D
                ? String.format(java.util.Locale.ROOT, "+%.2f", cb)
                : String.format(java.util.Locale.ROOT, "%.2f", cb);
        tooltip.add(Component.literal("CB: " + value + " (" + classification + ")")
                .withStyle(formatting));
    }

    public static void addLines(List<Component> tooltip, IndustrialFluid fluid) {
        MutableComponent formula = formulaComponent(fluid, false);
        if (formula != null) {
            tooltip.add(Component.literal("Formula: ")
                    .withStyle(ChatFormatting.BLUE)
                    .append(formula));
        }

        tooltip.add(colored("State: ", 0xFF66CC)
                .append(colored(fluid.isGas() ? "Gas" : "Fluid", 0xFF66CC)));
        tooltip.add(colored("Temperature: ", 0xFF3333)
                .append(colored(fluid.temperature() + " C", 0xFF3333)));
    }

    private static MutableComponent formulaComponent(IndustrialSubstance substance, boolean nested) {
        if (substance instanceof net.mads.industron.material.IndustrialMaterial material
                && material.elementSymbol().isPresent()) {
            return colored(material.elementSymbol().get(), material.color());
        }

        List<MaterialComponent> components = substance instanceof IndustrialFluid fluid
                ? fluid.components()
                : ((net.mads.industron.material.IndustrialMaterial) substance).components();

        if (components.isEmpty()) {
            String formula = substance.formula();
            return formula.isBlank() ? null : colored(formula, substance.color());
        }

        MutableComponent result = Component.empty();
        if (nested) {
            result.append(colored("(", substance.color()));
        }

        for (MaterialComponent component : components) {
            IndustrialSubstance child = component.substance();
            boolean childNested = child instanceof IndustrialFluid childFluid
                    ? !childFluid.components().isEmpty()
                    : child instanceof net.mads.industron.material.IndustrialMaterial childMaterial
                    && childMaterial.elementSymbol().isEmpty()
                    && !childMaterial.components().isEmpty();

            MutableComponent childFormula = formulaComponent(child, childNested);
            if (childFormula == null) {
                childFormula = colored(child.displayName(), child.color());
            }
            result.append(childFormula);
            if (component.amount() > 1) {
                result.append(colored(toSubscript(component.amount()), child.color()));
            }
        }

        if (nested) {
            result.append(colored(")", substance.color()));
        }
        return result;
    }

    private static MutableComponent colored(String text, int color) {
        return Component.literal(text)
                .withStyle(style -> style.withColor(TextColor.fromRgb(color)));
    }

    private static String toSubscript(int number) {
        String value = Integer.toString(number);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            result.append(switch (value.charAt(i)) {
                case '0' -> '₀';
                case '1' -> '₁';
                case '2' -> '₂';
                case '3' -> '₃';
                case '4' -> '₄';
                case '5' -> '₅';
                case '6' -> '₆';
                case '7' -> '₇';
                case '8' -> '₈';
                case '9' -> '₉';
                default -> value.charAt(i);
            });
        }
        return result.toString();
    }
}
