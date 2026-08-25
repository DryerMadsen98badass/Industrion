# Recipe generation pipeline

```text
RecipeRequest
  -> resolve inputs/target
  -> choose matching process/reaction rules
  -> build candidate compounds
  -> derive required co-reactants/byproducts
  -> balance reaction
  -> validate tier/context
  -> derive temperature/pressure/duration
  -> read tier EnergyProfile
  -> map to machine recipe type
  -> publish generated recipe
```

## Recipe identity

Generated recipe IDs må være stabile mellom oppstarter. ID skal bygges fra request ID eller en kanonisk, sortert reaction signature. Kollisjoner skal gi feil, ikke overskriving.

## Duration

Hvordan duration beregnes er åpent. Foretrukket retning er en formel basert på process family, reaction complexity, temperature/pressure og tier. Energy rate skal fortsatt komme kun fra tier-profilen.
