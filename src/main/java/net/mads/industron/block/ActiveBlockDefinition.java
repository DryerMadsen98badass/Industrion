package net.mads.industron.block;

import net.mads.industron.Industron;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record ActiveBlockDefinition(
        String id,
        String displayName,
        ResourceLocation idleTexture,
        List<ResourceLocation> activeTextures,
        MiningTier miningTier,
        Set<MiningTool> miningTools
) {

    public static final int MAX_ACTIVE_FRAMES = 10;

    public ActiveBlockDefinition(String id, String displayName, String idleTexture, String... activeTextures) {
        this(id, displayName, parseTexture(idleTexture), parseActiveTextures(id, activeTextures), MiningTier.STONE, EnumSet.of(MiningTool.PICKAXE));
    }

    public ActiveBlockDefinition {
        validateId(id);
        validateDisplayName(id, displayName);
        if (idleTexture == null) {
            throw new IllegalArgumentException("Active block idle texture cannot be null: " + id);
        }
        if (activeTextures == null || activeTextures.isEmpty()) {
            throw new IllegalArgumentException("Active block must have at least one active texture: " + id);
        }
        if (activeTextures.size() > MAX_ACTIVE_FRAMES) {
            throw new IllegalArgumentException("Active block cannot have more than " + MAX_ACTIVE_FRAMES + " active textures: " + id);
        }
        if (activeTextures.stream().anyMatch(texture -> texture == null)) {
            throw new IllegalArgumentException("Active block active textures cannot contain null: " + id);
        }
        if (miningTier == null) {
            throw new IllegalArgumentException("Active block mining tier cannot be null: " + id);
        }
        activeTextures = List.copyOf(activeTextures);
        miningTools = validateMiningTools(id, miningTools);
    }

    public ActiveBlockDefinition wood() { return withMiningTier(MiningTier.WOOD); }
    public ActiveBlockDefinition stone() { return withMiningTier(MiningTier.STONE); }
    public ActiveBlockDefinition iron() { return withMiningTier(MiningTier.IRON); }
    public ActiveBlockDefinition diamond() { return withMiningTier(MiningTier.DIAMOND); }
    public ActiveBlockDefinition netherite() { return withMiningTier(MiningTier.NETHERITE); }

    public ActiveBlockDefinition withMiningTier(MiningTier tier) {
        if (tier == null) {
            throw new IllegalArgumentException("Active block mining tier cannot be null: " + id);
        }
        return new ActiveBlockDefinition(id, displayName, idleTexture, activeTextures, tier, miningTools);
    }

    public ActiveBlockDefinition mineableWith(MiningTool tool, MiningTool... moreTools) {
        if (tool == null) {
            throw new IllegalArgumentException("Active block mining tool cannot be null: " + id);
        }
        EnumSet<MiningTool> updated = EnumSet.of(tool);
        if (moreTools != null) {
            for (MiningTool moreTool : moreTools) {
                if (moreTool == null) {
                    throw new IllegalArgumentException("Active block mining tool cannot be null: " + id);
                }
                updated.add(moreTool);
            }
        }
        return new ActiveBlockDefinition(id, displayName, idleTexture, activeTextures, miningTier, updated);
    }

    public float hardness() { return miningTier.hardness(); }
    public float resistance() { return miningTier.resistance(); }
    public int activeFrameCount() { return activeTextures.size(); }

    public String idleModelPath() { return "block/casings/active/" + id + "_idle"; }

    public String activeModelPath(int frame) {
        int index = Math.floorMod(frame, activeTextures.size());
        return "block/casings/active/" + id + "_active_" + (index + 1);
    }

    public ResourceLocation activeTexture(int frame) { return activeTextures.get(Math.floorMod(frame, activeTextures.size())); }

    private static List<ResourceLocation> parseActiveTextures(String id, String[] textures) {
        if (textures == null || textures.length == 0) {
            throw new IllegalArgumentException("Active block must have at least one active texture: " + id);
        }
        List<ResourceLocation> parsed = new ArrayList<>(textures.length);
        for (String texture : textures) {
            parsed.add(parseTexture(texture));
        }
        return parsed;
    }

    private static ResourceLocation parseTexture(String texture) {
        if (texture == null || texture.isBlank()) {
            throw new IllegalArgumentException("Active block texture cannot be blank");
        }
        if (texture.contains(":")) {
            ResourceLocation parsed = ResourceLocation.tryParse(texture);
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid active block texture: " + texture);
            }
            return parsed;
        }
        if (!ResourceLocation.isValidPath(texture)) {
            throw new IllegalArgumentException("Invalid active block texture path: " + texture);
        }
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, texture);
    }

    private static void validateId(String id) {
        if (id == null || id.isBlank() || !ResourceLocation.isValidPath(id)) {
            throw new IllegalArgumentException("Invalid active block id: " + id);
        }
    }

    private static void validateDisplayName(String id, String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Active block display name cannot be blank: " + id);
        }
    }

    private static Set<MiningTool> validateMiningTools(String id, Set<MiningTool> miningTools) {
        if (miningTools == null || miningTools.isEmpty()) {
            throw new IllegalArgumentException("Active block mining tools cannot be empty: " + id);
        }
        if (miningTools.contains(null)) {
            throw new IllegalArgumentException("Active block mining tools cannot contain null: " + id);
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(miningTools));
    }
}
