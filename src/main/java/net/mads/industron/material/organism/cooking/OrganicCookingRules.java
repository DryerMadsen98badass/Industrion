package net.mads.industron.material.organism.cooking;

import net.mads.industron.material.organism.*;

/** Deterministic timing; no random completion time, no nutrition/illness simulation. */
public final class OrganicCookingRules {
    private OrganicCookingRules() {}
    public static int cookedAt(OrganismItemCatalog.Entry entry) {
        return entry.part()==OrganismPart.FISH ? 800 : 1200;
    }
    public static int burntAt(OrganismItemCatalog.Entry entry) { return cookedAt(entry)+600; }
    public static OrganicForm state(OrganicForm current,int heat,int cooked,int burnt) {
        if(current==OrganicForm.BURNT || heat>=burnt)return OrganicForm.BURNT;
        if(current==OrganicForm.ROTTEN)return OrganicForm.ROTTEN;
        return current==OrganicForm.COOKED || heat>=cooked ? OrganicForm.COOKED : OrganicForm.RAW;
    }
}
