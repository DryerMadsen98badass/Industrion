package net.mads.industron.climate;

/** Pure body response, expressed per second of active simulation. */
public final class ThermalRules {
    private ThermalRules() {}
    public record Body(double temperature,double wetness) {}
    public static Body advance(Body body,double ambient,double wind,boolean wet,double heat,double insulation,
            double coldLimit,double hotLimit,boolean working) {
        double moisture=ClimateMath.clamp(body.wetness+(wet?.04:-(.004+heat*.0008)),0,1);
        double protection=insulation*(1-.65*moisture);
        double apparent=ambient-wind*(2+4*moisture);
        double minimum=coldLimit-protection*25,maximum=hotLimit-protection*4;
        double target=37+(apparent<minimum?(apparent-minimum)*.12:apparent>maximum?(apparent-maximum)*.15:0);
        if(working)target+=.4;
        target=ClimateMath.clamp(target,30,43);
        return new Body(ClimateMath.clamp(body.temperature+(target-body.temperature)*.005,30,43),moisture);
    }
    public static String feeling(double body) {
        if(body<=33)return "Dangerously cold";if(body<35)return "Very cold";if(body<36)return "Cold";
        if(body>=41)return "Dangerously hot";if(body>39)return "Very hot";if(body>38)return "Hot";return "Comfortable";
    }
    public static boolean strained(double body){return body<35.5||body>38.5;}
}
