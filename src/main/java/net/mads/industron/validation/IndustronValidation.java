package net.mads.industron.validation;

import net.mads.industron.Industron;
import net.mads.industron.validation.rules.AtomicModelValidator;
import net.mads.industron.validation.rules.AssemblyDefinitionValidator;
import net.mads.industron.validation.rules.CreateDependencyBoundaryValidator;
import net.mads.industron.validation.rules.DeterminismValidator;
import net.mads.industron.validation.rules.DomainInvariantValidator;
import net.mads.industron.validation.rules.FoundationDefinitionValidator;
import net.mads.industron.validation.rules.MaterialDefinitionValidator;
import net.mads.industron.validation.rules.PlantDefinitionValidator;
import net.mads.industron.validation.rules.MaterialCasingDefinitionValidator;
import net.mads.industron.validation.rules.StructureDefinitionValidator;

/** Single entry point for early Industron domain validation. */
public final class IndustronValidation {
    private static volatile ValidationReport successfulStartupReport;

    private static final ValidationPipeline PIPELINE = ValidationPipeline.builder()
            .rule(new FoundationDefinitionValidator())
            .rule(new MaterialDefinitionValidator())
            .rule(new PlantDefinitionValidator())
            .rule(new MaterialCasingDefinitionValidator())
            .rule(new AssemblyDefinitionValidator())
            .rule(new AtomicModelValidator())
            .rule(new StructureDefinitionValidator())
            .rule(new DomainInvariantValidator())
            .rule(new DeterminismValidator())
            .build();

    private IndustronValidation() {
    }

    public static ValidationReport validate(ValidationStage stage) {
        // A data run constructs the mod first, so STARTUP validation has already checked
        // the same immutable definition snapshot. Do not repeat the full pipeline.
        ValidationReport cached = successfulStartupReport;
        if (stage == ValidationStage.DATAGEN && cached != null) {
            Industron.LOGGER.info("Industron DATAGEN validation reused successful STARTUP validation");
            ValidationReport report = withBoundaryCheck(stage, cached);
            log(stage, report);
            return report;
        }

        ValidationReport report = PIPELINE.validate(ValidationContext.current(stage));
        if (stage == ValidationStage.DATAGEN || Boolean.getBoolean("industron.validateCreateBoundary")) {
            report = withBoundaryCheck(stage, report);
        }
        if (stage == ValidationStage.STARTUP && !report.hasErrors()) {
            successfulStartupReport = report;
        }
        log(stage, report);
        return report;
    }

    private static ValidationReport withBoundaryCheck(ValidationStage stage, ValidationReport base) {
        ValidationCollector collector = new ValidationCollector();
        new CreateDependencyBoundaryValidator().validate(ValidationContext.current(stage), collector);
        java.util.List<ValidationDiagnostic> combined = new java.util.ArrayList<>(base.diagnostics());
        combined.addAll(collector.build().diagnostics());
        return new ValidationReport(combined);
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
