package net.mads.industron.material.structure;

import java.util.List;

public final class StoneMaterials {
    /** Test material for the first structure-material pass. */
    public static final StoneMaterial TEST_STONE = stone(
            "test_stone",
            "Test Stone",
            0x7B8490,
            StoneModel.DIORITE
    );

    public static final List<StoneMaterial> ALL = List.of(TEST_STONE);

    private StoneMaterials() {
    }

    public static StoneMaterial stone(String id, String displayName, int color, StoneModel model) {
        return new StoneMaterial(id, displayName, color, model);
    }
}
