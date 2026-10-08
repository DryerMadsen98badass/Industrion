package net.mads.industron.tool;

import java.util.List;
import java.util.Locale;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialPart;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Role-specific equipment values; every number comes from the runtime equipment calculator. */
public final class EquipmentTooltip {
    public static void appendPart(List<Component> out, IndustrialSubstance material, MaterialPart part) {
        var stats = EquipmentStats.partStats(material, part);
        if (stats == null) return;
        String family = family(part);
        var tier = ToolMaterialRules.tier(material);
        out.add(Component.literal("Tier: " + tier.displayName()));
        boolean wood = ToolMaterialRules.kind(material) == ToolMaterialRules.Kind.WOOD;
        if (part == MaterialPart.SHIELD_HANDLE) {
            // Handle life limits both wood-bodied and metal-bodied shields; body identity is not known yet.
            line(out, "Durability limit (wood shield)", EquipmentStats.durabilityLimit(family, true, stats));
            line(out, "Durability limit (metal shield)", EquipmentStats.durabilityLimit(family, false, stats));
        } else {
            line(out, EquipmentStats.isArmourPart(part) ? "Durability" : "Durability limit",
                    EquipmentStats.durabilityLimit(family, wood, stats));
        }
        if (EquipmentStats.isArmourPart(part)) {
            line(out, "Protection", EquipmentStats.protection(family, stats.strengthFactor()));
            line(out, "Toughness", stats.toughness());
        } else if (part == MaterialPart.BOW_BODY || part == MaterialPart.CROSSBOW_LIMBS) {
            out.add(Component.literal("Draw time: " + number(EquipmentStats.drawTicks(family, stats.strengthFactor()) / 20.0) + " s"));
            line(out, "Projectile energy", EquipmentStats.projectileEnergy(family, stats.strengthFactor()));
        } else if (part == MaterialPart.FISHING_ROD_BODY) {
            out.add(Component.literal("Cast distance: " + number(EquipmentStats.castDistance(stats.strengthFactor())) + " blocks"));
            line(out, "Line load", EquipmentStats.lineLoad(stats.strengthFactor()));
        }
        line(out, "Weight", stats.weight());
    }

    public static void appendFinished(String family, ItemStack stack, EquipmentStats stats, List<Component> out) {
        out.add(Component.literal("Durability: " + Math.max(0, stack.getMaxDamage() - stack.getDamageValue()) + " / " + stack.getMaxDamage()));
        switch (family) {
            case "bow", "crossbow" -> {
                out.add(Component.literal("Draw time: " + number(stats.drawTicks() / 20.0) + " s"));
                line(out, "Projectile energy", stats.energy());
            }
            case "fishing_rod" -> {
                out.add(Component.literal("Cast distance: " + number(stats.castDistance()) + " blocks"));
                line(out, "Line load", stats.lineLoad());
            }
            case "helmet", "chestplate", "leggings", "boots" -> {
                line(out, "Protection", stats.armour());
                line(out, "Toughness", stats.toughness());
            }
            default -> { }
        }
        line(out, "Weight", stats.weight());
    }

    private static String family(MaterialPart part) {
        return switch (part) {
            case BOW_BODY -> "bow";
            case CROSSBOW_STOCK, CROSSBOW_LIMBS, CROSSBOW_TRIGGER -> "crossbow";
            case SHIELD_BODY, SHIELD_HANDLE -> "shield";
            case FISHING_ROD_BODY, FISHING_HOOK -> "fishing_rod";
            case HELMET_SHELL -> "helmet";
            case CHESTPLATE_SHELL -> "chestplate";
            case LEGGINGS_SHELL -> "leggings";
            case BOOTS_SHELL -> "boots";
            default -> throw new IllegalArgumentException("Not an equipment part: " + part);
        };
    }

    private static void line(List<Component> out, String label, double value) {
        out.add(Component.literal(label + ": " + number(value)));
    }
    private static String number(double value) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
    private EquipmentTooltip() { }
}
