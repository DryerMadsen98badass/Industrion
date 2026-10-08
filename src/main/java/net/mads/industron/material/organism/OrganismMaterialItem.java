package net.mads.industron.material.organism;

import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;

/** Generated physical form; raw/cooked/rotten/burnt retain the family's shared composition. */
public final class OrganismMaterialItem extends Item {
    private final OrganismItemCatalog.Entry definition;
    public OrganismMaterialItem(OrganismItemCatalog.Entry definition) {
        super(properties(definition)); this.definition=definition;
    }
    private static Properties properties(OrganismItemCatalog.Entry e) {
        Properties p=new Properties();
        // This is basic food compatibility, not the deferred nutrition/illness system.
        if(e.part().cookingFamily()) {
            int nutrition=e.form()==OrganicForm.COOKED ? 6 : e.form()==OrganicForm.RAW ? 2 : 1;
            p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(e.form()==OrganicForm.COOKED?0.6F:0.1F).build());
        }
        return p;
    }
    public OrganismItemCatalog.Entry definition() { return definition; }
}
