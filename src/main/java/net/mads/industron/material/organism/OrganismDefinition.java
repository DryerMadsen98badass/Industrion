package net.mads.industron.material.organism;

import net.mads.industron.material.MaterialComponent;
import java.util.*;

/** Flat fluent API: .part selects the receiver of subsequent .contains/.color/.existing/.loot. */
public final class OrganismDefinition {
    public enum LootMode { PRESERVE_EXISTING, REWRITE_EXISTING, REPLACE, EMPTY }
    public record Part(BiologicalMaterial material, Map<OrganicForm,String> existingItems,
                       Map<OrganicForm,String> textures, Map<String,OrganicForm> replacements, List<OrganismLoot> loot) {
        public Part {
            Objects.requireNonNull(material);
            existingItems = Map.copyOf(existingItems);
            textures = Map.copyOf(textures);
            replacements = Map.copyOf(replacements);
            loot = List.copyOf(loot);
        }
    }
    private final String id, name, entity;
    private final Map<OrganismPart,Part> parts;
    private final LootMode lootMode;
    private OrganismDefinition(Builder b) {
        id=b.id; name=b.name; entity=b.entity;
        lootMode=Objects.requireNonNull(b.lootMode, "Declare preserveLoot(), replaceLoot() or emptyLoot()");
        EnumMap<OrganismPart,Part> result = new EnumMap<>(OrganismPart.class);
        for (var entry : b.parts.entrySet()) {
            OrganismPart kind = entry.getKey(); MutablePart p = entry.getValue();
            if (p.color == null) throw new IllegalArgumentException(id + "/" + kind + " needs .color(...)");
            var material = new BiologicalMaterial(id + "_" + kind.suffix(), name + " " + title(kind.suffix()),
                    p.color, Set.of(), p.components);
            for (OrganismLoot loot : p.loot) {
                if (!kind.forms().contains(loot.form())) throw new IllegalArgumentException("Unsupported drop form: " + kind);
                if (loot.cookWhenBurning() && (loot.form() != OrganicForm.RAW || !kind.cookingFamily()))
                    throw new IllegalArgumentException("Only raw cooking-family loot supports cooking");
            }
            result.put(kind, new Part(material, p.existing, p.textures, p.replacements, p.loot));
        }
        boolean hasLoot = result.values().stream().anyMatch(p -> !p.loot().isEmpty());
        if (lootMode != LootMode.REPLACE && lootMode != LootMode.REWRITE_EXISTING && hasLoot) throw new IllegalArgumentException("New drops require replaceLoot()");
        if (lootMode == LootMode.REPLACE && !hasLoot) throw new IllegalArgumentException("Use emptyLoot() for intentionally empty loot");
        if ((lootMode == LootMode.PRESERVE_EXISTING || lootMode == LootMode.REWRITE_EXISTING) && entity == null) throw new IllegalArgumentException("No existing entity loot to preserve");
        Set<String> replaced = new HashSet<>();
        for (Part p : result.values()) for (String item : p.replacements().keySet()) {
            if (lootMode != LootMode.REWRITE_EXISTING) throw new IllegalArgumentException("Drop replacement requires rewriteExistingLoot()");
            if (!replaced.add(item)) throw new IllegalArgumentException("Conflicting replacement of " + item);
        }
        parts=Collections.unmodifiableMap(result);
    }
    public static Builder organism(String id, String name) { return new Builder(id,name); }
    public String id() { return id; }
    public String displayName() { return name; }
    public Optional<String> existingEntity() { return Optional.ofNullable(entity); }
    public Map<OrganismPart,Part> parts() { return parts; }
    public LootMode lootMode() { return lootMode; }
    private static String title(String value) {
        StringBuilder s = new StringBuilder();
        for (String word : value.split("_")) { if (s.length()>0) s.append(' '); s.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)); }
        return s.toString();
    }
    private static String resource(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) throw new IllegalArgumentException("Expected namespaced resource: " + id);
        return id;
    }
    private static final class MutablePart {
        Integer color;
        List<MaterialComponent> components = List.of();
        final Map<OrganicForm,String> existing = new EnumMap<>(OrganicForm.class);
        final Map<OrganicForm,String> textures = new EnumMap<>(OrganicForm.class);
        final Map<String,OrganicForm> replacements = new LinkedHashMap<>();
        final List<OrganismLoot> loot = new ArrayList<>();
    }
    public static final class Builder {
        private final String id,name;
        private String entity;
        private LootMode lootMode;
        private OrganismPart current;
        private final Map<OrganismPart,MutablePart> parts = new EnumMap<>(OrganismPart.class);
        private Builder(String id,String name) {
            if (id == null || !id.matches("[a-z0-9_]+") || name == null || name.isBlank()) throw new IllegalArgumentException("Invalid organism identity");
            this.id=id; this.name=name;
        }
        /** Before a part this binds the entity; inside a part this binds its raw item. */
        public Builder existing(String entity) {
            if (current != null) return existing(OrganicForm.RAW,entity);
            this.entity=resource(entity); return this;
        }
        public Builder rewriteExistingLoot() { lootMode=LootMode.REWRITE_EXISTING; return this; }
        public Builder preserveLoot() { lootMode=LootMode.PRESERVE_EXISTING; return this; }
        public Builder replaceLoot() { lootMode=LootMode.REPLACE; return this; }
        public Builder emptyLoot() { lootMode=LootMode.EMPTY; return this; }
        public Builder part(OrganismPart part) {
            Objects.requireNonNull(part);
            if (parts.containsKey(part)) throw new IllegalArgumentException("Duplicate part: " + part);
            current=part; parts.put(part,new MutablePart()); return this;
        }
        private MutablePart selected() {
            if (current==null) throw new IllegalStateException("Select .part(...) first");
            return parts.get(current);
        }
        public Builder color(int rgb) { selected().color=rgb; return this; }
        public Builder contains(MaterialComponent... values) { selected().components=List.of(values); return this; }
        public Builder existing(OrganicForm form,String item) {
            MutablePart part=selected(); validateForm(form);
            if (part.textures.containsKey(form)) throw new IllegalArgumentException("Existing items retain their own texture");
            if (part.existing.putIfAbsent(form,resource(item))!=null) throw new IllegalArgumentException("Duplicate existing form");
            return this;
        }
        /** The default texture override applies to the selected raw part. */
        public Builder texture(String texture) { return texture(OrganicForm.RAW,texture); }
        /** Explicit textures are always untinted. */
        public Builder texture(OrganicForm form,String texture) {
            MutablePart part=selected(); validateForm(form);
            if (part.existing.containsKey(form)) throw new IllegalArgumentException("Existing items retain their own texture");
            if (part.textures.putIfAbsent(form,resource(texture))!=null) throw new IllegalArgumentException("Duplicate texture override");
            return this;
        }
        private void validateForm(OrganicForm form) {
            if (!current.forms().contains(form)) throw new IllegalArgumentException(current + " does not support " + form);
        }
        public Builder replaceDrop(String originalItem, OrganicForm target) {
            MutablePart part=selected(); validateForm(target);
            if (part.replacements.putIfAbsent(resource(originalItem),target)!=null)
                throw new IllegalArgumentException("Duplicate drop replacement");
            return this;
        }
        public Builder loot(OrganismLoot.Builder loot) { selected().loot.add(loot.build()); return this; }
        public OrganismDefinition build() { return new OrganismDefinition(this); }
    }
}
