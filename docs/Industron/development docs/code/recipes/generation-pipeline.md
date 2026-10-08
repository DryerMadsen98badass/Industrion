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

## Current implementation

`ChemistryBootstrap -> ProcessPlanner -> ProcessSafetyValidator -> AutomaticChemistryRecipes -> ReflectiveRecipeEmitter` is the active generated chemistry pipeline. Registered composite materials with `DUST` can receive post-dust processing; mechanical ore preprocessing is separately emitted by `OreProcessingRecipes` and ends at that dust boundary.

Process choice is topology/property/phase driven. Generic elemental `DUST -> INGOT` is deliberately absent. Automatic liquid intermediates are registered as slurry, solution or reaction mixture when the plan needs them. Before Foundry supports reversible dynamic mixtures, the graph validator must evolve beyond its current conservative rule that rejects every directed cycle.
