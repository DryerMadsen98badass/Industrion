package net.mads.industron.worldgen;

import net.mads.industron.Industron;
import net.mads.industron.block.FallenStickBlock;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.structure.StructureMaterialItem;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Lets both generated wood sticks and vanilla Oak Stick place their matching fallen-stick block. */
@EventBusSubscriber(modid = Industron.MOD_ID)
public final class FallenStickPlacementHandler {
    private FallenStickPlacementHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getFace() != Direction.UP) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ItemStack stack = event.getItemStack();
        WoodMaterial wood = materialForStick(stack);
        if (wood == null) return;

        BlockPos placePos = event.getPos().above();
        if (!level.getBlockState(placePos).isAir()) return;
        if (!event.getEntity().mayUseItemAt(placePos, Direction.UP, stack)) return;

        var holder = BlockRegistry.getFallenStickBlock(wood);
        if (holder == null) return;
        FallenStickBlock fallen = holder.get();
        BlockState state = fallen.randomState(event.getEntity().getRandom());
        if (!state.canSurvive(level, placePos)) return;
        if (!level.setBlock(placePos, state, 11)) return;

        if (!event.getEntity().getAbilities().instabuild) {
            stack.shrink(1);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static WoodMaterial materialForStick(ItemStack stack) {
        if (stack.getItem() instanceof StructureMaterialItem structureItem
                && structureItem.part() == MaterialPart.STICK
                && structureItem.material() instanceof WoodMaterial wood) {
            return wood;
        }

        for (WoodMaterial wood : WoodMaterials.ALL) {
            if (!wood.hasExistingPart(MaterialPart.STICK)) continue;
            var item = BuiltInRegistries.ITEM.getOptional(wood.existingPart(MaterialPart.STICK)).orElse(null);
            if (item != null && stack.is(item)) return wood;
        }
        return null;
    }
}
