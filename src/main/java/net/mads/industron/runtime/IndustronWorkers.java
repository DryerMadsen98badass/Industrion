package net.mads.industron.runtime;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** One bounded executor. Jobs must capture ONLY immutable snapshots, never a host or world. */
public final class IndustronWorkers {
    private static ThreadPoolExecutor executor;
    private static final AtomicLong completed = new AtomicLong(), rejected = new AtomicLong();
    private static final AtomicLong discarded = new AtomicLong(), failed = new AtomicLong();
    private static final java.util.Set<CompletableFuture<?>> results = ConcurrentHashMap.newKeySet();
    private static final AtomicLong workerNs = new AtomicLong();
    private IndustronWorkers() {}

    public static synchronized void start() {
        stop();
        int requested = Integer.getInteger("industron.workers", 3);
        int count = Math.max(1, Math.min(requested, Math.max(1, Runtime.getRuntime().availableProcessors() - 1)));
        AtomicLong number = new AtomicLong();
        executor = new ThreadPoolExecutor(count, count, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(64), runnable -> {
                    Thread thread = new Thread(runnable, "Industron-worker-" + number.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        workerNs.set(0); completed.set(0); rejected.set(0); discarded.set(0); failed.set(0);
    }

    /** Null means saturated/stopped: caller retains synchronous or idle fallback. Never caller-runs. */
    public static synchronized <T> CompletableFuture<T> submit(String name, Supplier<T> calculation) {
        if (executor == null || executor.isShutdown()) { rejected.incrementAndGet(); return null; }
        CompletableFuture<T> result = new CompletableFuture<>();
        results.add(result);
        try {
            executor.execute(() -> {
                long started = System.nanoTime();
                String oldName = Thread.currentThread().getName();
                Thread.currentThread().setName(oldName + "/" + name);
                try { result.complete(calculation.get()); completed.incrementAndGet(); }
                catch (Throwable error) {
                    long failures = failed.incrementAndGet();
                    if (failures <= 3) net.mads.industron.Industron.LOGGER.error("Industron worker task failed: " + name, error);
                    result.completeExceptionally(error);
                } finally { workerNs.addAndGet(System.nanoTime() - started); results.remove(result); Thread.currentThread().setName(oldName); }
            });
        } catch (RejectedExecutionException ignored) { results.remove(result); rejected.incrementAndGet(); return null; }
        return result;
    }

    public static synchronized boolean available() { return executor != null && !executor.isShutdown() && executor.getQueue().remainingCapacity() > 0; }

    public static void discard() { discarded.incrementAndGet(); }
    public static synchronized String stats() {
        return "workers=" + (executor == null ? 0 : executor.getCorePoolSize())
                + ", pending=" + (executor == null ? 0 : executor.getQueue().size())
                + ", active=" + (executor == null ? 0 : executor.getActiveCount())
                + ", completed=" + completed.get() + ", rejected=" + rejected.get()
                + ", worker-total-ms=" + workerNs.get() / 1_000_000L
                + ", discarded=" + discarded.get() + ", failed=" + failed.get();
    }
    public static synchronized void stop() {
        if (executor != null) {
            for (var result : results) result.cancel(false);
            results.clear();
            executor.shutdownNow();
            // Computations check interruption; no world references or server callback queue survive stop.
            executor = null;
        }
    }
}
