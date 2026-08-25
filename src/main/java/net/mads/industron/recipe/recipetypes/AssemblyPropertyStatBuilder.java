package net.mads.industron.recipe.recipetypes;

import java.util.Objects;
import java.util.function.Consumer;

/** Fluent typed stat selector used after .stat(Stats.X). */
public final class AssemblyPropertyStatBuilder<T, P> {
    private final P parent;
    private final AssemblyProperty<T> property;
    private final Consumer<AssemblyRequirement> sink;

    public AssemblyPropertyStatBuilder(P parent, AssemblyProperty<T> property, Consumer<AssemblyRequirement> sink) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.property = Objects.requireNonNull(property, "property");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    public P is(T value) {
        sink.accept(property.is(value));
        return parent;
    }
}
