package net.mads.industron.material.plant.composting;

import net.mads.industron.Industron;
import net.mads.industron.material.defenitions.PlantMaterials;
import net.mads.industron.material.plant.PlantMaterial;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.plant.PlantProcessingPlanner;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Makes the vanilla Composter the primitive deterministic Industron plant-decomposition machine.
 *
 * <p>Each accepted solid item remains one material unit. Seven inserted units fill the vanilla
 * Composter; after its normal level-7 maturation tick the player extracts the same number of
 * material-specific fertilizer units. Different PlantMaterials are not mixed in one batch because
 * doing so would require a separately registered mixed-composition material.</p>
 */
@EventBusSubscriber(modid = Industron.MOD_ID)
public final class PlantComposterRuntime {
    private static final int READY_LEVEL = 8;
    private static final int MATURING_LEVEL = 7;

    private PlantComposterRuntime() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.COMPOSTER)) return;

        ItemStack held = event.getItemStack();
        PlantMaterial inputMaterial = materialFor(held);

        // Client prediction only needs to own recognized insertions. Server state owns extraction.
        if (!(event.getLevel() instanceof ServerLevel level)) {
            if (inputMaterial != null) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }

        BlockPos pos = event.getPos();
        var blockState = level.getBlockState(pos);
        int composterLevel = blockState.getValue(ComposterBlock.LEVEL);
        PlantComposterSavedData data = PlantComposterSavedData.get(level);
        PlantComposterSavedData.Entry stored = data.get(pos).orElse(null);

        // A level-0 vanilla/new composter cannot legitimately carry an old Industron batch.
        if (composterLevel == 0 && stored != null) {
            data.remove(pos);
            stored = null;
        }

        if (composterLevel == READY_LEVEL && stored != null) {
            extractFertilizer(level, pos, blockState, event, stored, data);
            return;
        }

        if (inputMaterial == null) {
            // Once an Industron batch exists, vanilla compostables may not be mixed into it.
            if (stored != null) own(event, InteractionResult.FAIL);
            return;
        }

        // Do not let vanilla consume an item while the batch is already maturing/full.
        if (composterLevel >= MATURING_LEVEL) {
            own(event, InteractionResult.SUCCESS);
            return;
        }

        // Do not merge an unknown pre-existing vanilla partial batch with deterministic chemistry.
        if (stored == null && composterLevel > 0) {
            own(event, InteractionResult.FAIL);
            return;
        }

        if (stored != null && !stored.materialId().equals(inputMaterial.id())) {
            own(event, InteractionResult.FAIL);
            return;
        }

        if (!event.getEntity().getAbilities().instabuild) held.shrink(1);

        int units = (stored == null ? 0 : stored.units()) + 1;
        data.set(pos, inputMaterial.id(), units);

        int nextLevel = Math.min(MATURING_LEVEL, composterLevel + 1);
        level.setBlock(pos, blockState.setValue(ComposterBlock.LEVEL, nextLevel), 3);
        if (nextLevel == MATURING_LEVEL) {
            // Vanilla Composter uses its scheduled tick to mature from 7 to 8.
            level.scheduleTick(pos, Blocks.COMPOSTER, 20);
        }
        own(event, InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!event.getState().is(Blocks.COMPOSTER)) return;
        PlantComposterSavedData.get(level).remove(event.getPos());
    }

    private static void extractFertilizer(
            ServerLevel level,
            BlockPos pos,
            net.minecraft.world.level.block.state.BlockState blockState,
            PlayerInteractEvent.RightClickBlock event,
            PlantComposterSavedData.Entry stored,
            PlantComposterSavedData data
    ) {
        PlantMaterial material = PlantMaterials.ALL.stream()
                .filter(candidate -> candidate.id().equals(stored.materialId()))
                .findFirst()
                .orElse(null);
        if (material == null) {
            data.remove(pos);
            return;
        }

        var fertilizer = PlantProcessingPlanner.requireIntermediate(material, PlantProcessingPlanner.FERTILIZER);
        var holder = ItemRegistry.getPlantProcessIntermediateItem(fertilizer.id());
        if (holder == null) {
            data.remove(pos);
            return;
        }

        ItemStack output = new ItemStack(holder.get(), stored.units());
        if (!event.getEntity().addItem(output)) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, output);
        }

        level.setBlock(pos, blockState.setValue(ComposterBlock.LEVEL, 0), 3);
        data.remove(pos);
        own(event, InteractionResult.SUCCESS);
    }

    private static PlantMaterial materialFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        for (PlantMaterial material : PlantMaterials.ALL) {
            for (var entry : material.existingParts().entrySet()) {
                PlantPart part = entry.getKey();
                if (part.biomassSource() && entry.getValue().equals(itemId)) return material;
            }

            var biomass = PlantProcessingPlanner.requireIntermediate(material, PlantProcessingPlanner.BIOMASS);
            var holder = ItemRegistry.getPlantProcessIntermediateItem(biomass.id());
            if (holder != null && stack.is(holder.get())) return material;
        }
        return null;
    }

    private static void own(PlayerInteractEvent.RightClickBlock event, InteractionResult result) {
        event.setCancellationResult(result);
        event.setCanceled(true);
    }
}
