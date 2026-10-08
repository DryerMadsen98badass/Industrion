package net.mads.industron.material;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.mads.industron.tool.ToolMaterialRules;
import net.mads.industron.tool.ToolStatCalculator;
import net.mads.industron.tool.ToolStats;

import java.util.List;
import java.util.Locale;

public class MaterialItem extends Item {
    private final IndustrialMaterial material;
    private final MaterialPart part;
    private final boolean magnetic;

    public MaterialItem(IndustrialMaterial material, MaterialPart part) {
        this(material, part, false);
    }

    public MaterialItem(IndustrialMaterial material, MaterialPart part, boolean magnetic) {
        super(properties(material, part));
        this.material = material;
        this.part = part;
        this.magnetic = magnetic;
    }

    private static Item.Properties properties(IndustrialMaterial material, MaterialPart part) {
        Item.Properties properties = new Item.Properties();
        if (ToolMaterialRules.isDirectFinishedToolPart(part) && ToolMaterialRules.allows(material, part)) {
            ToolStats stats = ToolStatCalculator.calculateSingle(material, part);
            return properties.durability(stats.durability());
        }
        return ToolMaterialRules.isToolPartForm(part) ? properties.stacksTo(1) : properties;
    }

    public IndustrialMaterial material() { return material; }
    public MaterialPart part() { return part; }
    public boolean magnetic() { return magnetic; }

    @Override
    public boolean isPiglinCurrency(ItemStack stack) {
        return net.mads.industron.progression.ProgressionMaterials.isCurrency(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        double meltingWork=net.mads.industron.material.recipes.OreWorkRules.durationMultiplier(part);
        if(meltingWork!=1.0)tooltip.add(Component.literal("Foundry melting work: "+format(meltingWork)+"x")
                .withStyle(ChatFormatting.GRAY));
        if (magnetic) {
            tooltip.add(Component.literal("Magnetic").withStyle(ChatFormatting.AQUA));
        }
        if (part == MaterialPart.IMPURE_DUST) {
            tooltip.add(Component.literal("Throw into a water source to wash into Dust").withStyle(ChatFormatting.GRAY));
        }
        if (ToolMaterialRules.isDirectFinishedToolPart(part) && ToolMaterialRules.allows(material, part)) {
            ToolStats stats = ToolStatCalculator.calculateSingle(material, part);
            int max = stack.getMaxDamage();
            int remaining = Math.max(0, max - stack.getDamageValue());
            tooltip.add(colored("Tier: ", 0xB0B0B0)
                    .append(colored(stats.tier().displayName(), stats.tier().color())));
            tooltip.add(colored("Durability: ", 0x55FF55)
                    .append(colored(remaining + " / " + max, 0xFFFFFF)));
            tooltip.add(colored("Efficiency: ", 0x55FFFF)
                    .append(colored(format(stats.efficiencySeconds()), 0xFFFFFF)));
            tooltip.add(colored("Damage: ", 0xFF5555)
                    .append(colored(format(stats.damage()), 0xFFFFFF)));
        }
    }

    /** NeoForge 1.21.1 stack-sensitive enchantability. */
    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ToolMaterialRules.isDirectFinishedToolPart(part) ? 10 : super.getEnchantmentValue(stack);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        if (!ToolMaterialRules.isDirectFinishedToolPart(part) || !ToolMaterialRules.allows(material, part)) {
            return super.getDefaultAttributeModifiers(stack);
        }

        ToolStats stats = ToolStatCalculator.calculateSingle(material, part);
        double addedDamage = Math.max(-1.0D, stats.damage() - 1.0D);
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, addedDamage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if (ToolMaterialRules.isDirectFinishedToolPart(part)
                && !level.isClientSide
                && state.getDestroySpeed(level, pos) != 0.0F) {
            stack.hurtAndBreak(1, miningEntity, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    private static MutableComponent colored(String text, int color) {
        return Component.literal(text)
                .withStyle(style -> style.withColor(TextColor.fromRgb(color & 0x00FFFFFF)));
    }

    private static String format(double value) {
        if (Math.rint(value) == value) return Integer.toString((int) value);
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
