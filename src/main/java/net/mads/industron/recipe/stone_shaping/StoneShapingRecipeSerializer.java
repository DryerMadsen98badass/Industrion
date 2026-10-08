package net.mads.industron.recipe.stone_shaping;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class StoneShapingRecipeSerializer implements RecipeSerializer<StoneShapingRecipe> {
    private static final StreamCodec<RegistryFriendlyByteBuf, StoneShapingRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(StoneShapingRecipe.CODEC.codec());

    @Override
    public MapCodec<StoneShapingRecipe> codec() {
        return StoneShapingRecipe.CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, StoneShapingRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
