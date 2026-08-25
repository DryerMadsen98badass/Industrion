package net.mads.industron.material.structure;

/** Selects the complete, role-based metal structure-set library. */
public enum MetalModel implements StructureModel {
    ALL;

    @Override
    public String category() {
        return "metal";
    }

    @Override
    public String id() {
        return "all";
    }
}
