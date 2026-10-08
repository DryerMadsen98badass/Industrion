# Ytelsespatch — 2026-10-08

Patchen bygger på siste kildearkiv `main(20261008-152944).zip`, med developer-dokumentasjonen fra `docs(8).zip`. 14 Java-filer er nye/endret. Øvrig kildekode, recipes, materialdefinisjoner, worldgen, assets og modellregistrering er bevart byte for byte.

## Konkrete årsaker rettet

1. Dynamiske JEI-plugins hentet hele recipe-lister og laget input/output-stacks på nytt ved hvert oppslag. De bruker nå recipe/item-indekser med component-filter og riktig reload-livstid. CE-registration grupperer oppskrifter én gang.
2. Ingredient-/brick-cacher og energirute-cacher mistet hele innholdet ved kapasitetsgrensen. De beholder nå nylig brukte entries og evicter enkeltvis.
3. Energinettet ble kartlagt på nytt etter at et lite snapshot allerede var laget, og ved avvist worker-jobb. Snapshotet gjenbrukes.
4. Alle energiruter ble ugyldige ved hver chunk load/unload og lokal wire/machine-endring. Cache-entries registrerer nå relevante chunks, med konservativ dekning av tomme ruter og unloaded/disabled naboer.
5. MaterialLookup og IndustrialFluidLookup gjentok katalogsøk på negative resultater. Bucket-/fluid-søk laget samtidig nye allFluids-lister. Bounded positive/negative cacher stanser gjentakelsen. Generiske container-items leses fortsatt dynamisk.
6. Klimakode beregnet samme sesong-/døgn-trigonometri for mange posisjoner ved samme tidspunkt. Eksakte time-faktorer gjenbrukes per tråd; ClimateWorld.sample unngår ekstra biome-oppslag.

## Kontrollerte før/etter-målinger

Dette er arbeidsmengde i test-fixtures med ekte patch-kode og originale metodeimplementasjoner som referanse. Det er ikke frame-/tick-/heap-målinger fra Minecraft.

| Situasjon | Før | Etter | Kontroll |
|---|---:|---:|---|
| Første oppslag i nett med åtte wires, workers tilgjengelige | 110 block-state-lesinger | 55 | Samme destinasjoner, paths og rekkefølge |
| JEI: 2500 syntetiske recipes, 200 items, 8000 input/output-spørringer | 40 803 782 stack-sammenligninger | 203 168 | Alle resultatlister identiske |
| 3000 gjentatte material-/fluid-oppslag uten treff, etter warm-up | 3000 allFluids-listetildelinger | 0 | Samme null-resultater |
| 1000 gjentatte runder av fire dynamiske Stone Shaping-spørringer | RecipeManager/getter-arbeid i opprinnelig query-kode | 0 nye recipe-list copies og 0 stack copies | Recipe-count, resultat og components beholdt |

Indeksbygging og første cache-miss har fortsatt en kostnad. Memory-cap er 4096 entries per identity-cache og 256 energirute-entries per level. Nye cacher har et begrenset retained-memory-forbruk; stabil total heap er ikke målt.

## Gjennomført verifisering

- Parsing av alle 1286 Java-filer i siste kildeversjon: ingen syntaksfeil.
- Kompilering av endrede indeks-/cache-/event-consumers mot kontroll-fixtures.
- API-kompilering av JEI-plugin/indeks, energinett, material/fluid-oppslag, brick-resolver og klimakode mot mapped Minecraft 1.21.1-signaturer med support-fixtures for egne/eksterne typer. Dette er ikke full mod-build.
- 130 000 klimacasetester, tre repetisjoner hver, inkludert åtte samtidige tråder: eksakt samme double-verdier som originalkode. Ekstra grensetester for negative dager, sesonggrenser, signed zero og ikke-finite verdier.
- 1500 energigrafer: samme BFS-paths/rekkefølge; source-side, disabled kanter, tomme ruter, chunkgrenser, stale workers, kø-fallback og overføringsregler kontrollert.
- 8000 JEI-indeks/referansesammenligninger, med components, count-variasjoner, duplicate inputs, dust-outputs, tools, reload og world switch/disconnect.
- 3000 blandede material/fluid-sammenligninger: custom/existing/magnetic parts, buckets, source/flowing, nye katalogtillegg, endrede container-innhold og existing vanilla-fluid-definisjoner.
- Ingredient-cacher: shared expansions, quantity matching, reload og avvist worker-jobb.
- Eksisterende pure snapshot-test: 10 000 quantity-matching-caser, overlappende inputs, immutable snapshots, cyclic BFS, revisjoner og interruption.
- Chunk load/unload-callbacks: server-thread scheduling, ingen blokk-/chunk-content-lesinger i callbacken og ingen invalidasjon av urelaterte ruter.

## Praktiske begrensninger

Det var ingen tilgjengelig Minecraft-klient, bruker-PC, latest.log, heapdump eller JFR-opptak i denne kildeleveransen. Derfor er det ingen dokumentert FPS-/TPS-prosent, GC-pauseforbedring eller stabil RAM-reduksjon. Innlastingsdata fra forespørselen er observasjoner fra brukerens tidligere økt, ikke en baseline målt her.

995 051 modelloppføringer, 402 030 swaps, 17 087 wrappers og 5664 MiB heap under innlasting beviser ikke årsaken til vedvarende lag. Modeller/geometrideling, worldgen, plantenes budsjetter/lagring og ly-/varme-geometrien er ikke endret. JEI category-warningene er heller ikke skjult. Gjenstående usikkerhet er registrert i TODO_Performance.md og technical-debt.md.
