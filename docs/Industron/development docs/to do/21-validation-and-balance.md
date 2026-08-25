# Phase 21 – Validation, performance og balance

## Atomic/material

- [ ] Generer representative element-sett over hele intended range, inkludert ca. 100 gameplay-elementer og edge tests for høye atomnumre.
- [ ] Finn property outliers og uønsket tier-monotoni.
- [ ] Verifiser metal/gem/classification og state transitions.
- [ ] Verifiser forms, ores, wires og fluid transport etter capability changes.

## Recipe types/process physics

- [ ] Test at hver recipe type avviser unsupported channels/conditions.
- [ ] Test physical mixture separation separat fra chemical decomposition.
- [ ] Regression: egnet fiktiv Material-A/Material-B particle mixture kan være physical-separation candidate.
- [ ] Regression: en bonded fictional compound kan ikke dekomponeres til parent atoms av centrifuging/distillation uten required bond/electron operations.
- [ ] Regression: en fiktiv two-fluid mixture kan bare distilleres når phase/boiling rules tillater det.
- [ ] Test electrolysis/redox process capability mot reaction intent.

## Composition/chemistry

- [ ] Test composition flattening og cycle detection.
- [ ] Test alloy property derivation: synergy, penalties, bounds og determinisme.
- [ ] Test molecular graph, valence budgets og single/double/triple bond validity.
- [ ] Test ionic charge balancing og alternative allowed ion states.
- [ ] Test structural family classification fra actual single/double/triple bond graph over de fiktive elementene.
- [ ] Test compounds/polymers for plausible tradeoffs, spesielt insulation vs temperature/mechanical properties.
- [ ] Verifiser reaction graph uten eksplosiv vekst.
- [ ] Mål antall generated/reachable substances og recipes; sett eksplisitte safety limits.

## Recipes

- [ ] Verifiser stable generated recipe IDs og duration/tier progression.
- [ ] Test exact item, tag, substance/form og fluid recipe inputs.
- [ ] Test process duration separat fra assembly tool duration.

## Assembly

- [ ] Validate alle `ComponentDefinition` graphs for cycles, missing dependencies og impossible requirements.
- [ ] Test scalar boundaries, f.eks. 149/150/151 og 319/320/321.
- [ ] Test at minimum + alle sterkere parts/components aksepteres.
- [ ] Test svært høye gyldige ratings, inkludert values nær capability datatype-grensen.
- [ ] Test actual-value propagation: recipe minimum 150 + chosen wire 320 skal gi dependent insulation >= 320.
- [ ] Test forward-feasibility: ekstrem parent som gjør dependent requirement umulig må avvises før consumption.
- [ ] Test range coverage med boundary cases rundt `CHEMICAL_RANGE.covers(-32, 120)`.
- [ ] Test at propagated environment requirements gjelder plates/screws/components, men ikke tools/byproducts.
- [ ] Test exact item ID, item tag, MaterialPart og Component input i samme plan.
- [ ] Test tool speed: raskere tool skal fullføre tool-step raskere uten å endre fixed waits.
- [ ] Test tool durability/damage ved success og null damage ved invalid interaction.
- [ ] Performance-test assembly plan expansion/load og runtime lookup; runtime skal bruke precompiled plans/indexes, ikke recursive search per click.

### Assembly binding/tool regressions for dagens API

- [ ] Free `.input(Component.A, amount)`: verifiser at separate material-leaves kan velge forskjellige materialer.
- [ ] Fixed `.input(Component.A, Metal.X, amount)`: verifiser at alle normale descendants forblir `Metal.X`.
- [ ] `inputAny(...)` inne i fixed tree: verifiser at bare den branchen blir free.
- [ ] `atLeastParent()`/`atMostParent()`: verifiser per-descendant-form sammenligning mot fixed parent og avvis relative requirement uten entydig fixed parent.
- [ ] Verifiser at practical stat som ikke er definert for en descendant form ikke silently godtas.
- [ ] Generated Frame/Casing: manglende fysisk materialform skal kunne gi skip; missing ComponentDefinition/cycle skal være hard definition-feil.
- [ ] Human timed tool: kontinuerlig hold kreves og release/tool-switch/avstand resetter progress.
- [ ] FakePlayer/Deployer: n aktiveringer × 2 ticks må tilsvare toolens totale `useTimeTicks()`; ingen instant completion.
- [ ] Verifiser durability én gang ved fullført tool-step.
- [ ] Test server restart midt i automatisert tool-step; dagens manglende serialisering av tool-progress skal enten dokumenteres eller implementeres før dette regnes som robust.

## System integration

- [ ] Verifiser Phase 04 casing qualification, stable generated IDs/assets og effective tier där designet bruker det.
- [ ] Verifiser Phase 07 Foundry size variants, exact heater footprint matching, cached heat-link og thermal scaling.
- [ ] Simuler maintenance over lange driftsperioder.
- [ ] Lag startup validation report og optional debug export for elements, generated substances, rejected recipes og assembly plans.

## Ferdig når

De nye domain-systemene er deterministiske, boundary-testet og kan kjøres i stor skala uten combinatorial explosion, runtime-search eller skjulte invalid states.
