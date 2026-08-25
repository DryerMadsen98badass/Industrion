package net.mads.industron.validation;

import net.mads.industron.Industron;
import net.mads.industron.validation.rules.AtomicModelValidator;
import net.mads.industron.validation.rules.AssemblyDefinitionValidator;
import net.mads.industron.validation.rules.CreateDependencyBoundaryValidator;
import net.mads.industron.validation.rules.DeterminismValidator;
import net.mads.industron.validation.rules.DomainInvariantValidator;
import net.mads.industron.validation.rules.FoundationDefinitionValidator;
import net.mads.industron.validation.rules.MaterialDefinitionValidator;
import net.mads.industron.validation.rules.StructureDefinitionValidator;

/** Single entry point for early Industron domain validation. */
public final class IndustronValidation {
    private static final ValidationPipeline PIPELINE = ValidationPipeline.builder()
            .rule(new FoundationDefinitionValidator())
            .rule(new MaterialDefinitionValidator())
            .rule(new AssemblyDefinitionValidator())
            .rule(new AtomicModelValidator())
            .rule(new StructureDefinitionValidator())
            .rule(new DomainInvariantValidator())
            .rule(new DeterminismValidator())
            .rule(new CreateDependencyBoundaryValidator())
            .build();

    private IndustronValidation() {
    }

    public static ValidationReport validate(ValidationStage stage) {
        ValidationReport report = PIPELINE.validate(ValidationContext.current(stage));
        log(stage, report);
        return report;
    }

    public static ValidationReport validateOrThrow(ValidationStage stage) {
        ValidationReport report = validate(stage);
        if (report.hasErrors()) {
            throw new ValidationException(report);
        }
        return report;
    }

    private static void log(ValidationStage stage, ValidationReport report) {
        if (report.diagnostics().isEmpty()) {
            Industron.LOGGER.info("Industron {} validation passed with no diagnostics", stage);
            return;
        }

        for (ValidationDiagnostic diagnostic : report.diagnostics()) {
            if (diagnostic.severity() == ValidationSeverity.ERROR) {
                Industron.LOGGER.error(diagnostic.formatted());
            } else {
                Industron.LOGGER.warn(diagnostic.formatted());
            }
        }
        Industron.LOGGER.info("Industron {} validation finished: {}", stage, report.summary());
    }
}
