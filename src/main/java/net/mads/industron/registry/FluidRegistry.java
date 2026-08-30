package net.mads.industron.registry;

import net.mads.industron.Industron;
import net.mads.industron.fluid.FiredFluidBucketWrapper;
import net.mads.industron.fluid.IndustrialFluid;
import net.mads.industron.fluid.IndustrialFluidLookup;
import net.mads.industron.fluid.IndustrialFluids;
import net.mads.industron.fluid.MaterialFluidType;
import net.mads.industron.fluid.NonPlaceableBucketItem;
import net.mads.industron.item.FiredBucketItem;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.MaterialPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class FluidRegistry {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Industron.MOD_ID);

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, Industron.MOD_ID);

    public static final DeferredRegister<Item> BUCKET_ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, Industron.MOD_ID);

    public static final Map<String, RegisteredFluid> MATERIAL_FLUIDS = new LinkedHashMap<>();
    public static final Map<String, RegisteredFluid> CHEMICAL_FLUIDS = new LinkedHashMap<>();

    public static final Map<Item, Item> FIRED_BUCKET_BY_NORMAL_BUCKET = new LinkedHashMap<>();
    public static final Map<Item, Item> NORMAL_BUCKET_BY_FIRED_BUCKET = new LinkedHashMap<>();

    public static final DeferredHolder<Item, FiredBucketItem> FIRED_WATER_BUCKET =
            BUCKET_ITEMS.register(
                    "fired_water_bucket",
                    () -> new FiredBucketItem(
                            Fluids.WATER,
                            new Item.Properties().stacksTo(1)
                    )
            );

    public static final DeferredHolder<Item, FiredBucketItem> FIRED_LAVA_BUCKET =
            BUCKET_ITEMS.register(
                    "fired_lava_bucket",
                    () -> new FiredBucketItem(
                            Fluids.LAVA,
                            new Item.Properties().stacksTo(1)
                    )
            );

    static {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            for (MaterialPart part : material.parts()) {
                if (!part.isFluid() || material.hasExistingPart(part)) {
                    continue;
                }
                if (part != MaterialPart.LIQUID && part != MaterialPart.GAS && part != MaterialPart.MOLTEN_FLUID) {
                    continue;
                }
                IndustrialFluid fluid = IndustrialFluidLookup.materialFluid(material, part);
                MATERIAL_FLUIDS.put(fluid.registryName(), registerFluid(fluid));
            }
        }

        for (IndustrialFluid fluid : IndustrialFluids.ALL) {
            if (isVanillaFluid(fluid)) {
                continue;
            }

            CHEMICAL_FLUIDS.put(fluid.registryName(), registerFluid(fluid));
        }
    }

    private FluidRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BUCKET_ITEMS.register(modEventBus);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new FluidBucketWrapper(stack),
                Items.BUCKET
        );

        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new FluidBucketWrapper(stack),
                Items.WATER_BUCKET
        );

        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new FluidBucketWrapper(stack),
                Items.LAVA_BUCKET
        );

        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new FiredFluidBucketWrapper(stack),
                ItemRegistry.FIRED_BUCKET.get()
        );

        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new FiredFluidBucketWrapper(stack),
                FIRED_WATER_BUCKET.get()
        );

        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new FiredFluidBucketWrapper(stack),
                FIRED_LAVA_BUCKET.get()
        );

        for (RegisteredFluid fluid : allFluids()) {
            event.registerItem(
                    Capabilities.FluidHandler.ITEM,
                    (stack, context) -> new FluidBucketWrapper(stack),
                    fluid.bucket().get()
            );

            event.registerItem(
                    Capabilities.FluidHandler.ITEM,
                    (stack, context) -> new FiredFluidBucketWrapper(stack),
                    fluid.firedBucket().get()
            );
        }
    }

    public static Collection<RegisteredFluid> allFluids() {
        return java.util.stream.Stream.concat(
                MATERIAL_FLUIDS.values().stream(),
                CHEMICAL_FLUIDS.values().stream()
        ).toList();
    }

    public static Collection<DeferredHolder<Item, ? extends Item>> getAllBucketItems() {
        Collection<DeferredHolder<Item, ? extends Item>> buckets = new ArrayList<>();

        buckets.add(FIRED_WATER_BUCKET);
        buckets.add(FIRED_LAVA_BUCKET);

        for (RegisteredFluid fluid : allFluids()) {
            buckets.add(fluid.bucket());
            buckets.add(fluid.firedBucket());
        }

        return buckets;
    }

    private static RegisteredFluid registerFluid(IndustrialFluid fluid) {
        DeferredHolder<FluidType, MaterialFluidType> type =
                FLUID_TYPES.register(
                        fluid.registryName(),
                        () -> new MaterialFluidType(fluid)
                );

        AtomicReference<DeferredHolder<Fluid, BaseFlowingFluid.Source>> source =
                new AtomicReference<>();

        AtomicReference<DeferredHolder<Fluid, BaseFlowingFluid.Flowing>> flowing =
                new AtomicReference<>();

        AtomicReference<DeferredHolder<Item, NonPlaceableBucketItem>> bucket =
                new AtomicReference<>();

        AtomicReference<DeferredHolder<Item, NonPlaceableBucketItem>> firedBucket =
                new AtomicReference<>();

        Supplier<BaseFlowingFluid.Properties> properties = () ->
                new BaseFlowingFluid.Properties(
                        type,
                        () -> source.get().get(),
                        () -> flowing.get().get()
                )
                        .bucket(() -> bucket.get().get())
                        .tickRate(fluid.isGas() ? 2 : 5)
                        .levelDecreasePerBlock(fluid.isGas() ? 2 : 1);

        source.set(
                FLUIDS.register(
                        fluid.registryName(),
                        () -> new BaseFlowingFluid.Source(properties.get())
                )
        );

        flowing.set(
                FLUIDS.register(
                        "flowing_" + fluid.registryName(),
                        () -> new BaseFlowingFluid.Flowing(properties.get())
                )
        );

        bucket.set(
                BUCKET_ITEMS.register(
                        fluid.bucketName(),
                        () -> new NonPlaceableBucketItem(
                                source.get().get(),
                                new Item.Properties()
                                        .craftRemainder(Items.BUCKET)
                                        .stacksTo(1)
                        )
                )
        );

        firedBucket.set(
                BUCKET_ITEMS.register(
                        "fired_" + fluid.bucketName(),
                        () -> new NonPlaceableBucketItem(
                                source.get().get(),
                                new Item.Properties().stacksTo(1)
                        )
                )
        );

        return new RegisteredFluid(
                fluid,
                type,
                source.get(),
                flowing.get(),
                bucket.get(),
                firedBucket.get()
        );
    }

    private static boolean isVanillaFluid(IndustrialFluid fluid) {
        return fluid.registryName().equals("water")
                || fluid.registryName().equals("lava");
    }

    public static void buildFiredBucketMaps() {
        if (!FIRED_BUCKET_BY_NORMAL_BUCKET.isEmpty()) {
            return;
        }

        putBucket(Items.BUCKET, ItemRegistry.FIRED_BUCKET.get());
        putBucket(Items.WATER_BUCKET, FIRED_WATER_BUCKET.get());
        putBucket(Items.LAVA_BUCKET, FIRED_LAVA_BUCKET.get());

        for (RegisteredFluid fluid : allFluids()) {
            putBucket(fluid.bucket().get(), fluid.firedBucket().get());
        }
    }

    private static void putBucket(Item normalBucket, Item firedBucket) {
        FIRED_BUCKET_BY_NORMAL_BUCKET.put(normalBucket, firedBucket);
        NORMAL_BUCKET_BY_FIRED_BUCKET.put(firedBucket, normalBucket);
    }

    public record RegisteredFluid(
            IndustrialFluid definition,
            DeferredHolder<FluidType, MaterialFluidType> type,
            DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
            DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
            DeferredHolder<Item, NonPlaceableBucketItem> bucket,
            DeferredHolder<Item, NonPlaceableBucketItem> firedBucket
    ) {
    }
}
