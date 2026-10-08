package net.mads.industron.material;

import net.mads.industron.block.BlockStrength;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record MaterialStoneSource(
        String id,
        Optional<ResourceLocation> existingBlock,
        Optional<ResourceLocation> texture,
        Optional<BlockStrength> strength
) {
    public MaterialStoneSource(
            String id,
            Optional<ResourceLocation> existingBlock,
            Optional<ResourceLocation> texture
    ) {
        this(id, existingBlock, texture, Optional.empty());
    }

    public MaterialStoneSource {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Stone source id cannot be blank");
        }

        existingBlock = existingBlock == null ? Optional.empty() : existingBlock;
        texture = texture == null ? Optional.empty() : texture;
        strength = strength == null ? Optional.empty() : strength;

        if (existingBlock.isPresent() == texture.isPresent()) {
            throw new IllegalArgumentException("Stone source must have exactly one existing block or texture");
        }
        if (existingBlock.isPresent() && strength.isPresent()) {
            throw new IllegalArgumentException("Existing stone sources cannot override the existing block strength: " + id);
        }
    }

    public static MaterialStoneSource existing(String id, ResourceLocation block) {
        return new MaterialStoneSource(id, Optional.of(block), Optional.empty(), Optional.empty());
    }

    public static MaterialStoneSource generated(String id, ResourceLocation texture) {
        return new MaterialStoneSource(id, Optional.empty(), Optional.of(texture), Optional.empty());
    }

    public MaterialStoneSource strength(float hardness) {
        return strength(hardness, hardness);
    }

    public MaterialStoneSource strength(float hardness, float resistance) {
        if (isExisting()) {
            throw new IllegalStateException("Cannot change strength of existing stone source: " + id);
        }
        return new MaterialStoneSource(
                id,
                existingBlock,
                texture,
                Optional.of(BlockStrength.of(hardness, resistance))
        );
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
