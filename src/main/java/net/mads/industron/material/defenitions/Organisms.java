package net.mads.industron.material.defenitions;

import java.util.List;
import net.mads.industron.material.organism.OrganismDefinition;
import static net.mads.industron.material.organism.OrganismDefinition.organism;
import static net.mads.industron.material.organism.OrganismPart.*;
import static net.mads.industron.material.organism.OrganicForm.*;
import static net.mads.industron.material.organism.OrganismLoot.drop;
import static net.mads.industron.material.defenitions.IndustrialMaterials.component;
import static net.mads.industron.material.defenitions.OrganicMaterials.*;
import static net.mads.industron.material.defenitions.AnimalMaterials.FLESH;
import static net.mads.industron.material.defenitions.AnimalMaterials.WATER;
import static net.mads.industron.material.defenitions.AnimalMaterials.BONE_MINERAL;

/** Java 1.21.1 mob catalog. Special vanilla drops stay in the imported baseline loot table.
 * Empty means no material death pool; equipment and living interactions are not erased.
 * A declared part never adds a death drop unless .loot or .replaceDrop explicitly says so.
 */
public final class Organisms {
    private Organisms() {}

    public static final OrganismDefinition ALLAY = organism("allay", "Allay")
            .existing("minecraft:allay")
            .emptyLoot()
            .build();

    public static final OrganismDefinition ARMADILLO = organism("armadillo", "Armadillo")
            .existing("minecraft:armadillo")
            .replaceLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .loot(drop(RAW).count(1,2).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .part(SCUTE).color(0xC9874B)
                .contains(component(KERATIN,2), component(BONE_MINERAL,1))
                .existing(RAW, "minecraft:armadillo_scute")
                .loot(drop(RAW).count(0,1).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition AXOLOTL = organism("axolotl", "Axolotl")
            .existing("minecraft:axolotl")
            .replaceLoot()
            .part(FISH).color(0xD6AA58)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(BONE_MINERAL,1), component(WATER,10))
                .loot(drop(RAW).count(1,1))
            .build();

    public static final OrganismDefinition BAT = organism("bat", "Bat")
            .existing("minecraft:bat")
            .replaceLoot()
            .part(MEAT).color(0x8F5C45)
                .contains(component(MUSCLE_PROTEIN,3), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .loot(drop(RAW).count(0,1).chance(0.75))
            .part(MEMBRANE).color(0x9D7A57)
                .contains(component(AnimalMaterials.MEMBRANE,1))
                .loot(drop(RAW).count(0,1).chance(0.5))
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,1).chance(0.5))
            .build();

    public static final OrganismDefinition BEE = organism("bee", "Bee")
            .existing("minecraft:bee")
            .replaceLoot()
            .part(MEAT).color(0xC48B32)
                .contains(component(MUSCLE_PROTEIN,2), component(COLLAGEN,1), component(WATER,5))
                .loot(drop(RAW).count(0,1).chance(0.5))
            .part(MEMBRANE).color(0xD9C477)
                .contains(component(AnimalMaterials.MEMBRANE,1), component(WAX,1))
                .loot(drop(RAW).count(0,1).chance(0.5))
            .build();

    public static final OrganismDefinition BLAZE = organism("blaze", "Blaze")
            .existing("minecraft:blaze")
            .rewriteExistingLoot()
            .build();

    public static final OrganismDefinition BOGGED = organism("bogged", "Bogged")
            .existing("minecraft:bogged")
            .rewriteExistingLoot()
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .existing(RAW, "minecraft:bone")
            .build();

    public static final OrganismDefinition BREEZE = organism("breeze", "Breeze")
            .existing("minecraft:breeze")
            .rewriteExistingLoot()
            .build();

