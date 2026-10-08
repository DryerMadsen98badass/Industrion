package net.mads.industron.climate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** All world access is on the owning game thread. Shelter is cached for five seconds. */
public final class ClimateWorld {
    private ClimateWorld() {}
    public record Shelter(boolean roof,int walls,double heat) { public double windExposure(){return roof ? Math.max(.1,1-walls*.22) : 1;} }
    public record Sample(double day,double temperature,double humidity,double wind,boolean rain,boolean snow,Shelter shelter) {}
    private record Cached(long expires,Shelter shelter) {}
    private static final Map<Level,LinkedHashMap<Long,Cached>> shelters=new WeakHashMap<>();
    public static long seed(ServerLevel level) { return ClimateMath.mix(level.getSeed()^0x49a0fc351ce2L); }
    public static double day(Level level) {
        return (level instanceof ServerLevel server ? server.getServer().overworld().getDayTime() : level.getDayTime())/24000.0;
    }
    public static double fixedTemperature(Level level) {return level.dimension()==Level.NETHER?48:level.dimension()==Level.END?8:24;}
    public static double ambient(Level level,BlockPos pos) {
        if(level.dimension()==Level.NETHER)return 48;
        if(level.dimension()==Level.END)return 8;
        var biome=level.getBiome(pos).value();
        return ClimateMath.temperature(day(level),biome.getBaseTemperature(),biome.getModifiedClimateSettings().downfall(),pos.getY(),level.isRaining()&&biome.hasPrecipitation(),level.dimension()==Level.OVERWORLD);
    }
    public static Shelter shelter(Level level,BlockPos pos) {
        var cache=shelters.computeIfAbsent(level,k->new LinkedHashMap<>());long key=pos.asLong(),now=level.getGameTime();
        Cached old=cache.get(key);if(old!=null && old.expires>=now)return old.shelter;
        boolean roof=!level.canSeeSky(pos),closeRoof=false;int walls=0;double heat=0;
        // Sky access alone is insufficient: a canopy twenty blocks above is not a warm room.
        for(int y=1;y<=6;y++) {BlockPos p=pos.above(y);if(!level.hasChunkAt(p))break;
            if(!level.getBlockState(p).isAir() && !level.getBlockState(p).getCollisionShape(level,p).isEmpty()){roof=true;closeRoof=true;break;}}
        for(Direction d:Direction.Plane.HORIZONTAL) {
            for(int distance=1;distance<=4;distance++) {BlockPos p=pos.relative(d,distance);
                if(!level.hasChunkAt(p))break;
                if(!level.getBlockState(p).getCollisionShape(level,p).isEmpty() && !level.getBlockState(p.above()).getCollisionShape(level,p.above()).isEmpty()){walls++;break;}}
        }
        if(level.getBrightness(LightLayer.BLOCK,pos)>=8)for(BlockPos p:BlockPos.betweenClosed(pos.offset(-3,-1,-3),pos.offset(3,2,3))) {
            if(!level.hasChunkAt(p))continue;var state=level.getBlockState(p);
            boolean source=state.is(Blocks.LAVA)||state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE)
                ||(state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT));
            if(!source)continue;
            // A solid obstacle between the source and occupant prevents heat through sealed walls.
            double distance=Math.sqrt(p.distSqr(pos));boolean blocked=false;
            for(int step=1;step<Math.ceil(distance);step++) {
                double ratio=step/distance;BlockPos middle=BlockPos.containing(pos.getX()+(p.getX()-pos.getX())*ratio,pos.getY()+(p.getY()-pos.getY())*ratio,pos.getZ()+(p.getZ()-pos.getZ())*ratio);
                if(!level.getBlockState(middle).getCollisionShape(level,middle).isEmpty()){blocked=true;break;}}
            if(!blocked)heat+=12/(1+distance);
        }
        Shelter value=new Shelter(roof,walls,Math.min(18,heat)*(closeRoof&&walls>=3?1.4:1));
        if(cache.size()>=2048)cache.remove(cache.keySet().iterator().next());cache.put(key,new Cached(now+100,value));return value;
    }
    public static Sample sample(Level level,BlockPos pos) {
        var b=level.getBiome(pos).value();Shelter shelter=shelter(level,pos);
        double humidity=b.getModifiedClimateSettings().downfall(),day=day(level);
        double ambient=level.dimension()==Level.NETHER?48:level.dimension()==Level.END?8:
            ClimateMath.temperature(day,b.getBaseTemperature(),humidity,pos.getY(),level.isRaining()&&b.hasPrecipitation(),level.dimension()==Level.OVERWORLD);
        long seed=level instanceof ServerLevel s?seed(s):0;
        boolean precipitation=level.isRaining()&&b.hasPrecipitation()&&level.canSeeSky(pos)&&!shelter.roof;
        boolean underwater=level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER);
        double local=underwater?ClimateMath.clamp(ambient*.35+8,1,30):ambient+(level.dimension()==Level.OVERWORLD?ClimateMath.sunExposure(day,shelter.roof,level.isRaining()):0);
        return new Sample(day,local+shelter.heat,humidity,underwater?0:ClimateMath.wind(seed,day)*shelter.windExposure(),
            precipitation&&ambient>0,precipitation&&ambient<=0,shelter);
    }
    public static void invalidate(Level level,BlockPos pos) {
        // Cached shelters near a changed block will be resampled; unrelated positions stay cached.
        var map=shelters.get(level);if(map!=null)map.keySet().removeIf(key->BlockPos.of(key).distSqr(pos)<=144);
    }
    public static void clear(){shelters.clear();}
}
