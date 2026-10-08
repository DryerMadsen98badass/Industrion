package net.mads.industron.farming;

/** Pure calendar math. One year is 100 world days; each season lasts 25 days. */
public final class SeasonCalendar {
    public static final int YEAR_DAYS = 100, SEASON_DAYS = 25;
    private SeasonCalendar() {}
    public static int season(double day) { return Math.floorMod((long)Math.floor(day), YEAR_DAYS) / SEASON_DAYS; }
    public static double temperature(double day, double biome, int height) {
        return net.mads.industron.climate.ClimateMath.temperature(day,biome,.5,height,false,true);
    }
    public static double rate(double day, double biome, double humidity, int height,
                              boolean watered, boolean lit, boolean tropical, boolean seasonal) {
        if (!lit) return 0;
        double t = seasonal ? temperature(day, biome, height) : 24;
        double minimum = tropical ? 12 : 2, optimum = tropical ? 25 : 18;
        if (t <= minimum || t >= 42) return 0;
        double warmth = Math.min(1, Math.min((t-minimum)/(optimum-minimum), (42-t)/(42-optimum)));
        return warmth * (watered ? 1 : Math.max(0.2, Math.min(1, humidity)));
    }
    public static double growth(double from, double to, double biome, double humidity, int height,
                                boolean watered, boolean lit, boolean tropical, boolean seasonal) {
        if (to <= from) return 0;
        // Whole years share the same climate; bounded work even after years without loading.
        double total = 0;
        long years = (long)((to-from)/YEAR_DAYS);
        if (years > 0) {
            double cycle = 0;
            for (int d=0; d<YEAR_DAYS; d++) cycle += rate(d,biome,humidity,height,watered,lit,tropical,seasonal);
            total = years * cycle; from += years * YEAR_DAYS;
        }
        while (from < to) {
            double end = Math.min(to, Math.floor(from)+1);
            total += (end-from)*rate(from,biome,humidity,height,watered,lit,tropical,seasonal);
            from=end;
        }
        return total;
    }
}
