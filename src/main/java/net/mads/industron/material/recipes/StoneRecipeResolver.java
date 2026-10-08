package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Shared StoneMaterial -> physical registry-id resolution for generated stone recipes. */
public final class StoneRecipeResolver {
    private StoneRecipeResolver() {
    }

    public static Optional<ResourceLocation> block(StoneMaterial stone, MaterialPart part) {
        if (stone == null || part == null || !part.isBlock() || stone.isWithout(part)) {
            return Optional.empty();
        }
        if (stone.hasExistingPart(part)) {
            return Optional.of(stone.existingPart(part));
        }
        return StructureMaterialGenerator.blockDefinitions(stone).stream()
                .filter(definition -> definition.part().orElse(null) == part)
                .map(StructureBlockDefinition::registryName)
                .map(id -> ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, id))
                .findFirst();
    }

    /** Resolves the first MaterialPart that actually exists for this StoneMaterial. */
    public static Optional<ResourceLocation> block(StoneMaterial stone, MaterialPart preferred, MaterialPart... fallbacks) {
        Optional<ResourceLocation> resolved = block(stone, preferred);
        if (resolved.isPresent()) return resolved;
        if (fallbacks == null) return Optional.empty();
        for (MaterialPart fallback : fallbacks) {
            resolved = block(stone, fallback);
            if (resolved.isPresent()) return resolved;
        }
        return Optional.empty();
    }

    public static Optional<ResourceLocation> item(StoneMaterial stone, MaterialPart part) {
        if (stone == null || part == null || part.isFluid() || stone.isWithout(part)) {
            return Optional.empty();
        }
        if (stone.hasExistingPart(part)) {
            return Optional.of(stone.existingPart(part));
        }
        if (part.isBlock()) {
            return block(stone, part);
        }
        if (!StructureMaterialGenerator.generatedItemForms(stone).contains(part)) {
            return Optional.empty();
        }
        return Optional.of(ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                part.registryName(stone)
        ));
    }

    /** Physical block placed by one Pebble item. */
    public static Optional<ResourceLocation> loosePebbleBlock(StoneMaterial stone) {
        if (item(stone, MaterialPart.PEBBLE).isEmpty()) return Optional.empty();
        return Optional.of(ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                stone.id() + "_loose_pebble"
        ));
    }

}
