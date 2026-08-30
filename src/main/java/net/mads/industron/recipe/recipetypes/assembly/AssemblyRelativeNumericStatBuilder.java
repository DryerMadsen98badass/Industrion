package net.mads.industron.recipe.recipetypes.assembly;

import java.util.Objects;
import java.util.function.Consumer;

/** Fluent relative stat selector used after ComponentDefinition inputAny(...).stat(Stats.X). */
public final class AssemblyRelativeNumericStatBuilder<P> {
    private final P parent;
    private final AssemblyCapability capability;
    private final Consumer<AssemblyRelativeRequirement> sink;

    public AssemblyRelativeNumericStatBuilder(
            P parent,
            AssemblyCapability capability,
            Consumer<AssemblyRelativeRequirement> sink
    ) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.capability = Objects.requireNonNull(capability, "capability");
        this.sink = Objects.requireNonNull(sink, "sink");
        if (capability.isRangeStat()) {
            throw new IllegalArgumentException(
                    capability.displayName() + " is a range capability; inputAny relative stats only support atLeastParent/atMostParent"
            );
        }
    }

    public P atLeastParent() {
        sink.accept(AssemblyRelativeRequirement.atLeastParent(capability));
        return parent;
    }

    public P atMostParent() {
        sink.accept(AssemblyRelativeRequirement.atMostParent(capability));
        return parent;
    }
}
