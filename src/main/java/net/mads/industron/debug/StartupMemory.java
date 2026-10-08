package net.mads.industron.debug;

import net.mads.industron.Industron;

/** Observes the actual mod JVM, not the Gradle daemon. Does not force GC. */
public final class StartupMemory {
    private StartupMemory() { }

    public static void log(String phase) {
        Runtime runtime = Runtime.getRuntime();
        long mib = 1024L * 1024L;
        Industron.LOGGER.info("Industron heap [{}]: used={} MiB, committed={} MiB, maximum={} MiB",
                phase, (runtime.totalMemory() - runtime.freeMemory()) / mib,
                runtime.totalMemory() / mib, runtime.maxMemory() / mib);
    }
}
