package net.mads.industron.block;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public enum MiningTool {

    PICKAXE(BlockTags.MINEABLE_WITH_PICKAXE),
    AXE(BlockTags.MINEABLE_WITH_AXE),
    SHOVEL(BlockTags.MINEABLE_WITH_SHOVEL),
    HOE(BlockTags.MINEABLE_WITH_HOE);

    private final TagKey<Block> tag;

    MiningTool(TagKey<Block> tag) {
        this.tag = tag;
    }

    public TagKey<Block> tag() {
        return tag;
    }
}
