# Phase 19 – Maintenance

Multiblocks har allerede separat CB/chemical-corrosion durability, og interaction-systemet har en `InteractionWearStore`. Dette er **ikke** det samme som planlagt maintenance-system.

- [ ] Definer maintenance-state separat fra CB/chemical-corrosion durability.
- [ ] Akkumuler kun active server ticks mens maskinen faktisk prosesserer.
- [ ] Utløs maintenance wear ved omtrent 72 000 aktive ticks (ca. 60 min ved 20 TPS), med valgt balanseringsvariant dokumentert.
- [ ] Velg tilfeldig gyldig maintenance block/component og trekk durability/wear.
- [ ] La eventuell material-derived wear resistance påvirke durability bare dersom formelen er eksplisitt og testet.
- [ ] Implementer inspection/tooltip eller GUI.
- [ ] Implementer repair før 0.
- [ ] Implementer failure ved 0.
- [ ] Velg og ødelegg 2–5 unike collateral blocks ved failure dersom denne designen beholdes.
- [ ] Ekskluder controller og ability blocks fra collateral destruction.
- [ ] Test chunk unload, restart, structure rebuild og server/client sync.

## Ferdig når

Maintenance er persistent, deterministisk nok til debugging og uavhengig av den eksisterende CB/chemical-corrosion durabilityen.
