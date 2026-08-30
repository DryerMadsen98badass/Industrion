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

## Current material-autorecipe integration

For the current implementation, generated composition processing is scoped to StoneMaterial, WoodMaterial and raw ore-source materials. See `material-autorecipes.md`.

The feed boundary is DUST for stone/raw ore-source and WOOD_PULP for wood. Ore-to-dust processing is deliberately later. Recipe tier is one tier below the resolved source tier, clamped at ULV. Process choice must be topology/property/phase driven and must respect actual RecipeType IO limits.
