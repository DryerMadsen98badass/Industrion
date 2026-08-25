package net.mads.industron.validation;

import java.util.Objects;

public record ValidationDiagnostic(
        ValidationSeverity severity,
        ValidationSubsystem subsystem,
        ValidationCode code,
        String subject,
        String message
) implements Comparable<ValidationDiagnostic> {
    public ValidationDiagnostic {
        severity = Objects.requireNonNull(severity, "severity");
        subsystem = Objects.requireNonNull(subsystem, "subsystem");
        code = Objects.requireNonNull(code, "code");
        subject = normalize(subject, "global");
        message = normalize(message, "No diagnostic message supplied");
    }

    public String formatted() {
        return "[" + severity + "][" + subsystem + "][" + code + "][" + subject + "] " + message;
    }

    @Override
    public int compareTo(ValidationDiagnostic other) {
        int severityOrder = severity.compareTo(other.severity);
        if (severityOrder != 0) return severityOrder;
        int subsystemOrder = subsystem.compareTo(other.subsystem);
        if (subsystemOrder != 0) return subsystemOrder;
        int codeOrder = code.compareTo(other.code);
        if (codeOrder != 0) return codeOrder;
        int subjectOrder = subject.compareTo(other.subject);
        if (subjectOrder != 0) return subjectOrder;
        return message.compareTo(other.message);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
