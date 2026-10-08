package net.mads.industron.climate;
import java.util.Locale;
public final class ClimatePresentation {
    private ClimatePresentation() {}
    public static String number(double n){return String.format(Locale.ROOT,"%.1f",n);}
    public static String seasons(ClimateGrowth.Environment e,PlantClimateProfile p) {
        if(!e.seasonal())return "No seasonal cycle";
        StringBuilder names=new StringBuilder();
        for(int s=0;s<4;s++)if(ClimateGrowth.integrate(s*25,s*25+25,e,p).growth()>1) {
            if(!names.isEmpty())names.append(", ");names.append(ClimateMath.seasonName(s*25));}
        return names.isEmpty()?"None in these conditions":names.toString();
    }
}
