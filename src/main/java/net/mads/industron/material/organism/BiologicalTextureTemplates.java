package net.mads.industron.material.organism;

import net.mads.industron.material.defenitions.AnimalMaterials;
import net.mads.industron.material.defenitions.OrganicMaterials;
import java.util.Map;

/** Shared grayscale silhouettes. Source assets come from the installed vanilla resource pack. */
public final class BiologicalTextureTemplates {
    private BiologicalTextureTemplates() {}
    public static final String DUST="item/material_sets/dust/normal/variant_1/base";
    public static final String PLANT_FIBER="item/plants/parts/fiber";
    public static final String BIOMASS="item/plants/intermediates/biomass";
    public static final Map<String,String> SOURCES=Map.of(
            path("flesh"),"minecraft:item/beef",
            path("hide"),"minecraft:item/rabbit_hide",
            path("oiled_hide"),"minecraft:item/leather",
            path("fat"),"minecraft:item/slime_ball",
            path("wax"),"minecraft:item/honeycomb",
            path("slime_compound"),"minecraft:item/slime_ball");
    private static String path(String group) {return "item/organic_sets/"+group+"/variant_1/base";}
    public static String texture(BiologicalMaterial material,BiologicalItemCatalog.Form form,String fallback) {
        if(form==BiologicalItemCatalog.Form.TISSUE) {
            if(material==AnimalMaterials.HIDE)return path("hide");
            if(material==AnimalMaterials.LEATHER)return path("oiled_hide");
            if(material==AnimalMaterials.FLESH)return path("flesh");
        }
        if(form==BiologicalItemCatalog.Form.COMPOUND) {
            if(material.roles().contains(OrganicRole.FAT))return path("fat");
            if(material.roles().contains(OrganicRole.WAX))return path("wax");
            if(material==OrganicMaterials.SLIME_COMPOUND)return path("slime_compound");
        }
        return fallback;
    }
}
