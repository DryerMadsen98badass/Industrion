package net.mads.industron.validation;

public final class ValidationException extends IllegalStateException {
    private final ValidationReport report;

    public ValidationException(ValidationReport report) {
        super(report == null ? "Industron validation failed" : report.formatted());
        this.report = report == null ? new ValidationReport(null) : report;
    }

    public ValidationReport report() {
        return report;
    }
}
