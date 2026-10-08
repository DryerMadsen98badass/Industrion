package net.mads.industron.material.defenitions;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.plant.PlantMaterial;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.WoodMaterial;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import static net.mads.industron.material.defenitions.CompoundMaterials.DULCARA;
import static net.mads.industron.material.defenitions.CompoundMaterials.LIGNARA;
import static net.mads.industron.material.defenitions.CompoundMaterials.RESYRA;
import static net.mads.industron.material.defenitions.CompoundMaterials.SYLVARA;

/**
 * Canonical organic definitions for vanilla plants and plant products.
 *
 * <p>These definitions give existing items a deterministic material identity and explicit plant
 * climate profiles. PlantPart + .contains(...) drive processing, compost and fuel generation,
 * while calendar growth consumes the same climate definitions. Every plant declares its composition directly through .contains(...); there is
 * no fixed category composition layer, so definitions may add any registered components they need.</p>
 */
public final class PlantMaterials {
    public static final PlantMaterial WHEAT = plant("wheat", "Wheat")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 1),
                    component(DULCARA, 4)
            )
            .existing(PlantPart.CROP, "minecraft:wheat")
            .existing(PlantPart.SEEDS, "minecraft:wheat_seeds")
            .existing(PlantPart.BALE, "minecraft:hay_block").climate(new net.mads.industron.climate.PlantClimateProfile(20,2,18,34,.35,2,2,false));

    public static final PlantMaterial CARROT = plant("carrot", "Carrot")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 1),
                    component(RESYRA, 1),
                    component(DULCARA, 5)
            ).existing(PlantPart.ROOT, "minecraft:carrot").climate(new net.mads.industron.climate.PlantClimateProfile(20,3,18,32,.4,3,2,false));
    public static final PlantMaterial POTATO = plant("potato", "Potato")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 1),
                    component(RESYRA, 1),
                    component(DULCARA, 5)
            ).existing(PlantPart.ROOT, "minecraft:potato").climate(new net.mads.industron.climate.PlantClimateProfile(20,4,16,30,.45,1,1,false));
    public static final PlantMaterial POISONOUS_POTATO = plant("poisonous_potato", "Poisonous Potato")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 1),
                    component(RESYRA, 1),
                    component(DULCARA, 5)
            ).existing(PlantPart.ROOT, "minecraft:poisonous_potato");
    public static final PlantMaterial BEETROOT = plant("beetroot", "Beetroot")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 1),
                    component(RESYRA, 1),
                    component(DULCARA, 5)
            )
            .existing(PlantPart.ROOT, "minecraft:beetroot")
            .existing(PlantPart.SEEDS, "minecraft:beetroot_seeds").climate(new net.mads.industron.climate.PlantClimateProfile(20,2,18,32,.35,2,2,false));

    public static final PlantMaterial SUGAR_CANE = plant("sugar_cane", "Sugar Cane")
            .contains(
                    component(LIGNARA, 2),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 1)
            ).existing(PlantPart.STEM, "minecraft:sugar_cane").climate(net.mads.industron.climate.PlantClimateProfile.TROPICAL);
    public static final PlantMaterial CACTUS = plant("cactus", "Cactus")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 1),
                    component(DULCARA, 4)
            ).existing(PlantPart.STEM, "minecraft:cactus").climate(net.mads.industron.climate.PlantClimateProfile.DESERT);
    public static final PlantMaterial BAMBOO = plant("bamboo_plant", "Bamboo Plant")
            .contains(WoodMaterials.BAMBOO.components().toArray(MaterialComponent[]::new))
            .existing(PlantPart.STEM, "minecraft:bamboo").climate(net.mads.industron.climate.PlantClimateProfile.TROPICAL);
    public static final PlantMaterial COCOA = plant("cocoa", "Cocoa")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 1),
                    component(RESYRA, 1),
                    component(DULCARA, 5)
            ).existing(PlantPart.FRUIT, "minecraft:cocoa_beans").climate(net.mads.industron.climate.PlantClimateProfile.TROPICAL);

    public static final PlantMaterial MELON = plant("melon", "Melon")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 1),
                    component(RESYRA, 1),
                    component(DULCARA, 5)
            )
            .existing(PlantPart.COMPRESSED_BLOCK, "minecraft:melon")
            .existing(PlantPart.FRUIT, "minecraft:melon_slice")
            .existing(PlantPart.SEEDS, "minecraft:melon_seeds");
    public static final PlantMaterial PUMPKIN = plant("pumpkin", "Pumpkin")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 1),
                    component(RESYRA, 1),
                    component(DULCARA, 5)
            )
            .existing(PlantPart.FRUIT_BLOCK, "minecraft:pumpkin")
            .existing(PlantPart.SEEDS, "minecraft:pumpkin_seeds");
    public static final PlantMaterial APPLE = plant("apple", "Apple")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 1),
                    component(RESYRA, 1),
                    component(DULCARA, 5)
            ).existing(PlantPart.FRUIT, "minecraft:apple");
    public static final PlantMaterial SWEET_BERRY = plant("sweet_berry", "Sweet Berry")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 1),
                    component(RESYRA, 1),
                    component(DULCARA, 5)
            ).existing(PlantPart.FRUIT, "minecraft:sweet_berries").climate(new net.mads.industron.climate.PlantClimateProfile(20,0,18,32,.4,3,2,true));
    public static final PlantMaterial GLOW_BERRY = plant("glow_berry", "Glow Berry")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 1),
                    component(RESYRA, 1),
                    component(DULCARA, 5)
            ).existing(PlantPart.FRUIT, "minecraft:glow_berries");

    public static final PlantMaterial KELP = plant("kelp", "Kelp")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.AQUATIC, "minecraft:kelp")
            .existing(PlantPart.DRIED, "minecraft:dried_kelp")
            .existing(PlantPart.BALE, "minecraft:dried_kelp_block").climate(net.mads.industron.climate.PlantClimateProfile.AQUATIC);
    public static final PlantMaterial SEAGRASS = plant("seagrass", "Seagrass")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            ).existing(PlantPart.AQUATIC, "minecraft:seagrass");
    public static final PlantMaterial LILY_PAD = plant("lily_pad", "Lily Pad")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            ).existing(PlantPart.AQUATIC, "minecraft:lily_pad");
    public static final PlantMaterial SEA_PICKLE = plant("sea_pickle", "Sea Pickle")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            ).existing(PlantPart.AQUATIC, "minecraft:sea_pickle");

    public static final PlantMaterial SHORT_GRASS = plant("short_grass", "Short Grass")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            ).existing(PlantPart.PLANT, "minecraft:short_grass");
    public static final PlantMaterial TALL_GRASS = plant("tall_grass", "Tall Grass")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            ).existing(PlantPart.PLANT, "minecraft:tall_grass");
    public static final PlantMaterial FERN = plant("fern", "Fern")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            ).existing(PlantPart.PLANT, "minecraft:fern");
    public static final PlantMaterial LARGE_FERN = plant("large_fern", "Large Fern")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            ).existing(PlantPart.PLANT, "minecraft:large_fern");
    public static final PlantMaterial DEAD_BUSH = plant("dead_bush", "Dead Bush")
            .contains(
                    component(LIGNARA, 2),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 1)
            ).existing(PlantPart.PLANT, "minecraft:dead_bush");

    public static final PlantMaterial VINES = plant("vines", "Vines")
            .contains(
                    component(LIGNARA, 2),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 1)
            ).existing(PlantPart.VINE, "minecraft:vine");
    public static final PlantMaterial WEEPING_VINES = plant("weeping_vines", "Weeping Vines")
            .contains(
                    component(LIGNARA, 2),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 1)
            ).existing(PlantPart.VINE, "minecraft:weeping_vines");
    public static final PlantMaterial TWISTING_VINES = plant("twisting_vines", "Twisting Vines")
            .contains(
                    component(LIGNARA, 2),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 1)
            ).existing(PlantPart.VINE, "minecraft:twisting_vines");
    public static final PlantMaterial HANGING_ROOTS = plant("hanging_roots", "Hanging Roots")
            .contains(
                    component(LIGNARA, 2),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 1)
            ).existing(PlantPart.ROOT, "minecraft:hanging_roots");
    public static final PlantMaterial MANGROVE_ROOTS = plant("mangrove_roots", "Mangrove Roots")
            .contains(
                    component(LIGNARA, 2),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 1)
            ).existing(PlantPart.ROOT, "minecraft:mangrove_roots");
    public static final PlantMaterial CRIMSON_ROOTS = plant("crimson_roots", "Crimson Roots")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 4),
                    component(DULCARA, 1)
            ).existing(PlantPart.ROOT, "minecraft:crimson_roots");
    public static final PlantMaterial WARPED_ROOTS = plant("warped_roots", "Warped Roots")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 4),
                    component(DULCARA, 1)
            ).existing(PlantPart.ROOT, "minecraft:warped_roots");
    public static final PlantMaterial NETHER_SPROUTS = plant("nether_sprouts", "Nether Sprouts")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 4),
                    component(DULCARA, 1)
            ).existing(PlantPart.PLANT, "minecraft:nether_sprouts");

    public static final PlantMaterial SMALL_DRIPLEAF = plant("small_dripleaf", "Small Dripleaf")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            ).existing(PlantPart.PLANT, "minecraft:small_dripleaf");
    public static final PlantMaterial BIG_DRIPLEAF = plant("big_dripleaf", "Big Dripleaf")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            ).existing(PlantPart.PLANT, "minecraft:big_dripleaf");
    public static final PlantMaterial SPORE_BLOSSOM = plant("spore_blossom", "Spore Blossom")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            ).existing(PlantPart.FLOWER, "minecraft:spore_blossom");
    public static final PlantMaterial GLOW_LICHEN = plant("glow_lichen", "Glow Lichen")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 5),
                    component(RESYRA, 1),
                    component(DULCARA, 1)
            ).existing(PlantPart.PLANT, "minecraft:glow_lichen");
    public static final PlantMaterial MOSS = plant("moss", "Moss")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 5),
                    component(RESYRA, 1),
                    component(DULCARA, 1)
            )
            .existing(PlantPart.BLOCK, "minecraft:moss_block")
            .existing(PlantPart.CARPET, "minecraft:moss_carpet");
    public static final PlantMaterial AZALEA = plant("azalea", "Azalea")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.PLANT, "minecraft:azalea")
            .existing(PlantPart.LEAVES, "minecraft:azalea_leaves");
    public static final PlantMaterial FLOWERING_AZALEA = plant("flowering_azalea", "Flowering Azalea")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.PLANT, "minecraft:flowering_azalea")
            .existing(PlantPart.LEAVES, "minecraft:flowering_azalea_leaves");
    public static final PlantMaterial PINK_PETALS = plant("pink_petals", "Pink Petals")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            ).existing(PlantPart.FLOWER, "minecraft:pink_petals");

    public static final PlantMaterial DANDELION = plant("dandelion", "Dandelion")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:dandelion");
    public static final PlantMaterial POPPY = plant("poppy", "Poppy")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:poppy");
    public static final PlantMaterial BLUE_ORCHID = plant("blue_orchid", "Blue Orchid")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:blue_orchid");
    public static final PlantMaterial ALLIUM = plant("allium", "Allium")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:allium");
    public static final PlantMaterial AZURE_BLUET = plant("azure_bluet", "Azure Bluet")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:azure_bluet");
    public static final PlantMaterial RED_TULIP = plant("red_tulip", "Red Tulip")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:red_tulip");
    public static final PlantMaterial ORANGE_TULIP = plant("orange_tulip", "Orange Tulip")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:orange_tulip");
    public static final PlantMaterial WHITE_TULIP = plant("white_tulip", "White Tulip")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:white_tulip");
    public static final PlantMaterial PINK_TULIP = plant("pink_tulip", "Pink Tulip")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:pink_tulip");
    public static final PlantMaterial OXEYE_DAISY = plant("oxeye_daisy", "Oxeye Daisy")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:oxeye_daisy");
    public static final PlantMaterial CORNFLOWER = plant("cornflower", "Cornflower")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:cornflower");
    public static final PlantMaterial LILY_OF_THE_VALLEY = plant("lily_of_the_valley", "Lily of the Valley")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:lily_of_the_valley");
    public static final PlantMaterial WITHER_ROSE = plant("wither_rose", "Wither Rose")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 4),
                    component(DULCARA, 1)
            ).existing(PlantPart.FLOWER, "minecraft:wither_rose");
    public static final PlantMaterial SUNFLOWER = plant("sunflower", "Sunflower")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:sunflower");
    public static final PlantMaterial LILAC = plant("lilac", "Lilac")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:lilac");
    public static final PlantMaterial ROSE_BUSH = plant("rose_bush", "Rose Bush")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:rose_bush");
    public static final PlantMaterial PEONY = plant("peony", "Peony")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:peony");
    public static final PlantMaterial TORCHFLOWER = plant("torchflower", "Torchflower")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:torchflower")
            .existing(PlantPart.SEEDS, "minecraft:torchflower_seeds");
    public static final PlantMaterial PITCHER_PLANT = plant("pitcher_plant", "Pitcher Plant")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 4),
                    component(RESYRA, 1),
                    component(DULCARA, 2)
            )
            .existing(PlantPart.FLOWER, "minecraft:pitcher_plant")
            .existing(PlantPart.SEEDS, "minecraft:pitcher_pod");

    public static final PlantMaterial BROWN_MUSHROOM = plant("brown_mushroom", "Brown Mushroom")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 4),
                    component(DULCARA, 1)
            )
            .existing(PlantPart.FUNGUS, "minecraft:brown_mushroom")
            .existing(PlantPart.BLOCK, "minecraft:brown_mushroom_block");
    public static final PlantMaterial RED_MUSHROOM = plant("red_mushroom", "Red Mushroom")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 4),
                    component(DULCARA, 1)
            )
            .existing(PlantPart.FUNGUS, "minecraft:red_mushroom")
            .existing(PlantPart.BLOCK, "minecraft:red_mushroom_block");
    public static final PlantMaterial MUSHROOM_STEM = plant("mushroom_stem", "Mushroom Stem")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 4),
                    component(DULCARA, 1)
            ).existing(PlantPart.BLOCK, "minecraft:mushroom_stem");
    public static final PlantMaterial CRIMSON_FUNGUS = plant("crimson_fungus", "Crimson Fungus")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 4),
                    component(DULCARA, 1)
            ).existing(PlantPart.FUNGUS, "minecraft:crimson_fungus");
    public static final PlantMaterial WARPED_FUNGUS = plant("warped_fungus", "Warped Fungus")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 4),
                    component(DULCARA, 1)
            ).existing(PlantPart.FUNGUS, "minecraft:warped_fungus");
    public static final PlantMaterial NETHER_WART = plant("nether_wart", "Nether Wart")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 4),
                    component(DULCARA, 1)
            )
            .existing(PlantPart.CROP, "minecraft:nether_wart")
            .existing(PlantPart.COMPRESSED_BLOCK, "minecraft:nether_wart_block").climate(new net.mads.industron.climate.PlantClimateProfile(20,30,48,65,.1,4,4,true));
    public static final PlantMaterial WARPED_WART = plant("warped_wart", "Warped Wart")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 4),
                    component(DULCARA, 1)
            ).existing(PlantPart.BLOCK, "minecraft:warped_wart_block");
    public static final PlantMaterial SHROOMLIGHT = plant("shroomlight", "Shroomlight")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 2),
                    component(RESYRA, 4),
                    component(DULCARA, 1)
            ).existing(PlantPart.BLOCK, "minecraft:shroomlight");

    public static final PlantMaterial CHORUS = plant("chorus", "Chorus")
            .contains(
                    component(LIGNARA, 1),
                    component(SYLVARA, 1),
                    component(RESYRA, 1),
                    component(DULCARA, 5)
            )
            .existing(PlantPart.FLOWER, "minecraft:chorus_flower")
            .existing(PlantPart.FRUIT, "minecraft:chorus_fruit");

    private static final List<PlantMaterial> VANILLA = List.of(
            WHEAT, CARROT, POTATO, POISONOUS_POTATO, BEETROOT,
            SUGAR_CANE, CACTUS, BAMBOO, COCOA,
            MELON, PUMPKIN, APPLE, SWEET_BERRY, GLOW_BERRY,
            KELP, SEAGRASS, LILY_PAD, SEA_PICKLE,
            SHORT_GRASS, TALL_GRASS, FERN, LARGE_FERN, DEAD_BUSH,
            VINES, WEEPING_VINES, TWISTING_VINES, HANGING_ROOTS, MANGROVE_ROOTS,
            CRIMSON_ROOTS, WARPED_ROOTS, NETHER_SPROUTS,
            SMALL_DRIPLEAF, BIG_DRIPLEAF, SPORE_BLOSSOM, GLOW_LICHEN, MOSS,
            AZALEA, FLOWERING_AZALEA, PINK_PETALS,
            DANDELION, POPPY, BLUE_ORCHID, ALLIUM, AZURE_BLUET,
            RED_TULIP, ORANGE_TULIP, WHITE_TULIP, PINK_TULIP,
            OXEYE_DAISY, CORNFLOWER, LILY_OF_THE_VALLEY, WITHER_ROSE,
            SUNFLOWER, LILAC, ROSE_BUSH, PEONY, TORCHFLOWER, PITCHER_PLANT,
            BROWN_MUSHROOM, RED_MUSHROOM, MUSHROOM_STEM, CRIMSON_FUNGUS, WARPED_FUNGUS,
            NETHER_WART, WARPED_WART, SHROOMLIGHT, CHORUS
    );

    /**
     * WoodMaterial remains the owner/registrar of leaves and saplings. These lightweight plant
     * definitions only bridge those already-existing forms into plant processing and fuel logic.
     */
    private static final List<PlantMaterial> WOOD_FOLIAGE = WoodMaterials.ALL.stream()
            .map(PlantMaterials::woodFoliage)
            .filter(material -> !material.existingParts().isEmpty())
            .toList();

    public static final List<PlantMaterial> ALL = combined(VANILLA, WOOD_FOLIAGE);

    private PlantMaterials() {
    }

    public static PlantMaterial plant(String id, String displayName) {
        return new PlantMaterial(id, displayName);
    }

    public static PlantMaterial plant(String id, String displayName, int color) {
        return new PlantMaterial(id, displayName, color);
    }

    public static MaterialComponent component(IndustrialSubstance substance, int amount) {
        return new MaterialComponent(substance, amount);
    }



    private static PlantMaterial woodFoliage(WoodMaterial wood) {
        PlantMaterial material = plant(wood.id() + "_foliage", wood.displayName() + " Foliage")
                .contains(wood.components().toArray(MaterialComponent[]::new));

        ResourceLocation leaves = woodPartId(wood, MaterialPart.LEAVES);
        if (leaves != null) {
            material = material.existing(PlantPart.LEAVES, leaves);
        }
        ResourceLocation sapling = woodPartId(wood, MaterialPart.SAPLING);
        if (sapling != null) {
            material = material.existing(PlantPart.SAPLING, sapling);
        }
        return material;
    }

    /**
     * Resolves both vanilla/existing foliage and foliage generated by WoodMaterial. PlantMaterial
     * never registers these blocks; it only points at the id already owned by the wood system.
     */
    private static ResourceLocation woodPartId(WoodMaterial wood, MaterialPart part) {
        if (wood.hasExistingPart(part)) {
            ResourceLocation id = wood.existingPart(part);
            // Some wood parts are valid world blocks without an inventory item (for example
            // minecraft:bamboo_sapling). Plant processing/fuel operates on item forms, so bridge
            // only existing foliage that can actually exist in an ItemStack.
            if ("minecraft".equals(id.getNamespace()) && BuiltInRegistries.ITEM.getOptional(id).isEmpty()) {
                return null;
            }
            return id;
        }
        boolean generated = StructureMaterialGenerator.blockDefinitions(wood).stream()
                .anyMatch(definition -> definition.part().filter(part::equals).isPresent());
        return generated
                ? ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, part.registryName(wood))
                : null;
    }

    private static List<PlantMaterial> combined(List<PlantMaterial> first, List<PlantMaterial> second) {
        List<PlantMaterial> all = new ArrayList<>(first.size() + second.size());
        all.addAll(first);
        all.addAll(second);
        return List.copyOf(all);
    }
}
