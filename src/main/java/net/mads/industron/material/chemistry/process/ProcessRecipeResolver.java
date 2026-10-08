package net.mads.industron.material.chemistry.process;

import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeTypeDefinition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Resolves typed physical intent to a registered process/RecipeType identity. */
public final class ProcessRecipeResolver {
    public record Resolution(ProcessKind kind, RecipeTypeDefinition recipeType) {
    }

    public record Result(Optional<Resolution> resolution, String diagnostic) {
        public Result {
            resolution = resolution == null ? Optional.empty() : resolution;
            diagnostic = diagnostic == null ? "" : diagnostic;
        }

        public static Result success(ProcessKind kind, RecipeTypeDefinition recipeType) {
            return new Result(Optional.of(new Resolution(kind, recipeType)), "");
        }

        public static Result failure(String diagnostic) {
            return new Result(Optional.empty(), diagnostic);
        }
    }

    /**
     * General resolver for chemistry/Foundry callers that only know the physical intent.
     * The closest exact capability set wins. A process is never selected merely because its IO fits.
     */
    public Result resolve(ProcessIntent intent) {
        List<ProcessKind> candidates = new ArrayList<>();
        Map<ProcessKind, String> rejected = new LinkedHashMap<>();

        for (ProcessKind kind : ProcessKind.values()) {
            ProcessRuleSet rules = ProcessRuleSet.forKind(kind);
            Set<ProcessOperation> missing = new java.util.LinkedHashSet<>(intent.requiredOperations());
            missing.removeAll(rules.operations());
            if (!missing.isEmpty()) {
                rejected.put(kind, "missing operations " + missing);
                continue;
            }

            Optional<String> semanticProblem = ProcessSemantics.validate(
                    kind, intent.inputs(), intent.outputs(), intent.requirements());
            if (semanticProblem.isPresent()) {
                rejected.put(kind, semanticProblem.orElseThrow());
                continue;
            }

            RecipeTypeDefinition recipeType = recipeType(kind);
            if (recipeType == null) {
                rejected.put(kind, "no registered RecipeTypeDefinition");
                continue;
            }
            candidates.add(kind);
        }

        candidates.sort(Comparator
                .comparingInt((ProcessKind kind) -> extraOperationCount(kind, intent.requiredOperations()))
                .thenComparingInt(ProcessKind::ordinal));

        if (candidates.isEmpty()) {
            return Result.failure(noMatchDiagnostic(intent, rejected));
        }

        ProcessKind kind = candidates.getFirst();
        return Result.success(kind, recipeType(kind));
    }

    /**
     * Exact validation used when an existing plan already selected a process identity. This keeps
     * generated plans stable while enforcing the same typed operation/state contract.
     */
    public Result resolveExact(ProcessKind requested, ProcessIntent intent) {
        ProcessRuleSet rules = ProcessRuleSet.forKind(requested);
        if (!rules.supportsAll(intent.requiredOperations())) {
            Set<ProcessOperation> missing = new java.util.LinkedHashSet<>(intent.requiredOperations());
            missing.removeAll(rules.operations());
            return Result.failure(requested + " does not support required operations " + missing + ".");
        }
        Optional<String> semanticProblem = ProcessSemantics.validate(
                requested, intent.inputs(), intent.outputs(), intent.requirements());
        if (semanticProblem.isPresent()) {
            return Result.failure(requested + " cannot accept this process state: " + semanticProblem.orElseThrow() + ".");
        }
        RecipeTypeDefinition type = recipeType(requested);
        if (type == null) {
            return Result.failure("No registered RecipeTypeDefinition maps to process " + requested + ".");
        }
        return Result.success(requested, type);
    }