    public static final OrganismDefinition CAMEL = organism("camel", "Camel")
            .existing("minecraft:camel")
            .replaceLoot()
            .part(MEAT).color(0xC8793E)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,2), component(WATER,7))
                .loot(drop(RAW).count(2,5).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(HIDE).color(0xB27A3E)
                .contains(component(AnimalMaterials.HIDE,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition CAT = organism("cat", "Cat")
            .existing("minecraft:cat")
            .rewriteExistingLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .loot(drop(RAW).count(1,2).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(HIDE).color(0xC7964F)
                .contains(component(AnimalMaterials.HIDE,1))
                .loot(drop(RAW).count(0,1).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition CAVE_SPIDER = organism("cave_spider", "Cave Spider")
            .existing("minecraft:cave_spider")
            .rewriteExistingLoot()
            .part(MEAT).color(0x5F6942)
                .contains(component(MUSCLE_PROTEIN,3), component(COLLAGEN,1), component(WATER,5), component(TOXIN,1))
                .loot(drop(RAW).count(0,1).lootingBonus(0,1))
            .part(SHELL).color(0x6E7A3A)
                .contains(component(KERATIN,2), component(BONE_MINERAL,1))
                .loot(drop(RAW).count(0,1).lootingBonus(0,1))
            .part(SILK).color(0xEEDCA6)
                .contains(component(SILK_PROTEIN,1))
                .existing(RAW, "minecraft:string")
            .part(EYE).color(0x9E4C4C)
                .contains(component(AnimalMaterials.EYE,1))
                .existing(RAW, "minecraft:spider_eye")
            .build();

    public static final OrganismDefinition CHICKEN = organism("chicken", "Chicken")
            .existing("minecraft:chicken")
            .rewriteExistingLoot()
            .part(MEAT).color(0xC8793E)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,9))
                .existing(RAW, "minecraft:chicken")
                .existing(COOKED, "minecraft:cooked_chicken")
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .part(FEATHER).color(0xEBCF8A)
                .contains(component(KERATIN,1))
                .existing(RAW, "minecraft:feather")
            .build();

    public static final OrganismDefinition COD = organism("cod", "Cod")
            .existing("minecraft:cod")
            .rewriteExistingLoot()
            .part(FISH).color(0xD6AA58)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(BONE_MINERAL,1), component(WATER,12))
                .existing(RAW, "minecraft:cod")
                .existing(COOKED, "minecraft:cooked_cod")
            .build();

    public static final OrganismDefinition COW = organism("cow", "Cow")
            .existing("minecraft:cow")
            .rewriteExistingLoot()
            .part(MEAT).color(0xC8793E)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,2), component(WATER,8))
                .existing(RAW, "minecraft:beef")
                .existing(COOKED, "minecraft:cooked_beef")
            .part(HIDE).color(0xB27A3E)
                .contains(component(AnimalMaterials.HIDE,1))
                .replaceDrop("minecraft:leather", RAW)
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition CREEPER = organism("creeper", "Creeper")
            .existing("minecraft:creeper")
            .rewriteExistingLoot()
            .part(MEAT).color(0x7F9A43)
                .contains(component(MUSCLE_PROTEIN,3), component(COLLAGEN,1), component(WATER,6), component(TOXIN,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .part(BONE).color(0xD4C470)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .part(POWDER).color(0x4A4A4A)
                .contains(component(CHARCOAL,1), component(TOXIN,1), component(BONE_MINERAL,1))
                .existing(RAW, "minecraft:gunpowder")
            .build();

    public static final OrganismDefinition DOLPHIN = organism("dolphin", "Dolphin")
            .existing("minecraft:dolphin")
            .rewriteExistingLoot()
            .part(FISH).color(0xD6AA58)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,2), component(BONE_MINERAL,1), component(WATER,10))
                .loot(drop(RAW).count(2,4).lootingBonus(0,1).cookWhenBurning())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition DONKEY = organism("donkey", "Donkey")
            .existing("minecraft:donkey")
            .rewriteExistingLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .loot(drop(RAW).count(2,4).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(HIDE).color(0xB27A3E)
                .contains(component(AnimalMaterials.HIDE,1))
                .replaceDrop("minecraft:leather", RAW)
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition DROWNED = organism("drowned", "Drowned")
            .existing("minecraft:drowned")
            .rewriteExistingLoot()
            .part(MEAT).color(0x8F7A34)
                .contains(component(FLESH,1))
                .existing(ROTTEN, "minecraft:rotten_flesh")
            .part(BONE).color(0xD8C171)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition ELDER_GUARDIAN = organism("elder_guardian", "Elder Guardian")
            .existing("minecraft:elder_guardian")
            .rewriteExistingLoot()
            .part(FISH).color(0xD6AA58)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(BONE_MINERAL,3), component(WATER,9))
                .loot(drop(RAW).count(3,6).lootingBonus(0,2))
            .part(SCUTE).color(0xA6A35F)
                .contains(component(KERATIN,2), component(BONE_MINERAL,2))
                .loot(drop(RAW).count(2,5).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition ENDER_DRAGON = organism("ender_dragon", "Ender Dragon")
            .existing("minecraft:ender_dragon")
            .rewriteExistingLoot()
            .part(MEAT).color(0x5B3C74)
                .contains(component(MUSCLE_PROTEIN,6), component(COLLAGEN,2), component(ANIMAL_FAT,1), component(WATER,6), component(TOXIN,1))
                .loot(drop(RAW).count(16,32).lootingBonus(0,4))
            .part(HIDE).color(0x3B2A45)
                .contains(component(AnimalMaterials.HIDE,2), component(KERATIN,2))
                .loot(drop(RAW).count(8,16).lootingBonus(0,2))
            .part(BONE).color(0xB7A169)
                .contains(component(AnimalMaterials.BONE,2))
                .loot(drop(RAW).count(12,24).lootingBonus(0,4))
            .build();

    public static final OrganismDefinition ENDERMAN = organism("enderman", "Enderman")
            .existing("minecraft:enderman")
            .rewriteExistingLoot()
            .part(MEAT).color(0x5B3C74)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(WATER,5))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .part(BONE).color(0xB7A169)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition ENDERMITE = organism("endermite", "Endermite")
            .existing("minecraft:endermite")
            .replaceLoot()
            .part(MEAT).color(0x6E5A91)
                .contains(component(MUSCLE_PROTEIN,2), component(COLLAGEN,1), component(WATER,4))
                .loot(drop(RAW).count(0,1).chance(0.75))
            .part(SHELL).color(0x7F70A6)
                .contains(component(KERATIN,1), component(BONE_MINERAL,1))
                .loot(drop(RAW).count(0,1).chance(0.5))
            .build();

    public static final OrganismDefinition EVOKER = organism("evoker", "Evoker")
            .existing("minecraft:evoker")
            .rewriteExistingLoot()
            .part(MEAT).color(0xA76558)
                .contains(component(FLESH,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition FOX = organism("fox", "Fox")
            .existing("minecraft:fox")
            .replaceLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .loot(drop(RAW).count(1,2).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(HIDE).color(0xC06B2E)
                .contains(component(AnimalMaterials.HIDE,1))
                .loot(drop(RAW).count(0,1).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition FROG = organism("frog", "Frog")
            .existing("minecraft:frog")
            .replaceLoot()
            .part(MEAT).color(0xA06D45)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,10))
                .loot(drop(RAW).count(1,2).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(MEMBRANE).color(0x9EB96B)
                .contains(component(AnimalMaterials.MEMBRANE,1))
                .loot(drop(RAW).count(0,1).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,1).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition GHAST = organism("ghast", "Ghast")
            .existing("minecraft:ghast")
            .rewriteExistingLoot()
            .part(MEAT).color(0xC7A178)
                .contains(component(MUSCLE_PROTEIN,3), component(COLLAGEN,1), component(WATER,6), component(TOXIN,1))
                .loot(drop(RAW).count(2,5).lootingBonus(0,1))
            .part(MEMBRANE).color(0xD8BD8D)
                .contains(component(AnimalMaterials.MEMBRANE,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition GIANT = organism("giant", "Giant")
            .existing("minecraft:giant")
            .replaceLoot()
            .part(MEAT).color(0x8F7A34)
                .contains(component(FLESH,1))
                .loot(drop(ROTTEN).count(4,8).lootingBonus(0,2))
            .part(BONE).color(0xD8C171)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(2,5).lootingBonus(0,2))
            .build();

    public static final OrganismDefinition GLOW_SQUID = organism("glow_squid", "Glow Squid")
            .existing("minecraft:glow_squid")
            .rewriteExistingLoot()
            .part(MEAT).color(0x5E8F83)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,12))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .part(MEMBRANE).color(0x60A897)
                .contains(component(AnimalMaterials.MEMBRANE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition GOAT = organism("goat", "Goat")
            .existing("minecraft:goat")
            .replaceLoot()
            .part(MEAT).color(0xC8793E)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(HIDE).color(0xB27A3E)
                .contains(component(AnimalMaterials.HIDE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .part(HORN).color(0xD0AE63)
                .contains(component(KERATIN,1))
                .existing(RAW, "minecraft:goat_horn")
                .loot(drop(RAW).count(0,1).chance(0.1).lootingChanceBonus(0.03).adultOnly())
            .build();

    public static final OrganismDefinition GUARDIAN = organism("guardian", "Guardian")
            .existing("minecraft:guardian")
            .rewriteExistingLoot()
            .part(FISH).color(0xD6AA58)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(BONE_MINERAL,2), component(WATER,9))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .part(SCUTE).color(0x8BAA64)
                .contains(component(KERATIN,2), component(BONE_MINERAL,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition HOGLIN = organism("hoglin", "Hoglin")
            .existing("minecraft:hoglin")
            .rewriteExistingLoot()
            .part(MEAT).color(0xC8793E)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,3), component(WATER,8))
                .existing(RAW, "minecraft:porkchop")
                .existing(COOKED, "minecraft:cooked_porkchop")
            .part(HIDE).color(0xB27A3E)
                .contains(component(AnimalMaterials.HIDE,1))
                .replaceDrop("minecraft:leather", RAW)
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition HORSE = organism("horse", "Horse")
            .existing("minecraft:horse")
            .rewriteExistingLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .loot(drop(RAW).count(2,5).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(HIDE).color(0xB27A3E)
                .contains(component(AnimalMaterials.HIDE,1))
                .replaceDrop("minecraft:leather", RAW)
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition HUSK = organism("husk", "Husk")
            .existing("minecraft:husk")
            .rewriteExistingLoot()
            .part(MEAT).color(0x8F7A34)
                .contains(component(FLESH,1))
                .existing(ROTTEN, "minecraft:rotten_flesh")
            .part(BONE).color(0xD8C171)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition ILLUSIONER = organism("illusioner", "Illusioner")
            .existing("minecraft:illusioner")
            .replaceLoot()
            .part(MEAT).color(0xA76558)
                .contains(component(FLESH,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition IRON_GOLEM = organism("iron_golem", "Iron Golem")
            .existing("minecraft:iron_golem")
            .rewriteExistingLoot()
            .build();

    public static final OrganismDefinition LLAMA = organism("llama", "Llama")
            .existing("minecraft:llama")
            .rewriteExistingLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .loot(drop(RAW).count(2,4).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(HIDE).color(0xB27A3E)
                .contains(component(AnimalMaterials.HIDE,1))
                .replaceDrop("minecraft:leather", RAW)
            .part(WOOL).color(0xC9A76A)
                .contains(component(AnimalMaterials.WOOL,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition MAGMA_CUBE = organism("magma_cube", "Magma Cube")
            .existing("minecraft:magma_cube")
            .rewriteExistingLoot()
            .part(net.mads.industron.material.organism.OrganismPart.SLIME).color(0xC7672D)
                .contains(component(AnimalMaterials.SLIME,1), component(CHARCOAL,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition MOOSHROOM = organism("mooshroom", "Mooshroom")
            .existing("minecraft:mooshroom")
            .rewriteExistingLoot()
            .part(MEAT).color(0xC8793E)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,2), component(WATER,8))
                .existing(RAW, "minecraft:beef")
                .existing(COOKED, "minecraft:cooked_beef")
            .part(HIDE).color(0xB27A3E)
                .contains(component(AnimalMaterials.HIDE,1))
                .replaceDrop("minecraft:leather", RAW)
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition MULE = organism("mule", "Mule")
            .existing("minecraft:mule")
            .rewriteExistingLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .loot(drop(RAW).count(2,4).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(HIDE).color(0xB27A3E)
                .contains(component(AnimalMaterials.HIDE,1))
                .replaceDrop("minecraft:leather", RAW)
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition OCELOT = organism("ocelot", "Ocelot")
            .existing("minecraft:ocelot")
            .replaceLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .loot(drop(RAW).count(1,2).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(HIDE).color(0xC7964F)
                .contains(component(AnimalMaterials.HIDE,1))
                .loot(drop(RAW).count(0,1).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition PANDA = organism("panda", "Panda")
            .existing("minecraft:panda")
            .rewriteExistingLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,2), component(WATER,8))
                .loot(drop(RAW).count(2,4).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(HIDE).color(0x8F7A57)
                .contains(component(AnimalMaterials.HIDE,1))
                .loot(drop(RAW).count(1,2).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition PARROT = organism("parrot", "Parrot")
            .existing("minecraft:parrot")
            .replaceLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,3), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .loot(drop(RAW).count(0,1).chance(0.75).adultOnly().cookWhenBurning())
            .part(FEATHER).color(0xEBCF8A)
                .contains(component(KERATIN,1))
                .existing(RAW, "minecraft:feather")
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,1).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition PHANTOM = organism("phantom", "Phantom")
            .existing("minecraft:phantom")
            .rewriteExistingLoot()
            .part(MEMBRANE).color(0xD0C36D)
                .contains(component(AnimalMaterials.MEMBRANE,1))
                .existing(RAW, "minecraft:phantom_membrane")
            .build();

    public static final OrganismDefinition PIG = organism("pig", "Pig")
            .existing("minecraft:pig")
            .rewriteExistingLoot()
            .part(MEAT).color(0xC8793E)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,3), component(WATER,8))
                .existing(RAW, "minecraft:porkchop")
                .existing(COOKED, "minecraft:cooked_porkchop")
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition PIGLIN = organism("piglin", "Piglin")
            .existing("minecraft:piglin")
            .replaceLoot()
            .part(MEAT).color(0xC8793E)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,2), component(WATER,7))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).cookWhenBurning())
            .part(HIDE).color(0xB27A3E)
                .contains(component(AnimalMaterials.HIDE,1))
                .loot(drop(RAW).count(0,1).lootingBonus(0,1))
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition PIGLIN_BRUTE = organism("piglin_brute", "Piglin Brute")
            .existing("minecraft:piglin_brute")
            .replaceLoot()
            .part(MEAT).color(0xC8793E)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,2), component(WATER,7))
                .loot(drop(RAW).count(2,4).lootingBonus(0,1).cookWhenBurning())
            .part(HIDE).color(0xB27A3E)
                .contains(component(AnimalMaterials.HIDE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition PILLAGER = organism("pillager", "Pillager")
            .existing("minecraft:pillager")
            .replaceLoot()
            .part(MEAT).color(0xA76558)
                .contains(component(FLESH,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition POLAR_BEAR = organism("polar_bear", "Polar Bear")
            .existing("minecraft:polar_bear")
            .rewriteExistingLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,3), component(WATER,7))
                .loot(drop(RAW).count(2,5).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(HIDE).color(0xE2D2A6)
                .contains(component(AnimalMaterials.HIDE,1))
                .loot(drop(RAW).count(1,2).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition PUFFERFISH = organism("pufferfish", "Pufferfish")
            .existing("minecraft:pufferfish")
            .rewriteExistingLoot()
            .part(FISH).color(0xD6AA58)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(BONE_MINERAL,1), component(WATER,12), component(TOXIN,1))
                .existing(RAW, "minecraft:pufferfish")
            .build();

    public static final OrganismDefinition RABBIT = organism("rabbit", "Rabbit")
            .existing("minecraft:rabbit")
            .rewriteExistingLoot()
            .part(MEAT).color(0xC8793E)
                .contains(component(MUSCLE_PROTEIN,6), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .existing(RAW, "minecraft:rabbit")
                .existing(COOKED, "minecraft:cooked_rabbit")
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .part(HIDE).color(0xC28B4A)
                .contains(component(AnimalMaterials.HIDE,1))
                .existing(RAW, "minecraft:rabbit_hide")
            .part(FOOT).color(0xC7964F)
                .contains(component(AnimalMaterials.BONE,2), component(AnimalMaterials.HIDE,1), component(FLESH,1), component(FUR_FIBER,1))
                .existing(RAW, "minecraft:rabbit_foot")
            .build();

    public static final OrganismDefinition RAVAGER = organism("ravager", "Ravager")
            .existing("minecraft:ravager")
            .rewriteExistingLoot()
            .part(MEAT).color(0xC8793E)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,2), component(ANIMAL_FAT,2), component(WATER,7))
                .loot(drop(RAW).count(4,8).lootingBonus(0,2).cookWhenBurning())
            .part(HIDE).color(0x7D5F42)
                .contains(component(AnimalMaterials.HIDE,1))
                .loot(drop(RAW).count(2,4).lootingBonus(0,1))
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(3,6).lootingBonus(0,2))
            .part(HORN).color(0xD0AE63)
                .contains(component(KERATIN,1), component(BONE_MINERAL,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition SALMON = organism("salmon", "Salmon")
            .existing("minecraft:salmon")
            .rewriteExistingLoot()
            .part(FISH).color(0xD6AA58)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,3), component(BONE_MINERAL,1), component(WATER,10))
                .existing(RAW, "minecraft:salmon")
                .existing(COOKED, "minecraft:cooked_salmon")
            .build();

    public static final OrganismDefinition SHEEP = organism("sheep", "Sheep")
            .existing("minecraft:sheep")
            .rewriteExistingLoot()
            .part(MEAT).color(0xC8793E)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,2), component(WATER,7))
                .existing(RAW, "minecraft:mutton")
                .existing(COOKED, "minecraft:cooked_mutton")
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .part(WOOL).color(0xF1D88E)
                .contains(component(AnimalMaterials.WOOL,1))
                .existing(RAW, "minecraft:white_wool")
            .build();

    public static final OrganismDefinition SHULKER = organism("shulker", "Shulker")
            .existing("minecraft:shulker")
            .rewriteExistingLoot()
            .part(MEAT).color(0x7F5F91)
                .contains(component(MUSCLE_PROTEIN,3), component(COLLAGEN,1), component(WATER,5))
                .loot(drop(RAW).count(1,2).lootingBonus(0,1))
            .part(SHELL).color(0x8A6AA3)
                .contains(component(KERATIN,2), component(BONE_MINERAL,2))
                .existing(RAW, "minecraft:shulker_shell")
                .loot(drop(RAW).count(0,1).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition SILVERFISH = organism("silverfish", "Silverfish")
            .existing("minecraft:silverfish")
            .replaceLoot()
            .part(MEAT).color(0x9B8F5B)
                .contains(component(MUSCLE_PROTEIN,2), component(COLLAGEN,1), component(WATER,4))
                .loot(drop(RAW).count(0,1).chance(0.75))
            .part(SHELL).color(0xB8A15D)
                .contains(component(KERATIN,1), component(BONE_MINERAL,1))
                .loot(drop(RAW).count(0,1).chance(0.5))
            .build();

    public static final OrganismDefinition SKELETON = organism("skeleton", "Skeleton")
            .existing("minecraft:skeleton")
            .rewriteExistingLoot()
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .existing(RAW, "minecraft:bone")
            .build();

    public static final OrganismDefinition SKELETON_HORSE = organism("skeleton_horse", "Skeleton Horse")
            .existing("minecraft:skeleton_horse")
            .rewriteExistingLoot()
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .existing(RAW, "minecraft:bone")
                .loot(drop(RAW).count(2,5).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition SLIME = organism("slime", "Slime")
            .existing("minecraft:slime")
            .rewriteExistingLoot()
            .part(net.mads.industron.material.organism.OrganismPart.SLIME).color(0x78A75B)
                .contains(component(AnimalMaterials.SLIME,1))
                .existing(RAW, "minecraft:slime_ball")
            .build();

    public static final OrganismDefinition SNIFFER = organism("sniffer", "Sniffer")
            .existing("minecraft:sniffer")
            .replaceLoot()
            .part(MEAT).color(0xC8793E)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,2), component(WATER,8))
                .loot(drop(RAW).count(3,6).lootingBonus(0,2).adultOnly().cookWhenBurning())
            .part(HIDE).color(0xB06F3B)
                .contains(component(AnimalMaterials.HIDE,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(2,4).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition SNOW_GOLEM = organism("snow_golem", "Snow Golem")
            .existing("minecraft:snow_golem")
            .rewriteExistingLoot()
            .build();

    public static final OrganismDefinition SPIDER = organism("spider", "Spider")
            .existing("minecraft:spider")
            .rewriteExistingLoot()
            .part(MEAT).color(0x6E5B42)
                .contains(component(MUSCLE_PROTEIN,3), component(COLLAGEN,1), component(WATER,5))
                .loot(drop(RAW).count(1,2).lootingBonus(0,1))
            .part(SHELL).color(0x7A5D3A)
                .contains(component(KERATIN,2), component(BONE_MINERAL,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .part(SILK).color(0xEEDCA6)
                .contains(component(SILK_PROTEIN,1))
                .existing(RAW, "minecraft:string")
            .part(EYE).color(0x9E4C4C)
                .contains(component(AnimalMaterials.EYE,1))
                .existing(RAW, "minecraft:spider_eye")
            .build();

    public static final OrganismDefinition SQUID = organism("squid", "Squid")
            .existing("minecraft:squid")
            .rewriteExistingLoot()
            .part(MEAT).color(0x8A6F55)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,12))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .part(MEMBRANE).color(0xA68766)
                .contains(component(AnimalMaterials.MEMBRANE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .part(INK_SAC).color(0x353440)
                .contains(component(AnimalMaterials.INK_SAC,1))
                .existing(RAW, "minecraft:ink_sac")
            .build();

    public static final OrganismDefinition STRAY = organism("stray", "Stray")
            .existing("minecraft:stray")
            .rewriteExistingLoot()
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .existing(RAW, "minecraft:bone")
            .build();

    public static final OrganismDefinition STRIDER = organism("strider", "Strider")
            .existing("minecraft:strider")
            .rewriteExistingLoot()
            .part(MEAT).color(0x9B4F3D)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,2), component(ANIMAL_FAT,1), component(WATER,5))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly())
            .part(HIDE).color(0xA55A45)
                .contains(component(AnimalMaterials.HIDE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xC8A85A)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition TADPOLE = organism("tadpole", "Tadpole")
            .existing("minecraft:tadpole")
            .replaceLoot()
            .part(MEAT).color(0x9E8A4C)
                .contains(component(MUSCLE_PROTEIN,1), component(WATER,6))
                .loot(drop(RAW).count(0,1).chance(0.4))
            .build();

    public static final OrganismDefinition TRADER_LLAMA = organism("trader_llama", "Trader Llama")
            .existing("minecraft:trader_llama")
            .rewriteExistingLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .loot(drop(RAW).count(2,4).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(HIDE).color(0xB27A3E)
                .contains(component(AnimalMaterials.HIDE,1))
                .replaceDrop("minecraft:leather", RAW)
            .part(WOOL).color(0xC9A76A)
                .contains(component(AnimalMaterials.WOOL,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition TROPICAL_FISH = organism("tropical_fish", "Tropical Fish")
            .existing("minecraft:tropical_fish")
            .rewriteExistingLoot()
            .part(FISH).color(0xD6AA58)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(BONE_MINERAL,1), component(WATER,9))
                .existing(RAW, "minecraft:tropical_fish")
            .build();

    public static final OrganismDefinition TURTLE = organism("turtle", "Turtle")
            .existing("minecraft:turtle")
            .rewriteExistingLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,9))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .part(SCUTE).color(0x5B8D59)
                .contains(component(KERATIN,1))
                .existing(RAW, "minecraft:turtle_scute")
            .build();

    public static final OrganismDefinition VEX = organism("vex", "Vex")
            .existing("minecraft:vex")
            .emptyLoot()
            .build();

    public static final OrganismDefinition VILLAGER = organism("villager", "Villager")
            .existing("minecraft:villager")
            .replaceLoot()
            .part(MEAT).color(0xA76558)
                .contains(component(FLESH,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition VINDICATOR = organism("vindicator", "Vindicator")
            .existing("minecraft:vindicator")
            .rewriteExistingLoot()
            .part(MEAT).color(0xA76558)
                .contains(component(FLESH,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition WANDERING_TRADER = organism("wandering_trader", "Wandering Trader")
            .existing("minecraft:wandering_trader")
            .replaceLoot()
            .part(MEAT).color(0xA76558)
                .contains(component(FLESH,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition WARDEN = organism("warden", "Warden")
            .existing("minecraft:warden")
            .rewriteExistingLoot()
            .part(MEAT).color(0x2E6B63)
                .contains(component(MUSCLE_PROTEIN,5), component(COLLAGEN,2), component(ANIMAL_FAT,1), component(WATER,5), component(TOXIN,1))
                .loot(drop(RAW).count(6,12).lootingBonus(0,2))
            .part(BONE).color(0x8FA66A)
                .contains(component(AnimalMaterials.BONE,2))
                .loot(drop(RAW).count(4,8).lootingBonus(0,2))
            .part(SCUTE).color(0x315F58)
                .contains(component(KERATIN,2), component(BONE_MINERAL,1))
                .loot(drop(RAW).count(2,5).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition WITCH = organism("witch", "Witch")
            .existing("minecraft:witch")
            .rewriteExistingLoot()
            .part(MEAT).color(0xA76558)
                .contains(component(FLESH,1), component(TOXIN,1))
                .loot(drop(RAW).count(1,3).lootingBonus(0,1))
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition WITHER = organism("wither", "Wither")
            .existing("minecraft:wither")
            .rewriteExistingLoot()
            .part(BONE).color(0x4E3A28)
                .contains(component(AnimalMaterials.BONE,6), component(CHARCOAL,2), component(TOXIN,1))
                .loot(drop(RAW).count(8,16).lootingBonus(0,3))
            .build();

    public static final OrganismDefinition WITHER_SKELETON = organism("wither_skeleton", "Wither Skeleton")
            .existing("minecraft:wither_skeleton")
            .rewriteExistingLoot()
            .part(BONE).color(0x5C452C)
                .contains(component(AnimalMaterials.BONE,4), component(CHARCOAL,1))
                .replaceDrop("minecraft:bone", RAW)
            .build();

    public static final OrganismDefinition WOLF = organism("wolf", "Wolf")
            .existing("minecraft:wolf")
            .replaceLoot()
            .part(MEAT).color(0xB86B3D)
                .contains(component(MUSCLE_PROTEIN,4), component(COLLAGEN,1), component(ANIMAL_FAT,1), component(WATER,8))
                .loot(drop(RAW).count(1,2).lootingBonus(0,1).adultOnly().cookWhenBurning())
            .part(HIDE).color(0x9B7A55)
                .contains(component(AnimalMaterials.HIDE,1))
                .loot(drop(RAW).count(0,1).lootingBonus(0,1).adultOnly())
            .part(BONE).color(0xF0D487)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1).adultOnly())
            .build();

    public static final OrganismDefinition ZOGLIN = organism("zoglin", "Zoglin")
            .existing("minecraft:zoglin")
            .rewriteExistingLoot()
            .part(MEAT).color(0x8F7A34)
                .contains(component(FLESH,1))
                .existing(ROTTEN, "minecraft:rotten_flesh")
            .part(BONE).color(0xD8C171)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition ZOMBIE = organism("zombie", "Zombie")
            .existing("minecraft:zombie")
            .rewriteExistingLoot()
            .part(MEAT).color(0x8F7A34)
                .contains(component(FLESH,1))
                .existing(ROTTEN, "minecraft:rotten_flesh")
            .part(BONE).color(0xD8C171)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition ZOMBIE_HORSE = organism("zombie_horse", "Zombie Horse")
            .existing("minecraft:zombie_horse")
            .rewriteExistingLoot()
            .part(MEAT).color(0x8F7A34)
                .contains(component(FLESH,1))
                .existing(ROTTEN, "minecraft:rotten_flesh")
            .part(BONE).color(0xD8C171)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition ZOMBIE_VILLAGER = organism("zombie_villager", "Zombie Villager")
            .existing("minecraft:zombie_villager")
            .rewriteExistingLoot()
            .part(MEAT).color(0x8F7A34)
                .contains(component(FLESH,1))
                .existing(ROTTEN, "minecraft:rotten_flesh")
            .part(BONE).color(0xD8C171)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final OrganismDefinition ZOMBIFIED_PIGLIN = organism("zombified_piglin", "Zombified Piglin")
            .existing("minecraft:zombified_piglin")
            .rewriteExistingLoot()
            .part(MEAT).color(0x8F7A34)
                .contains(component(FLESH,1))
                .existing(ROTTEN, "minecraft:rotten_flesh")
            .part(BONE).color(0xD8C171)
                .contains(component(AnimalMaterials.BONE,1))
                .loot(drop(RAW).count(0,2).lootingBonus(0,1))
            .build();

    public static final List<OrganismDefinition> ALL = List.of(
            ALLAY,
            ARMADILLO,
            AXOLOTL,
            BAT,
            BEE,
            BLAZE,
            BOGGED,
            BREEZE,
            CAMEL,
            CAT,
            CAVE_SPIDER,
            CHICKEN,
            COD,
            COW,
            CREEPER,
            DOLPHIN,
            DONKEY,
            DROWNED,
            ELDER_GUARDIAN,
            ENDER_DRAGON,
            ENDERMAN,
            ENDERMITE,
            EVOKER,
            FOX,
            FROG,
            GHAST,
            GIANT,
            GLOW_SQUID,
            GOAT,
            GUARDIAN,
            HOGLIN,
            HORSE,
            HUSK,
            ILLUSIONER,
            IRON_GOLEM,
            LLAMA,
            MAGMA_CUBE,
            MOOSHROOM,
            MULE,
            OCELOT,
            PANDA,
            PARROT,
            PHANTOM,
            PIG,
            PIGLIN,
            PIGLIN_BRUTE,
            PILLAGER,
            POLAR_BEAR,
            PUFFERFISH,
            RABBIT,
            RAVAGER,
            SALMON,
            SHEEP,
            SHULKER,
            SILVERFISH,
            SKELETON,
            SKELETON_HORSE,
            SLIME,
            SNIFFER,
            SNOW_GOLEM,
            SPIDER,
            SQUID,
            STRAY,
            STRIDER,
            TADPOLE,
            TRADER_LLAMA,
            TROPICAL_FISH,
            TURTLE,
            VEX,
            VILLAGER,
            VINDICATOR,
            WANDERING_TRADER,
            WARDEN,
            WITCH,
            WITHER,
            WITHER_SKELETON,
            WOLF,
            ZOGLIN,
            ZOMBIE,
            ZOMBIE_HORSE,
            ZOMBIE_VILLAGER,
            ZOMBIFIED_PIGLIN);
}
