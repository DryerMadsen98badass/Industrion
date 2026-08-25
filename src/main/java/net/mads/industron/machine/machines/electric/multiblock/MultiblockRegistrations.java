package net.mads.industron.machine.machines.electric.multiblock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

public final class MultiblockRegistrations {
    private MultiblockRegistrations() {
    }

    public static void registerControllerBlocks(
            DeferredRegister<Block> blocks,
            Map<String, DeferredHolder<Block, MultiblockControllerBlock>> controllers
    ) {
        for (MultiblockControllerDefinition controller : MultiblockDefinitions.controllers()) {
            controllers.put(controller.registryName(), blocks.register(controller.registryName(), () -> new MultiblockControllerBlock(controller)));
        }
    }

    public static void registerControllerItems(
            DeferredRegister<Item> items,
            Map<String, DeferredHolder<Item, BlockItem>> controllerItems,
            Map<String, DeferredHolder<Block, MultiblockControllerBlock>> controllerBlocks
    ) {
        for (MultiblockControllerDefinition controller : MultiblockDefinitions.controllers()) {
            controllerItems.put(controller.registryName(), items.register(controller.registryName(), () ->
                    new BlockItem(controllerBlocks.get(controller.registryName()).get(), new Item.Properties())));
        }
    }
}
