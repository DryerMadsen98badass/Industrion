package net.mads.industron.tool;

import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One registered item per tool family. The concrete materials live on the ItemStack in
 * {@link ToolComponents#PARTS}, so every valid part combination shares this item class.
 */
public final class ComposedToolItem extends Item implements MaterialEquipment {
    private final ToolDefinition definition;

    public ComposedToolItem(ToolDefinition definition, Properties properties) {
        super(withDefaultToolData(definition, properties));
        this.definition = definition;
    }

    /**
     * Vanilla/JEI/tag/grindstone contexts often create a plain ItemStack from the registered Item
     * without going through Assembly. Give every composed-tool Item a real canonical material
     * combination as its default components so those stacks still render, expose stats and behave
     * like a normal damageable tool. Assembly outputs overwrite these components with the actual
     * captured materials and durability.
     */
    public static Properties withDefaultToolData(ToolDefinition definition, Properties properties) {
        Map<String, String> materialKeys = new LinkedHashMap<>();
        for (ToolDefinition.PartSlot slot : definition.parts()) {
            var candidates = ToolMaterialRules.candidates(slot.part());
            if (candidates.isEmpty()) {
                throw new IllegalStateException(
                        "No default material candidate for tool part " + definition.id() + "/" + slot.role()
                );
            }
            materialKeys.put(slot.role(), ToolMaterialResolver.key(candidates.getFirst()));
        }

        ToolStackData data = new ToolStackData(materialKeys);
        ToolStats stats = ToolStatCalculator.calculate(definition, data);
        if (stats == null) {
            throw new IllegalStateException("Cannot calculate default stats for tool " + definition.id());
        }

        return properties
                .stacksTo(1)
                .durability(stats.durability())
                .component(ToolComponents.PARTS.get(), data);
    }

    public ToolDefinition definition() {
        return definition;
    }

    public ToolStackData data(ItemStack stack) {
        return stack == null ? null : stack.get(ToolComponents.PARTS.get());
    }

    public ToolStats stats(ItemStack stack) {
        return ToolStatCalculator.calculate(definition, data(stack));
    }

    public boolean valid(ItemStack stack) {
        return stats(stack) != null;
    }

    /** NeoForge 1.21.1 stack-sensitive enchantability. */
    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return valid(stack) ? 10 : 0;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ToolStats stats = stats(stack);
        if (stats == null) return ItemAttributeModifiers.EMPTY;

        // Players already have 1 base attack damage. Add only the remainder so the displayed
        // ToolStats.damage value is the effective base hit damage while this tool is held.
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
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // A value above 1 lets vanilla's Efficiency enchantment contribute to Player#getDestroySpeed.
        // Industron's own mining calculator divides this base back out, so material efficiency is not double-counted.
        return valid(stack) ? 2.0F : 1.0F;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if (valid(stack) && !level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) {
            stack.hurtAndBreak(1, miningEntity, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ToolStats stats = stats(stack);
        if (stats == null) return;

        int max = stack.getMaxDamage();
        int remaining = Math.max(0, max - stack.getDamageValue());
        tooltip.add(colored("Tier: ", 0xB0B0B0)
                .append(colored(stats.tier().displayName(), stats.tier().color())));
        tooltip.add(colored("Durability: ", 0x55FF55)
                .append(colored(remaining + " / " + max, 0x55FF55)));
        if (ToolStatPresentation.showsEfficiency(definition)) tooltip.add(colored("Efficiency: ", 0x55FFFF)
                .append(colored(format(stats.efficiencySeconds()), 0x55FFFF)));
        if (ToolStatPresentation.showsDamage(definition)) tooltip.add(colored("Damage: ", 0xFF5555)
                .append(colored(format(stats.damage()), 0xFF5555)));
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
