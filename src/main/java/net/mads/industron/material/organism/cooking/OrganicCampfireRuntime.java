package net.mads.industron.material.organism.cooking;

import net.mads.industron.Industron;
import net.mads.industron.material.organism.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Server-authoritative manual retrieval. Item custom data preserves heat through removal and saves. */
@EventBusSubscriber(modid=Industron.MOD_ID)
public final class OrganicCampfireRuntime {
    private static final String HEAT="industron_organic_heat";
    private OrganicCampfireRuntime() {}
    public static OrganismItemCatalog.Entry definition(ItemStack stack) {
        if(stack.isEmpty() || net.mads.industron.farming.FarmingCooking.uncleanFish(stack))return null;
        var entry=OrganismItemCatalog.byItem(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        return entry!=null && entry.part().cookingFamily() ? entry : null;
    }
    public static boolean containsOrganic(CampfireBlockEntity fire) {
        return fire.getItems().stream().anyMatch(s->definition(s)!=null || net.mads.industron.farming.FarmingCooking.profile(s)!=null);
    }
    public static int heat(ItemStack stack,OrganismItemCatalog.Entry entry) {
        int minimum=entry.form()==OrganicForm.BURNT?OrganicCookingRules.burntAt(entry)
                : entry.form()==OrganicForm.COOKED?OrganicCookingRules.cookedAt(entry):0;
        CustomData data=stack.get(DataComponents.CUSTOM_DATA);
        return Math.min(OrganicCookingRules.burntAt(entry),Math.max(minimum,data==null?0:data.copyTag().getInt(HEAT)));
    }
    private static void heat(ItemStack stack,int value) {
        CustomData data=stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag=data==null?new CompoundTag():data.copyTag();tag.putInt(HEAT,value);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    public static void tick(Level level,BlockPos pos,BlockState state,CampfireBlockEntity fire) {
        var access=(OrganicCampfireAccess)fire;
        int[] progress=access.industron$cookingProgress(),times=access.industron$cookingTime();
        boolean changed=false;
        for(int slot=0;slot<fire.getItems().size();slot++) {
            ItemStack stack=fire.getItems().get(slot);if(stack.isEmpty())continue;
            var profile=net.mads.industron.farming.FarmingCooking.profile(stack);
            if(profile!=null) {
                ItemStack replacement=net.mads.industron.farming.FarmingCooking.tick(stack,profile);
                if(replacement.getItem()!=stack.getItem())changed=true;
                fire.getItems().set(slot,replacement);
                progress[slot]=net.mads.industron.farming.FarmingCooking.heat(replacement,profile);times[slot]=profile.cook();continue;
            }
            var entry=definition(stack);
            if(entry==null) {
                // Other mods' and plant recipes keep vanilla behavior even in a mixed campfire.
                progress[slot]++;
                if(progress[slot]>=times[slot]) {
                    var input=new SingleRecipeInput(stack);
                    ItemStack result=level.getRecipeManager().getRecipeFor(RecipeType.CAMPFIRE_COOKING,input,level)
                            .map(r->r.value().assemble(input,level.registryAccess())).orElse(stack);
                    if(result.isItemEnabled(level.enabledFeatures())) {
                        Containers.dropItemStack(level,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,result);
                        fire.getItems().set(slot,ItemStack.EMPTY);progress[slot]=0;changed=true;
                    }
                }
                continue;
            }
            int elapsed=Math.min(heat(stack,entry)+1,OrganicCookingRules.burntAt(entry));
            OrganicForm target=OrganicCookingRules.state(entry.form(),elapsed,OrganicCookingRules.cookedAt(entry),OrganicCookingRules.burntAt(entry));
            if(target!=entry.form()) {
                var form=OrganismItemCatalog.form(entry.owner(),entry.part(),target);
                var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(form.itemId()));
                ItemStack replacement=new ItemStack(item.builtInRegistryHolder(),stack.getCount(),stack.getComponentsPatch());
                fire.getItems().set(slot,replacement);stack=replacement;changed=true;
            }
            heat(stack,elapsed);progress[slot]=elapsed;times[slot]=OrganicCookingRules.cookedAt(entry);
        }
        fire.setChanged();
        if(changed)level.sendBlockUpdated(pos,state,state,3);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if(event.getHand()!=InteractionHand.MAIN_HAND || event.getEntity().isSpectator())return;
        if(!(event.getLevel().getBlockEntity(event.getPos()) instanceof CampfireBlockEntity fire))return;
        var held=event.getItemStack();var entry=definition(held);
        // Empty hand takes one selected slot. Sneaking allows taking while holding another item.
        boolean taking=held.isEmpty() || event.getEntity().isShiftKeyDown();
        if(taking) {
            var point=event.getHitVec().getLocation();
            boolean east=point.x-event.getPos().getX()>=0.5,south=point.z-event.getPos().getZ()>=0.5;
            int quadrant=south?(east?2:3):(east?1:0);
            int facing=fire.getBlockState().getValue(CampfireBlock.FACING).get2DDataValue();
            int slot=Math.floorMod(quadrant-facing,4);
            ItemStack stored=fire.getItems().get(slot);
            if(stored.isEmpty())return;
            if(!event.getLevel().isClientSide) {
                fire.getItems().set(slot,ItemStack.EMPTY);
                ((OrganicCampfireAccess)fire).industron$cookingProgress()[slot]=0;
                if(!event.getEntity().getInventory().add(stored))event.getEntity().drop(stored,false);
                fire.setChanged();event.getLevel().sendBlockUpdated(event.getPos(),fire.getBlockState(),fire.getBlockState(),3);
            }
            own(event);return;
        }
        var profile=net.mads.industron.farming.FarmingCooking.profile(held);
        if(entry==null && profile==null)return;
        if(!event.getLevel().isClientSide) {
            var access=(OrganicCampfireAccess)fire;
            for(int slot=0;slot<fire.getItems().size();slot++) if(fire.getItems().get(slot).isEmpty()) {
                ItemStack inserted=held.copyWithCount(1);
                if(!event.getEntity().getAbilities().instabuild)held.shrink(1);
                fire.getItems().set(slot,inserted);access.industron$cookingProgress()[slot]=entry!=null?heat(inserted,entry):net.mads.industron.farming.FarmingCooking.heat(inserted,profile);
                access.industron$cookingTime()[slot]=entry!=null?OrganicCookingRules.cookedAt(entry):profile.cook();
                fire.setChanged();event.getLevel().sendBlockUpdated(event.getPos(),fire.getBlockState(),fire.getBlockState(),3);break;
            }
        }
        own(event);
    }
    private static void own(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
