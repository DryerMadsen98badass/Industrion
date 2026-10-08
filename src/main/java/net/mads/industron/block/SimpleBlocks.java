package net.mads.industron.block;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.defenitions.OrganicMaterials;
import net.mads.industron.recipe.recipes.assembly.Tool;

import java.util.List;

public final class SimpleBlocks {
    /**
     * Primitive-pyrolysis charcoal storage block. The block intentionally reuses the vanilla
     * Coal Block texture and represents exactly nine Charcoal material units through .contains(...).
     */
    public static final SimpleBlockDefinition CHARCOAL_BLOCK = new SimpleBlockDefinition(
            "charcoal_block",
            "Charcoal Block",
            "minecraft:block/coal_block",
            0xFFFFFF
    )
            .breakingTier(MachineTier.ULV)
            .contains(OrganicMaterials.CHARCOAL, 9)
            .strength(0.5F)
            .mineableWith(Tool.PICKAXE);

    public static final List<SimpleBlockDefinition> ALL = List.of(
            CHARCOAL_BLOCK
    );
    public static final List<ActiveBlockDefinition> ACTIVE = List.of();

    private SimpleBlocks() {
    }
}
