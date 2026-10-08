package net.mads.industron.material.plant;

import net.mads.industron.Industron;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Shared tags for plant-processing outputs. */
public final class PlantTags {
    public static final TagKey<Item> PLANT_FIBERS = itemTag("plant_fibers");
    public static final TagKey<Item> STRINGS = itemTag("strings");
    public static final TagKey<Item> FERTILIZERS = itemTag("fertilizers");

    private PlantTags() {
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, path));
    }
}
