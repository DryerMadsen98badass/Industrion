package net.mads.industron.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class CERecipeSerializer implements RecipeSerializer<CERecipe> {
    private static final StreamCodec<RegistryFriendlyByteBuf, CERecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CERecipe.CODEC.codec());

    @Override
    public MapCodec<CERecipe> codec() {
        return CERecipe.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, CERecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
