package net.mads.industron.machine.coil;

import net.mads.industron.Industron;
import net.minecraft.resources.ResourceLocation;

public record CoilDefinition(String name, String id, int heat) {
    public String displayName() {
        return name + " Coil";
    }

    public String blockId() {
        return id + "_coil";
    }

    public String itemId() {
        return id + "_heating_coil";
    }

    public ResourceLocation offTexture() {
        return texture(id + "_coil_off");
    }

    public ResourceLocation onTexture() {
        return texture(id + "_coil_on");
    }

    public ResourceLocation frameTexture() {
        return texture("coil_frame");
    }

    private static ResourceLocation texture(String texture) {
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "block/casings/coils/" + texture);
    }
}
