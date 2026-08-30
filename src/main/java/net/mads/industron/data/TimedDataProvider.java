package net.mads.industron.data;

import net.mads.industron.Industron;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lightweight timing wrapper used to make expensive datagen providers visible in the log.
 *
 * Minecraft can start multiple data providers at the same time. Industron has a large,
 * highly interconnected material/asset graph, so running all providers concurrently causes
 * a large peak heap usage: every provider can keep model builders, JsonObjects, texture
 * buffers and recipe objects alive at the same time. This wrapper serializes providers while
 * preserving the normal DataProvider contract. The output is unchanged; only the peak memory
 * usage and ordering are controlled.
 */
final class TimedDataProvider implements DataProvider {
    private static final AtomicReference<CompletableFuture<Void>> QUEUE =
            new AtomicReference<>(CompletableFuture.completedFuture(null));

    private final DataProvider delegate;

    TimedDataProvider(DataProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        long started = System.nanoTime();

        while (true) {
            CompletableFuture<Void> previous = QUEUE.get();
            CompletableFuture<Void> next = previous
                    .handle((ignored, throwable) -> null)
                    .thenCompose(ignored -> runDelegate(output, started));
            if (QUEUE.compareAndSet(previous, next)) {
                return next;
            }
        }
    }

    private CompletableFuture<Void> runDelegate(CachedOutput output, long started) {
        logHeapBefore();
        try {
            return delegate.run(output)
                    .whenComplete((ignored, throwable) -> {
                        logElapsed(started, throwable);
                        logHeapAfter();
                    })
                    .thenApply(ignored -> null);
        } catch (RuntimeException exception) {
            logElapsed(started, exception);
            logHeapAfter();
            return CompletableFuture.failedFuture(exception);
        }
    }

    private void logElapsed(long started, Throwable throwable) {
        long millis = (System.nanoTime() - started) / 1_000_000L;
        if (throwable == null) {
            Industron.LOGGER.info(
                    "Datagen provider '{}' finished in {} ms",
                    delegate.getName(),
                    millis
            );
        } else {
            Industron.LOGGER.error(
                    "Datagen provider '{}' failed after {} ms",
                    delegate.getName(),
                    millis,
                    throwable
            );
        }
    }

    private void logHeapBefore() {
        if (!Boolean.getBoolean("industron.datagen.memoryLog")) {
            return;
        }
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long max = runtime.maxMemory() / (1024L * 1024L);
        Industron.LOGGER.info(
                "Datagen heap before '{}': {} MiB used / {} MiB max",
                delegate.getName(),
                used,
                max
        );
    }

    private void logHeapAfter() {
        if (!Boolean.getBoolean("industron.datagen.memoryLog")) {
            return;
        }
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        long max = runtime.maxMemory() / (1024L * 1024L);
        Industron.LOGGER.info(
                "Datagen heap after: {} MiB used / {} MiB max",
                used,
                max
        );
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
}
