package net.mads.industron.machine.foundry;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.material.defenitions.ClayMaterials;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.machine.foundry.casting.CastingDefinitions;
import net.mads.industron.machine.foundry.casting.TerracottaMoldDefinitions;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import java.util.*;
import java.util.function.Supplier;

public final class CastingRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Industron.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Industron.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Industron.MOD_ID);
    public static final Map<String, Supplier<Block>> CASTERS = new LinkedHashMap<>();
    public static final Map<String, Supplier<Block>> FAUCETS = new LinkedHashMap<>();
    public static final Map<String, Supplier<Block>> DRAINS = new LinkedHashMap<>();
    public static final Map<String, Supplier<Item>> MOLDS = new LinkedHashMap<>();
    public static final Map<String, Supplier<Item>> UNFIRED_MOLDS = new LinkedHashMap<>();
    public static final Map<String, Supplier<Item>> DRIED_UNFIRED_MOLDS = new LinkedHashMap<>();
    public static final List<Supplier<Item>> BLOCK_ITEMS = new ArrayList<>();
    static {
        for (IndustrialMaterial ceramic : IndustrialMaterials.ALL) {
            if (ceramic.isClayMaterial()) {
                register(ceramic, "caster", CASTERS, () -> new CastingBlock(ceramic, false));
                register(ceramic, "faucet", FAUCETS, () -> new CastingBlock(ceramic, true));
            }
            if (!ceramic.supportsCeramicMolds()) continue;
            for (CastingDefinitions.Form form : CastingDefinitions.ALL) {
                String id = moldId(ceramic, form);
                MOLDS.put(id, ITEMS.register(id, () -> new CastingMoldItem(ceramic, form)));
            }
            for (CastingDefinitions.PendingMold pending : CastingDefinitions.PENDING_MOLDS) {
                String id = moldId(ceramic, pending.mold());
                MOLDS.put(id, ITEMS.register(id, () -> new CastingMoldItem(ceramic, pending)));
            }
            for (TerracottaMoldDefinitions.Definition definition : TerracottaMoldDefinitions.ALL) {
                MaterialPart moldPart = definition.moldPart();
                String unfiredId = unfiredMoldId(ceramic, moldPart);
                String driedId = driedUnfiredMoldId(ceramic, moldPart);
                UNFIRED_MOLDS.put(unfiredId, ITEMS.register(unfiredId,
                        () -> new CastingMoldStageItem(ceramic, moldPart, CastingMoldStageItem.Stage.UNFIRED)));
                DRIED_UNFIRED_MOLDS.put(driedId, ITEMS.register(driedId,
                        () -> new CastingMoldStageItem(ceramic, moldPart, CastingMoldStageItem.Stage.DRIED_UNFIRED)));
            }
        }
    }
    public static final Supplier<BlockEntityType<CastingBlockEntity>> ENTITY = ENTITIES.register("caster", () ->
            BlockEntityType.Builder.of(CastingBlockEntity::new, java.util.stream.Stream.concat(
                    CASTERS.values().stream(), FAUCETS.values().stream()).map(Supplier::get).toArray(Block[]::new)).build(null));
    private CastingRegistry() {}
    private static void register(IndustrialMaterial clay, String suffix, Map<String, Supplier<Block>> map, Supplier<Block> factory) {
        var block = BLOCKS.register(clay.id() + "_" + suffix, factory);
        map.put(clay.id(), block);
        BLOCK_ITEMS.add(ITEMS.register(clay.id() + "_" + suffix, () -> new BlockItem(block.get(), new Item.Properties()) {
            @Override public Component getName(ItemStack stack) {
                return Component.literal(clay.displayName() + " " + (suffix.equals("caster") ? "Caster" : "Faucet"));
            }
        }));
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(CastingRegistry::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENTITY.get(), (caster, side) -> caster.fluidCapability());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(), (caster, side) -> caster.itemCapability(side));
    }

    public static String moldId(IndustrialMaterial clay, CastingDefinitions.Form form) {
        return moldId(clay, form.mold());
    }

    public static String moldId(IndustrialMaterial clay, MaterialPart moldPart) {
        return clay.id() + "_" + moldPart.id();
    }

    public static String unfiredMoldId(IndustrialMaterial clay, MaterialPart moldPart) {
        return "unfired_" + moldId(clay, moldPart);
    }

    public static String driedUnfiredMoldId(IndustrialMaterial clay, MaterialPart moldPart) {
        return "dried_unfired_" + moldId(clay, moldPart);
    }


    /** Resolves the raw material used to form a ceramic mold in the Caster. */
    public static IndustrialMaterial moldMaterial(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        MaterialLookup.MaterialTarget target = MaterialLookup.find(stack);
        if (target != null
                && target.part() == MaterialPart.CLAY
                && target.material().isClayMaterial()) {
            return target.material();
        }
        if (stack.is(Items.NETHERRACK)) return ClayMaterials.NETHER;
        return null;
    }

    public static ItemStack unfiredMold(IndustrialMaterial clay, MaterialPart moldPart) {
        Supplier<Item> item = UNFIRED_MOLDS.get(unfiredMoldId(clay, moldPart));
        return item == null ? ItemStack.EMPTY : new ItemStack(item.get());
    }

    public static Collection<Supplier<Item>> moldStageItems() {
        List<Supplier<Item>> result = new ArrayList<>(UNFIRED_MOLDS.size() + DRIED_UNFIRED_MOLDS.size());
        result.addAll(UNFIRED_MOLDS.values());
        result.addAll(DRIED_UNFIRED_MOLDS.values());
        return List.copyOf(result);
    }
}
