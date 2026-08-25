package net.mads.industron.data;

import net.mads.industron.Industron;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.util.concurrent.CompletableFuture;

/** Lightweight timing wrapper used to make expensive datagen providers visible in the log. */
final class TimedDataProvider implements DataProvider {
    private final DataProvider delegate;

    TimedDataProvider(DataProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        long started = System.nanoTime();
        try {
            return delegate.run(output).whenComplete((ignored, throwable) -> logElapsed(started, throwable));
        } catch (RuntimeException exception) {
            logElapsed(started, exception);
            throw exception;
        }
    }

    private void logElapsed(long started, Throwable throwable) {
        long millis = (System.nanoTime() - started) / 1_000_000L;
        if (throwable == null) {
            Industron.LOGGER.info("Datagen provider '{}' finished in {} ms", delegate.getName(), millis);
        } else {
            Industron.LOGGER.error("Datagen provider '{}' failed after {} ms", delegate.getName(), millis);
        }
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
}
