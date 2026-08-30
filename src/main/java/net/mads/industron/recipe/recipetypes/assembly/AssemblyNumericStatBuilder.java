package net.mads.industron.recipe.recipetypes.assembly;

import java.util.Objects;
import java.util.function.Consumer;

/** Fluent numeric stat selector used after .stat(Stats.X). */
public final class AssemblyNumericStatBuilder<P> {
    private final P parent;
    private final AssemblyCapability capability;
    private final Consumer<AssemblyRequirement> sink;

    public AssemblyNumericStatBuilder(P parent, AssemblyCapability capability, Consumer<AssemblyRequirement> sink) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.capability = Objects.requireNonNull(capability, "capability");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    public P atLeast(double value) {
        sink.accept(capability.atLeast(value));
        return parent;
    }

    public P atMost(double value) {
        sink.accept(capability.atMost(value));
        return parent;
    }

    public P exactly(double value) {
        sink.accept(capability.exactly(value));
        return parent;
    }

    public P range(double min, double max) {
        sink.accept(capability.range(min, max));
        return parent;
    }

    public P covers(double min, double max) {
        sink.accept(capability.covers(min, max));
        return parent;
    }
}
