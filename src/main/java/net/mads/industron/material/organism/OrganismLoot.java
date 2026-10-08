package net.mads.industron.material.organism;

/** Declarative values for a future standard Minecraft loot-table exporter, not a second RNG. */
public record OrganismLoot(OrganicForm form, int minimum, int maximum,
                           int lootingMinimum, int lootingMaximum,
                           double chance, double lootingChanceBonus,
                           boolean adultOnly, boolean playerKillOnly, boolean cookWhenBurning) {
    public OrganismLoot {
        if (form == null || minimum < 0 || maximum < minimum || lootingMinimum < 0 || lootingMaximum < lootingMinimum)
            throw new IllegalArgumentException("Invalid loot counts");
        if (!Double.isFinite(chance) || chance < 0 || chance > 1 ||
            !Double.isFinite(lootingChanceBonus) || lootingChanceBonus < 0 || lootingChanceBonus > 1)
            throw new IllegalArgumentException("Invalid loot probability");
    }
    public static Builder drop(OrganicForm form) { return new Builder(form); }
    public static final class Builder {
        private final OrganicForm form;
        private int min = 1, max = 1, lootMin, lootMax;
        private double chance = 1, bonus;
        private boolean adult, player, cook;
        private Builder(OrganicForm form) { this.form = form; }
        public Builder count(int min, int max) { this.min=min; this.max=max; return this; }
        public Builder lootingBonus(int min, int max) { lootMin=min; lootMax=max; return this; }
        public Builder chance(double value) { chance=value; return this; }
        public Builder lootingChanceBonus(double value) { bonus=value; return this; }
        public Builder adultOnly() { adult=true; return this; }
        public Builder playerKillOnly() { player=true; return this; }
        public Builder cookWhenBurning() { cook=true; return this; }
        public OrganismLoot build() { return new OrganismLoot(form,min,max,lootMin,lootMax,chance,bonus,adult,player,cook); }
    }
}
