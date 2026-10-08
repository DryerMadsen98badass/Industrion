package net.mads.industron.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Owning-thread cache. Keys use identity; a miss evicts only the least recently used entry. */
public final class BoundedIdentityCache<K, V> {
    private final Map<IdentityKey<K>, V> values;
    private final IdentityKey<K> lookup = new IdentityKey<>(null);

    public BoundedIdentityCache(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Cache capacity must be positive");
        values = new LinkedHashMap<>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<IdentityKey<K>, V> eldest) {
                return size() > capacity;
            }
        };
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> factory) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(factory);
        // This lookup key is never inserted. Cache hits need no temporary key allocation.
        lookup.value = key;
        V value;
        try { value = values.get(lookup); }
        finally { lookup.value = null; }
        if (value == null) {
            value = factory.apply(key);
            if (value != null) values.put(new IdentityKey<>(key), value);
        }
        return value;
    }

    public int size() { return values.size(); }
    public void clear() { values.clear(); }

    private static final class IdentityKey<K> {
        private K value;
        private IdentityKey(K value) { this.value = value; }
        @Override public int hashCode() { return System.identityHashCode(value); }
        @Override public boolean equals(Object other) {
            return other instanceof IdentityKey<?> key && value == key.value;
        }
    }
}
