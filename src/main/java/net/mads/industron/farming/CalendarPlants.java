package net.mads.industron.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Server-only calendar growth. Climate is sampled before unloading, not retroactively rewritten. */
public final class CalendarPlants {
    private CalendarPlants() {}
    public static boolean supports(BlockState s) {
        Block b=s.getBlock();return b instanceof CropBlock || b instanceof StemBlock || b instanceof AttachedStemBlock || b instanceof SweetBerryBushBlock
            || b instanceof NetherWartBlock || b instanceof CocoaBlock || column(s);
    }
    private static boolean column(BlockState s) {
        Block b=s.getBlock();return b instanceof SugarCaneBlock || b instanceof CactusBlock || b instanceof BambooStalkBlock || b instanceof KelpBlock || b instanceof KelpPlantBlock;
    }
    private static IntegerProperty age(BlockState s) {
        for(var p:s.getProperties())if(p instanceof IntegerProperty i && p.getName().equals("age"))return i;
        return null;
    }
    public static BlockPos root(ServerLevel l,BlockPos pos,BlockState s) {
        if(column(s))while(pos.getY()>l.getMinBuildHeight() && (l.getBlockState(pos.below()).is(s.getBlock()) || ((s.getBlock() instanceof KelpBlock || s.getBlock() instanceof KelpPlantBlock) && l.getBlockState(pos.below()).is(Blocks.KELP_PLANT))))pos=pos.below();
        return pos;
    }
    private static double day(ServerLevel l) { return l.getServer().overworld().getDayTime()/24000.0; }
    public static void planted(ServerLevel l,BlockPos p) {
        BlockState s=l.getBlockState(p);if(!supports(s))return;p=root(l,p,s);
        net.mads.industron.climate.ClimateWorker.forget(l,p);CropCalendarData.get(l).remove(p);update(l,p);
    }
    public static void update(ServerLevel l,BlockPos original) {
        if(!l.hasChunkAt(original))return;
        BlockState s=l.getBlockState(original);var data=CropCalendarData.get(l);
        if(!supports(s)) {data.remove(original);return;}
        BlockPos p=root(l,original,s);s=l.getBlockState(p);
        Block identity=s.getBlock();
        if(identity instanceof KelpPlantBlock)identity=Blocks.KELP;
        if(s.is(Blocks.ATTACHED_MELON_STEM))identity=Blocks.MELON_STEM;
        if(s.is(Blocks.ATTACHED_PUMPKIN_STEM))identity=Blocks.PUMPKIN_STEM;
        String id=BuiltInRegistries.BLOCK.getKey(identity).toString();
        PlantRecord entry=data.get(p);double now=day(l);IntegerProperty prop=age(s);
        var profile=net.mads.industron.climate.PlantClimates.block(s);
        double required=profile.growthDays();
        int maximum=prop==null?1:prop.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(1);
        if(entry==null || !id.equals(entry.getString("Block"))) {
            entry=new PlantRecord();entry.putString("Block",id);entry.putDouble("Last",now);entry.putDouble("Planted",now);entry.putLong("Generation",l.random.nextLong());
            entry.putDouble("Growth",!column(s) && prop!=null?required*s.getValue(prop)/maximum:0);
            sample(l,p,s,entry);
            if(s.getBlock() instanceof AttachedStemBlock)entry.putDouble("Growth",required);
        }
        if(!column(s) && prop!=null && entry.contains("Age") && s.getValue(prop)<entry.getInt("Age")) {
            entry.putDouble("Growth",required*s.getValue(prop)/maximum);entry.putDouble("Last",now);
        }
        double last=entry.getDouble("Last");now=Math.max(now,last);
        if(!entry.contains("Planted"))entry.putDouble("Planted",last);
        var environment=new net.mads.industron.climate.ClimateGrowth.Environment(entry.getDouble("Temperature"),entry.getDouble("Humidity"),p.getY(),
            entry.getBoolean("Water"),entry.getBoolean("Light"),l.dimension()==net.minecraft.world.level.Level.OVERWORLD,
            net.mads.industron.climate.ClimateWorld.seed(l),entry.getDouble("ShelterHeat"),net.mads.industron.climate.ClimateWorld.fixedTemperature(l),entry.contains("RainMode")?entry.getInt("RainMode"):-1,entry.getDouble("Stress"),entry.getBoolean("Aquatic"),entry.getBoolean("Roof"));
        // Publish new plant identity before deferring work, so the completion can validate it.
        data.put(p,entry);
        var completed=net.mads.industron.climate.ClimateWorker.compute(l,p,entry.getLong("Generation"),last,now,entry.contains("UnloadedFrom")?entry.getDouble("UnloadedFrom"):-1,environment,profile);
        if(completed==null)return;
        now=completed.to();double added=completed.result().growth();
        double growth=Math.min(required,entry.getDouble("Growth")+added);
        double stress=Math.max(0,Math.min(1,entry.getDouble("Stress")+completed.result().stress()));
        if(entry.contains("UnloadedFrom") && now>=entry.getDouble("UnloadedFrom")) {
            entry.putDouble("UnloadedAdded",completed.unloadedGrowth());entry.putDouble("UnloadedDays",now-entry.getDouble("UnloadedFrom"));entry.remove("UnloadedFrom");
        }
        entry.putDouble("LastAdded",added);entry.putDouble("LastElapsed",now-last);
        entry.putDouble("Stress",stress);entry.putDouble("Last",now);entry.putDouble("Growth",growth);
        sample(l,p,s,entry);data.put(p,entry);
        if(stress>=1) {
            // Remove the whole living column without mature crop drops.
            BlockPos cursor=p;while(l.hasChunkAt(cursor)&&supports(l.getBlockState(cursor))&&column(s)) {
                l.setBlock(cursor,Blocks.AIR.defaultBlockState(),3);cursor=cursor.above();}
            if(!column(s))l.setBlock(p,Blocks.AIR.defaultBlockState(),3);
            data.remove(p);return;
        }
        if(column(s)) {
            int max=s.getBlock() instanceof BambooStalkBlock?12:(s.getBlock() instanceof KelpBlock || s.getBlock() instanceof KelpPlantBlock)?15:3;
            int wanted=1+(int)Math.floor(Math.min(required,growth)/required*(max-1));
            BlockPos top=p;while((l.getBlockState(top.above()).is(s.getBlock()) || ((s.getBlock() instanceof KelpBlock || s.getBlock() instanceof KelpPlantBlock) && (l.getBlockState(top.above()).is(Blocks.KELP)||l.getBlockState(top.above()).is(Blocks.KELP_PLANT)))) && top.getY()<l.getMaxBuildHeight()-1)top=top.above();
            int height=top.getY()-p.getY()+1;
            // Harvesting upper sections spends growth, so replanting is never an instant regrowth exploit.
            int previous=entry.contains("Height")?entry.getInt("Height"):height;
            if(height<previous) {growth=Math.max(0,growth-(previous-height)*required/(max-1));entry.putDouble("Growth",growth);wanted=height;}
            while(height<wanted && top.getY()<l.getMaxBuildHeight()-1) {
                BlockPos next=top.above();BlockState nextState=(s.getBlock() instanceof KelpPlantBlock ? Blocks.KELP : s.getBlock()).defaultBlockState();
                if(s.getBlock() instanceof KelpBlock || s.getBlock() instanceof KelpPlantBlock) {if(!l.getBlockState(next).is(Blocks.WATER))break;}
                else if(!l.getBlockState(next).isAir())break;
                if(!l.isAreaLoaded(next,1) || !nextState.canSurvive(l,next))break;
                if(s.getBlock() instanceof KelpBlock || s.getBlock() instanceof KelpPlantBlock)l.setBlock(top,Blocks.KELP_PLANT.defaultBlockState(),3);
                l.setBlock(next,nextState,3);top=next;height++;
            }
            entry.putInt("Height",height);data.setDirty();
        } else if(prop!=null) {
            int target=Math.min(maximum,(int)Math.floor(growth/required*maximum));
            if(target!=s.getValue(prop))l.setBlock(p,s.setValue(prop,target),3);
            entry.putInt("Age",target);data.setDirty();
        }
    }
    public static boolean allowFruit(ServerLevel l,BlockPos p,BlockState s) {
        if(!(s.getBlock() instanceof StemBlock) || s.getValue(StemBlock.AGE)<7)return false;
        var e=CropCalendarData.get(l).get(p);double now=day(l);
        if(e==null || e.getDouble("Growth")<net.mads.industron.climate.PlantClimates.block(s).growthDays() || now-e.getDouble("Fruit")<5)return false;
        e.putDouble("Fruit",now);CropCalendarData.get(l).setDirty();return true;
    }
    private static void sample(ServerLevel l,BlockPos p,BlockState s,PlantRecord e) {
        var biome=l.getBiome(p).value();e.putDouble("Temperature",biome.getBaseTemperature());
        e.putDouble("Humidity",biome.getModifiedClimateSettings().downfall());
        BlockState below=l.getBlockState(p.below());
        boolean water=below.hasProperty(FarmBlock.MOISTURE) && below.getValue(FarmBlock.MOISTURE)>0;
        if(s.getBlock() instanceof CactusBlock || s.getBlock() instanceof KelpBlock || s.getBlock() instanceof KelpPlantBlock)water=true;
        if(!water)for(BlockPos nearby:BlockPos.betweenClosed(p.offset(-2,-1,-2),p.offset(2,0,2)))
            if(l.hasChunkAt(nearby) && l.getFluidState(nearby).is(net.minecraft.tags.FluidTags.WATER)) {water=true;break;}
        e.putInt("RainMode",l.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE)?-1:l.isRaining()?1:0);
        var shelter=net.mads.industron.climate.ClimateWorld.shelter(l,p);
        e.putDouble("ShelterHeat",shelter.heat());e.putBoolean("Roof",shelter.roof());
        boolean aquatic=s.getBlock() instanceof KelpBlock ||s.getBlock() instanceof KelpPlantBlock;
        e.putBoolean("Aquatic",aquatic);
        e.putBoolean("Water",water);e.putBoolean("Light",l.getBrightness(LightLayer.SKY,p)>=(aquatic?5:9) || l.getBrightness(LightLayer.BLOCK,p)>=9 || s.getBlock() instanceof NetherWartBlock);
    }
}
