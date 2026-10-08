package net.mads.industron.material.chemistry.process;

import java.util.List;
import java.util.Set;

/**
 * Abstract process demand used between chemistry and RecipeType selection.
 * It carries physical operations and state, never a controller name or power source.
 */
public record ProcessIntent(
        Set<ProcessOperation> requiredOperations,
        List<ProcessMaterial> inputs,
        List<ProcessMaterial> outputs,
        List<ProcessRequirement> requirements,
        int requiredTemperature,
        String explanation
) {
    public ProcessIntent {
        requiredOperations = requiredOperations == null ? Set.of() : Set.copyOf(requiredOperations);
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        outputs = outputs == null ? List.of() : List.copyOf(outputs);
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        requiredTemperature = Math.max(0, requiredTemperature);
        explanation = explanation == null ? "" : explanation;
        if (requiredOperations.isEmpty()) {
            throw new IllegalArgumentException("ProcessIntent requires at least one typed operation");
        }
    }

    public static ProcessIntent fromStep(ProcessStep step) {
        return new ProcessIntent(
                ProcessRuleSet.forKind(step.kind()).operations(),
                step.inputs(),
                step.outputs(),
                step.requirements(),
                step.requiredTemperature(),
                step.explanation()
        );
    }
}
