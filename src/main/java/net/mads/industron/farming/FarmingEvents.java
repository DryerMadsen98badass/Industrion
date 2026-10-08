package net.mads.industron.farming;

import net.mads.industron.Industron;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;

@EventBusSubscriber(modid=Industron.MOD_ID)
public final class FarmingEvents {
    private FarmingEvents() {}
    @SubscribeEvent public static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent e) {
        e.getDispatcher().register(com.mojang.brigadier.builder.LiteralArgumentBuilder.<net.minecraft.commands.CommandSourceStack>literal("industron_calendar")
            .executes(context -> {
                long day=context.getSource().getServer().overworld().getDayTime()/24000;
                String[] seasons={"Spring","Summer","Autumn","Winter"};
                context.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(
                    "Day "+(day+1)+" | Year "+(day/100+1)+" | "+seasons[SeasonCalendar.season(day)]+" | Season day "+(Math.floorMod(day,25)+1)),false);
                return 1;
            }));
    }
    private record Pending(ServerLevel level,long chunk) {}
    private record Plant(ServerLevel level,long pos) {}
    private static final java.util.Queue<Pending> chunks=new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final java.util.Queue<Plant> plants=new java.util.ArrayDeque<>();
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) {
        net.mads.industron.climate.ClimateWorker.drain();
        for(ServerLevel level:e.getServer().getAllLevels())CropCalendarData.get(level).tickLoaded(level,64);
        for(int count=0;count<8 && !chunks.isEmpty();count++) {
            var next=chunks.poll();
            if(next.level().getServer()!=e.getServer())continue;
            for(long pos:CropCalendarData.get(next.level()).positions(next.chunk()))plants.add(new Plant(next.level(),pos));
        }
        for(int count=0;count<256 && !plants.isEmpty();count++) {
            var next=plants.poll();if(next.level().getServer()==e.getServer())CalendarPlants.update(next.level(),BlockPos.of(next.pos()));
        }
    }
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var data=CropCalendarData.get(level);double day=net.mads.industron.climate.ClimateWorld.day(level);
        for(long key:data.positions(event.getChunk().getPos().toLong())) {
            BlockPos pos=BlockPos.of(key);var entry=data.get(pos);
            if(entry!=null){entry.putDouble("UnloadedFrom",day);data.setDirty();}
            net.mads.industron.climate.ClimateWorker.forget(level,pos);
        }
    }
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e) {chunks.clear();plants.clear();net.mads.industron.climate.ClimateWorker.stop();net.mads.industron.climate.ClimateWorld.clear();}
    @SubscribeEvent public static void place(BlockEvent.EntityPlaceEvent e) {
        if(e.getLevel() instanceof ServerLevel l)CalendarPlants.planted(l,e.getPos());
    }
    @SubscribeEvent public static void harvest(BlockEvent.BreakEvent e) {
        if(e.getLevel() instanceof ServerLevel l && CalendarPlants.supports(e.getState()))CalendarPlants.update(l,e.getPos());
    }
    @SubscribeEvent public static void grow(CropGrowEvent.Pre e) {
        if(e.getLevel() instanceof ServerLevel l && CalendarPlants.supports(e.getState())) {
            BlockPos pos=e.getPos();
            if(!CalendarPlants.supports(l.getBlockState(pos)) && CalendarPlants.supports(l.getBlockState(pos.below())))pos=pos.below();
            CalendarPlants.update(l,pos);
            e.setResult(e.getState().getBlock() instanceof net.minecraft.world.level.block.StemBlock && e.getState().getValue(net.minecraft.world.level.block.StemBlock.AGE)==7 && CalendarPlants.allowFruit(l,pos,l.getBlockState(pos))
                ?CropGrowEvent.Pre.Result.GROW:CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }
    @SubscribeEvent public static void bone(BonemealEvent e) {
        if(CalendarPlants.supports(e.getLevel().getBlockState(e.getPos())))e.setCanceled(true);
    }
    @SubscribeEvent public static void load(ChunkEvent.Load e) {
        if(e.getLevel() instanceof ServerLevel l) {
            chunks.add(new Pending(l,e.getChunk().getPos().toLong()));
        }
    }
}
