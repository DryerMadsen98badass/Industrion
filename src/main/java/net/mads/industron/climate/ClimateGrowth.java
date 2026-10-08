package net.mads.industron.climate;

/** Immutable snapshots are the only values passed to the climate worker. */
public final class ClimateGrowth {
    private ClimateGrowth() {}
    public record Environment(double biome,double humidity,int height,boolean water,boolean light,
            boolean seasonal,long seed,double shelterHeat,double fixedTemperature,int rainMode,double initialStress,boolean aquatic,boolean shaded) {
        public Environment(double biome,double humidity,int height,boolean water,boolean light,boolean seasonal,long seed,double shelterHeat) {
            this(biome,humidity,height,water,light,seasonal,seed,shelterHeat,24,-1,0,false,false);
        }
        public Environment(double biome,double humidity,int height,boolean water,boolean light,boolean seasonal,long seed,double shelterHeat,double fixedTemperature) {
            this(biome,humidity,height,water,light,seasonal,seed,shelterHeat,fixedTemperature,-1,0,false,false);
        }
    }
    public record Result(double growth,double stress) {}
    public static Result integrate(double from,double to,Environment e,PlantClimateProfile profile) {
        if(to<=from)return new Result(0,0);
        double growth=0,stress=ClimateMath.clamp(e.initialStress,0,1);
        // Bounded historical integration: the most recent year represents very old periods.
        double start=Math.max(from,to-100), scale=(to-from)/(to-start);
        while(start<to) {
            double end=Math.min(to,(Math.floor(start*8)+1)/8), midpoint=(start+end)/2;
            boolean rainy=e.seasonal&&e.humidity>0&&(e.rainMode<0?ClimateMath.rain(e.seed,midpoint):e.rainMode==1);
            double t=(e.seasonal?ClimateMath.temperature(midpoint,e.biome,e.humidity,e.height,rainy,true):e.fixedTemperature);
            if(e.aquatic)t=ClimateMath.clamp(t*.35+8,1,30);
            else if(e.seasonal)t+=ClimateMath.sunExposure(midpoint,e.shaded,rainy);
            t+=e.shelterHeat;
            double rate=profile.rate(t,e.humidity,e.water||(rainy&&!e.shaded),e.light),dt=end-start;
            growth+=rate*dt;
            if(t<profile.minimum() && !profile.dormant())stress+=dt/profile.frostToleranceDays();
            else if(t>profile.maximum())stress+=dt/profile.heatToleranceDays();
            else if(!e.water && e.humidity<profile.minimumHumidity()*.25)stress+=dt/4;
            else stress-=dt*.25;
            stress=ClimateMath.clamp(stress,0,1);
            if(stress>=1)break;
            start=end;
        }
        return new Result(stress>=1?growth:growth*scale,stress-e.initialStress);
    }
    public static double harvestDay(double now,double progress,Environment e,PlantClimateProfile p) {
        double remaining=p.growthDays()-progress;
        if(remaining<=0)return now;
        for(int step=0;step<1600;step++) {
            double end=now+.125;remaining-=integrate(now,end,e,p).growth;now=end;
            if(remaining<=0)return now;
        }
        return -1;
    }
}
