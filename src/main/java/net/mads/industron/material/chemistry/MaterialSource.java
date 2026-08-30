package net.mads.industron.material.chemistry;

import java.util.Map;
import java.util.Objects;

public record MaterialSource(MaterialSourceType type, String id, Map<String, String> metadata) {
    public MaterialSource {
        type = Objects.requireNonNull(type, "type");
        id = Objects.requireNonNull(id, "id").trim().toLowerCase(java.util.Locale.ROOT);
        if (id.isEmpty()) throw new IllegalArgumentException("source id cannot be blank");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public MaterialSource(MaterialSourceType type, String id) {
        this(type, id, Map.of());
    }
}
