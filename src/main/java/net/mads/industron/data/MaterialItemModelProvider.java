package net.mads.industron.data;

import net.mads.industron.Industron;
import net.mads.industron.item.SimpleItemDefinition;
import net.mads.industron.item.SimpleItems;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.defenitions.PlantMaterials;
import net.mads.industron.material.plant.PlantMaterialGenerator;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.plant.PlantProcessingPlanner;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialTextures;
import net.mads.industron.registry.FluidRegistry;
import net.mads.industron.recipe.recipes.assembly.ToolDefinitions;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;
import java.util.Optional;

public class MaterialItemModelProvider extends ItemModelProvider {

    private static final ResourceLocation FIRED_BUCKET_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    Industron.MOD_ID,
                    "item/standalone/bucket_brick"
            );
    private static final ResourceLocation MAGNETIC_OVERLAY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    Industron.MOD_ID,
                    "item/material_sets/magnetic_overlay/variant_1/overlay"
            );

    private final ExistingFileHelper existingFileHelper;

    public MaterialItemModelProvider(
            PackOutput output,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                Industron.MOD_ID,
                existingFileHelper
        );

        this.existingFileHelper = existingFileHelper;
    }

    @Override
    protected void registerModels() {
        registerMaterialItemModels();
        registerFluidBucketModels();
        registerSimpleItemModels();
        for (var definition : net.mads.industron.material.organism.BiologicalItemCatalog.ALL) {
            if(net.mads.industron.material.organism.BiologicalTextureTemplates.SOURCES.containsKey(definition.texture())) {
                existingFileHelper.trackGenerated(ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,definition.texture()),
                        net.neoforged.neoforge.client.model.generators.ModelProvider.TEXTURE);
            }
            singleTexture(definition.id(), ResourceLocation.withDefaultNamespace("item/generated"),
                    "layer0", ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, definition.texture()));
        }
        registerPlantMaterialModels();
        registerPlantProcessIntermediateModels();
        registerComposedToolModels();
        registerMachineControlScheduleModel();
        registerMultiblockDevToolModel();
        registerCreativeGogglesModel();
    }

    private void registerMaterialItemModels() {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            for (MaterialPart part : material.parts()) {
                if (material.hasExistingPart(part) || !part.isItem()
                        || net.mads.industron.energy.WireThickness.ALL.stream().anyMatch(thickness -> thickness.materialPart() == part)) {
                    continue;
                }

                String name = part.registryName(material);
                ResourceLocation magneticOverlay = net.mads.industron.material.MaterialVariantResolver
                        .magneticOverlayTexture(material, part)
                        .orElse(MAGNETIC_OVERLAY_TEXTURE);

                if (material.hasCustomPartTexture(part)) {
                    var customModel = singleTexture(
                            name,
                            ResourceLocation.withDefaultNamespace("item/generated"),
                            "layer0",
                            material.customPartTexture(part)
                    );
                    ToolPartModelAlignment.apply(customModel, part);
                    if (net.mads.industron.material.MaterialFormGenerator.hasMagneticVariant(material, part)) {
                        getBuilder(part.magneticRegistryName(material))
                                .parent(new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("item/generated")))
                                .texture("layer0", material.customPartTexture(part))
                                .texture("layer1", magneticOverlay);
                    }
                    continue;
                }

                MaterialPart texturePart = net.mads.industron.material.MaterialVariantResolver.coldTexturePart(part);
                var textures = net.mads.industron.material.MaterialVariantResolver.itemTextures(material, texturePart);
                if (textures.isEmpty()) {
                    ResourceLocation clayFallback = switch (part) {
                        case CLAY -> ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,
                                "item/material_sets/clay/normal/variant_1/base");
                        case UNFIRED_BRICK -> ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,
                                "item/material_sets/brick/unfired/variant_1/base");
                        case DRIED_UNFIRED_BRICK -> ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,
                                "item/material_sets/brick/dried/variant_1/base");
                        case BRICK -> ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,
                                "item/material_sets/brick/normal/variant_1/base");
                        case CRACKED_BRICK -> ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,
                                "item/material_sets/brick/cracked/variant_1/base");
                        default -> null;
                    };
                    if (clayFallback != null) {
                        singleTexture(name, ResourceLocation.withDefaultNamespace("item/generated"), "layer0", clayFallback);
                    }
                    continue;
                }

                var model = getBuilder(name)
                        .parent(new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("item/generated")))
                        .texture("layer0", textures.get().base());
                int nextLayer = 1;
                if (textures.get().secondary().isPresent()) {
                    model.texture("layer" + nextLayer++, textures.get().secondary().get());
                }
                if (textures.get().overlay().isPresent()) {
                    model.texture("layer" + nextLayer++, textures.get().overlay().get());
                }
                var hotOverlay = net.mads.industron.material.MaterialVariantResolver.hotOverlayTexture(material, part);
                if (hotOverlay.isPresent()) model.texture("layer" + nextLayer, hotOverlay.get());
                ToolPartModelAlignment.apply(model, part);

                if (net.mads.industron.material.MaterialFormGenerator.hasMagneticVariant(material, part)) {
                    int magneticLayer = 1;
                    if (textures.get().secondary().isPresent()) {
                        magneticLayer++;
                    }
                    if (textures.get().overlay().isPresent()) {
                        magneticLayer++;
                    }
                    if (hotOverlay.isPresent()) {
                        magneticLayer++;
                    }

                    // Magnetic items reuse the normal item's complete parent/layer chain and
                    // add only the magnetic overlay. This keeps every magnetic item fully
                    // functional while avoiding a second copy of all normal layers.
                    getBuilder(part.magneticRegistryName(material))
                            .parent(getBuilder(name))
                            .texture("layer" + magneticLayer, magneticOverlay);
                }
            }
        }
    }


    private void registerPlantMaterialModels() {
        for (var material : PlantMaterials.ALL) {
            for (PlantPart part : PlantMaterialGenerator.generatedItemForms(material)) {
                ResourceLocation texture;
                if (material.hasCustomPartTexture(part)) {
                    texture = material.customPartTexture(part);
                } else if (part == PlantPart.FIBER) {
                    texture = ResourceLocation.fromNamespaceAndPath(
                            Industron.MOD_ID, "item/plants/parts/fiber"
                    );
                } else {
                    texture = ResourceLocation.withDefaultNamespace("item/string");
                }
                singleTexture(
                        part.registryName(material),
                        ResourceLocation.withDefaultNamespace("item/generated"),
                        "layer0",
                        texture
                );
            }
        }
    }

    private void registerPlantProcessIntermediateModels() {
        for (var intermediate : PlantProcessingPlanner.allRequiredIntermediates()) {
            if (!intermediate.isSolid()) continue;
            ResourceLocation texture = intermediate.itemTexture().orElseGet(() ->
                    ResourceLocation.fromNamespaceAndPath(
                            Industron.MOD_ID,
                            "item/plants/intermediates/generic"
                    )
            );
            singleTexture(
                    intermediate.id(),
                    ResourceLocation.withDefaultNamespace("item/generated"),
                    "layer0",
                    texture
            );
        }
    }

    private void registerComposedToolModels() {
        for (var definition : ToolDefinitions.ALL) {
            if (!definition.isAssembledTool() || !definition.isFinishedToolEnabled()) continue;
            getBuilder(definition.id())
                    .parent(new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("builtin/entity")));
        }
    }

    private void registerFluidBucketModels() {
        singleTexture(
                "fired_bucket",
                ResourceLocation.withDefaultNamespace(
                        "item/generated"
                ),
                "layer0",
                FIRED_BUCKET_TEXTURE
        );

        firedVanillaBucketModel(
                "fired_water_bucket",
                Fluids.WATER
        );

        firedVanillaBucketModel(
                "fired_lava_bucket",
                Fluids.LAVA
        );

        for (FluidRegistry.RegisteredFluid fluid
                : FluidRegistry.allFluids()) {

            normalFluidBucketModel(fluid);
            firedFluidBucketModel(fluid);
        }
    }

    private void normalFluidBucketModel(
            FluidRegistry.RegisteredFluid fluid
    ) {
        getBuilder(
                fluid.definition().bucketName()
        )
                .parent(
                        new ModelFile.UncheckedModelFile(
                                ResourceLocation.fromNamespaceAndPath(
                                        "neoforge",
                                        "item/bucket_drip"
                                )
                        )
                )
                .customLoader(
                        DynamicFluidContainerModelBuilder::begin
                )
                .fluid(
                        fluid.source().get()
                )
                .flipGas(
                        fluid.definition().isGas()
                )
                .applyFluidLuminosity(
                        fluid.definition().lightLevel() > 0
                );
    }

    private void firedFluidBucketModel(
            FluidRegistry.RegisteredFluid fluid
    ) {
        getBuilder(
                fluid.firedBucket().getId().getPath()
        )
                .parent(
                        new ModelFile.UncheckedModelFile(
                                ResourceLocation.fromNamespaceAndPath(
                                        "neoforge",
                                        "item/bucket_drip"
                                )
                        )
                )
                .texture(
                        "base",
                        FIRED_BUCKET_TEXTURE
                )
                .customLoader(
                        DynamicFluidContainerModelBuilder::begin
                )
                .fluid(
                        fluid.source().get()
                )
                .flipGas(
                        fluid.definition().isGas()
                )
                .applyFluidLuminosity(
                        fluid.definition().lightLevel() > 0
                );
    }

    private void firedVanillaBucketModel(
            String id,
            Fluid fluid
    ) {
        getBuilder(id)
                .parent(
                        new ModelFile.UncheckedModelFile(
                                ResourceLocation.fromNamespaceAndPath(
                                        "neoforge",
                                        "item/bucket_drip"
                                )
                        )
                )
                .texture(
                        "base",
                        FIRED_BUCKET_TEXTURE
                )
                .customLoader(
                        DynamicFluidContainerModelBuilder::begin
                )
                .fluid(fluid);
    }

    private void registerMachineControlScheduleModel() {
        singleTexture(
                "machine_control_schedule",
                ResourceLocation.withDefaultNamespace("item/generated"),
                "layer0",
                ResourceLocation.fromNamespaceAndPath(
                        Industron.MOD_ID,
                        "item/standalone/machine_controll_schedule"
                )
        );
    }

    private void registerMultiblockDevToolModel() {
        singleTexture(
                "multiblock_dev_tool",
                ResourceLocation.withDefaultNamespace("item/generated"),
                "layer0",
                ResourceLocation.fromNamespaceAndPath(
                        Industron.MOD_ID,
                        "item/standalone/multiblock_dev_tool"
                )
        );
    }

    private void registerCreativeGogglesModel() {
        getBuilder("creative_goggles")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.fromNamespaceAndPath("create", "item/goggles")
                ));
    }

    private void registerSimpleItemModels() {
        for (SimpleItemDefinition definition : SimpleItems.ALL) {
            if (definition.id().equals("flint_and_pebble")) {
                registerFlintAndPebbleModel();
                continue;
            }

            ResourceLocation texture =
                    resolveSimpleItemTexture(definition);

            singleTexture(
                    definition.id(),
                    ResourceLocation.withDefaultNamespace(
                            "item/generated"
                    ),
                    "layer0",
                    texture
            );
        }
    }

    /**
     * Uses the same stone texture and 8x3x8 pebble geometry as Stone Pebble, then places
     * the existing vanilla Flint texture on a 4x4 face directly above it (50% width/height).
     */
    private void registerFlintAndPebbleModel() {
        var model = getBuilder("flint_and_pebble")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.withDefaultNamespace("block/block")
                ))
                .texture("particle", ResourceLocation.withDefaultNamespace("block/stone"))
                .texture("stone", ResourceLocation.withDefaultNamespace("block/stone"))
                .texture("flint", ResourceLocation.withDefaultNamespace("item/flint"));

        model.element()
                .from(4, 0, 4)
                .to(12, 3, 12)
                .allFaces((direction, face) -> face.texture("#stone"));

        model.element()
                .from(6, 3.01F, 6)
                .to(10, 3.10F, 10)
                .face(Direction.UP)
                .uvs(0, 0, 16, 16)
                .texture("#flint")
                .end();
    }

    private ResourceLocation resolveSimpleItemTexture(
            SimpleItemDefinition definition
    ) {
        String texturePath = definition.texture();

        if (texturePath.equals(definition.id())) {
            return findSimpleItemTexture(
                    definition.id()
            );
        }

        if (texturePath.contains(":")) {
            ResourceLocation texture =
                    ResourceLocation.tryParse(texturePath);

            if (texture == null) {
                throw new IllegalStateException(
                        "Invalid texture for simple item '"
                                + definition.id()
                                + "': "
                                + texturePath
                );
            }

            return texture;
        }

        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                texturePath
        );
    }

    private ResourceLocation findSimpleItemTexture(
            String itemId
    ) {
        for (String folder : SIMPLE_ITEM_TEXTURE_FOLDERS) {
            ResourceLocation texture =
                    ResourceLocation.fromNamespaceAndPath(
                            Industron.MOD_ID,
                            folder + "/" + itemId
                    );

            boolean exists = existingFileHelper.exists(
                    texture,
                    PackType.CLIENT_RESOURCES,
                    ".png",
                    "textures"
            );

            if (exists) {
                return texture;
            }
        }

        throw new IllegalStateException(
                "Could not find texture for simple item '"
                        + itemId
                        + "'. Expected a file named '"
                        + itemId
                        + ".png' in one of these folders: "
                        + SIMPLE_ITEM_TEXTURE_FOLDERS
        );
    }

    private static final List<String>
            SIMPLE_ITEM_TEXTURE_FOLDERS = List.of(
            "item/standalone/materials",
            "block/machines/machines/kinetic/sifter",
            "item/standalone"
    );
}
