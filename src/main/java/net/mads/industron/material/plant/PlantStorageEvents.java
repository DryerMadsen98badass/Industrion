package net.mads.industron.material.plant;

import net.mads.industron.Industron;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid=Industron.MOD_ID)
public final class PlantStorageEvents {
    private PlantStorageEvents() {}
    @SubscribeEvent public static void place(PlayerInteractEvent.RightClickBlock event) {
        if(event.getHand()!=InteractionHand.MAIN_HAND||event.isCanceled()
            ||net.mads.industron.control.ControlKeyState.isHeld(event.getEntity()))return;
        var player=event.getEntity();var level=event.getLevel();ItemStack held=event.getItemStack();
        var material=PlantStorage.forItem(BuiltInRegistries.ITEM.getKey(held.getItem()));
        if(material==null||held.isEmpty())return;
        // World-only storage cannot retain arbitrary names or other custom item components.
        if(!ItemStack.isSameItemSameComponents(held,new ItemStack(held.getItem())))return;
        BlockPos pos=event.getPos();BlockState state=level.getBlockState(pos);boolean adding=state.getBlock() instanceof PlantStorageBlock;
        if(adding) {
            var pile=(PlantStorageBlock)state.getBlock();
            if(!pile.material().storageItem().equals(material.storageItem()))return;
            if(state.getValue(PlantStorageBlock.COUNT)>=PlantStorage.CAPACITY) {
                if(!player.isShiftKeyDown()||event.getFace()!=Direction.UP)return;
                adding=false;
            }
        }
        if(!adding) {
            // Sneaking starts storage without stealing normal planting/placement interactions.
            if(!player.isShiftKeyDown()||event.getFace()!=Direction.UP)return;
            pos=pos.above();state=level.getBlockState(pos);
            if(!state.isAir()||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))return;
        }
        if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,event.getFace(),held))return;
        BlockState next=adding?state.setValue(PlantStorageBlock.COUNT,state.getValue(PlantStorageBlock.COUNT)+1)
            :BlockRegistry.PLANT_STORAGE_BLOCKS.get(material.id()).get().defaultBlockState();
        if(!level.isUnobstructed(null,next.getCollisionShape(level,pos).move(pos.getX(),pos.getY(),pos.getZ())))return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        if(!level.isClientSide()) {
            var snapshot=net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
            if(!level.setBlock(pos,next,3))return;
            // Placement hooks observe the new stack state, just as with ordinary block placement.
            if(net.neoforged.neoforge.event.EventHooks.onBlockPlace(player,snapshot,event.getFace())) {
                level.setBlock(pos,state,3);return;
            }
            if(!player.getAbilities().instabuild)held.shrink(1);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(material.displayName()+" Stack: "+next.getValue(PlantStorageBlock.COUNT)+"/64"),true);
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.GRASS_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,.8F,.9F);
        }
    }
}
