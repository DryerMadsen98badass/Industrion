package net.mads.industron.validation;

import java.util.List;

public record ValidationReport(List<ValidationDiagnostic> diagnostics) {
    public ValidationReport {
        diagnostics = diagnostics == null
                ? List.of()
                : diagnostics.stream().sorted().toList();
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity() == ValidationSeverity.ERROR);
    }

    public long errorCount() {
        return diagnostics.stream().filter(d -> d.severity() == ValidationSeverity.ERROR).count();
    }

    public long warningCount() {
        return diagnostics.stream().filter(d -> d.severity() == ValidationSeverity.WARNING).count();
    }

    public List<ValidationDiagnostic> errors() {
        return diagnostics.stream().filter(d -> d.severity() == ValidationSeverity.ERROR).toList();
    }

    public List<ValidationDiagnostic> warnings() {
        return diagnostics.stream().filter(d -> d.severity() == ValidationSeverity.WARNING).toList();
    }

    public String summary() {
        return errorCount() + " error(s), " + warningCount() + " warning(s)";
    }

    public String formatted() {
        if (diagnostics.isEmpty()) return "Industron validation: no diagnostics";
        StringBuilder result = new StringBuilder("Industron validation: ")
                .append(summary());
        for (ValidationDiagnostic diagnostic : diagnostics) {
            result.append(System.lineSeparator())
                    .append(" - ")
                    .append(diagnostic.formatted());
        }
        return result.toString();
    }
}
