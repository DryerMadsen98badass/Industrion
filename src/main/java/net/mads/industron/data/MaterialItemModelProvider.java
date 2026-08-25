package net.mads.industron.data;

import net.mads.industron.Industron;
import net.mads.industron.item.SimpleItemDefinition;
import net.mads.industron.item.SimpleItems;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialMaterials;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialTextures;
import net.mads.industron.registry.FluidRegistry;
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
        registerMachineControlScheduleModel();
        registerMultiblockDevToolModel();
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
                    singleTexture(
                            name,
                            ResourceLocation.withDefaultNamespace("item/generated"),
                            "layer0",
                            material.customPartTexture(part)
                    );
                    if (net.mads.industron.material.MaterialFormGenerator.hasMagneticVariant(material, part)) {
                        getBuilder(part.magneticRegistryName(material))
                                .parent(new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("item/generated")))
                                .texture("layer0", material.customPartTexture(part))
                                .texture("layer1", magneticOverlay);
                    }
                    continue;
                }

                MaterialPart texturePart = part == MaterialPart.HOT_INGOT ? MaterialPart.INGOT : part;
                var textures = net.mads.industron.material.MaterialVariantResolver.itemTextures(material, texturePart);
                if (textures.isEmpty()) {
                    // The item is still registered. No texture/model is generated until a variant exists.
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
                if (part == MaterialPart.HOT_INGOT) {
                    var hotOverlay = net.mads.industron.material.MaterialVariantResolver.hotIngotOverlayTexture(material);
                    if (hotOverlay.isPresent()) model.texture("layer" + nextLayer, hotOverlay.get());
                }

                if (net.mads.industron.material.MaterialFormGenerator.hasMagneticVariant(material, part)) {
                    var magneticModel = getBuilder(part.magneticRegistryName(material))
                            .parent(new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("item/generated")))
                            .texture("layer0", textures.get().base());
                    int magneticLayer = 1;
                    if (textures.get().secondary().isPresent()) {
                        magneticModel.texture("layer" + magneticLayer++, textures.get().secondary().get());
                    }
                    if (textures.get().overlay().isPresent()) {
                        magneticModel.texture("layer" + magneticLayer++, textures.get().overlay().get());
                    }
                    if (part == MaterialPart.HOT_INGOT) {
                        var hotOverlay = net.mads.industron.material.MaterialVariantResolver.hotIngotOverlayTexture(material);
                        if (hotOverlay.isPresent()) magneticModel.texture("layer" + magneticLayer++, hotOverlay.get());
                    }
                    magneticModel.texture("layer" + magneticLayer, magneticOverlay);
                }
            }
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

    private void registerSimpleItemModels() {
        for (SimpleItemDefinition definition : SimpleItems.ALL) {
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
