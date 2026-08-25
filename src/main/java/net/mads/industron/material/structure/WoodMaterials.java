package net.mads.industron.material.structure;

import java.util.List;

public final class WoodMaterials {
    /** Test material for the first structure-material pass. */
    public static final WoodMaterial TEST_LOG = wood(
            "test",
            "Test",
            0x8A623D,
            WoodModel.SPRUCE
    );

    public static final List<WoodMaterial> ALL = List.of(TEST_LOG);

    private WoodMaterials() {
    }

    public static WoodMaterial wood(String id, String displayName, int color, WoodModel model) {
        return new WoodMaterial(id, displayName, color, model);
    }
}
