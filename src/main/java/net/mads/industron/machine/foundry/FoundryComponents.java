package net.mads.industron.machine.foundry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mads.industron.Industron;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class FoundryComponents {
    private static final DeferredRegister<DataComponentType<?>> TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Industron.MOD_ID);
    private static final Codec<MoltenRatio> RATIO_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
            .comapFlatMap(data -> {
                try { return DataResult.success(MoltenRatio.decode(data)); }
                catch (RuntimeException exception) { return DataResult.error(() -> "Invalid molten composition"); }
            }, MoltenRatio::encode);
    public static final Supplier<DataComponentType<MoltenRatio>> COMPOSITION = TYPES.register(
            "molten_composition", () -> DataComponentType.<MoltenRatio>builder().persistent(RATIO_CODEC).build());
    public static final Supplier<DataComponentType<Integer>> TEMPERATURE = TYPES.register(
            "molten_temperature", () -> DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 1_000_000)).build());
    private FoundryComponents() {}
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
