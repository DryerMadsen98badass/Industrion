package net.mads.industron.client;
import net.mads.industron.Industron;
import net.mads.industron.item.CreativeGogglesItem;
import net.mads.industron.climate.*;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
@EventBusSubscriber(modid=Industron.MOD_ID,value=Dist.CLIENT)
public final class PlantClimateTooltip {
    private PlantClimateTooltip() {}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        double insulation=ClothingInsulation.item(event.getItemStack());
        if(insulation>0)event.getToolTip().add(Component.literal("Thermal insulation: "+Math.round(insulation*100)+"%"));
        if(!CreativeGogglesItem.isWearing(event.getEntity()))return;
        var p=PlantClimates.item(event.getItemStack());if(p==null)return;
        event.getToolTip().add(Component.literal("Growth requirement: "+ClimatePresentation.number(p.growthDays())+" effective days"));
        event.getToolTip().add(Component.literal("Temperature range: "+ClimatePresentation.number(p.minimum())+"–"+ClimatePresentation.number(p.maximum())+" °C"));
        event.getToolTip().add(Component.literal("Optimum: "+ClimatePresentation.number(p.optimum())+" °C; minimum humidity "+Math.round(p.minimumHumidity()*100)+"%"));
        if(p.dormant())event.getToolTip().add(Component.literal("Can become dormant in cold conditions."));
    }
}
