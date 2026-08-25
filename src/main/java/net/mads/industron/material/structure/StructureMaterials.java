package net.mads.industron.material.structure;

import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;

import java.util.ArrayList;
import java.util.List;

public final class StructureMaterials {
    public static final List<StructureMaterial> ALL;

    static {
        List<StructureMaterial> all = new ArrayList<>();
        all.addAll(WoodMaterials.ALL);
        all.addAll(StoneMaterials.ALL);
        all.addAll(MetalMaterials.ALL);
        all.addAll(GemMaterials.ALL);
        ALL = List.copyOf(all);
    }

    private StructureMaterials() {
    }

    public static MaterialComponent component(IndustrialSubstance substance, int amount) {
        return new MaterialComponent(substance, amount);
    }
}
