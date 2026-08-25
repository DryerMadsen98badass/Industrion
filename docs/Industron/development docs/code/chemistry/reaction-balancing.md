# Reaction balancing and validation

## Pipeline

1. Samle foreslåtte reactants og products.
2. Bygg en matrise med atomtall per formula.
3. Løs heltallskoeffisienter.
4. Kontroller total charge.
5. Kontroller state/context-krav.
6. Kontroller at alle co-reactants og byproducts finnes eller kan genereres.
7. Kontroller at reaction energy/stability er tillatt av en regel.
8. Returner balanced reaction eller konkret feil.

## Feileksempel

```text
REACTION_MISSING_COMPOUND
Request: industron:produce_x
Rule: industron:chemistry/oxidation
Required byproduct: q2r
Reason: formula is required by conservation, but no valid compound can be built.
```

Ingen fallback skal stille opp en ubalansert eller oppdiktet recipe.
