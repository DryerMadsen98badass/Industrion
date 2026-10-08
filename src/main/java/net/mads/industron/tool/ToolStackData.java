package net.mads.industron.tool;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.LinkedHashMap;
import java.util.Map;

/** Persistent role -> material identity map for one assembled tool. */
public record ToolStackData(Map<String, String> materials) {
    public static final Codec<ToolStackData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
            .xmap(ToolStackData::new, ToolStackData::materials);
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolStackData> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public ToolStackData {
        materials = Map.copyOf(new LinkedHashMap<>(materials == null ? Map.of() : materials));
    }

    public String materialKey(String role) {
        return materials.get(role);
    }
}
