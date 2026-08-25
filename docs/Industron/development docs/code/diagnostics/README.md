# Diagnostics and validation

Genereringsfeil skal være handlingsrettede.

## Minimumsfelter i en feil

- error code;
- phase;
- source file eller definition ID;
- rule ID;
- berørte elementer/compounds;
- konkret årsak;
- forslag til hva som mangler når det er mulig.

## Eksempel

```text
INDUSTRON-CHEM-0042
Phase: REACTION_VALIDATION
Request: industron:recipes/varelium_acid
Rule: industron:chemistry/acid_formation
Problem: Balanced reaction requires compound "ax2q", but the formula is not valid under current ion rules.
Action: Define a valid formation path or change the requested target.
```

Ved oppstart bør systemet skrive en summary med antall elementer, ion states, generated forms, compounds, recipes, casing variants, warnings og errors.
