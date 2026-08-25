package net.mads.industron.validation;

@FunctionalInterface
public interface ValidationRule {
    void validate(ValidationContext context, ValidationCollector diagnostics);
}
