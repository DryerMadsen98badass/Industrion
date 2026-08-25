# Phase 09 – Alloy-system

Målet er at kombinasjon av materialer kan skape nye materials med egne egenskaper, inkludert alloys som blir sterkere, svakere, mer sprø, mer ledende eller mer varmebestandige enn en enkel weighted average skulle tilsi. Recipe/process-reglene fra Phase 05–06 og Foundry/heat-grunnlaget fra Phase 07 finnes før denne fasen, men automatic recipes bygges først i Phase 14.

## Gjenstår

- [ ] Definer typed `AlloyDefinition`/generated alloy substance med 2+ components og ratios.
- [ ] Skill fysisk powder/molten mixture fra ferdig alloy/metallic phase.
- [ ] Deriver alloy-properties fra components uten ren naiv weighted average.
- [ ] Bruk weighted baseline + deterministiske synergy/penalty-termer fra f.eks. atomic-size mismatch, bonding/cohesion, crystal compatibility og composition ratio.
- [ ] Sørg for diminishing returns slik at en liten mengde av ett ekstremt material ikke gjør hele alloyen ekstrem.
- [ ] Deriver material classes/capabilities på nytt fra resultatet; ikke arve `metal`, `insulator` osv. blindt fra parent materials.
- [ ] Tillat at en alloy kan være bedre enn begge parents i én property og dårligere i en annen.
- [ ] Definer state/phase/melting behavior fra composition + interaction model.
- [ ] En ferdig alloy skal ikke automatisk kunne «centrifugeres tilbake» til parents; separation må følge Phase 06 process rules.
- [ ] Lag tests der noen alloys blir sterkere enn begge parents, noen blir svakere/brittle, og tradeoffs oppstår deterministisk.
- [ ] Koble generated alloy tilbake til Phase 03 capability-systemet, slik at en ny alloy automatisk kan kvalifisere som wire, plate, casing, pipe osv.; casing qualification skal gå gjennom Phase 04-generatoren.

## Senere utvidelse

- [ ] Vurder processing state som cast/annealed/hardened uten å gjøre dette nødvendig for første fungerende alloy-system.

## Ferdig når

To eller flere materials kan gi en deterministisk alloy med egen identity, properties, classes, forms og capabilities uten håndskrevne per-alloy gameplay-statistikker.
