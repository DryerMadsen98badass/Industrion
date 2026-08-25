package net.mads.industron.material;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MaterialItem extends Item {
    private final IndustrialMaterial material;
    private final MaterialPart part;
    private final boolean magnetic;

    public MaterialItem(IndustrialMaterial material, MaterialPart part) {
        this(material, part, false);
    }

    public MaterialItem(IndustrialMaterial material, MaterialPart part, boolean magnetic) {
        super(new Item.Properties());
        this.material = material;
        this.part = part;
        this.magnetic = magnetic;
    }

    public IndustrialMaterial material() { return material; }
    public MaterialPart part() { return part; }
    public boolean magnetic() { return magnetic; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (magnetic) {
            tooltip.add(Component.literal("Magnetic").withStyle(ChatFormatting.AQUA));
        }
        if (part == MaterialPart.IMPURE_DUST) {
            tooltip.add(Component.literal("Throw into a water source to wash into Dust").withStyle(ChatFormatting.GRAY));
        }
    }
}
