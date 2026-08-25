package net.mads.industron.debug;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CEPerformanceProfiler {
    private static final long TICK_BUDGET_NS = 50_000_000L;
    private static final int BUDGET_UNITS = 100_000;
    private static final long[] totalNs = new long[Metric.values().length];
    private static final long[] calls = new long[Metric.values().length];
    private static final long[] maxNs = new long[Metric.values().length];
    private static boolean enabled;
    private static long lastGameTime = Long.MIN_VALUE;
    private static long observedTicks;

    private CEPerformanceProfiler() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void enable() {
        enabled = true;
        reset();
    }

    public static void disable() {
        enabled = false;
    }

    public static void reset() {
        for (int i = 0; i < totalNs.length; i++) {
            totalNs[i] = 0;
            calls[i] = 0;
            maxNs[i] = 0;
        }
        lastGameTime = Long.MIN_VALUE;
        observedTicks = 0;
    }

    public static long begin(Level level) {
        if (!enabled || level == null || level.isClientSide()) {
            return 0;
        }
        long gameTime = level.getGameTime();
        if (gameTime != lastGameTime) {
            lastGameTime = gameTime;
            observedTicks++;
        }
        return System.nanoTime();
    }

    public static void record(Metric metric, long startNs) {
        if (!enabled || startNs == 0) {
            return;
        }
        long elapsed = System.nanoTime() - startNs;
        int index = metric.ordinal();
        totalNs[index] += elapsed;
        calls[index]++;
        if (elapsed > maxNs[index]) {
            maxNs[index] = elapsed;
        }
    }

    public static List<Component> stats() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("CE profiler: " + (enabled ? "ON" : "OFF")));
        lines.add(Component.literal("Budget: " + BUDGET_UNITS + " units = 50.000 ms/tick"));

        long ticks = Math.max(1, observedTicks);
        long rootNs = totalNs[Metric.MULTIBLOCK_TICK.ordinal()] + totalNs[Metric.WIRE_NETWORK.ordinal()];
        lines.add(Component.literal("Total avg/tick: " + formatNs(rootNs / ticks) + " (" + units(rootNs / ticks) + " units) over " + observedTicks + " ticks"));

        Metric machineMetric = Metric.MULTIBLOCK_TICK;
        long machineCalls = calls[machineMetric.ordinal()];
        long machineAvg = machineCalls <= 0 ? 0 : totalNs[machineMetric.ordinal()] / machineCalls;
        lines.add(Component.literal("Machine avg: " + formatNs(machineAvg) + " (" + units(machineAvg) + " units), calls " + machineCalls));

        for (Metric metric : Metric.values()) {
            int index = metric.ordinal();
            long metricCalls = calls[index];
            long avg = metricCalls <= 0 ? 0 : totalNs[index] / metricCalls;
            long perTick = totalNs[index] / ticks;
            lines.add(Component.literal(metric.label + ": " + metricCalls
                    + " calls, avg " + formatNs(avg)
                    + ", max " + formatNs(maxNs[index])
                    + ", avg/tick " + units(perTick) + " units"));
        }
        return lines;
    }

    private static long units(long ns) {
        return Math.min(BUDGET_UNITS, Math.max(0, ns * BUDGET_UNITS / TICK_BUDGET_NS));
    }

    private static String formatNs(long ns) {
        if (ns >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.3f ms", ns / 1_000_000.0D);
        }
        return String.format(Locale.ROOT, "%.3f us", ns / 1_000.0D);
    }

    public enum Metric {
        MULTIBLOCK_TICK("multiblock_tick"),
        MULTIBLOCK_RECIPE_TICK("multiblock_recipe_tick"),
        RECIPE_LOOKUP("recipe_lookup"),
        ENERGY_TRANSFER("energy_transfer"),
        WIRE_NETWORK("wire_network");

        private final String label;

        Metric(String label) {
            this.label = label;
        }
    }
}
