# Chemistry rule system

## To typer regler

### Core Java rules

Brukes når en ny algoritme er nødvendig, som ionic formula construction, covalent bonding, decomposition eller balancing.

### Datadrevne rules

Brukes for terskler, contexts, prioritet og konfigurasjon av eksisterende rule types. Disse kan lastes på datapack reload.

## Foreslått interface – ikke implementert

```java
interface ChemistryRule {
    RuleId id();
    int priority();
    RulePhase phase();
    boolean matches(ReactionContext context);
    RuleResult apply(ReactionContext context);
}
```

## Nye regler

- Ny algoritme: opprett en ny Java rule type, tester og data codec.
- Ny variant av eksisterende algoritme: legg til en datafil som bruker en registrert rule type.
- Regelrekkefølge skal være stabil: phase, priority og rule ID.
- Konflikter skal enten løses med eksplisitt prioritet eller rapporteres som ambiguity.

## Reload

Reload kan erstatte data rules og regenerere reaction graph/recipes, men kan ikke uten videre registrere nye Minecraft-items etter registry-fasen.
