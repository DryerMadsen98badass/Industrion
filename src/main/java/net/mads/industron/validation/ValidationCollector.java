package net.mads.industron.validation;

import java.util.ArrayList;
import java.util.List;

/** Mutable collector used only while a validation pass is running. */
public final class ValidationCollector {
    private final List<ValidationDiagnostic> diagnostics = new ArrayList<>();

    public void error(
            ValidationSubsystem subsystem,
            ValidationCode code,
            String subject,
            String message
    ) {
        add(ValidationSeverity.ERROR, subsystem, code, subject, message);
    }

    public void warning(
            ValidationSubsystem subsystem,
            ValidationCode code,
            String subject,
            String message
    ) {
        add(ValidationSeverity.WARNING, subsystem, code, subject, message);
    }

    public void add(
            ValidationSeverity severity,
            ValidationSubsystem subsystem,
            ValidationCode code,
            String subject,
            String message
    ) {
        diagnostics.add(new ValidationDiagnostic(severity, subsystem, code, subject, message));
    }

    public ValidationReport build() {
        return new ValidationReport(diagnostics);
    }
}
