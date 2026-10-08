package net.mads.industron.climate;

/** Pure, deterministic climate arithmetic; safe on worker threads. Tick 0 is 06:00. */
public final class ClimateMath {
    public static final int YEAR_DAYS=100, SEASON_DAYS=25, DAY_TICKS=24000;
    // Exact day keys, two reusable entries per thread; no Level, biome, position or entity is retained.
    // The second entry handles sunExposure's tick-rounded day without evicting the ambient day.
    private static final ThreadLocal<TimeCache> TIME = ThreadLocal.withInitial(TimeCache::new);
    private ClimateMath() {}
    public static int season(double day) { return Math.floorMod((long)Math.floor(day), YEAR_DAYS)/SEASON_DAYS; }
    public static String seasonName(double day) { return switch(season(day)){case 0->"Spring";case 1->"Summer";case 2->"Autumn";default->"Winter";}; }
    public static double seasonalOffset(double day) { return TIME.get().at(day).seasonalOffset(); }
    public static double daylightTicks(double day) { return TIME.get().at(day).daylightTicks(); }
    public static double fraction(double value) { return value-Math.floor(value); }
    public static long solarTicks(long worldTicks) {
        double day=worldTicks/(double)DAY_TICKS;
        double daylight=daylightTicks(day), dawn=6000-daylight/2, dusk=6000+daylight/2;
        double tick=Math.floorMod(worldTicks,DAY_TICKS);
        double since=fraction((tick-dawn)/DAY_TICKS)*DAY_TICKS;
        double mapped=since<daylight?since*12000/daylight:12000+(since-daylight)*12000/(DAY_TICKS-daylight);
        return (long)mapped;
    }
    public static double dailyOffset(double day,double humidity,boolean cloudy) {
        double wave=TIME.get().at(day).dailyWave();
        return wave*(3+6*(1-clamp(humidity,0,1)))*(cloudy?.45:1);
    }
    public static long mix(long n) { n=(n^(n>>>30))*0xbf58476d1ce4e5b9L; n=(n^(n>>>27))*0x94d049bb133111ebL; return n^(n>>>31); }
    public static boolean rain(long seed,double day) {
        long slot=(long)Math.floor(day*8);
        double probability=.24+.12*Math.cos(2*Math.PI*(day-12.5)/YEAR_DAYS);
        return (mix(seed+slot)>>>11)*0x1.0p-53 < probability;
    }
    public static boolean thunder(long seed,double day) {return rain(seed,day)&&(mix(seed+(long)Math.floor(day*8)+193)>>>11)*0x1.0p-53<(.06+.08*Math.max(0,Math.cos(2*Math.PI*(day-37.5)/YEAR_DAYS)));}
    public static double wind(long seed,double day) { return .15+.85*(mix(seed+(long)Math.floor(day*8)+71)>>>11)*0x1.0p-53; }
    public static double sunExposure(double day,boolean shaded,boolean rainy) {
        if(shaded||rainy)return 0;
        long solar=Math.floorMod(solarTicks((long)Math.floor(day*DAY_TICKS)),DAY_TICKS);
        return solar>=12000?0:4*Math.sin(Math.PI*solar/12000.0);
    }
    public static double temperature(double day,double biome,double humidity,int height,boolean rainy,boolean seasonal) {
        if(!seasonal) return 24;
        return 5+biome*25+seasonalOffset(day)+dailyOffset(day,humidity,rainy)
            -Math.max(0,height-64)*.065-(rainy?2:0);
    }
    public static double clamp(double n,double min,double max) { return Math.max(min,Math.min(max,n)); }

    private static final class TimeCache {
        private TimeTerms current = new TimeTerms();
        private TimeTerms previous = new TimeTerms();
        private TimeTerms at(double day) {
            if (current.matches(day)) return current;
            TimeTerms reuse = previous;
            previous = current; current = reuse;
            if (!current.matches(day)) current.reset(day);
            return current;
        }
    }

    private static final class TimeTerms {
        private double day, seasonal, daylight, wave;
        private long dayBits;
        private boolean valid, hasSeasonal, hasDaylight, hasWave;
        private boolean matches(double day) { return valid && dayBits == Double.doubleToLongBits(day); }
        private void reset(double day) {
            this.day = day; dayBits = Double.doubleToLongBits(day); valid = true;
            hasSeasonal = hasDaylight = hasWave = false;
        }
        private double seasonalOffset() {
            if (!hasSeasonal) { seasonal = -3+11*Math.cos(2*Math.PI*(day-37.5)/YEAR_DAYS); hasSeasonal = true; }
            return seasonal;
        }
        private double daylightTicks() {
            if (!hasDaylight) { daylight = 12000+4000*Math.cos(2*Math.PI*(day-37.5)/YEAR_DAYS); hasDaylight = true; }
            return daylight;
        }
        private double dailyWave() {
            if (!hasWave) {
                double tick=fraction(day)*DAY_TICKS;
                // Minimum 05:00, maximum 14:00; a longer cooling evening/night.
                double minimum=6000-daylightTicks()/2-1000;
                double since=fraction((tick-minimum)/DAY_TICKS)*DAY_TICKS;
                double warming=fraction((8000-minimum)/DAY_TICKS)*DAY_TICKS;
                wave=since<warming ? -Math.cos(Math.PI*since/warming) : Math.cos(Math.PI*(since-warming)/(DAY_TICKS-warming));
                hasWave = true;
            }
            return wave;
        }
    }
}
