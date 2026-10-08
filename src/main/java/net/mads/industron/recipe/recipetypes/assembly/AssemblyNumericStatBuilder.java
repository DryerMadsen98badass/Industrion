package net.mads.industron.recipe.recipetypes.assembly;

import java.util.Objects;
import java.util.function.Consumer;

/** Fluent numeric stat selector used after .stat(Stats.X). */
public final class AssemblyNumericStatBuilder<P> {
    @FunctionalInterface
    public interface CapturedSink {
        void accept(AssemblyCapability candidate, String captureRole, AssemblyCapability source, boolean atLeast);
    }

    private final P parent;
    private final AssemblyCapability capability;
    private final Consumer<AssemblyRequirement> sink;
    private final CapturedSink capturedSink;

    public AssemblyNumericStatBuilder(P parent, AssemblyCapability capability, Consumer<AssemblyRequirement> sink) {
        this(parent, capability, sink, null);
    }

    public AssemblyNumericStatBuilder(
            P parent,
            AssemblyCapability capability,
            Consumer<AssemblyRequirement> sink,
            CapturedSink capturedSink
    ) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.capability = Objects.requireNonNull(capability, "capability");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.capturedSink = capturedSink;
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

    /** Candidate stat must be at least the selected stat from a previously captured material input. */
    public P atLeastInput(String captureRole, AssemblyCapability sourceCapability) {
        if (capturedSink == null) {
            throw new IllegalStateException("This stat context does not support cross-input comparisons");
        }
        capturedSink.accept(capability, captureRole, Objects.requireNonNull(sourceCapability), true);
        return parent;
    }

    public P atMostInput(String captureRole, AssemblyCapability sourceCapability) {
        if (capturedSink == null) {
            throw new IllegalStateException("This stat context does not support cross-input comparisons");
        }
        capturedSink.accept(capability, captureRole, Objects.requireNonNull(sourceCapability), false);
        return parent;
    }
}
