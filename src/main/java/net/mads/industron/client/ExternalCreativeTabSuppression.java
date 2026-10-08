package net.mads.industron.client;

import net.mads.industron.Industron;
import net.mads.industron.material.ExternalMaterialSuppression;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Keeps externally replaced blocks/items registered but out of normal creative access. */
@EventBusSubscriber(modid = Industron.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ExternalCreativeTabSuppression {
    private ExternalCreativeTabSuppression() {
    }

    @SubscribeEvent
    public static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        java.util.stream.Stream.concat(
                        java.util.stream.Stream.concat(
                                ExternalMaterialSuppression.suppressedExternalTransportIds().stream(),
                                ExternalMaterialSuppression.suppressedExternalTestToolIds().stream()
                        ),
                        ExternalMaterialSuppression.suppressedExternalProcessingBlockIds().stream()
                )
                .distinct()
                .forEach(id -> BuiltInRegistries.ITEM.getOptional(id).ifPresent(item ->
                        event.remove(
                                new ItemStack(item),
                                CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                        )
                ));
    }
}
