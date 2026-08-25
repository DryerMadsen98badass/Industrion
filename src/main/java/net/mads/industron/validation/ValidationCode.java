package net.mads.industron.validation;

/**
 * Typed validation codes. New systems should extend this enum instead of inventing
 * free-form error prefixes in exception strings.
 */
public enum ValidationCode {
    INVALID_DEFINITION,
    INVALID_ID,
    INVALID_REFERENCE,
    DUPLICATE_ID,
    DUPLICATE_SYMBOL,
    DUPLICATE_ATOMIC_NUMBER,
    MISSING_RESOURCE,
    COMPOSITION_CYCLE,
    COMPONENT_CYCLE,
    ASSEMBLY_CYCLE,
    INVALID_CLASSIFICATION,
    INVALID_ELECTRICAL_BEHAVIOR,
    INVALID_ION_STATE,
    INCONSISTENT_DERIVED_DATA,
    NON_DETERMINISTIC_OUTPUT,
    DOMAIN_INVARIANT_FAILED,
    VALIDATOR_FAILURE
}
