package net.mads.industron.machine.foundry;

import net.mads.industron.material.*;
import net.mads.industron.machine.foundry.casting.CastingDefinitions;
import net.mads.industron.machine.foundry.casting.TerracottaMoldDefinitions;
import net.mads.industron.material.recipes.MaterialRecipeHelper;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;

public final class CastingBlockEntity extends BlockEntity {
    private ItemStack mold = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    /** Up to four matching raw ceramic items packed around a terracotta positive while a mold is formed. */
    private ItemStack formingClay = ItemStack.EMPTY;
    private int progress;
    private int requiredTicks;
    private boolean enabled;
    private int flowTick;
    private FluidStack pouring = FluidStack.EMPTY;
    private final FluidTank tank = new FluidTank(1) {
        @Override public boolean isFluidValid(FluidStack fluid) { return accepts(fluid); }
        @Override public int fill(FluidStack fluid, FluidAction action) {
            if (!server() || !accepts(fluid)) return 0;
            int accepted = Math.min(fluid.getAmount(), getCapacity() - getFluidAmount());
            if (accepted <= 0) return 0;
            if (action.execute()) {
                int temperature = Math.max(FoundryMetallurgy.temperature(fluid),
                        getFluid().isEmpty() ? 0 : FoundryMetallurgy.temperature(getFluid()));
                FluidStack next = fluid.copyWithAmount(getFluidAmount() + accepted);
                next.set(FoundryComponents.TEMPERATURE.get(), temperature);
                setFluid(next);
                progress = 0;
                requiredTicks = CastingTiming.duration(getCapacity());
                sync();
            }
            return accepted;
        }
        @Override protected void onContentsChanged() { progress = 0; sync(); }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            return server() && progress == 0 ? super.drain(maxDrain, action) : FluidStack.EMPTY;
        }
        @Override public FluidStack drain(FluidStack fluid, FluidAction action) {
            return server() && progress == 0 ? super.drain(fluid, action) : FluidStack.EMPTY;
        }
    };

    private final IItemHandler items = new IItemHandler() {
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) { return (slot == 0 ? mold : slot == 1 ? output : ItemStack.EMPTY).copy(); }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0
                    && stack.getItem() instanceof CastingMoldItem moldItem
                    && moldItem.hasCastingRecipe();
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!server() || !isItemValid(slot, stack) || !mold.isEmpty() || !output.isEmpty() || !tank.isEmpty()) return stack;
            if (!simulate) {
                mold = stack.copyWithCount(1);
                updateCapacity();
                sync();
            }
            return stack.copyWithCount(stack.getCount() - 1);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!server() || amount < 1 || slot < 0 || slot > 1 || (slot == 0 && (!tank.isEmpty() || progress > 0 || !output.isEmpty()))) return ItemStack.EMPTY;
            ItemStack result = getStackInSlot(slot);
            if (!simulate && !result.isEmpty()) {
                if (slot == 0) { mold = ItemStack.EMPTY; updateCapacity(); }
                else output = ItemStack.EMPTY;
                sync();
            }
            return result;
        }
    };
    private final IItemHandler topItems = new SidedItems(Direction.UP);
    private final IItemHandler bottomItems = new SidedItems(Direction.DOWN);
    private final IItemHandler sideItems = new SidedItems(null);

    public CastingBlockEntity(BlockPos pos, BlockState state) { super(CastingRegistry.ENTITY.get(), pos, state); }
    private boolean server() { return level != null && !level.isClientSide() && !isRemoved(); }
    public CastingBlock block() { return (CastingBlock) getBlockState().getBlock(); }
    public ItemStack mold() { return mold.copy(); }
    public ItemStack formingClay() { return formingClay.copy(); }
    public ItemStack output() { return output.copy(); }
    public FluidStack fluid() { return tank.getFluid().copy(); }
    public FluidStack pouring() { return pouring.copy(); }
    public float fillFraction() { return (float) tank.getFluidAmount() / Math.max(1, tank.getCapacity()); }
    public IFluidHandler fluidCapability() { return block().faucet() ? null : tank; }
    public IItemHandler itemCapability(Direction side) {
        if (block().faucet()) return null;
        if (side == Direction.UP) return topItems;
        if (side == Direction.DOWN) return bottomItems;
        return sideItems;
    }
    private CastingDefinitions.Form form() {
        return mold.getItem() instanceof CastingMoldItem item ? item.form() : null;
    }
    private void updateCapacity() { tank.setCapacity(form() == null ? 1 : form().millibuckets()); }

    private boolean accepts(FluidStack fluid) {
        if (block().faucet() || fluid.isEmpty() || !output.isEmpty() || !formingClay.isEmpty() || form() == null
                || FoundryMetallurgy.unidentified(fluid)) return false;
        var material = FoundryMetallurgy.material(fluid);
        if (!(mold.getItem() instanceof CastingMoldItem moldItem)) return false;
        if (material == null || !material.has(form().hot())) return false;
        int temperature = FoundryMetallurgy.temperature(fluid);
        if (temperature < material.castTemperature() || temperature > block().clay().properties().maxOperatingTemperature()
                || temperature > moldItem.clay().properties().maxOperatingTemperature()) return false;
        return tank.isEmpty() || tank.getFluid().is(fluid.getFluid());
    }

    public void interact(Player player, InteractionHand hand) {
        if (!server()) return;
        if (block().faucet()) {
            // A click starts one pour; clicking during a pour never toggles it off
            // or queues another cast while the current result is still present.
            if (!enabled && level.getBlockEntity(worldPosition.below()) instanceof CastingBlockEntity caster
                    && !caster.block().faucet() && caster.form() != null && caster.output.isEmpty()
                    && caster.formingClay.isEmpty() && caster.progress == 0
                    && caster.tank.getFluidAmount() < caster.tank.getCapacity()) {
                enabled = true;
                flowTick = 0;
                sync();
            }
            return;
        }

        ItemStack held = player.getItemInHand(hand);

        // Mold-making is a separate Caster mode. A terracotta positive selects the shape;
        // exactly four matching clay items select the ceramic material.
        TerracottaMoldDefinitions.Definition terracotta = TerracottaMoldDefinitions.find(mold);
        if (terracotta != null) {
            interactMoldForming(player, hand, held, terracotta);
            return;
        }

        if (!output.isEmpty()) {
            giveToPlayer(player, items.extractItem(1, 1, false));
            return;
        }
        if (!tank.isEmpty() || progress > 0 || !formingClay.isEmpty()) return;

        if (!held.isEmpty() && FluidUtil.interactWithFluidHandler(player, hand, tank)) return;

        if (held.isEmpty()) {
            giveToPlayer(player, items.extractItem(0, 1, false));
            return;
        }

        TerracottaMoldDefinitions.Definition insertedTerracotta = TerracottaMoldDefinitions.find(held);
        if (insertedTerracotta != null && mold.isEmpty()) {
            mold = held.copyWithCount(1);
            player.setItemInHand(hand, held.copyWithCount(Math.max(0, held.getCount() - 1)));
            updateCapacity();
            sync();
            return;
        }

        ItemStack remainder = items.insertItem(0, held, false);
        // Creative insertion still consumes one physical mold, preventing retrieval duplicates.
        player.setItemInHand(hand, remainder);
    }

    private void interactMoldForming(
            Player player,
            InteractionHand hand,
            ItemStack held,
            TerracottaMoldDefinitions.Definition terracotta
    ) {
        if (!tank.isEmpty() || progress > 0 || !output.isEmpty()) return;

        if (!held.isEmpty()) {
            IndustrialMaterial formingMaterial = CastingRegistry.moldMaterial(held);
            if (formingMaterial == null) return;

            IndustrialMaterial storedMaterial = CastingRegistry.moldMaterial(formingClay);
            if (!formingClay.isEmpty() && storedMaterial != formingMaterial) return;
            if (formingClay.getCount() >= ClayMaterialRules.MOLD_CLAY_COUNT) return;

            if (formingClay.isEmpty()) formingClay = held.copyWithCount(1);
            else formingClay.grow(1);
            player.setItemInHand(hand, held.copyWithCount(Math.max(0, held.getCount() - 1)));
            sync();
            return;
        }

        // Empty-hand interaction before all four clay items are present cancels cleanly.
        if (formingClay.getCount() < ClayMaterialRules.MOLD_CLAY_COUNT) {
            ItemStack returnedTerracotta = mold.copy();
            ItemStack returnedClay = formingClay.copy();
            mold = ItemStack.EMPTY;
            formingClay = ItemStack.EMPTY;
            updateCapacity();
            giveToPlayer(player, returnedTerracotta);
            giveToPlayer(player, returnedClay);
            sync();
            return;
        }

        IndustrialMaterial ceramic = CastingRegistry.moldMaterial(formingClay);
        if (ceramic == null) return;
        ItemStack unfired = CastingRegistry.unfiredMold(ceramic, terracotta.moldPart());
        if (unfired.isEmpty()) return;

        // First completed click removes/returns the reusable terracotta positive. The newly
        // formed wet ceramic mold stays in the Caster output until the next click/extraction.
        ItemStack returnedTerracotta = mold.copy();
        mold = ItemStack.EMPTY;
        formingClay = ItemStack.EMPTY;
        output = unfired;
        updateCapacity();
        giveToPlayer(player, returnedTerracotta);
        sync();
    }

    private void giveToPlayer(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.addItem(stack)) player.drop(stack, false);
    }

    public void tick() {
        if (!server()) return;
        if (block().faucet()) { tickFaucet(); return; }
        if (tank.isEmpty() || tank.getFluidAmount() < tank.getCapacity() || !output.isEmpty() || form() == null) return;
        if (!accepts(tank.getFluid())) return;
        progress++;
        setChanged();
        if (progress < requiredTicks) return;
        var material = FoundryMetallurgy.material(tank.getFluid());
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(MaterialRecipeHelper.itemId(material, form().hot())));
        if (item == Items.AIR) return;
        output = new ItemStack(item);
        tank.setFluid(FluidStack.EMPTY);
        progress = 0;
        sync();
    }

    private BlockPos faucetController;
    private long nextControllerSearch;

    private IFluidHandler faucetSource(BlockPos wall, Direction facing) {
        IFluidHandler foundry = foundryOutputSource(wall);
        if (foundry != null) return foundry;
        IFluidHandler direct = level.getCapability(Capabilities.FluidHandler.BLOCK, wall, facing);
        if (direct == null) direct = level.getCapability(Capabilities.FluidHandler.BLOCK, wall, facing.getOpposite());
        if (direct != null) return direct;
        if (level.getBlockEntity(wall) instanceof FoundryBlockEntity controller
                && controller.isController() && controller.isFormed()) return controller.fluidStorage();
        if (FoundryBrickResolver.resolve(level.getBlockState(wall)).isEmpty()
                && !(level.getBlockState(wall).getBlock() instanceof FoundryPartBlock)) return null;
        if (faucetController != null && level.hasChunkAt(faucetController)
                && level.getBlockEntity(faucetController) instanceof FoundryBlockEntity controller
                && controller.isController() && controller.isFormed()
                && (controller.formedBrickPositions().contains(wall)
                || controller.abilityPositions(MultiblockAbility.FLUID_OUTPUT).contains(wall))) return controller.fluidStorage();
        faucetController = null;
        if (level.getGameTime() < nextControllerSearch) return null;
        nextControllerSearch = level.getGameTime() + 20;
        // Inspect loaded block entities only. Controller height can differ from the faucet's height.
        for (int cx = (wall.getX() - 8) >> 4; cx <= (wall.getX() + 8) >> 4; cx++) {
            for (int cz = (wall.getZ() - 8) >> 4; cz <= (wall.getZ() + 8) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                for (var entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (entity instanceof FoundryBlockEntity controller && controller.isController()
                            && controller.isFormed() && (controller.formedBrickPositions().contains(wall)
                            || controller.abilityPositions(MultiblockAbility.FLUID_OUTPUT).contains(wall))) {
                        faucetController = controller.getBlockPos();
                        return controller.fluidStorage();
                    }
                }
            }
        }
        return null;
    }

    private IFluidHandler foundryOutputSource(BlockPos wall) {
        if (!(level.getBlockState(wall).getBlock() instanceof FoundryPartBlock partBlock)) return null;
        if (level.getBlockEntity(wall) instanceof FoundryBlockEntity foundry) {
            if (foundry.isController() && foundry.isFormed()) return foundry.fluidStorage();
            if (partBlock.partType() == FoundryPartType.FLUID_OUTPUT_HATCH) {
                BlockPos controllerPos = foundry.controllerPos();
                if (controllerPos != null && level.hasChunkAt(controllerPos)
                        && level.getBlockEntity(controllerPos) instanceof FoundryBlockEntity controller
                        && controller.isController() && controller.isFormed()
                        && controller.abilityPositions(MultiblockAbility.FLUID_OUTPUT).contains(wall)) {
                    faucetController = controller.getBlockPos();
                    return controller.fluidStorage();
                }
            }
        }
        return null;
    }

    private void tickFaucet() {
        FluidStack next = FluidStack.EMPTY;
        int transferAmount = CastingTiming.transfer(flowTick);
        flowTick = (flowTick + 1) % 20;
        if (enabled || level.hasNeighborSignal(worldPosition)) {
            Direction facing = getBlockState().getValue(CastingBlock.FACING);
            BlockPos sourcePos = worldPosition.relative(facing.getOpposite());
            BlockPos targetPos = worldPosition.below();
            if (level.hasChunkAt(sourcePos) && level.hasChunkAt(targetPos)) {
                IFluidHandler source = faucetSource(sourcePos, facing);
                // The faucet transfers only to our transactional caster, never an arbitrary mutable handler.
                if (source != null && level.getBlockEntity(targetPos) instanceof CastingBlockEntity caster && !caster.block().faucet()) {
                    for (int i = 0; i < source.getTanks(); i++) {
                        FluidStack layer = source.getFluidInTank(i).copy();
                        if (layer.isEmpty() || FoundryMetallurgy.temperature(layer) > block().clay().properties().maxOperatingTemperature()) continue;
                        layer.setAmount(Math.min(transferAmount, layer.getAmount()));
                        FluidStack offered = source.drain(layer, IFluidHandler.FluidAction.SIMULATE);
                        int accepted = caster.tank.fill(offered, IFluidHandler.FluidAction.SIMULATE);
                        if (accepted <= 0) continue;
                        offered.setAmount(accepted);
                        FluidStack drained = source.drain(offered, IFluidHandler.FluidAction.EXECUTE);
                        caster.tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                        next = drained;
                        // Finish the manual cycle immediately, before the caster can
                        // solidify and have its result extracted by a hopper.
                        if (enabled && caster.tank.getFluidAmount() >= caster.tank.getCapacity()) {
                            enabled = false;
                            setChanged();
                        }
                        break;
                    }
                }
            }
        }
        // An empty source or a blocked/missing receiver ends this attempt too.
        // Redstone retains its continuous automation behavior independently.
        if (enabled && next.isEmpty()) {
            enabled = false;
            setChanged();
        }
        if (pouring.isEmpty() != next.isEmpty() || (!next.isEmpty() && !FluidStack.isSameFluidSameComponents(pouring, next))) {
            pouring = next.copy(); sync();
        }
    }

    public void dropContents() {
        if (!server()) return;
        if (!mold.isEmpty()) Block.popResource(level, worldPosition, mold.copy());
        if (!formingClay.isEmpty()) Block.popResource(level, worldPosition, formingClay.copy());
        if (!output.isEmpty()) Block.popResource(level, worldPosition, output.copy());
        mold = ItemStack.EMPTY;
        formingClay = ItemStack.EMPTY;
        output = ItemStack.EMPTY;
        tank.setFluid(FluidStack.EMPTY);
        pouring = FluidStack.EMPTY;
        progress = 0;
        enabled = false;
        setChanged();
    }
    private void sync() {
        setChanged();
        if (server()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Mold", mold.saveOptional(registries));
        tag.put("FormingClay", formingClay.saveOptional(registries));
        tag.put("Output", output.saveOptional(registries));
        tag.put("Fluid", tank.getFluid().saveOptional(registries));
        tag.put("Pouring", pouring.saveOptional(registries));
        tag.putInt("Progress", progress);
        tag.putInt("Duration", requiredTicks);
        tag.putBoolean("Enabled", enabled);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mold = ItemStack.parseOptional(registries, tag.getCompound("Mold"));
        formingClay = ItemStack.parseOptional(registries, tag.getCompound("FormingClay"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        updateCapacity();
        tank.setFluid(FluidStack.parseOptional(registries, tag.getCompound("Fluid")));
        pouring = FluidStack.parseOptional(registries, tag.getCompound("Pouring"));
        progress = Math.max(0, tag.getInt("Progress"));
        requiredTicks = CastingTiming.duration(tank.getCapacity());
        enabled = tag.getBoolean("Enabled");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag(); saveAdditional(tag, registries); return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }
    @Override public void onDataPacket(net.minecraft.network.Connection connection,
            ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }

    private final class SidedItems implements IItemHandler {
        private final Direction side;
        private SidedItems(Direction side) { this.side = side; }
        @Override public int getSlots() { return items.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return canInsert(slot) && items.isItemValid(slot, stack);
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return canInsert(slot) ? items.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return canExtract(slot) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        private boolean canInsert(int slot) {
            return side != Direction.DOWN && slot == 0;
        }
        private boolean canExtract(int slot) {
            if (side == Direction.UP) return false;
            if (slot == 1) return true;
            // Even handlers that inspect the mold slot first must take the
            // finished product before they can remove the mold.
            return slot == 0 && output.isEmpty() && formingClay.isEmpty() && tank.isEmpty() && progress == 0;
        }
    }
}