    /**
     * Validation entry point for hand-authored or runtime routes that already selected a RecipeType.
     * This is the same contract used by generated chemistry and future Foundry runtime mixtures.
     */
    public Result validateRecipeType(RecipeTypeDefinition requestedType, ProcessIntent intent) {
        Optional<ProcessKind> kind = processKind(requestedType);
        if (kind.isEmpty()) {
            return Result.failure("RecipeType " + requestedType.id() + " has no Phase-06 process semantics mapping.");
        }
        ProcessKind mapped = kind.orElseThrow();
        ProcessRuleSet rules = ProcessRuleSet.forKind(mapped);
        if (!rules.supportsAll(intent.requiredOperations())) {
            Set<ProcessOperation> missing = new java.util.LinkedHashSet<>(intent.requiredOperations());
            missing.removeAll(rules.operations());
            return Result.failure("RecipeType " + requestedType.id() + " is missing operations " + missing + ".");
        }
        Optional<String> semanticProblem = ProcessSemantics.validate(
                mapped, intent.inputs(), intent.outputs(), intent.requirements());
        if (semanticProblem.isPresent()) {
            return Result.failure("RecipeType " + requestedType.id() + " rejects the process state: "
                    + semanticProblem.orElseThrow() + ".");
        }
        return Result.success(mapped, requestedType);
    }

    /** Returns the typed rules mapped to a registered RecipeType, when it represents a Phase-06 process. */
    public Optional<ProcessRuleSet> rulesFor(RecipeTypeDefinition recipeType) {
        return processKind(recipeType).map(ProcessRuleSet::forKind);
    }

    /**
     * Returns the existing registered recipe type for an already-selected process kind.
     *
     * <p>This is deliberately a lookup only: callers may use IO limits to decide whether the
     * selected chemistry can be represented, but those limits must never be used to choose a
     * different chemistry family.</p>
     */
    public Optional<RecipeTypeDefinition> recipeTypeFor(ProcessKind kind) {
        return Optional.ofNullable(recipeType(kind));
    }

    /** Reverse RecipeType -> ProcessKind mapping; controller/power identity is intentionally absent. */
    public Optional<ProcessKind> processKind(RecipeTypeDefinition recipeType) {
        if (recipeType == null) return Optional.empty();
        String path = recipeType.id().getPath();
        for (ProcessKind kind : ProcessKind.values()) {
            if (kind.recipeTypeIds().contains(path)) return Optional.of(kind);
        }
        return Optional.empty();
    }

    private static int extraOperationCount(ProcessKind kind, Set<ProcessOperation> required) {
        return Math.max(0, ProcessRuleSet.forKind(kind).operations().size() - required.size());
    }

    private static RecipeTypeDefinition recipeType(ProcessKind kind) {
        for (String id : kind.recipeTypeIds()) {
            RecipeTypeDefinition definition = CERecipeTypes.byId(RecipeTypeDefinition.id(id));
            if (definition != null) return definition;
        }
        return null;
    }

    private static String noMatchDiagnostic(ProcessIntent intent, Map<ProcessKind, String> rejected) {
        StringBuilder message = new StringBuilder("No registered process supports operations ")
                .append(intent.requiredOperations())
                .append(" for input states ")
                .append(describeInputs(intent.inputs()))
                .append('.');
        if (!intent.explanation().isBlank()) {
            message.append(" Intent: ").append(intent.explanation()).append('.');
        }

        List<String> relevant = rejected.entrySet().stream()
                .filter(entry -> ProcessRuleSet.forKind(entry.getKey()).operations().stream()
                        .anyMatch(intent.requiredOperations()::contains))
                .limit(8)
                .map(entry -> entry.getKey() + " rejected: " + entry.getValue())
                .toList();
        if (!relevant.isEmpty()) {
            message.append(" Candidate diagnostics: ").append(String.join("; ", relevant)).append('.');
        }
        return message.toString();
    }

    private static String describeInputs(List<ProcessMaterial> inputs) {
        return inputs.stream()
                .map(value -> value.materialId() + "[" + value.phase() + ", " + value.substanceState() + "]")
                .toList()
                .toString();
    }
}
