package net.mads.industron.recipe.recipetypes;

import java.util.Objects;

/**
 * Semantic assembly node.
 *
 * <p>A component never matches an item by itself. Its physical inputs live in
 * {@code ComponentDefinitions}. This is what makes {@code Component.X}
 * recursive while {@code Material.X} and string item ids remain leaf inputs.</p>
 */
public record AssemblyComponent(String id, String displayName) {
    public AssemblyComponent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        if (id.isBlank()) throw new IllegalArgumentException("Component id cannot be blank");
        if (displayName.isBlank()) throw new IllegalArgumentException("Component display name cannot be blank");
    }
}
