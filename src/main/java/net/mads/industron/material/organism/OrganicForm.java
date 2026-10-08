package net.mads.industron.material.organism;

/** Family-relative forms. Form quantities must be supplied by the existing quantity system. */
public enum OrganicForm {
    RAW, COOKED, ROTTEN, BURNT, DUST, SMALL_DUST, TINY_DUST;

    public String registryName(String materialId, OrganismPart part) {
        String base = materialId + "_" + part.suffix();
        return switch (this) {
            case RAW -> part == OrganismPart.MEAT || part == OrganismPart.FISH ? "raw_" + base : base;
            case COOKED -> "cooked_" + base;
            case ROTTEN -> "rotten_" + base;
            case BURNT -> "burnt_" + base;
            case DUST -> base + "_dust";
            case SMALL_DUST -> "small_" + base + "_dust";
            case TINY_DUST -> "tiny_" + base + "_dust";
        };
    }
}
