package net.mads.industron.recipe.chiseling;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ChiselingRecipeSerializer implements RecipeSerializer<ChiselingRecipe> {
    private static final StreamCodec<RegistryFriendlyByteBuf, ChiselingRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(ChiselingRecipe.CODEC.codec());

    @Override
    public MapCodec<ChiselingRecipe> codec() {
        return ChiselingRecipe.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ChiselingRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
