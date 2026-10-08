package net.mads.industron.climate;

/** Plant-specific physiology. Growth is measured in effective days, not loaded ticks. */
public record PlantClimateProfile(double growthDays, double minimum, double optimum, double maximum,
        double minimumHumidity, double frostToleranceDays, double heatToleranceDays, boolean dormant) {
    public static final PlantClimateProfile TEMPERATE=new PlantClimateProfile(20,2,18,38,.35,2,2,false);
    public static final PlantClimateProfile TROPICAL=new PlantClimateProfile(20,12,25,42,.55,.75,2,false);
    public static final PlantClimateProfile DESERT=new PlantClimateProfile(20,5,28,48,.05,2,4,true);
    public static final PlantClimateProfile AQUATIC=new PlantClimateProfile(20,0,16,32,0,3,2,true);
    public PlantClimateProfile {
        if(!Double.isFinite(growthDays+minimum+optimum+maximum+minimumHumidity+frostToleranceDays+heatToleranceDays)
            ||growthDays<=0 || minimum>=optimum || optimum>=maximum || minimumHumidity<0 || minimumHumidity>1
            ||frostToleranceDays<=0 ||heatToleranceDays<=0) throw new IllegalArgumentException("Invalid plant climate profile");
    }
    public double rate(double temperature,double humidity,boolean watered,boolean lit) {
        if(!lit ||temperature<=minimum ||temperature>=maximum)return 0;
        double warmth=Math.min(1,Math.min((temperature-minimum)/(optimum-minimum),(maximum-temperature)/(maximum-optimum)));
        return warmth*(watered?1:Math.min(1,Math.max(0,humidity)/Math.max(.01,minimumHumidity)));
    }
    public String stopped(double temperature,double humidity,boolean watered,boolean lit) {
        if(!lit)return "Not enough light";
        if(temperature<=minimum)return dormant?"Winter dormancy":"Too cold";
        if(temperature>=maximum)return "Too hot";
        if(!watered && humidity<=0)return "Too dry";
        return "";
    }
}
