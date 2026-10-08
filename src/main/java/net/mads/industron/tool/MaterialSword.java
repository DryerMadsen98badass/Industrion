package net.mads.industron.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Native sword behaviour, enchantments and hit wear; material parts supply damage and durability. */
public final class MaterialSword extends SwordItem implements MaterialEquipment {
    private final ToolDefinition definition;

    public MaterialSword(ToolDefinition definition, Properties properties) {
        super(defaultTier(definition), ComposedToolItem.withDefaultToolData(definition, properties));
        this.definition = definition;
    }

    private static Tier defaultTier(ToolDefinition definition) {
        var keys = new LinkedHashMap<String, String>();
        for (var slot : definition.parts()) {
            var candidates = ToolMaterialRules.candidates(slot.part());
            if (candidates.isEmpty()) throw new IllegalStateException("Missing sword material: " + slot.role());
            keys.put(slot.role(), ToolMaterialResolver.key(candidates.get(0)));
        }
        ToolStats stats = ToolStatCalculator.calculate(definition, new ToolStackData(keys));
        if (stats == null) throw new IllegalStateException("Invalid default sword parts");
        // TieredItem sets MAX_DAMAGE in its constructor, so its tier must use the real default-part life.
        return new Tier() {
            public int getUses() { return stats.durability(); }
            public float getSpeed() { return 1; }
            public float getAttackDamageBonus() { return 0; }
            public TagKey<Block> getIncorrectBlocksForDrops() { return Tiers.IRON.getIncorrectBlocksForDrops(); }
            public int getEnchantmentValue() { return 10; }
            public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
        };
    }

    @Override public ToolDefinition definition() { return definition; }
    @Override public int getEnchantmentValue(ItemStack stack) { return valid(stack) ? 10 : 0; }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return false; }

    @Override public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ToolStats stats = stats(stack);
        if (stats == null) return ItemAttributeModifiers.EMPTY;
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                        Math.max(-1, stats.damage() - 1), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID,
                        -2.4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, out, flag);
        ToolStats stats = stats(stack);
        if (stats == null) return;
        out.add(Component.literal("Tier: " + stats.tier().displayName()));
        out.add(Component.literal("Durability: " + Math.max(0, stack.getMaxDamage() - stack.getDamageValue()) + " / " + stack.getMaxDamage()));
        out.add(Component.literal(String.format(Locale.ROOT, "Damage: %.2f", stats.damage())));
        out.add(Component.literal("Attack speed: 1.6"));
    }
}
