package net.mads.industron.item;

import net.mads.industron.registry.FluidRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class FiredBucketItem extends BucketItem {

    public FiredBucketItem(
            Fluid content,
            Item.Properties properties
    ) {
        super(content, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        FluidRegistry.buildFiredBucketMaps();

        if (content != Fluids.EMPTY) {
            return useFilledBucket(
                    level,
                    player,
                    hand
            );
        }

        return useEmptyBucket(
                level,
                player,
                hand
        );
    }

    private InteractionResultHolder<ItemStack> useFilledBucket(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        InteractionResultHolder<ItemStack> result =
                super.use(
                        level,
                        player,
                        hand
                );

        ItemStack convertedResult =
                convertNormalBucketToFired(
                        result.getObject()
                );

        return new InteractionResultHolder<>(
                result.getResult(),
                convertedResult
        );
    }

    private InteractionResultHolder<ItemStack> useEmptyBucket(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack heldStack =
                player.getItemInHand(hand);

        BlockHitResult hitResult =
                getPlayerPOVHitResult(
                        level,
                        player,
                        ClipContext.Fluid.SOURCE_ONLY
                );

        if (hitResult.getType()
                == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(
                    heldStack
            );
        }

        if (hitResult.getType()
                != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(
                    heldStack
            );
        }

        BlockPos pos =
                hitResult.getBlockPos();

        Direction direction =
                hitResult.getDirection();

        BlockPos adjacentPos =
                pos.relative(direction);

        if (!level.mayInteract(
                player,
                pos
        ) || !player.mayUseItemAt(
                adjacentPos,
                direction,
                heldStack
        )) {
            return InteractionResultHolder.fail(
                    heldStack
            );
        }

        BlockState state =
                level.getBlockState(pos);

        if (!(state.getBlock()
                instanceof BucketPickup bucketPickup)) {
            return InteractionResultHolder.fail(
                    heldStack
            );
        }

        ItemStack normalFilledBucket =
                bucketPickup.pickupBlock(
                        player,
                        level,
                        pos,
                        state
                );

        if (normalFilledBucket.isEmpty()) {
            return InteractionResultHolder.fail(
                    heldStack
            );
        }

        ItemStack firedFilledBucket =
                convertNormalBucketToFired(
                        normalFilledBucket
                );

        if (firedFilledBucket.isEmpty()
                || firedFilledBucket.getItem()
                == normalFilledBucket.getItem()) {
            return InteractionResultHolder.fail(
                    heldStack
            );
        }

        bucketPickup
                .getPickupSound(state)
                .ifPresent(
                        sound ->
                                level.playSound(
                                        player,
                                        pos,
                                        sound,
                                        SoundSource.BLOCKS,
                                        1.0F,
                                        1.0F
                                )
                );

        level.gameEvent(
                player,
                GameEvent.FLUID_PICKUP,
                pos
        );

        player.awardStat(
                Stats.ITEM_USED.get(this)
        );

        if (player
                instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.FILLED_BUCKET.trigger(
                    serverPlayer,
                    firedFilledBucket
            );
        }

        if (player.getAbilities().instabuild) {
            return InteractionResultHolder.sidedSuccess(
                    heldStack,
                    level.isClientSide
            );
        }

        if (heldStack.getCount() == 1) {
            return InteractionResultHolder.sidedSuccess(
                    firedFilledBucket,
                    level.isClientSide
            );
        }

        heldStack.shrink(1);

        if (!player
                .getInventory()
                .add(firedFilledBucket)) {
            player.drop(
                    firedFilledBucket,
                    false
            );
        }

        return InteractionResultHolder.sidedSuccess(
                heldStack,
                level.isClientSide
        );
    }

    private static ItemStack convertNormalBucketToFired(
            ItemStack normalStack
    ) {
        if (normalStack.isEmpty()) {
            return normalStack;
        }

        Item firedBucket =
                FluidRegistry
                        .FIRED_BUCKET_BY_NORMAL_BUCKET
                        .get(normalStack.getItem());

        if (firedBucket == null) {
            return normalStack;
        }

        return new ItemStack(
                firedBucket,
                normalStack.getCount()
        );
    }
}