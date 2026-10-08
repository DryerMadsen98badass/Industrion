package net.mads.industron.tool;
import com.mojang.serialization.MapCodec;
import net.mads.industron.Industron;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class EquipmentLootRegistry {
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS=
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,Industron.MOD_ID);
    static { SERIALIZERS.register("equipment_replacement",()->EquipmentLootModifier.CODEC); }
    private EquipmentLootRegistry(){}
    public static void register(IEventBus bus){SERIALIZERS.register(bus);}
}
