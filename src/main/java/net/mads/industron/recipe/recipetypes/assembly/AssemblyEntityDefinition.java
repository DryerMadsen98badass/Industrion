package net.mads.industron.recipe.recipetypes.assembly;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Code-first entity form used by Assembly bases/results.
 *
 * <p>The id is an Assembly identity, not necessarily an EntityType registry id. This matters for
 * entities such as vanilla boats: every wood species shares the same EntityType but still needs a
 * species-specific Assembly recipe. The matcher/factory therefore own the exact entity semantics,
 * while the display stack gives JEI an ordinary item representation.</p>
 */
public final class AssemblyEntityDefinition {
    private static final Map<ResourceLocation, AssemblyEntityDefinition> DEFINITIONS = new LinkedHashMap<>();

    private final ResourceLocation id;
    private final Supplier<ItemStack> displayStack;
    private final Predicate<Entity> matcher;
    private final Function<ServerLevel, ? extends Entity> factory;

    private AssemblyEntityDefinition(
            ResourceLocation id,
            Supplier<ItemStack> displayStack,
            Predicate<Entity> matcher,
            Function<ServerLevel, ? extends Entity> factory
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayStack = Objects.requireNonNull(displayStack, "displayStack");
        this.matcher = Objects.requireNonNull(matcher, "matcher");
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    public static AssemblyEntityDefinition entity(
            ResourceLocation id,
            Supplier<ItemStack> displayStack,
            Predicate<Entity> matcher,
            Function<ServerLevel, ? extends Entity> factory
    ) {
        AssemblyEntityDefinition definition = new AssemblyEntityDefinition(id, displayStack, matcher, factory);
        AssemblyEntityDefinition previous = DEFINITIONS.putIfAbsent(id, definition);
        return previous == null ? definition : previous;
    }

    public static AssemblyEntityDefinition require(ResourceLocation id) {
        AssemblyEntityDefinition definition = DEFINITIONS.get(id);
        if (definition == null) {
            throw new IllegalStateException("Unknown Assembly entity definition: " + id);
        }
        return definition;
    }

    public ResourceLocation id() {
        return id;
    }

    public boolean matches(Entity entity) {
        return entity != null && entity.isAlive() && matcher.test(entity);
    }

    public ItemStack displayStack() {
        ItemStack stack = displayStack.get();
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public Entity spawn(ServerLevel level, Vec3 position, float yRot) {
        Entity entity = factory.apply(Objects.requireNonNull(level, "level"));
        if (entity == null) {
            throw new IllegalStateException("Assembly entity factory returned null for " + id);
        }
        entity.setPos(position.x, position.y, position.z);
        entity.setYRot(yRot);
        entity.setXRot(0.0F);
        if (!level.addFreshEntity(entity)) {
            throw new IllegalStateException("Could not spawn Assembly entity output " + id);
        }
        return entity;
    }
}
