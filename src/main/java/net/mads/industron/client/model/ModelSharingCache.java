package net.mads.industron.client.model;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/** One baking event only: never retain old models across resource reloads. */
public final class ModelSharingCache<T> {
    private final Map<Object, IdentityHashMap<T, T>> groups = new HashMap<>();
    private int requests;
    private int created;

    public T wrap(Object renderingKey, T original, Function<T, T> factory) {
        requests++;
        IdentityHashMap<T, T> models = groups.computeIfAbsent(renderingKey, key -> new IdentityHashMap<>());
        T existing = models.get(original);
        if (existing != null) return existing;
        T result = factory.apply(original);
        models.put(original, result);
        created++;
        return result;
    }

    public int requests() { return requests; }
    public int created() { return created; }
}
