# Phase 14 – Automatic process- og recipe-generation

Den generelle CE recipe/runtime-infrastrukturen finnes allerede. Phase 05 definerer recipe types, Phase 06 deres fysiske process rules, og Phase 08–13 produserer validated substances/reactions. Denne fasen kobler dem sammen.

## 1. Typed RecipeRequest

- [ ] Definer typed `RecipeRequest` som peker på canonical substance/material/component references, ikke string material-lookups.
- [ ] `RecipeRequest` skal inneholde inputs/outputs, process intent, conditions, amount, complexity/difficulty og stable source identity.
- [ ] Ikke balansere chemistry på nytt her; requesten skal allerede være validert av Phase 13.

## 2. Process selection

- [ ] Map `ProcessIntent` til en recipe type som støtter alle nødvendige operations fra Phase 06.
- [ ] Ingen «best guess» fallback dersom process physics ikke matcher.
- [ ] Hvis flere processes er gyldige kan en deterministic priority/cost rule velge default eller generere flere legitime process paths.

Fiktive eksempelretninger:

```text
Physical Material-A / Material-B particle mixture
-> SEPARATE_PHYSICAL_PHASES
-> passende physical separator kan være kandidat

Bonded Compound X decomposition
-> bond breaking + eventuell electron transfer
-> electrochemical/chemical process kan være kandidat
-> physical separator er ugyldig

Fluid mixture of Substance C + Substance D
-> fractionate by boiling/volatility
-> DISTILLATION/FRACTIONATION kan være kandidat etter Process Rules
```

## 3. Inputs/outputs

- [ ] Støtt exact item/block IDs når process faktisk krever akkurat den vanilla/mod itemen.
- [ ] Støtt typed item tags når enhver item i taggen er valid.
- [ ] Støtt substance/material-form inputs for generated materials.
- [ ] Støtt fluids/gases og amounts gjennom typed fluid/substance-system.
- [ ] Hold process recipe inputs atskilt fra capability-based assembly requirements; de løser forskjellige problemer.

## 4. Tier, duration og energy

Rekkefølgen skal være:

```text
Er reaction/process fysisk mulig?
-> Hvilken process kan utføre den?
-> Hvor vanskelig er den?
-> minimum tier / temperature / CB/andre relevante conditions
-> base duration
-> machine-owned CE/t/resource usage
```

- [ ] Generer stable recipe IDs fra request ID eller canonical reaction/process signature.
- [ ] Deriver minimum tier fra process complexity, material/reaction properties og required conditions.
- [ ] Deriver base duration fra process family/type, bond/phase work, amount, temperature/CB/condition distance og tier.
- [ ] Duration skal være deterministisk og testbar.
- [ ] Recipe angir processing requirement/tier; machine/tier-systemet eier CE/t der dagens arkitektur bestemmer det.
- [ ] Definer overclocking separat fra canonical base duration.

## 5. Alloys

- [ ] Alloy mixing/smelting requests skal bruke actual component ratios og alloy phase requirements.
- [ ] Powder mixture recipe og finished alloy recipe er forskjellige states/operations.
- [ ] Generated alloy properties/tier kan påvirke required temperature, process tier og duration; Foundry integration følger Phase 07 heat/melting contract.

## 6. Generated material recipe paths og diagnostics

- [ ] `material/recipes/` er canonical home for automatic recipes fra `StoneMaterial`, `GemMaterial`, `WoodMaterial`, `IndustrialMaterial` og senere generated substances.
- [ ] Hold generated material recipes atskilt fra håndskrevne `recipe/recipes/<type>/<tier>...` files.

## 7. Diagnostics og generated content

- [ ] Rapporter rejected generated recipes med konkret reason/path.
- [ ] Implementer datapack reload bare for rules/requests som faktisk kan endres etter registry freeze.
- [ ] Lag custom Industron recipes som erstatter suppressede Minecraft/Create material recipes.
- [ ] Verifiser at reachable-substance + reaction + recipe generation ikke lager uendelige chains.

## Ferdig når

Alloy/chemistry-systemet kan produsere deterministic CE recipes med fysisk korrekt process type, riktige inputs/outputs, tier og duration uten håndskrevne per-material copies.
