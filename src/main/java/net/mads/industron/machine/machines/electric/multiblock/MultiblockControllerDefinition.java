package net.mads.industron.machine.machines.electric.multiblock;

import net.mads.industron.Industron;
import net.mads.industron.block.MiningTier;
import net.mads.industron.block.MiningTool;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

public final class MultiblockControllerDefinition {
    private static final String DEFAULT_CASING_TEXTURE = "block/machines/ino/casing";
    private static final List<MultiblockControllerDefinition> ALL = new ArrayList<>();

    private final String registryName;
    private final String displayName;
    private final Map<Side, String> sideTextures;
    private final Map<Side, Integer> sideTextureColors;
    private final Map<Side, List<String>> sideOverlays;
    private final MiningTier miningTier;
    private final EnumSet<MiningTool> miningTools;

    @Nullable
    private final String model;

    private MultiblockControllerDefinition(Builder builder) {
        this.registryName = builder.registryName;
        this.displayName = builder.displayName;
        this.sideTextures = Collections.unmodifiableMap(new EnumMap<>(builder.sideTextures));
        this.sideTextureColors = Collections.unmodifiableMap(new EnumMap<>(builder.sideTextureColors));

        EnumMap<Side, List<String>> overlays = new EnumMap<>(Side.class);
        builder.sideOverlays.forEach((side, frames) -> overlays.put(side, List.copyOf(frames)));
        this.sideOverlays = Collections.unmodifiableMap(overlays);
        this.miningTier = builder.miningTier;
        this.miningTools = builder.miningTools.clone();
        this.model = builder.model;

        validate();
    }

    public static Builder machine() {
        return new Builder();
    }

    public static Builder controller() {
        return machine();
    }

    public static MultiblockControllerDefinition controller(MultiblockControllerDefinition controller) {
        return controller;
    }

    public String registryName() {
        return registryName;
    }

