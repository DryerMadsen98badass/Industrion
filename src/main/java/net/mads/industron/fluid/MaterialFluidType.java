package net.mads.industron.fluid;

import net.mads.industron.Industron;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

public class MaterialFluidType extends FluidType {
    private final ResourceLocation texture;
    private final int color;

    public MaterialFluidType(IndustrialFluid fluid) {
        super(FluidType.Properties.create()
                .descriptionId("fluid_type." + Industron.MOD_ID + "." + fluid.registryName())
                .temperature(fluid.temperature())
                .density(fluid.density())
                .viscosity(fluid.viscosity())
                .lightLevel(fluid.lightLevel())
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY));
        this.texture = net.mads.industron.material.MaterialVariantResolver
                .fluidTexture(fluid, "base")
                .orElse(ResourceLocation.fromNamespaceAndPath(
                        Industron.MOD_ID,
                        "block/material_sets/" + fluid.textureName() + "/variant_1/base"
                ));
        this.color = 0xFF000000 | fluid.color();
    }

    public ResourceLocation texture() {
        return texture;
    }

    public int color() {
        return color;
    }
}
