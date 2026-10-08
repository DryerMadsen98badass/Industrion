package net.mads.industron.material.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.OptionalInt;

/** Vanilla chiseled-bookshelf interaction backed by a custom generated-wood BlockEntityType. */
public final class StructureWoodChiseledBookshelfBlock extends ChiseledBookShelfBlock {
    private final WoodMaterial material;

    public StructureWoodChiseledBookshelfBlock(WoodMaterial material, BlockBehaviour.Properties properties) {
        super(properties);
        this.material = material;
    }

    public WoodMaterial material() {
        return material;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof StructureWoodChiseledBookshelfBlockEntity shelf)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!stack.is(ItemTags.BOOKSHELF_BOOKS)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        OptionalInt slot = getHitSlot(hit, state);
        if (slot.isEmpty()) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (state.getValue(SLOT_OCCUPIED_PROPERTIES.get(slot.getAsInt()))) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        addBook(level, pos, player, shelf, stack, slot.getAsInt());
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof StructureWoodChiseledBookshelfBlockEntity shelf)) {
            return InteractionResult.PASS;
        }
        OptionalInt slot = getHitSlot(hit, state);
        if (slot.isEmpty()) return InteractionResult.PASS;
        if (!state.getValue(SLOT_OCCUPIED_PROPERTIES.get(slot.getAsInt()))) {
            return InteractionResult.CONSUME;
        }
        removeBook(level, pos, player, shelf, slot.getAsInt());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private OptionalInt getHitSlot(BlockHitResult hit, BlockState state) {
        return getRelativeHitCoordinatesForBlockFace(hit, state.getValue(HorizontalDirectionalBlock.FACING))
                .map(relative -> {
                    int row = relative.y >= 0.5F ? 0 : 1;
                    int column = relative.x < 0.375F ? 0 : relative.x < 0.6875F ? 1 : 2;
                    return OptionalInt.of(column + row * 3);
                })
                .orElseGet(OptionalInt::empty);
    }

    private static Optional<Vec2> getRelativeHitCoordinatesForBlockFace(BlockHitResult hit, Direction facing) {
        Direction direction = hit.getDirection();
        if (facing != direction) return Optional.empty();
        BlockPos outside = hit.getBlockPos().relative(direction);
        Vec3 relative = hit.getLocation().subtract(outside.getX(), outside.getY(), outside.getZ());
        double x = relative.x();
        double y = relative.y();
        double z = relative.z();
        return switch (direction) {
            case NORTH -> Optional.of(new Vec2((float) (1.0D - x), (float) y));
            case SOUTH -> Optional.of(new Vec2((float) x, (float) y));
            case WEST -> Optional.of(new Vec2((float) z, (float) y));
            case EAST -> Optional.of(new Vec2((float) (1.0D - z), (float) y));
            case DOWN, UP -> Optional.empty();
        };
    }

    private static void addBook(
            Level level,
            BlockPos pos,
            Player player,
            StructureWoodChiseledBookshelfBlockEntity shelf,
            ItemStack stack,
            int slot
    ) {
        if (level.isClientSide) return;
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        SoundEvent sound = stack.is(Items.ENCHANTED_BOOK)
                ? SoundEvents.CHISELED_BOOKSHELF_INSERT_ENCHANTED
                : SoundEvents.CHISELED_BOOKSHELF_INSERT;
        shelf.setItem(slot, stack.consumeAndReturn(1, player));
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static void removeBook(
            Level level,
            BlockPos pos,
            Player player,
            StructureWoodChiseledBookshelfBlockEntity shelf,
            int slot
    ) {
        if (level.isClientSide) return;
        ItemStack stack = shelf.removeItem(slot, 1);
        SoundEvent sound = stack.is(Items.ENCHANTED_BOOK)
                ? SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED
                : SoundEvents.CHISELED_BOOKSHELF_PICKUP;
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StructureWoodChiseledBookshelfBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!oldState.is(newState.getBlock())) {
            boolean dropped = false;
            if (level.getBlockEntity(pos) instanceof StructureWoodChiseledBookshelfBlockEntity shelf && !shelf.isEmpty()) {
                for (int i = 0; i < shelf.getContainerSize(); i++) {
                    ItemStack stack = shelf.getItem(i);
                    if (!stack.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
                shelf.clearContent();
                dropped = true;
            }
            super.onRemove(oldState, level, pos, newState, movedByPiston);
            if (dropped) level.updateNeighbourForOutputSignal(pos, this);
        }
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide()) return 0;
        return level.getBlockEntity(pos) instanceof StructureWoodChiseledBookshelfBlockEntity shelf
                ? shelf.getLastInteractedSlot() + 1
                : 0;
    }
}
