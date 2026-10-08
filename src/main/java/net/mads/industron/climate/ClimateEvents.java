package net.mads.industron.climate;

import net.mads.industron.Industron;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=Industron.MOD_ID)
public final class ClimateEvents {
    private ClimateEvents() {}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        ServerLevel world=event.getServer().overworld();long tick=world.getGameTime();
        if(tick%200==0 && world.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
            boolean rain=ClimateMath.rain(ClimateWorld.seed(world),ClimateWorld.day(world));
            world.setWeatherParameters(rain?0:400,400,rain,ClimateMath.thunder(ClimateWorld.seed(world),ClimateWorld.day(world)));
        }
        // Surface updates are budgeted around active players; no extra chunks are loaded.
        if(tick%20==0)for(ServerPlayer player:event.getServer().getPlayerList().getPlayers()) {
            ServerLevel level=player.serverLevel();if(level.dimension()!=Level.OVERWORLD)continue;
            for(int attempt=0;attempt<2;attempt++) {
                int x=player.getBlockX()+level.random.nextInt(33)-16,z=player.getBlockZ()+level.random.nextInt(33)-16;
                BlockPos test=new BlockPos(x,player.getBlockY(),z);if(!level.hasChunkAt(test))continue;
                BlockPos surface=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,test);if(!level.isAreaLoaded(surface,1))continue;
                double temperature=ClimateWorld.ambient(level,surface);var state=level.getBlockState(surface.below());
                if(temperature>2 && state.is(Blocks.SNOW))level.removeBlock(surface.below(),false);
                else if(temperature>2 && state.is(Blocks.ICE))level.setBlock(surface.below(),Blocks.WATER.defaultBlockState(),3);
                else if(temperature<=0 && state.is(Blocks.WATER)&&level.getFluidState(surface.below()).isSource())level.setBlock(surface.below(),Blocks.ICE.defaultBlockState(),3);
            }
        }
    }
    @SubscribeEvent public static void entity(EntityTickEvent.Post event) {
        if(!(event.getEntity() instanceof LivingEntity entity)||!(entity.level() instanceof ServerLevel level)
            ||!entity.isAlive() ||(entity.tickCount+entity.getId())%20!=0 ||EntityClimate.immune(entity))return;
        if(entity instanceof ServerPlayer player && (player.isCreative()||player.isSpectator())) {
            var state=EntityClimate.state(entity);state.putDouble("Body",37);state.putDouble("Wet",0);send(player);return;
        }
        var climate=ClimateWorld.sample(level,entity.blockPosition());var state=EntityClimate.state(entity);
        double old=EntityClimate.body(entity);
        boolean wet=entity.isInWater()||climate.rain();
        var next=ThermalRules.advance(new ThermalRules.Body(old,state.getDouble("Wet")),climate.temperature(),climate.wind(),wet,
            climate.shelter().heat(),ClothingInsulation.total(entity),EntityClimate.coldLimit(entity),EntityClimate.hotLimit(entity),entity.isSprinting());
        state.putDouble("Body",next.temperature());state.putDouble("Wet",next.wetness());
        if(entity instanceof ServerPlayer player) {
            String feeling=ThermalRules.feeling(next.temperature());
            if(!feeling.equals(state.getString("Feeling")))player.displayClientMessage(Component.literal(feeling),true);
            state.putString("Feeling",feeling);
            if(ThermalRules.strained(next.temperature())) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,0,false,false,true));
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,40,0,false,false,true));
                player.causeFoodExhaustion(.02F);
            }
            send(player);
        }
        if((next.temperature()<=33||next.temperature()>=41)&&entity.tickCount%100<20)
            entity.hurt(next.temperature()<=33?entity.damageSources().freeze():entity.damageSources().onFire(),1);
        if(entity instanceof Animal animal && ThermalRules.strained(next.temperature()) && entity.tickCount%200<20)
            seekShelter(level,animal,climate.temperature());
    }
    private static void seekShelter(ServerLevel level,Animal animal,double current) {
        if(!animal.getNavigation().isDone())return;
        BlockPos best=null;double bestScore=Math.abs(current-18);
        // Six candidates once per ten seconds, then one pathfinding request.
        for(int i=0;i<6;i++) {
            BlockPos p=animal.blockPosition().offset(level.random.nextInt(17)-8,0,level.random.nextInt(17)-8);
            if(!level.hasChunkAt(p))continue;
            if(!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()||level.getBlockState(p.below()).isAir())continue;
            var c=ClimateWorld.sample(level,p);double score=Math.abs(c.temperature()-18)+(c.rain()?5:0)+c.wind()*2;
            if(score<bestScore){bestScore=score;best=p;}
        }
        if(best!=null)animal.getNavigation().moveTo(best.getX()+.5,best.getY(),best.getZ()+.5,1);
    }
    public static void send(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,net.mads.industron.network.ClimateStatePayload.snapshot(player,false));
    }
    @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event){if(event.getLevel() instanceof Level level)ClimateWorld.invalidate(level,event.getPos());}
    @SubscribeEvent public static void broken(BlockEvent.BreakEvent event){ClimateWorld.invalidate(event.getPlayer().level(),event.getPos());}
    @SubscribeEvent public static void clone(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
        // Respawn starts comfortable; persistent exposure is retained through normal save/load.
        if(event.isWasDeath())event.getEntity().getPersistentData().remove("IndustronThermal");
        else if(event.getOriginal().getPersistentData().contains("IndustronThermal"))
            event.getEntity().getPersistentData().put("IndustronThermal",event.getOriginal().getPersistentData().getCompound("IndustronThermal").copy());
    }
}