    public String displayName() {
        return displayName;
    }

    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, registryName);
    }

    public Map<Side, String> sideTextures() {
        return sideTextures;
    }

    public String sideTexture(Side side) {
        return sideTextures.getOrDefault(side, DEFAULT_CASING_TEXTURE);
    }

    @Nullable
    public Integer sideTextureColor(Side side) {
        return sideTextureColors.get(side);
    }

    public boolean hasSideTextureColor(Side side) {
        return sideTextureColors.containsKey(side);
    }

    public Map<Side, List<String>> sideOverlays() {
        return sideOverlays;
    }

    public List<String> overlayFrames(Side side) {
        return sideOverlays.getOrDefault(side, List.of());
    }

    public boolean hasOverlay(Side side) {
        return !overlayFrames(side).isEmpty();
    }

    @Nullable
    public String idleOverlay(Side side) {
        List<String> frames = overlayFrames(side);
        return frames.isEmpty() ? null : frames.getFirst();
    }

    public List<String> activeOverlays(Side side) {
        List<String> frames = overlayFrames(side);
        return frames.size() <= 1 ? List.of() : frames.subList(1, frames.size());
    }

    public int activeOverlayFrameCount() {
        return sideOverlays.keySet().stream().mapToInt(side -> activeOverlays(side).size()).max().orElse(0);
    }

    public MiningTier miningTier() {
        return miningTier;
    }

    public Set<MiningTool> miningTools() {
        return Collections.unmodifiableSet(miningTools);
    }

    public float hardness() {
        return miningTier.hardness();
    }

    public float resistance() {
        return miningTier.resistance();
    }

    @Nullable
    public String model() {
        return model;
    }

    private void validate() {
        if (registryName == null || registryName.isBlank() || !ResourceLocation.isValidPath(registryName)) {
            throw new IllegalArgumentException("Invalid multiblock controller id: " + registryName);
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Multiblock controller display name cannot be blank: " + registryName);
        }
        for (Map.Entry<Side, String> entry : sideTextures.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                throw new IllegalArgumentException("Blank texture for " + entry.getKey() + " on controller " + registryName);
            }
        }
        for (Map.Entry<Side, List<String>> entry : sideOverlays.entrySet()) {
            if (entry.getValue().isEmpty() || entry.getValue().stream().anyMatch(frame -> frame == null || frame.isBlank())) {
                throw new IllegalArgumentException("Invalid overlay frames for " + entry.getKey() + " on controller " + registryName);
            }
        }
        if (model != null && model.isBlank()) {
            throw new IllegalArgumentException("Multiblock controller model cannot be blank: " + registryName);
        }
    }

    public static MultiblockControllerDefinition of(
            String registryName,
            String displayName,
            String casingTexture,
            String offOverlayTexture,
            String onOverlayTexture
    ) {
        return machine()
                .machineDefinition(Option.id(registryName))
                .machineDefinition(Option.displayName(displayName))
                .machineDefinition(Option.frontTexture(casingTexture))
                .machineDefinition(Option.backTexture(casingTexture))
                .machineDefinition(Option.leftTexture(casingTexture))
                .machineDefinition(Option.rightTexture(casingTexture))
                .machineDefinition(Option.topTexture(casingTexture))
                .machineDefinition(Option.bottomTexture(casingTexture))
                .machineDefinition(Option.frontOverlay(offOverlayTexture, onOverlayTexture))
                .build();
    }

    public static MultiblockControllerDefinition tinted(
            String registryName,
            String displayName,
            String casingTexture,
            String offOverlayTexture,
            String onOverlayTexture,
            int tintColor
    ) {
        return machine()
                .machineDefinition(Option.id(registryName))
                .machineDefinition(Option.displayName(displayName))
                .machineDefinition(Option.frontTexture(casingTexture).color(tintColor))
                .machineDefinition(Option.backTexture(casingTexture).color(tintColor))
                .machineDefinition(Option.leftTexture(casingTexture).color(tintColor))
                .machineDefinition(Option.rightTexture(casingTexture).color(tintColor))
                .machineDefinition(Option.topTexture(casingTexture).color(tintColor))
                .machineDefinition(Option.bottomTexture(casingTexture).color(tintColor))
                .machineDefinition(Option.frontOverlay(offOverlayTexture, onOverlayTexture))
                .build();
    }

    public static List<MultiblockControllerDefinition> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static void register(MultiblockControllerDefinition definition) {
        boolean alreadyRegistered = ALL.stream().anyMatch(existing -> existing.registryName().equals(definition.registryName()));
        if (!alreadyRegistered) {
            ALL.add(definition);
        }
    }

    public enum Side {
        FRONT,
        BACK,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM;

        public int tintIndex() {
            return ordinal();
        }

        @Nullable
        public static Side fromTintIndex(int tintIndex) {
            Side[] sides = values();
            return tintIndex >= 0 && tintIndex < sides.length ? sides[tintIndex] : null;
        }
    }

    @FunctionalInterface
    public interface Option {
        void apply(Builder builder);

        static Option id(String id) {
            return builder -> builder.registryName = id;
        }

        static Option displayName(String displayName) {
            return builder -> builder.displayName = displayName;
        }

        static TextureOption frontTexture(String texture) {
            return sideTexture(Side.FRONT, texture);
        }

        static TextureOption backTexture(String texture) {
            return sideTexture(Side.BACK, texture);
        }

        static TextureOption leftTexture(String texture) {
            return sideTexture(Side.LEFT, texture);
        }

        static TextureOption rightTexture(String texture) {
            return sideTexture(Side.RIGHT, texture);
        }

        static TextureOption topTexture(String texture) {
            return sideTexture(Side.TOP, texture);
        }

        static TextureOption bottomTexture(String texture) {
            return sideTexture(Side.BOTTOM, texture);
        }

        static Option frontOverlay(String idleOverlay, String... activeOverlays) {
            return sideOverlay(Side.FRONT, idleOverlay, activeOverlays);
        }

        static Option backOverlay(String idleOverlay, String... activeOverlays) {
            return sideOverlay(Side.BACK, idleOverlay, activeOverlays);
        }

        static Option leftOverlay(String idleOverlay, String... activeOverlays) {
            return sideOverlay(Side.LEFT, idleOverlay, activeOverlays);
        }

        static Option rightOverlay(String idleOverlay, String... activeOverlays) {
            return sideOverlay(Side.RIGHT, idleOverlay, activeOverlays);
        }

        static Option topOverlay(String idleOverlay, String... activeOverlays) {
            return sideOverlay(Side.TOP, idleOverlay, activeOverlays);
        }

        static Option bottomOverlay(String idleOverlay, String... activeOverlays) {
            return sideOverlay(Side.BOTTOM, idleOverlay, activeOverlays);
        }

        static Option model(String model) {
            return builder -> builder.model = Objects.requireNonNull(model);
        }

        static Option mineableWith(MiningTool tool, MiningTool... moreTools) {
            return builder -> {
                builder.miningTools = EnumSet.of(Objects.requireNonNull(tool));
                if (moreTools != null) {
                    Collections.addAll(builder.miningTools, moreTools);
                }
            };
        }

        static Option miningTier(MiningTier miningTier) {
            return builder -> builder.miningTier = Objects.requireNonNull(miningTier);
        }

        private static TextureOption sideTexture(Side side, String texture) {
            return new TextureOption(side, texture);
        }

        private static Option sideOverlay(Side side, String idleOverlay, String... activeOverlays) {
            return builder -> {
                List<String> frames = new ArrayList<>();
                frames.add(Objects.requireNonNull(idleOverlay));
                if (activeOverlays != null) {
                    Collections.addAll(frames, activeOverlays);
                }
                builder.sideOverlays.put(side, frames);
            };
        }

        final class TextureOption implements Option {
            private final Side side;
            private final String texture;
            private Integer color;

            private TextureOption(Side side, String texture) {
                this.side = Objects.requireNonNull(side);
                this.texture = Objects.requireNonNull(texture);
            }

            public TextureOption color(int color) {
                this.color = color;
                return this;
            }

            @Override
            public void apply(Builder builder) {
                builder.sideTextures.put(side, texture);
                if (color == null) {
                    builder.sideTextureColors.remove(side);
                } else {
                    builder.sideTextureColors.put(side, color);
                }
            }
        }
    }

    public static final class Builder {
        private String registryName;
        private String displayName;
        private final EnumMap<Side, String> sideTextures = new EnumMap<>(Side.class);
        private final EnumMap<Side, Integer> sideTextureColors = new EnumMap<>(Side.class);
        private final EnumMap<Side, List<String>> sideOverlays = new EnumMap<>(Side.class);
        private MiningTier miningTier = MiningTier.STONE;
        private EnumSet<MiningTool> miningTools = EnumSet.of(MiningTool.PICKAXE);
        private String model;

        private Builder() {
        }

        public Builder machineDefinition(Option option) {
            Objects.requireNonNull(option, "Multiblock controller option").apply(this);
            return this;
        }

        public Builder controllerDefinition(Option option) {
            return machineDefinition(option);
        }

        public MultiblockControllerDefinition build() {
            MultiblockControllerDefinition definition = new MultiblockControllerDefinition(this);
            register(definition);
            return definition;
        }
    }
}
