package net.mads.industron.data;

import net.mads.industron.Industron;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.StructureMaterialVariantResolver;
import net.mads.industron.material.structure.StructureMaterials;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Generates item models for structure-material dust and wood-pulp forms. */
public final class StructureMaterialItemModelProvider extends ItemModelProvider {

    public StructureMaterialItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Industron.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Structure Material Item Models: " + Industron.MOD_ID;
    }

    @Override
    protected void registerModels() {
        for (StructureMaterial material : StructureMaterials.ALL) {
            for (MaterialPart part : StructureMaterialGenerator.generatedItemForms(material)) {
                StructureMaterialVariantResolver.itemTextures(material, part).ifPresent(textures -> {
                    var model = getBuilder(part.registryName(material))
                            .parent(new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("item/generated")))
                            .texture("layer0", textures.base());
                    textures.secondary().ifPresent(texture -> model.texture("layer1", texture));
                    textures.overlay().ifPresent(texture -> model.texture("layer2", texture));
                });
            }
        }
    }
}
