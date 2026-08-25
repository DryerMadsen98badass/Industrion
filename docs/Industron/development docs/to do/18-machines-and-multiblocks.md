# Phase 18 – Machines og multiblocks – gjenværende arbeid

Core single-block runtime, CE recipe execution, multiblock pattern/predicates, abilities/ports, tiered power input, GUI/preview og machine control finnes allerede. Denne fasen integrerer de nye material/process-systemene i eksisterende runtime.

- [ ] Koble generated process requests til machine/runtime når Phase 14 er klar; concrete machine beholder én RecipeType.
- [ ] Koble generated recipe requests til eksisterende single-block/multiblock recipe runtime.
- [ ] Integrer material-derived casing definitions fra Phase 04 i multiblock predicates/validation.
- [ ] Beregn faktisk processing/effective tier fra power tier og casing capability/tier der designet eksplisitt krever det.
- [ ] Beskytt mot at ability/port tier feilaktig oppgraderer selve casing/structure tier.
- [ ] Utvid existing structure diagnostics med konkrete casing-property failures.
- [ ] Legg regression tests rundt tier restrictions, power detection, recipe selection og formed/unformed transitions.
- [ ] Verifiser at generated chemistry/process recipes ikke krever spesiallogikk i controller når samme informasjon kan ligge i recipe/process context.

## Ferdig når

Eksisterende machine-runtime kan kjøre generated process/chemistry recipes og material-derived multiblocks uten parallelle hardkodede tier-/materialregler, og Phase 07 Foundry/heater-design passer inn uten å bli en generell multi-RecipeType-controller.
