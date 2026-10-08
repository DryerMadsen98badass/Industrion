package net.mads.industron.recipe.recipetypes.assembly.workbench;

import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Manual-only assembly storage. No item capability or Container is exposed. */
public final class AssemblyWorkbenchBlockEntity extends BlockEntity {
    private ItemStack displayedStack = ItemStack.EMPTY;
    private boolean resultReady;
    private CompoundTag assemblyData = new CompoundTag();

    public AssemblyWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ASSEMBLY_WORKBENCH.get(), pos, state);
    }

    public ItemStack displayedStack() { return displayedStack; }
    public boolean hasDisplayedStack() { return !displayedStack.isEmpty(); }
    public boolean resultReady() { return resultReady; }

    public void setDisplayedStack(ItemStack stack, boolean result) {
        displayedStack = stack.copy();
        resultReady = result && !displayedStack.isEmpty();
        contentChanged();
    }

    public ItemStack takeDisplayedStack() {
        ItemStack result = displayedStack;
        displayedStack = ItemStack.EMPTY;
        resultReady = false;
        contentChanged();
        return result;
    }

    public void clearDisplayedStack() {
        if (displayedStack.isEmpty() && !resultReady) return;
        displayedStack = ItemStack.EMPTY;
        resultReady = false;
        contentChanged();
    }

    public boolean hasAssemblyData() { return !assemblyData.isEmpty(); }
    public CompoundTag assemblyData() { return assemblyData.copy(); }

    public void setAssemblyData(CompoundTag data) {
        assemblyData = data.copy();
        setChanged();
    }

    public void clearAssemblyData() {
        if (assemblyData.isEmpty()) return;
        assemblyData = new CompoundTag();
        setChanged();
    }

    private void contentChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!displayedStack.isEmpty()) tag.put("DisplayedStack", displayedStack.saveOptional(registries));
        tag.putBoolean("ResultReady", resultReady);
        if (!assemblyData.isEmpty()) tag.put("Assembly", assemblyData.copy());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        displayedStack = tag.contains("DisplayedStack")
                ? ItemStack.parseOptional(registries, tag.getCompound("DisplayedStack"))
                : ItemStack.EMPTY;
        resultReady = tag.getBoolean("ResultReady") && !displayedStack.isEmpty();
        assemblyData = tag.contains("Assembly") ? tag.getCompound("Assembly").copy() : new CompoundTag();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (!displayedStack.isEmpty()) tag.put("DisplayedStack", displayedStack.saveOptional(registries));
        tag.putBoolean("ResultReady", resultReady);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}
