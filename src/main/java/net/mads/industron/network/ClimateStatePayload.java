package net.mads.industron.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.mads.industron.climate.*;
import net.mads.industron.farming.*;
import net.mads.industron.item.CreativeGogglesItem;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Only the wearer and inspected plant are synchronized; no whole-world plant state is sent. */
public record ClimateStatePayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<ClimateStatePayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("industron","climate_state"));
    public static final StreamCodec<ByteBuf,ClimateStatePayload> STREAM_CODEC=ByteBufCodecs.COMPOUND_TAG.map(ClimateStatePayload::new,ClimateStatePayload::data);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static ClimateStatePayload snapshot(ServerPlayer player,boolean open) {
        var level=player.serverLevel();var climate=ClimateWorld.sample(level,player.blockPosition());CompoundTag tag=new CompoundTag();
        tag.putBoolean("Open",open);tag.putString("Dimension",level.dimension().location().toString());
        tag.putDouble("Day",climate.day());tag.putDouble("Ambient",climate.temperature());tag.putDouble("Humidity",climate.humidity());
        tag.putDouble("Wind",climate.wind());tag.putBoolean("Rain",climate.rain());tag.putBoolean("Snow",climate.snow());
        tag.putBoolean("Seasonal",level.dimension()==net.minecraft.world.level.Level.OVERWORLD);
        tag.putDouble("Body",EntityClimate.body(player));tag.putDouble("Wet",EntityClimate.state(player).getDouble("Wet"));
        tag.putDouble("Insulation",ClothingInsulation.total(player));tag.putBoolean("Roof",climate.shelter().roof());tag.putInt("Walls",climate.shelter().walls());
        if(CreativeGogglesItem.isWearing(player) && player.pick(6,0,false) instanceof BlockHitResult hit) {
            BlockPos target=hit.getBlockPos();var state=level.getBlockState(target);
            if(CalendarPlants.supports(state)) {
                BlockPos root=CalendarPlants.root(level,target,state);var entry=CropCalendarData.get(level).get(root);
                if(entry!=null) {
                    CompoundTag plant=entry.copy();var profile=PlantClimates.block(state);var local=ClimateWorld.sample(level,root);
                    double temperature=local.temperature();plant.putDouble("ActualTemperature",temperature);
                    plant.putDouble("Minimum",profile.minimum());plant.putDouble("Optimum",profile.optimum());plant.putDouble("Maximum",profile.maximum());
                    plant.putDouble("MinimumHumidity",profile.minimumHumidity());plant.putDouble("Required",profile.growthDays());
                    boolean watered=entry.getBoolean("Water")||local.rain();
                    plant.putDouble("Rate",profile.rate(temperature,local.humidity(),watered,entry.getBoolean("Light")));
                    plant.putString("Stopped",profile.stopped(temperature,local.humidity(),watered,entry.getBoolean("Light")));
                    var environment=new ClimateGrowth.Environment(entry.getDouble("Temperature"),entry.getDouble("Humidity"),root.getY(),entry.getBoolean("Water"),entry.getBoolean("Light"),
                        level.dimension()==net.minecraft.world.level.Level.OVERWORLD,ClimateWorld.seed(level),entry.getDouble("ShelterHeat"),ClimateWorld.fixedTemperature(level),entry.contains("RainMode")?entry.getInt("RainMode"):-1,0,entry.getBoolean("Aquatic"),entry.getBoolean("Roof"));
                    plant.putDouble("Harvest",ClimateGrowth.harvestDay(climate.day(),entry.getDouble("Growth"),environment,profile));
                    plant.putString("Seasons",ClimatePresentation.seasons(environment,profile));
                    tag.put("Plant",plant);tag.putLong("Target",target.asLong());
                }
            }
        }
        return new ClimateStatePayload(tag);
    }
    public static void handle(ClimateStatePayload payload,IPayloadContext context) {
        context.enqueueWork(()->net.mads.industron.client.ClimateClient.accept(payload));
    }
}
