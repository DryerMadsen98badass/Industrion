# TODO — runtime performance

Baseline: `main(20261008-152944).zip`. Implementert 2026-10-08.

## Gjennomført

- [x] Indekser Stone Shaping/Chiseling i JEI etter item, og behold eksakt component-matching og recipe-rekkefølge.
- [x] Gjenbruk JEI-indeks til RecipeManager/byName-versjonen endres; håndter world switch, sync/reload og meny uten verden.
- [x] Gruppér CE-recipes i én gjennomgang under JEI-registrering.
- [x] Bytt full cache-tømming ved 4096 ingredient-/brick-entries til bounded identity LRU med én eviction per miss.
- [x] Cache også material-/væske-misser. Behold material-, part-, magnetic- og fluid-mappings.
- [x] Behold FluidUtil-oppslag per container-stack, slik at endret innhold på samme item fortsatt oppdages.
- [x] Bruk energitopologien som allerede er oppdaget ved små nett og avvist worker-submission.
- [x] Begrens energirute-cache til 256 entries med incremental LRU, uten full flush.
- [x] Invalider kun energiruter som avhenger av berørte chunks; inkluder tomme ruter, manglende/disabled naboer og pending jobs.
- [x] Kjør chunk load/unload-invalidasjon på servertråden.
- [x] Memoiser eksakte sesong-/døgnfaktorer i to små entries per tråd; unngå nytt biome-oppslag i ClimateWorld.sample.
- [x] Sammenlign mot originalkode, kontroller API-signaturer og parse hele siste kildeversjon.

## Fortsatt usikkert

- [ ] Faktiske frame-tider, server-tick p95/p99 og største spike på mål-PC-en.
- [ ] GC-pauser og stabil heap separat fra ressurs-/modellinnlasting.
- [ ] Modell-/blockstate-kardinalitet og retained heap. Wrapper-deling finnes allerede.
- [ ] Årsaken til JEI sine `Unknown recipe category`-advarsler i faktiske logger. Ikke skjul advarsler eller fjern recipes uten å identifisere dem.
- [ ] GeologyDepositFeature-kostnad ved ny chunk-generering.
- [ ] Shelter-miss/heat-search og plants/autosave-kostnad med faktisk antall dyr, planter og rom.

Disse punktene er usikkerhet i grunnlaget; patchen ber ikke brukeren om å utføre en testøkt. Ingen FPS-, TPS- eller RAM-prosent er lovet.
