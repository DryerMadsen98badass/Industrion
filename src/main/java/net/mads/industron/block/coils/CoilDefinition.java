package net.mads.industron.block.coils;

import net.mads.industron.Industron;
import net.mads.industron.machine.MachineTier;
import net.minecraft.resources.ResourceLocation;

public final class CoilDefinition {
    private final String id;
    private final String displayName;
    private final int heat;
    private final MachineTier tier;
    private final TextureLayer off;
    private final TextureLayer on;
    private final TextureLayer frame;

    private CoilDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = requireText(builder.displayName, "displayName");
        if (builder.heat < 0) {
            throw new IllegalStateException("Coil temperature must be >= 0 for " + id);
        }
        this.heat = builder.heat;
        if (builder.tier == null || builder.tier == MachineTier.NONE) {
            throw new IllegalStateException("Coil breaking tier must be a real tier for " + id);
        }
        this.tier = builder.tier;
        this.off = new TextureLayer(requireTexture(builder.offTexture, "offTexture"), builder.offColor);
        this.on = new TextureLayer(requireTexture(builder.onTexture, "onTexture"), builder.onColor);
        this.frame = new TextureLayer(requireTexture(builder.frameTexture, "frameTexture"), builder.frameColor);
    }

    public static Builder coil(String id) {
        return new Builder(id);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int heat() {
        return heat;
    }

    public MachineTier tier() {
        return tier;
    }


    public String blockId() {
        return id.endsWith("_coil") ? id : id + "_coil";
    }

    public String itemId() {
        return blockId();
    }

    public ResourceLocation offTexture() {
        return off.texture();
    }

    public ResourceLocation onTexture() {
        return on.texture();
    }

    public ResourceLocation frameTexture() {
        return frame.texture();
    }

    public TextureLayer off() {
        return off;
    }

    public TextureLayer on() {
        return on;
    }

    public TextureLayer frame() {
        return frame;
    }

    private static ResourceLocation requireTexture(ResourceLocation texture, String field) {
        if (texture == null) {
            throw new IllegalStateException("Missing " + field + " for coil");
        }
        return texture;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing " + field + " for coil");
        }
        return value;
    }

    private static ResourceLocation texture(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Texture path cannot be blank");
        }

        int separator = path.indexOf(':');
        if (separator >= 0) {
            String namespace = path.substring(0, separator);
            String resourcePath = path.substring(separator + 1);
            return ResourceLocation.fromNamespaceAndPath(namespace, resourcePath);
        }
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, path);
    }

    public record TextureLayer(ResourceLocation texture, Integer color) {
        public boolean hasColor() {
            return color != null;
        }
    }

    public static final class Builder {
        private enum Layer {
            OFF,
            ON,
            FRAME
        }

        private final String id;
        private String displayName;
        private int heat = -1;
        private MachineTier tier;
        private ResourceLocation offTexture;
        private ResourceLocation onTexture;
        private ResourceLocation frameTexture;
        private Integer offColor;
        private Integer onColor;
        private Integer frameColor;
        private Layer lastTextureLayer;

        private Builder(String id) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Coil id cannot be blank");
            }
            this.id = id;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder temperature(int temperature) {
            this.heat = temperature;
            return this;
        }

        public Builder tier(MachineTier tier) {
            this.tier = tier;
            return this;
        }


        public Builder offTexture(String path) {
            this.offTexture = texture(path);
            this.lastTextureLayer = Layer.OFF;
            return this;
        }

        public Builder onTexture(String path) {
            this.onTexture = texture(path);
            this.lastTextureLayer = Layer.ON;
            return this;
        }

        public Builder frameTexture(String path) {
            this.frameTexture = texture(path);
            this.lastTextureLayer = Layer.FRAME;
            return this;
        }

        /**
         * Applies an optional tint to the texture declared immediately before this call.
         * If no color is set, Minecraft renders the PNG unchanged.
         */
        public Builder color(int rgb) {
            if (lastTextureLayer == null) {
                throw new IllegalStateException("color(...) must come after a coil texture");
            }

            int normalized = rgb & 0x00FFFFFF;
            switch (lastTextureLayer) {
                case OFF -> this.offColor = normalized;
                case ON -> this.onColor = normalized;
                case FRAME -> this.frameColor = normalized;
            }
            return this;
        }

        public CoilDefinition build() {
            return new CoilDefinition(this);
        }
    }
}
