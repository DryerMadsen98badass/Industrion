package net.mads.industron.material.plant;

/** Logical permanent/raw forms owned by a {@link PlantMaterial}. Route intermediates live separately. */
public enum PlantPart {
    /** Generic harvested plant item when a more specific role is unnecessary. */
    PLANT("", "", false, true, true),
    /** Harvested cereal/crop body. */
    CROP("_crop", " Crop", false, true, true),
    /** Seed/propagation item. Growth semantics remain vanilla until the later plant runtime. */
    SEEDS("_seeds", " Seeds", false, true, false),
    /** Edible or structural root/tuber. */
    ROOT("_root", " Root", false, true, false),
    /** Cane, reed, cactus, bamboo or another harvested stem. */
    STEM("_stem", " Stem", false, true, true),
    /** Fruit/berry/slice item. */
    FRUIT("_fruit", " Fruit", false, true, false),
    /** Natural full fruit/produce block such as a pumpkin. */
    FRUIT_BLOCK("_fruit_block", " Fruit Block", false, true, false),
    /** Generic natural plant-derived block that is not a 9x compressed storage form. */
    BLOCK("_block", " Block", false, true, false),
    /** Deliberately compressed 9-unit plant storage block when the source recipe is 9:1. */
    COMPRESSED_BLOCK("_compressed_block", " Compressed Block", false, true, false),
    /** World-only storage: 64 of the material's main harvested form; never a block item or process unit. */
    STORAGE_BLOCK("_plant_stack", " Stack", false, false, false),
    /** Flower or blossom item. */
    FLOWER("_flower", " Flower", false, true, true),
    /** Vine-like harvested item. */
    VINE("_vine", " Vine", false, true, true),
    /** Aquatic plant item. */
    AQUATIC("_aquatic", " Aquatic Plant", false, true, true),
    /** Fungal organic source. Kept in the same organic pipeline without changing vanilla growth. */
    FUNGUS("_fungus", " Fungus", false, true, false),
    /** Compressed plant form such as hay/dried-kelp blocks. */
    BALE("_bale", " Bale", false, true, false),
    /** Existing dried plant form such as dried kelp. */
    DRIED("_dried", " Dried", false, true, false),
    /** Carpet-like plant form such as moss carpet. */
    CARPET("_carpet", " Carpet", false, true, false),
    /** Tree foliage bridged from WoodMaterial; the item/block remains owned by WoodMaterial. */
    LEAVES("_leaves", " Leaves", false, true, true),
    /** Tree sapling/propagule bridged from WoodMaterial; the item/block remains owned by WoodMaterial. */
    SAPLING("_sapling", " Sapling", false, true, true),

    /** Separated/worked plant fibre. This is a real usable form, not a route-only intermediate. */
    FIBER("_fiber", " Fiber", true, false, false),
    /** Hand-twisted string/cord produced from this plant's fibre. */
    STRING("_string", " String", true, false, false);

    private final String suffix;
    private final String displaySuffix;
    private final boolean generatedProduct;
    private final boolean processSource;
    private final boolean fiberSource;

    PlantPart(
            String suffix,
            String displaySuffix,
            boolean generatedProduct,
            boolean processSource,
            boolean fiberSource
    ) {
        this.suffix = suffix;
        this.displaySuffix = displaySuffix;
        this.generatedProduct = generatedProduct;
        this.processSource = processSource;
        this.fiberSource = fiberSource;
    }

    public String registryName(PlantMaterial material) {
        return material.id() + suffix;
    }

    public String readableName(PlantMaterial material) {
        return material.displayName() + displaySuffix;
    }

    public boolean generatedProcessingForm() {
        return generatedProduct;
    }

    /** True when this raw/existing form can enter generic plant processing. */
    public boolean biomassSource() {
        return processSource;
    }

    /** True when this physical form may be manually worked into fibre if composition permits it. */
    public boolean fiberSource() {
        return fiberSource;
    }
}
