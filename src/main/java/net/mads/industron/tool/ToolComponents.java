package net.mads.industron.tool;

import net.mads.industron.Industron;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ToolComponents {
    private static final DeferredRegister<DataComponentType<?>> TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Industron.MOD_ID);

    public static final Supplier<DataComponentType<ToolStackData>> PARTS = TYPES.register(
            "tool_parts",
            () -> DataComponentType.<ToolStackData>builder()
                    .persistent(ToolStackData.CODEC)
                    .networkSynchronized(ToolStackData.STREAM_CODEC)
                    .build()
    );

    private ToolComponents() {
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
