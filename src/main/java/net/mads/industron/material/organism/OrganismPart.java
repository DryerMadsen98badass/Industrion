package net.mads.industron.material.organism;

import java.util.Set;

/** Physical families, never species-specific recipes. Wet meat does not implicitly generate dust. */
public enum OrganismPart {
    MEAT("meat", true, false), FISH("fish", true, false),
    HIDE("hide", false, false), BONE("bone", false, true),
    ANTLER("antler", false, true), HORN("horn", false, true),
    FEATHER("feather", false, true), WOOL("wool", false, false),
    SILK("silk", false, false), SHELL("shell", false, true),
    SCUTE("scute", false, true), EYE("eye", false, false),
    MEMBRANE("membrane", false, false), INK_SAC("ink_sac", false, false),
    SLIME("slime", false, false), POWDER("powder", false, false),
    EGG("egg", false, false), FOOT("foot", false, false);

    private final String suffix;
    private final boolean cooking;
    private final boolean powder;
    OrganismPart(String suffix, boolean cooking, boolean powder) {
        this.suffix = suffix;
        this.cooking = cooking;
        this.powder = powder;
    }
    public String suffix() { return suffix; }
    public boolean cookingFamily() { return cooking; }
    public Set<OrganicForm> forms() {
        if (cooking) return Set.of(OrganicForm.RAW, OrganicForm.COOKED, OrganicForm.ROTTEN, OrganicForm.BURNT);
        if (powder) return Set.of(OrganicForm.RAW, OrganicForm.DUST, OrganicForm.SMALL_DUST, OrganicForm.TINY_DUST);
        return Set.of(OrganicForm.RAW);
    }
}
