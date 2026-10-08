package net.mads.industron.material.organism;

import net.mads.industron.material.defenitions.Organisms;
import java.util.*;

/** Single item lookup shared by registration, datagen, recipes and campfire runtime. */
public final class OrganismItemCatalog {
    public record Entry(String itemId, String displayName, OrganismDefinition owner,
                        OrganismPart part, OrganicForm form, BiologicalMaterial material,
                        boolean existing, String texture, boolean tinted) {}
    public static final List<Entry> ALL = entries(Organisms.ALL);
    private static final Map<String,Entry> BY_ITEM = index();
    private OrganismItemCatalog() {}
    public static Entry byItem(String id) { return BY_ITEM.get(id); }
    public static List<Entry> generated() { return ALL.stream().filter(e->!e.existing()).toList(); }
    public static Entry form(OrganismDefinition owner, OrganismPart part, OrganicForm form) {
        return ALL.stream().filter(e->e.owner()==owner && e.part()==part && e.form()==form).findFirst().orElseThrow();
    }
    public static String texture(OrganismDefinition owner,OrganismPart part,OrganicForm form) {
        if(form==OrganicForm.DUST)return "industron:item/material_sets/dust/normal/variant_1/base";
        if(form==OrganicForm.SMALL_DUST)return "industron:item/material_sets/dust/small/variant_1/base";
        if(form==OrganicForm.TINY_DUST)return "industron:item/material_sets/dust/tiny/variant_1/base";
        String group=part.cookingFamily() ? form.name().toLowerCase(Locale.ROOT)+"_"+part.suffix()
                : part.suffix();
        return "industron:item/organic_sets/"+group+"/"+variant(owner,part)+"/base";
    }
    public static String variant(OrganismDefinition owner,OrganismPart part) {
        if (!part.cookingFamily()) return "variant_1";
        var mapping=owner.parts().get(part).existingItems();
        // Generated cooked/burnt counterparts must match the silhouette of an existing raw item.
        String raw=mapping.get(OrganicForm.RAW);
        var templates=part==OrganismPart.MEAT ? List.of("beef","porkchop","chicken","mutton","rabbit")
                : List.of("cod","salmon","tropical_fish","pufferfish");
        int index=templates.indexOf(raw==null ? "" : raw.replace("minecraft:",""));
        if(index>=0) return "variant_"+(index+1);
        if(part==OrganismPart.MEAT && "minecraft:rotten_flesh".equals(mapping.get(OrganicForm.ROTTEN))) return "variant_6";
        Set<String> candidates=new TreeSet<>();
        for(int i=1;i<=templates.size();i++) candidates.add("variant_"+i);
        return OrganicVariantResolver.select(owner.id(),part.suffix(),List.of(candidates,candidates,candidates,candidates));
    }
    public static List<Entry> entries(List<OrganismDefinition> definitions) {
        definitions=List.copyOf(definitions);
        List<Entry> result=new ArrayList<>();
        Set<String> generated=new HashSet<>();
        Map<String,String> aliases=new HashMap<>();
        Map<String,String> sharedForms=new HashMap<>();
        Map<String,Entry> physicalItems=new HashMap<>();
        for(var owner:definitions) for(var part:owner.parts().entrySet()) {
            String fingerprint=signature(part.getValue().material());
            String anchor=part.getValue().existingItems().get(OrganicForm.RAW);
            if(anchor==null)anchor=part.getValue().existingItems().get(OrganicForm.ROTTEN);
            String sharedKey=anchor==null?null:part.getKey().name()+"/"+anchor+"/"+fingerprint;
            for(var form:OrganicForm.values()) {
                if(!part.getKey().forms().contains(form)) continue;
                String existing=part.getValue().existingItems().get(form);
                String id=existing==null ? "industron:"+form.registryName(owner.id(),part.getKey()) : existing;
                boolean reused=false;
                if(existing==null && sharedKey!=null) {
                    String old=sharedForms.putIfAbsent(sharedKey+"/"+form.name(),id);
                    if(old!=null) { id=old; reused=true; }
                }
                if(existing==null && !reused && !generated.add(id)) throw new IllegalStateException("Duplicate organic item: "+id);
                if(existing!=null) {
                    String old=aliases.putIfAbsent(id,fingerprint);
                    if(old!=null&&!old.equals(fingerprint)) throw new IllegalStateException("Conflicting .contains for "+id);
                }
                String override=part.getValue().textures().get(form);
                String display=title(form.registryName(owner.id(),part.getKey()));
                var entry=new Entry(id,display,owner,part.getKey(),form,part.getValue().material(),existing!=null || reused,
                        override==null?texture(owner,part.getKey(),form):override,override==null);
                var previous=physicalItems.putIfAbsent(id,entry);
                if(previous!=null) {
                    if(!signature(previous.material()).equals(fingerprint))
                        throw new IllegalArgumentException("Item has conflicting .contains: "+id);
                    if(previous.part()!=entry.part() || previous.form()!=entry.form())
                        throw new IllegalArgumentException("Item has conflicting organic forms: "+id);
                    if(reused && (previous.tinted()!=entry.tinted() || !previous.texture().equals(entry.texture())
                            || (entry.tinted() && previous.material().color()!=entry.material().color())))
                        throw new IllegalArgumentException("Shared generated item has conflicting texture/color: "+id
                                + "; aliases must agree on the presentation of their shared forms");
                }
                result.add(entry);
            }
        }
        return List.copyOf(result);
    }
    private static Map<String,Entry> index() {
        Map<String,Entry> result=new LinkedHashMap<>();
        for(var entry:ALL) result.putIfAbsent(entry.itemId(),entry);
        return Collections.unmodifiableMap(result);
    }
    private static String signature(BiologicalMaterial material) {
        int gcd=0;
        for(var c:material.components()) {int b=c.amount();while(b!=0){int r=gcd%b;gcd=b;b=r;}}
        final int divisor=gcd;
        return material.components().stream().map(c->c.substance().id()+"="+(c.amount()/divisor)).sorted().reduce("",(a,b)->a+";"+b);
    }
    private static String title(String text) {
        StringJoiner joiner=new StringJoiner(" ");
        for(String word:text.split("_"))joiner.add(Character.toUpperCase(word.charAt(0))+word.substring(1));
        return joiner.toString();
    }
}
