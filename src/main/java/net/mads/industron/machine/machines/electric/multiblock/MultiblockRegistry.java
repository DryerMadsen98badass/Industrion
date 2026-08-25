package net.mads.industron.machine.machines.electric.multiblock;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MultiblockRegistry {
    private static final List<MultiblockDefinition> DEFINITIONS = new ArrayList<>();

    private MultiblockRegistry() {
    }

    public static MultiblockDefinition register(MultiblockDefinition definition) {
        if (byController(definition.controllerId()).isPresent()) {
            return definition;
        }

        DEFINITIONS.add(definition);
        return definition;
    }

    public static List<MultiblockDefinition> all() {
        return List.copyOf(DEFINITIONS);
    }

    public static Optional<MultiblockDefinition> byController(ResourceLocation controllerId) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.controllerId().equals(controllerId))
                .findFirst();
    }

    public static ResourceLocation id(String id) {
        return ResourceLocation.parse(id.contains(":") ? id : "industron:" + id);
    }
}
