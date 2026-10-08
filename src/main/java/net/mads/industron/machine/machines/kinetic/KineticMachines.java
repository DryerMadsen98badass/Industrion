package net.mads.industron.machine.machines.kinetic;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.mads.industron.integration.create.kinetic.CEKineticRecipeHost;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import java.util.List;
import net.mads.industron.machine.machines.kinetic.lathe.LatheBlock;
import net.mads.industron.machine.machines.kinetic.lathe.LatheBlockEntity;
import net.mads.industron.machine.machines.kinetic.centrifuge.CentrifugeBlock;
import net.mads.industron.machine.machines.kinetic.centrifuge.CentrifugeBlockEntity;
import net.mads.industron.machine.machines.kinetic.sifter.MechanicalSifterBlock;
import net.mads.industron.machine.machines.kinetic.sifter.MechanicalSifterBlockEntity;
import net.mads.industron.machine.machines.kinetic.pulverizer.PulverizerBlock;
import net.mads.industron.machine.machines.kinetic.pulverizer.PulverizerBlockEntity;
import net.mads.industron.machine.machines.kinetic.wire_drawing.WireDrawingMachineBlock;
import net.mads.industron.machine.machines.kinetic.wire_drawing.WireDrawingMachineBlockEntity;
import net.mads.industron.machine.machines.kinetic.winding.WindingMachineBlock;
import net.mads.industron.machine.machines.kinetic.winding.WindingMachineBlockEntity;
import net.mads.industron.machine.machines.kinetic.bender.MechanicalBenderBlock;
import net.mads.industron.machine.machines.kinetic.bender.MechanicalBenderBlockEntity;
import net.mads.industron.machine.machines.kinetic.magnetic_separator.MagneticSeparatorBlock;
import net.mads.industron.machine.machines.kinetic.magnetic_separator.MagneticSeparatorBlockEntity;

/** Explicit registrations for native Create machines. */
public final class KineticMachines {
    public static DeferredHolder<Block, LatheBlock> LATHE;
    public static DeferredHolder<Item, BlockItem> LATHE_ITEM;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<LatheBlockEntity>> LATHE_ENTITY;
    public static DeferredHolder<Block, CentrifugeBlock> CENTRIFUGE;
    public static DeferredHolder<Item, BlockItem> CENTRIFUGE_ITEM;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE_ENTITY;
    public static DeferredHolder<Block, MechanicalSifterBlock> MECHANICALSIFTER;
    public static DeferredHolder<Item, BlockItem> MECHANICALSIFTER_ITEM;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalSifterBlockEntity>> MECHANICALSIFTER_ENTITY;
    public static DeferredHolder<Block, PulverizerBlock> PULVERIZER;
    public static DeferredHolder<Item, BlockItem> PULVERIZER_ITEM;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<PulverizerBlockEntity>> PULVERIZER_ENTITY;
    public static DeferredHolder<Block, WireDrawingMachineBlock> WIREDRAWINGMACHINE;
    public static DeferredHolder<Item, BlockItem> WIREDRAWINGMACHINE_ITEM;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<WireDrawingMachineBlockEntity>> WIREDRAWINGMACHINE_ENTITY;
    public static DeferredHolder<Block, WindingMachineBlock> WINDINGMACHINE;
    public static DeferredHolder<Item, BlockItem> WINDINGMACHINE_ITEM;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<WindingMachineBlockEntity>> WINDINGMACHINE_ENTITY;
    public static DeferredHolder<Block, MechanicalBenderBlock> MECHANICALBENDER;
    public static DeferredHolder<Item, BlockItem> MECHANICALBENDER_ITEM;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalBenderBlockEntity>> MECHANICALBENDER_ENTITY;
    public static DeferredHolder<Block, MagneticSeparatorBlock> MAGNETICSEPARATOR;
    public static DeferredHolder<Item, BlockItem> MAGNETICSEPARATOR_ITEM;
    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<MagneticSeparatorBlockEntity>> MAGNETICSEPARATOR_ENTITY;

