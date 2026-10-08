package net.mads.industron.material.recipes;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyComponent;
import net.mads.industron.recipe.recipetypes.assembly.ComponentDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.mads.industron.recipe.recipetypes.assembly.ComponentDefinition.component;

/** Per-wood semantic component: one Bark item mounted with one Mallet operation. */
public final class WoodAssemblyComponents {
    private static final Map<String, AssemblyComponent> BARK_FASTENING = new LinkedHashMap<>();
    public static final List<ComponentDefinition> DEFINITIONS = buildDefinitions();

    private WoodAssemblyComponents() {
    }

    public static AssemblyComponent barkFastening(WoodMaterial wood) {
        AssemblyComponent component = BARK_FASTENING.get(wood.id());
        if (component == null) {
            throw new IllegalArgumentException("No bark fastening component for wood " + wood.id());
        }
        return component;
    }

    private static List<ComponentDefinition> buildDefinitions() {
        List<ComponentDefinition> result = new ArrayList<>();
        for (WoodMaterial wood : WoodMaterials.ALL) {
            if (!WoodRecipeIds.has(wood, MaterialPart.BARK)) continue;
            AssemblyComponent bark = new AssemblyComponent(
                    wood.id() + "_bark_fastening",
                    wood.displayName() + " Bark Fastening"
            );
            BARK_FASTENING.put(wood.id(), bark);
            result.add(component(bark)
                    .input(WoodRecipeIds.stringId(wood, MaterialPart.BARK))
                    .tool(Tool.MALLET, 1)
                    .build());
        }
        return List.copyOf(result);
    }
}
