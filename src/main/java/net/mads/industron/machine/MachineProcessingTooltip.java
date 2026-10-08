package net.mads.industron.machine;

import java.math.BigDecimal;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

/** Shared by machine items, goggles and machine status providers. */
public final class MachineProcessingTooltip {
    private MachineProcessingTooltip() {}
    public static Component tier(MachineTier tier) {
        return Component.literal("Machine Tier: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(tier.displayName()).withStyle(s -> s.withColor(TextColor.fromRgb(tier.color()))));
    }
    public static void processing(List<Component> tooltip, ProcessingProfile profile) {
        if (profile.durationMultiplier() == 1) return;
        String multiplier = BigDecimal.valueOf(profile.durationMultiplier()).stripTrailingZeros().toPlainString();
        tooltip.add(Component.literal("Processing time: " + multiplier + "× recipe duration"
            + (profile.referenceRpm() == 0 ? "" : " at " + profile.referenceRpm() + " RPM")).withStyle(ChatFormatting.GRAY));
    }
}
