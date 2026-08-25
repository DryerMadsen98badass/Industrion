package net.mads.industron.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ordered, deterministic validation pipeline. A broken validator becomes a normal
 * diagnostic so later validators can still report independent problems in the same pass.
 */
public final class ValidationPipeline {
    private final List<ValidationRule> rules;

    private ValidationPipeline(List<ValidationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static Builder builder() {
        return new Builder();
    }

    public ValidationReport validate(ValidationContext context) {
        Objects.requireNonNull(context, "context");
        ValidationCollector diagnostics = new ValidationCollector();
        for (ValidationRule rule : rules) {
            try {
                rule.validate(context, diagnostics);
            } catch (RuntimeException exception) {
                diagnostics.error(
                        ValidationSubsystem.FOUNDATION,
                        ValidationCode.VALIDATOR_FAILURE,
                        rule.getClass().getName(),
                        exception.getClass().getSimpleName() + ": " + safeMessage(exception)
                );
            }
        }
        return diagnostics.build();
    }

    public static final class Builder {
        private final List<ValidationRule> rules = new ArrayList<>();

        public Builder rule(ValidationRule rule) {
            rules.add(Objects.requireNonNull(rule, "rule"));
            return this;
        }

        public ValidationPipeline build() {
            return new ValidationPipeline(rules);
        }
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "no message" : message;
    }
}
