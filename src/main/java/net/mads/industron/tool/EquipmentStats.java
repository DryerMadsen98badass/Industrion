package net.mads.industron.tool;

import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.minecraft.world.item.ItemStack;

/** Equipment uses fatigue/toughness and the weakest loaded part, independently of mining stats. */
public record EquipmentStats(int durability, int drawTicks, double energy, double armour,
                             double toughness, double weight, double castDistance, double lineLoad) {
    public static boolean isEquipment(String id) {
        return switch(id) { case "bow", "crossbow", "shield", "fishing_rod", "helmet", "chestplate", "leggings", "boots" -> true; default -> false; };
    }
    public static boolean isArmourPart(MaterialPart part) {
        return part == MaterialPart.HELMET_SHELL || part == MaterialPart.CHESTPLATE_SHELL
                || part == MaterialPart.LEGGINGS_SHELL || part == MaterialPart.BOOTS_SHELL;
    }
    public static boolean isEquipmentPart(MaterialPart part) {
        return isArmourPart(part) || switch (part) {
            case BOW_BODY, CROSSBOW_STOCK, CROSSBOW_LIMBS, CROSSBOW_TRIGGER,
                    SHIELD_BODY, SHIELD_HANDLE, FISHING_ROD_BODY, FISHING_HOOK -> true;
            default -> false;
        };
    }

    /** Intrinsic part limits, shared by pre-assembly tooltips and finished equipment. */
    public record PartStats(double lifeFactor, double strengthFactor, double toughness, double weight) { }

    public static PartStats partStats(IndustrialSubstance material, MaterialPart part) {
        if (!ToolMaterialRules.allows(material, part)) return null;
        var p = ToolMaterialStatCalculator.profile(material);
        boolean wood = ToolMaterialRules.kind(material) == ToolMaterialRules.Kind.WOOD;
        double reference = wood ? 24 : 65;
        double life = clamp((p.fatigue() * .45 + p.toughness() * .35 + p.wear() * .20) / reference, .5, 3);
        double strength = clamp(Math.min(p.structural(), p.tensile()) / reference, .5, 1.25);
        // An unspecified item amount is one material unit (144 mB), never one millibucket.
        double amount = (part.materialAmountMb() > 0 ? part.materialAmountMb() : 144) / 144.0;
        double density = material instanceof IndustrialMaterial m ? Math.max(1, m.properties().density()) / 65.0 : .45;
        return new PartStats(life, strength, clamp(p.toughness() / 65, 0, 3), amount * clamp(density, .25, 3));
    }

    public static int baseDurability(String id, boolean woodBody) {
        return switch (id) {
            case "bow" -> 384; case "crossbow" -> 512; case "fishing_rod" -> 192;
            case "shield" -> woodBody ? 256 : 768; case "helmet", "boots" -> 256;
            case "chestplate" -> 512; case "leggings" -> 448; default -> 256;
        };
    }

    public static int durabilityLimit(String id, boolean woodBody, PartStats part) {
        return Math.max(1, (int) Math.round(baseDurability(id, woodBody) * part.lifeFactor()));
    }

    public static double protection(String id, double strength) {
        double base = switch (id) { case "helmet", "boots" -> 3; case "chestplate" -> 8; case "leggings" -> 6; default -> 0; };
        return Math.min(10, base * strength);
    }

    public static int drawTicks(String id, double strength) {
        return (int) Math.round((id.equals("crossbow") ? 60 : 40) / Math.sqrt(strength));
    }

    public static double projectileEnergy(String id, double strength) {
        return Math.min(id.equals("crossbow") ? 2 : 1.25, (id.equals("crossbow") ? 1.5 : 1) * strength);
    }

    public static double castDistance(double strength) { return Math.min(30, 24 * Math.sqrt(strength)); }
    public static double lineLoad(double strength) { return 10 * strength; }
    public static EquipmentStats calculate(ToolDefinition definition, ToolStackData data) {
        if (data == null) return null;
        double weakest=Double.POSITIVE_INFINITY, strength=1, toughness=0, weight=0;
        boolean woodBase=false;
        for (var slot: definition.parts()) {
            IndustrialSubstance material=ToolMaterialResolver.resolve(data.materialKey(slot.role()));
            if (!ToolMaterialRules.allows(material,slot.part())) return null;
            PartStats p=partStats(material,slot.part());
            boolean wood=ToolMaterialRules.kind(material)==ToolMaterialRules.Kind.WOOD;
            weakest=Math.min(weakest,p.lifeFactor());
            if (slot.role().equals(definition.strengthReferenceRole())) {
                strength=p.strengthFactor();
                toughness=p.toughness();
            }
            if (slot.role().equals(definition.baseRole())) woodBase=wood;
            weight+=p.weight();
        }
        String id=definition.id();
        double protection=protection(id,strength);
        return new EquipmentStats(Math.max(1,(int)Math.round(baseDurability(id,woodBase)*weakest)), drawTicks(id,strength),projectileEnergy(id,strength),
                protection,protection==0?0:toughness,weight,castDistance(strength),lineLoad(strength));
    }
    public static EquipmentStats of(ItemStack stack) {
        if(stack.getItem() instanceof MaterialEquipment item) {
            return isEquipment(item.definition().id()) ? calculate(item.definition(),item.data(stack)) : null;
        }
        var definition=EquipmentExistingItems.definition(stack);
        return definition==null?null:calculate(definition,stack.get(ToolComponents.PARTS.get()));
    }
    public static int colour(ItemStack stack) {
        if (!(stack.getItem() instanceof MaterialEquipment item)) return -1;
        ToolStackData data=item.data(stack);if(data==null)return -1;
        var material=ToolMaterialResolver.resolve(data.materialKey(item.definition().strengthReferenceRole()));
        return material==null?-1:0xff000000|material.color();
    }
    private static double clamp(double v,double min,double max){return Math.max(min,Math.min(max,v));}
}
