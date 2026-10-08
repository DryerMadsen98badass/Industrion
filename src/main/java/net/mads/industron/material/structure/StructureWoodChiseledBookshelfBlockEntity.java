package net.mads.industron.material.structure;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.function.Predicate;

/** Six-slot chiseled bookshelf storage backed by Industron's generated wood block entity type. */
public final class StructureWoodChiseledBookshelfBlockEntity extends BlockEntity implements Container {
    public static final int MAX_BOOKS_IN_STORAGE = 6;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final NonNullList<ItemStack> items = NonNullList.withSize(MAX_BOOKS_IN_STORAGE, ItemStack.EMPTY);
    private int lastInteractedSlot = -1;

    public StructureWoodChiseledBookshelfBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.STRUCTURE_WOOD_CHISELED_BOOKSHELF.get(), pos, state);
    }

    private void updateState(int slot) {
        if (slot < 0 || slot >= MAX_BOOKS_IN_STORAGE) {
            LOGGER.error("Expected slot 0-5, got {}", slot);
            return;
        }
        lastInteractedSlot = slot;
        BlockState state = getBlockState();
        for (int i = 0; i < ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); i++) {
            boolean occupied = !getItem(i).isEmpty();
            BooleanProperty property = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i);
            state = state.setValue(property, occupied);
        }
        Objects.requireNonNull(level).setBlock(worldPosition, state, 3);
        level.gameEvent(GameEvent.BLOCK_CHANGE, worldPosition, GameEvent.Context.of(state));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        lastInteractedSlot = tag.getInt("last_interacted_slot");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, true, registries);
        tag.putInt("last_interacted_slot", lastInteractedSlot);
    }

    public int count() {
        return (int) items.stream().filter(Predicate.not(ItemStack::isEmpty)).count();
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public int getContainerSize() {
        return MAX_BOOKS_IN_STORAGE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = Objects.requireNonNullElse(items.get(slot), ItemStack.EMPTY);
        items.set(slot, ItemStack.EMPTY);
        if (!stack.isEmpty()) updateState(slot);
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (stack.is(ItemTags.BOOKSHELF_BOOKS)) {
            items.set(slot, stack);
            updateState(slot);
        } else if (stack.isEmpty()) {
            removeItem(slot, 1);
        }
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return target.hasAnyMatching(existing -> existing.isEmpty()
                || ItemStack.isSameItemSameComponents(stack, existing)
                && existing.getCount() + stack.getCount() <= target.getMaxStackSize(existing));
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(ItemTags.BOOKSHELF_BOOKS) && getItem(slot).isEmpty() && stack.getCount() == getMaxStackSize();
    }

    public int getLastInteractedSlot() {
        return lastInteractedSlot;
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Items");
    }
}
