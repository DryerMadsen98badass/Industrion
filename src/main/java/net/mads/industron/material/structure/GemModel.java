package net.mads.industron.material.structure;

/** Quartz is the universal geometry/texture family for decorative gem structures. */
public enum GemModel implements StructureModel {
    QUARTZ;

    @Override
    public String category() {
        return "gem";
    }

    @Override
    public String id() {
        return "quartz";
    }
}
