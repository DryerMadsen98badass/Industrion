package net.mads.industron.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class NonPlaceableBucketItem extends BucketItem {
    public NonPlaceableBucketItem(Fluid content, Properties properties) {
        super(content, properties);
    }

    @Override
    public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult result, @Nullable ItemStack container) {
        return false;
    }
}
