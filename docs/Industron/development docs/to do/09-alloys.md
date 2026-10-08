# Phase 09 – Definerte alloys og uklassifiserte blandinger

**Status:** Dagens chemistry kan klassifisere `ALLOY`, generere `ALLOYING`-syntese for passende deklarerte materialer og velge molten electrorefining for metallic lattice-separasjon. Det mangler fortsatt runtime alloy-resolution i Foundry, dynamiske udefinerte blandinger og full alloy-propertymodell.

## Definert alloy

- [ ] En definert alloy bruker den eksisterende `.contains(component(...), ...)`-compositionen med 2+ komponenter.
- [ ] Ratio normaliseres med GCD og sorterte stabile substance-ID-er.
- [ ] `1 A + 2 B` og `2 A + 4 B` er samme ratio; `2 A + 2 B` er ikke match.
- [ ] `.contains(...)` kan referere til elementer, metaller, alloys og andre composite substances.
- [ ] Match direct top-level signature før flattened elemental signature.
- [ ] Flattened signature brukes til massebalanse; den alene beviser ikke samme structure.
- [ ] Ambiguous definitions med samme flattened composition, men forskjellig topology, skal avvises/rapporteres.
- [ ] Exact alloy index bygges én gang ved load/reload; ingen scan av alle materials ved hver tank tick.

## Uklassifisert mixture

- [ ] En ikke-matchende ratio forblir en virkelig `UNCLASSIFIED_MOLTEN_MIXTURE`; systemet skal ikke finne «closest alloy».
- [ ] UI viser bare faktisk innhold, mengder, temperatur, structure/state og eventuell validert separation route.
- [ ] UI skal ikke vise manglende mengde til en mulig alloy.
- [ ] Bruk generic registry carriers med data components i stedet for ett nytt registry item/fluid per tilfeldig blanding.
- [ ] Støtt minst 20 constituents med bounded payload og canonical serialization.
- [ ] Uklassifisert mixture kan støpes til hot generic form, kjøles, males til dust og separeres igjen uten å miste composition.
- [ ] Stack/tank merging krever identisk canonical payload og kompatibel thermal/state.

## To composition-ledgers

1. **Top-level constituent ledger** bevarer hva spilleren faktisk blandet og er foretrukket recovery target.
2. **Flattened elemental ledger** er autoritativ for conservation, dupe validation og nested `.contains(...)`.

Ingen transformation får endre flattened ledger uten ekstra balanserte material-inputs/outputs.

## Alloy formation og properties

- [ ] Skill powder mixture, molten mixture, homogeneous metallic phase og solidified alloy.
- [ ] En exact ratio er nødvendig, men phase/temperature/compatibility må også være gyldig før finished alloy-state dannes.
- [ ] Deriver properties fra weighted baseline + deterministiske synergy/penalty-termer: atomic-size mismatch, cohesion/bonding, crystal compatibility, solubility-like behavior og ratio.
- [ ] Bruk diminishing returns slik at en svært liten ekstrem komponent ikke dominerer hele alloyen.
- [ ] Re-classify capabilities fra resultatet; ikke arve `metal`, `wire`, `casing` osv. blindt.
- [ ] En alloy kan bli bedre enn parents i én property og dårligere i en annen.
- [ ] Processing state som cast/annealed/hardened er senere utvidelse og må ikke duplisere substance identity unødvendig.

## Separasjon

- [ ] En physical powder/phase mixture kan bruke magnetic/density/phase separation når egenskapene faktisk skiller fraksjonene.
- [ ] En homogeneous metallic lattice krever normalt molten electrorefining eller annen fysisk begrunnet metallurgisk route.
- [ ] Electrolysis/electrowinning krever gyldig solution/molten ionic state.
- [ ] Acid route må først lage slurry/solution; tørr dust destilleres aldri.
- [ ] Komplekse blandinger bruker concentrates/residual mixtures i flere terminerende steg når maskin-IO ikke rommer alle outputs.
- [ ] En definert alloy er ikke automatisk centrifugerbar tilbake til parents.
- [ ] Alle routes valideres mot exact massevektor og hele recipe-grafen før publisering.

## Ytelse

- [ ] Normalisering er `O(k log k)` ved innholdsendring.
- [ ] Maks 20 komponenter gir høyst 400 pairwise property checks når cache må bygges.
- [ ] Cache key inkluderer canonical composition, structure/state og rules revision.
- [ ] Exact lookup skjer synkront; bare stor immutable planlegging kan gå på bounded shared worker.
- [ ] Ingen recipe/material scan per tick og ingen egen thread per Foundry.

## Tester

- [ ] Exact/scaled/non-match ratios.
- [ ] Nested composite constituents.
- [ ] Ambiguous flattened signatures.
- [ ] Deterministisk canonical payload og serialization.
- [ ] 20-component performance.
- [ ] Generic mixture cast/cool/grind/separate round-trip uten gevinst/tap.
- [ ] Defined alloy property tradeoffs og bounds.
- [ ] Ingen closest/missing UI.
- [ ] Ingen profitable graph cycle, heller ikke med tiny/small/chance conversions.

## Ferdig når

Definerte exact ratios kan bli canonical alloys med avledede properties, mens alle andre ratios forblir trygge uklassifiserte blandinger som kan lagres, støpes og separeres uten hardkodede navn, registry explosion eller materialduping. Se også `07-foundry-and-heater-multiblocks.md`.
