package net.mads.industron.tool;
import net.mads.industron.Industron;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.List;

@EventBusSubscriber(modid=Industron.MOD_ID)
public final class EquipmentGameplayEvents {
    private EquipmentGameplayEvents(){}
    @SubscribeEvent public static void equipmentDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event){
        if(event.getEntity() instanceof net.minecraft.world.entity.player.Player)return;
        // Mob equipment drops are created directly, outside the loot-table modifier.
        boolean nether = net.mads.industron.progression.ProgressionMaterials.isNether(event.getEntity().level());
        event.getDrops().forEach(drop->drop.setItem(EquipmentReplacement.replace(
                net.mads.industron.progression.ProgressionMaterials.replace(drop.getItem(), nether), nether)));
    }
    @SubscribeEvent public static void existingArmourTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event){
        var stack=event.getItemStack();if(stack.getItem() instanceof MaterialEquipment)return;
        EquipmentStats stats=EquipmentStats.of(stack);if(stats==null)return;
        var definition=EquipmentExistingItems.definition(stack);
        if(definition!=null)EquipmentTooltip.appendFinished(definition.id(),stack,stats,event.getToolTip());
    }
    @SubscribeEvent public static void villagers(VillagerTradesEvent event){event.getTrades().values().forEach(EquipmentGameplayEvents::replaceTrades);}
    @SubscribeEvent public static void wanderers(WandererTradesEvent event){replaceTrades(event.getGenericTrades());replaceTrades(event.getRareTrades());}
    private static void replaceTrades(List<VillagerTrades.ItemListing> trades){
        trades.replaceAll(original -> (entity,random) -> {
            return net.mads.industron.progression.ProgressionTrades.replace(original.getOffer(entity,random));
        });
    }
    @SubscribeEvent public static void fishingLine(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof FishingHook hook)||hook.level().isClientSide)return;
        var player=hook.getPlayerOwner();if(player==null)return;
        ItemStack rod=player.getMainHandItem();EquipmentStats stats=EquipmentStats.of(rod);
        if(stats==null||!(rod.getItem() instanceof EquipmentItems.MaterialFishingRod)) {
            rod=player.getOffhandItem();stats=EquipmentStats.of(rod);
        }
        if(stats==null||!(rod.getItem() instanceof EquipmentItems.MaterialFishingRod))return;
        if(player.distanceToSqr(hook)>stats.castDistance()*stats.castDistance()){
            rod.hurtAndBreak(1,player,rod==player.getMainHandItem()?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            hook.discard();
        }
    }
}