    public static void registerBlocks(DeferredRegister<Block> blocks) {
        LATHE = blocks.register("lathe", () -> new LatheBlock(BlockBehaviour.Properties.of().strength(3F, 6F).sound(SoundType.METAL).noOcclusion()));
        CENTRIFUGE = blocks.register("mechanical_centrifuge", () -> new CentrifugeBlock(BlockBehaviour.Properties.of().strength(3F, 6F).sound(SoundType.METAL).noOcclusion()));
        MECHANICALSIFTER = blocks.register("mechanical_sifter", () -> new MechanicalSifterBlock(BlockBehaviour.Properties.of().strength(3F, 6F).sound(SoundType.METAL).noOcclusion()));
        PULVERIZER = blocks.register("pulverizer", () -> new PulverizerBlock(BlockBehaviour.Properties.of().strength(3F, 6F).sound(SoundType.METAL).noOcclusion()));
        WIREDRAWINGMACHINE = blocks.register("wire_drawing_machine", () -> new WireDrawingMachineBlock(BlockBehaviour.Properties.of().strength(3F, 6F).sound(SoundType.METAL).noOcclusion()));
        WINDINGMACHINE = blocks.register("winding_machine", () -> new WindingMachineBlock(BlockBehaviour.Properties.of().strength(3F, 6F).sound(SoundType.METAL).noOcclusion()));
        MECHANICALBENDER = blocks.register("mechanical_bender", () -> new MechanicalBenderBlock(BlockBehaviour.Properties.of().strength(3F, 6F).sound(SoundType.METAL).noOcclusion()));
        MAGNETICSEPARATOR = blocks.register("magnetic_separator", () -> new MagneticSeparatorBlock(BlockBehaviour.Properties.of().strength(3F, 6F).sound(SoundType.METAL).noOcclusion()));
    }
    public static void registerItems(DeferredRegister<Item> items) {
        LATHE_ITEM = items.register("lathe", () -> new KineticMachineItem(LATHE.get(), new Item.Properties(), net.mads.industron.recipe.CERecipeTypes.TURNING, 16));
        CENTRIFUGE_ITEM = items.register("mechanical_centrifuge", () -> new KineticMachineItem(CENTRIFUGE.get(), new Item.Properties(), net.mads.industron.recipe.CERecipeTypes.CENTRIFUGING, 32));
        MECHANICALSIFTER_ITEM = items.register("mechanical_sifter", () -> new KineticMachineItem(MECHANICALSIFTER.get(), new Item.Properties(), net.mads.industron.recipe.CERecipeTypes.SIFTING, 8));
        PULVERIZER_ITEM = items.register("pulverizer", () -> new KineticMachineItem(PULVERIZER.get(), new Item.Properties(), net.mads.industron.recipe.CERecipeTypes.PULVERIZING, 32));
        WIREDRAWINGMACHINE_ITEM = items.register("wire_drawing_machine", () -> new KineticMachineItem(WIREDRAWINGMACHINE.get(), new Item.Properties(), net.mads.industron.recipe.CERecipeTypes.WIRE_DRAWING, 16));
        WINDINGMACHINE_ITEM = items.register("winding_machine", () -> new KineticMachineItem(WINDINGMACHINE.get(), new Item.Properties(), net.mads.industron.recipe.CERecipeTypes.WINDING, 8));
        MECHANICALBENDER_ITEM = items.register("mechanical_bender", () -> new KineticMachineItem(MECHANICALBENDER.get(), new Item.Properties(), net.mads.industron.recipe.CERecipeTypes.BENDING, 16));
        MAGNETICSEPARATOR_ITEM = items.register("magnetic_separator", () -> new KineticMachineItem(MAGNETICSEPARATOR.get(), new Item.Properties(), net.mads.industron.recipe.CERecipeTypes.MAGNETIC_SEPARATION, 32));
    }
    public static void registerBlockEntities(DeferredRegister<BlockEntityType<?>> entities) {
        LATHE_ENTITY = entities.register("lathe", () -> BlockEntityType.Builder.of(LatheBlockEntity::new, LATHE.get()).build(null));
        CENTRIFUGE_ENTITY = entities.register("mechanical_centrifuge", () -> BlockEntityType.Builder.of(CentrifugeBlockEntity::new, CENTRIFUGE.get()).build(null));
        MECHANICALSIFTER_ENTITY = entities.register("mechanical_sifter", () -> BlockEntityType.Builder.of(MechanicalSifterBlockEntity::new, MECHANICALSIFTER.get()).build(null));
        PULVERIZER_ENTITY = entities.register("pulverizer", () -> BlockEntityType.Builder.of(PulverizerBlockEntity::new, PULVERIZER.get()).build(null));
        WIREDRAWINGMACHINE_ENTITY = entities.register("wire_drawing_machine", () -> BlockEntityType.Builder.of(WireDrawingMachineBlockEntity::new, WIREDRAWINGMACHINE.get()).build(null));
        WINDINGMACHINE_ENTITY = entities.register("winding_machine", () -> BlockEntityType.Builder.of(WindingMachineBlockEntity::new, WINDINGMACHINE.get()).build(null));
        MECHANICALBENDER_ENTITY = entities.register("mechanical_bender", () -> BlockEntityType.Builder.of(MechanicalBenderBlockEntity::new, MECHANICALBENDER.get()).build(null));
        MAGNETICSEPARATOR_ENTITY = entities.register("magnetic_separator", () -> BlockEntityType.Builder.of(MagneticSeparatorBlockEntity::new, MAGNETICSEPARATOR.get()).build(null));
    }
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        capabilities(event, LATHE_ENTITY.get());
        capabilities(event, CENTRIFUGE_ENTITY.get());
        capabilities(event, MECHANICALSIFTER_ENTITY.get());
        capabilities(event, PULVERIZER_ENTITY.get());
        capabilities(event, WIREDRAWINGMACHINE_ENTITY.get());
        capabilities(event, WINDINGMACHINE_ENTITY.get());
        capabilities(event, MECHANICALBENDER_ENTITY.get());
        capabilities(event, MAGNETICSEPARATOR_ENTITY.get());
    }
    private static <T extends KineticBlockEntity & CEKineticRecipeHost> void capabilities(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> be.ceProcessing().itemCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> be.ceProcessing().fluidCapability(side));
    }
    public static void registerStress() {
        BlockStressValues.IMPACTS.register(LATHE.get(), () -> 4.0D);
        BlockStressValues.IMPACTS.register(CENTRIFUGE.get(), () -> 8.0D);
        BlockStressValues.IMPACTS.register(MECHANICALSIFTER.get(), () -> 2.0D);
        BlockStressValues.IMPACTS.register(PULVERIZER.get(), () -> 8.0D);
        BlockStressValues.IMPACTS.register(WIREDRAWINGMACHINE.get(), () -> 4.0D);
        BlockStressValues.IMPACTS.register(WINDINGMACHINE.get(), () -> 2.0D);
        BlockStressValues.IMPACTS.register(MECHANICALBENDER.get(), () -> 4.0D);
        BlockStressValues.IMPACTS.register(MAGNETICSEPARATOR.get(), () -> 4.0D);
    }
    public static List<DeferredHolder<Block, ? extends Block>> blocks() { return List.of(LATHE, CENTRIFUGE, MECHANICALSIFTER, PULVERIZER, WIREDRAWINGMACHINE, WINDINGMACHINE, MECHANICALBENDER, MAGNETICSEPARATOR); }
    public static List<DeferredHolder<Item, BlockItem>> items() { return List.of(LATHE_ITEM, CENTRIFUGE_ITEM, MECHANICALSIFTER_ITEM, PULVERIZER_ITEM, WIREDRAWINGMACHINE_ITEM, WINDINGMACHINE_ITEM, MECHANICALBENDER_ITEM, MAGNETICSEPARATOR_ITEM); }
    public static boolean isMachine(Block block) { return block instanceof LatheBlock || block instanceof CentrifugeBlock || block instanceof MechanicalSifterBlock || block instanceof PulverizerBlock || block instanceof WireDrawingMachineBlock || block instanceof WindingMachineBlock || block instanceof MechanicalBenderBlock || block instanceof MagneticSeparatorBlock; }
    public static net.mads.industron.recipe.RecipeTypeDefinition recipeType(Block block) {
        if (block == LATHE.get()) return net.mads.industron.recipe.CERecipeTypes.TURNING;
        if (block == CENTRIFUGE.get()) return net.mads.industron.recipe.CERecipeTypes.CENTRIFUGING;
        if (block == MECHANICALSIFTER.get()) return net.mads.industron.recipe.CERecipeTypes.SIFTING;
        if (block == PULVERIZER.get()) return net.mads.industron.recipe.CERecipeTypes.PULVERIZING;
        if (block == WIREDRAWINGMACHINE.get()) return net.mads.industron.recipe.CERecipeTypes.WIRE_DRAWING;
        if (block == WINDINGMACHINE.get()) return net.mads.industron.recipe.CERecipeTypes.WINDING;
        if (block == MECHANICALBENDER.get()) return net.mads.industron.recipe.CERecipeTypes.BENDING;
        if (block == MAGNETICSEPARATOR.get()) return net.mads.industron.recipe.CERecipeTypes.MAGNETIC_SEPARATION;
        throw new IllegalArgumentException("Not an Industron native kinetic machine: " + block);
    }
    private KineticMachines() {}
}
