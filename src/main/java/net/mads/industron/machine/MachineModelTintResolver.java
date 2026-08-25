package net.mads.industron.machine;

import net.mads.industron.Industron;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlocks;
import org.jetbrains.annotations.Nullable;

public final class MachineModelTintResolver {
    private MachineModelTintResolver() {
    }

    @Nullable
    public static Integer resolve(String model) {
        if (model == null || model.isBlank()) {
            return null;
        }

        String namespace = Industron.MOD_ID;
        String path = model;
        int separator = model.indexOf(':');
        if (separator >= 0) {
            namespace = model.substring(0, separator);
            path = model.substring(separator + 1);
        }

        if (!Industron.MOD_ID.equals(namespace)) {
            return null;
        }

        if (path.startsWith("block/")) {
            path = path.substring("block/".length());
        }

        String modelId = path.substring(path.lastIndexOf('/') + 1);
        for (SimpleBlockDefinition definition : SimpleBlocks.ALL) {
            if (definition.id().equals(modelId) && definition.hasColor()) {
                return definition.blockColor();
            }
        }

        for (MachineTier tier : MachineTier.ALL) {
            if (tier.casingRegistryName().equals(modelId)) {
                return tier.color();
            }
        }

        return null;
    }
}
