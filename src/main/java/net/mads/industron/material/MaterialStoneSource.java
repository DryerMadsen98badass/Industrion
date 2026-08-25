package net.mads.industron.material;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record MaterialStoneSource(
        String id,
        Optional<ResourceLocation> existingBlock,
        Optional<ResourceLocation> texture
) {
    public MaterialStoneSource {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Stone source id cannot be blank");
        }

        existingBlock = existingBlock == null ? Optional.empty() : existingBlock;
        texture = texture == null ? Optional.empty() : texture;

        if (existingBlock.isPresent() == texture.isPresent()) {
            throw new IllegalArgumentException("Stone source must have exactly one existing block or texture");
        }
    }

    public static MaterialStoneSource existing(String id, ResourceLocation block) {
        return new MaterialStoneSource(id, Optional.of(block), Optional.empty());
    }

    public static MaterialStoneSource generated(String id, ResourceLocation texture) {
        return new MaterialStoneSource(id, Optional.empty(), Optional.of(texture));
    }

    public boolean isExisting() {
        return existingBlock.isPresent();
    }

    public String registryName(IndustrialMaterial material) {
        return material.id() + "_" + id;
    }

    public String displayName(IndustrialMaterial material) {
        return material.displayName() + " " + readableId(id);
    }

    private static String readableId(String id) {
        StringBuilder readable = new StringBuilder();
        boolean upperNext = true;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c == '_' || c == '-' || c == '/') {
                readable.append(' ');
                upperNext = true;
                continue;
            }

            readable.append(upperNext ? Character.toUpperCase(c) : c);
            upperNext = false;
        }
        return readable.toString();
    }
}
