package net.mads.industron.material.structure;

import java.util.Map;
import java.util.Optional;

public record StructureBlockDefinition(
        StructureMaterial material,
        String registryName,
        String displayName,
        Shape shape,
        String textureFile,
        Optional<String> topTextureFile,
        Optional<String> bottomTextureFile,
        Optional<String> itemTextureFile,
        Optional<String> baseRegistryName,
        Optional<StructureMaterialPart> part,
        Optional<StructureMaterialPart> basePart,
        ModelKind modelKind,
        Optional<String> modelTemplate,
        Map<String, String> textureFiles
) {
    /** Compatibility constructor used by the regular wood and stone generators. */
    public StructureBlockDefinition(
            StructureMaterial material,
            String registryName,
            String displayName,
            Shape shape,
            String textureFile,
            Optional<String> topTextureFile,
            Optional<String> bottomTextureFile,
            Optional<String> itemTextureFile,
            Optional<String> baseRegistryName,
            Optional<StructureMaterialPart> part,
            Optional<StructureMaterialPart> basePart
    ) {
        this(
                material,
                registryName,
                displayName,
                shape,
                textureFile,
                topTextureFile,
                bottomTextureFile,
                itemTextureFile,
                baseRegistryName,
                part,
                basePart,
                ModelKind.DEFAULT,
                Optional.empty(),
                Map.of()
        );
    }

    public StructureBlockDefinition {
        if (material == null) {
            throw new IllegalArgumentException("Structure block material cannot be null");
        }
        if (registryName == null || registryName.isBlank()) {
            throw new IllegalArgumentException("Structure block registry name cannot be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Structure block display name cannot be blank");
        }
        if (shape == null) {
            throw new IllegalArgumentException("Structure block shape cannot be null: " + registryName);
        }
        if (textureFile == null || textureFile.isBlank()) {
            throw new IllegalArgumentException("Structure block texture cannot be blank: " + registryName);
        }
        topTextureFile = topTextureFile == null ? Optional.empty() : topTextureFile;
        bottomTextureFile = bottomTextureFile == null ? Optional.empty() : bottomTextureFile;
        itemTextureFile = itemTextureFile == null ? Optional.empty() : itemTextureFile;
        baseRegistryName = baseRegistryName == null ? Optional.empty() : baseRegistryName;
        part = part == null ? Optional.empty() : part;
        basePart = basePart == null ? Optional.empty() : basePart;
        modelKind = modelKind == null ? ModelKind.DEFAULT : modelKind;
        modelTemplate = modelTemplate == null ? Optional.empty() : modelTemplate;
        textureFiles = textureFiles == null ? Map.of() : Map.copyOf(textureFiles);
    }

    public Optional<String> texture(String key) {
        return Optional.ofNullable(textureFiles.get(key));
    }

    public String requiredTexture(String key) {
        return texture(key).orElseThrow(() -> new IllegalStateException(
                registryName + " is missing the required texture slot '" + key + "'"
        ));
    }

    public enum Shape {
        CUBE,
        PILLAR,
        SLAB,
        STAIRS,
        WALL,
        FENCE,
        FENCE_GATE,
        BUTTON,
        PRESSURE_PLATE,
        DOOR,
        TRAPDOOR,
        LEAVES,
        SAPLING,
        WINDOW,
        BARS,
        BRACKET,
        BULB,
        LADDER,
        SCAFFOLD,
        WINDOW_PANE
    }

    /**
     * Selects the exact model family while Shape selects the runtime block class.
     * This separation is needed because, for example, vanilla and Create doors
     * share DoorBlock state properties but use different geometry and texture slots.
     */
    public enum ModelKind {
        DEFAULT,
        CUTOUT_CUBE,
        CREATE_BARS,
        VANILLA_BARS,
        CREATE_BRACKET,
        COPPER_BULB,
        CREATE_DOOR,
        VANILLA_DOOR,
        CREATE_LADDER,
        CREATE_SCAFFOLD,
        CREATE_TRAIN_TRAPDOOR,
        VANILLA_TRAPDOOR,
        CREATE_WINDOW,
        CREATE_WINDOW_PANE
    }
}
