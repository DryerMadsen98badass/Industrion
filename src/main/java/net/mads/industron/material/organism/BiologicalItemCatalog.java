package net.mads.industron.material.organism;

import net.mads.industron.material.defenitions.OrganicMaterials;
import net.mads.industron.material.defenitions.AnimalMaterials;
import java.util.*;

/** Permanent shared constituents. Never duplicates plant string or an existing Minecraft item. */
public final class BiologicalItemCatalog {
    public enum Form { COMPOUND, FIBER, POWDER, TISSUE }
    public record Entry(String id, String displayName, BiologicalMaterial material, String texture, Form form) {}
    public static final List<Entry> ALL = create();
    private BiologicalItemCatalog() {}
    private static List<Entry> create() {
        List<Entry> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (BiologicalMaterial material : OrganicMaterials.BIOLOGICAL) {
            boolean fiber=material.roles().contains(OrganicRole.FIBER);
            boolean powder=material.roles().contains(OrganicRole.PROTEIN)
                    || material.roles().contains(OrganicRole.MINERAL)
                    || material.roles().contains(OrganicRole.PIGMENT)
                    || material.roles().contains(OrganicRole.TOXIN);
            String id=material.id() + (powder ? "_dust" : "");
            if (!ids.add(id)) throw new IllegalStateException("Duplicate biological item: " + id);
            String texture=fiber ? BiologicalTextureTemplates.PLANT_FIBER
                    : powder ? BiologicalTextureTemplates.DUST
                    : BiologicalTextureTemplates.BIOMASS;
            result.add(new Entry(id,material.displayName() + (powder ? " Dust" : ""),material,texture,powder?Form.POWDER:fiber?Form.FIBER:Form.COMPOUND));
            if (fiber) {
                String dustId=material.id() + "_dust";
                if (!ids.add(dustId)) throw new IllegalStateException("Duplicate fiber dust: " + dustId);
                result.add(new Entry(dustId, material.displayName() + " Dust", material,
                        BiologicalTextureTemplates.DUST,Form.POWDER));
            }
        }
        result.add(new Entry("flesh", "Flesh", AnimalMaterials.FLESH,
                BiologicalTextureTemplates.BIOMASS,Form.TISSUE));
        result.add(new Entry("hide", "Hide", AnimalMaterials.HIDE,
                BiologicalTextureTemplates.BIOMASS,Form.TISSUE));
        result.add(new Entry("bone_mineral_dust", "Bone Mineral Dust", AnimalMaterials.BONE_MINERAL,
                BiologicalTextureTemplates.DUST,Form.POWDER));
        result.add(new Entry("oiled_hide", "Oiled Hide", AnimalMaterials.LEATHER,
                BiologicalTextureTemplates.BIOMASS,Form.TISSUE));
        for(var intermediate:OrganicFractionPlanner.PLAN.intermediates()) {
            result.add(new Entry(intermediate.id(),intermediate.name(),intermediate.material(),
                    intermediate.powdered() ? BiologicalTextureTemplates.DUST
                            : BiologicalTextureTemplates.BIOMASS,
                    intermediate.powdered()?Form.POWDER:Form.COMPOUND));
        }
        result.replaceAll(entry->new Entry(entry.id(),entry.displayName(),entry.material(),
                BiologicalTextureTemplates.texture(entry.material(),entry.form(),entry.texture()),entry.form()));
        Set<String> allIds=new HashSet<>();
        for(var entry:result)if(!allIds.add(entry.id()))throw new IllegalStateException("Duplicate biological item: "+entry.id());
        return List.copyOf(result);
    }
}
