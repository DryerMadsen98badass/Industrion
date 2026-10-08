# Technical debt / planlagte oppryddinger

Denne filen beskriver broer som fortsatt finnes eller sannsynligvis blir midlertidige mens content/casing-, recipe/process-, Foundry-, chemistry- og assembly-systemene bygges.

## TD-0001: Legacy fields i `IndustrialMaterial`

`IndustrialMaterial` kan fortsatt inneholde compatibility-/historiske felt for furnace fuel parts, stone sources, centrifuge/electrolyser og smelting behavior.

- Mål: composition, process rules og generated recipes skal eie denne logikken der det passer.
- Ikke fjern felt før alle consumers er kartlagt.
- Etter Phase 13–14: dependency-scan og fjern fields/getters som ikke lenger har legitim runtime-bruk.

## TD-0002: Legacy recipe compatibility

`CERecipe`/recipe-systemet kan fortsatt ha legacy compatibility-felter, mens ny retning er typed RecipeTypes + machine/recipe conditions + machine-owned resource usage.

- Phase 05–06 definerer stable process contracts før cleanup.
- `RecipeRequest` i Phase 14 skal ikke gjeninnføre manuelt CE/t som ny sannhetskilde.
- Legacy decode paths fjernes først når eksisterende data ikke lenger trenger dem.

## TD-0003: External material suppression er en overgangsbro

Minecraft/Create registry objects beholdes, mens gamle progression recipes/presentation for utvalgte materialfamilier kan suppresses gjennom existing bridge-lag.

- Mål: Industron skal eie custom progression recipes for disse materialene.
- Eksplisitte funksjonelle exceptions skal fortsatt være tilgjengelige.
- Når Phase 14 custom/generated recipes er komplette, gjennomgå suppression-listen og fjern unødvendige brede regler.

## TD-0004: Structure/content generator validation

Phase 00 har felles validation-fundament, men content generation trenger complete checks for existing/generated collision, missing model/texture references, casing generation og stable variant discovery.

- Dette lukkes primært i Phase 04 og verifiseres igjen i Phase 21.

## TD-0005: Static registry vs generated chemistry

Minecraft registry freeze gjør at et ubegrenset runtime chemistry-system ikke kan opprette vilkårlige nye item/block IDs etter load.

- Første løsning bør begrense registry-backed substances til requested/reachable generation før registration/datagen.
- Hvis ekte runtime-discovered materials ønskes senere, vurder generic item/fluid carriers med data components.
- Ikke la begrensningen lekke inn som hardkodede materialnavn i chemistry eller assembly API.

## TD-0006: Testdekning etter Phase 00–03

- Phase 00–03 har foundation/material/capability coverage.
- Phase 04: casing/content generation determinism/collision tests.
- Phase 05–06: RecipeType/process-physics tests.
- Phase 07: Foundry size/heater-link/thermal/runtime performance tests.
- Phase 08–14: composition, alloy, bond, compound, reaction og generated recipe tests.
- Phase 15–17: component/assembly resolution tests.
- Phase 21 samler full performance/system regression.

## TD-0007: Assembly API må ikke bake inn midlertidige materialnavn

Før generated alloys/polymers er ferdige kan det være fristende å kode konkrete materialnavn direkte i component/assembly-regler.

- Ikke gjør dette som permanent API.
- Bruk typed `MaterialPart`, components og capability/property requirements.
- Exact external item IDs brukes bare når recipe faktisk mener akkurat den itemen.
- Dette er nødvendig for at framtidige generated materials automatisk skal kunne kvalifisere uten recipe-endringer.

## TD-0008: Process physics må ikke ligge i chemistry generators

Når chemistry implementeres skal det ikke hardkode «denne reaction går i denne controlleren» som spredte special cases.

- Phase 06 eier hva process types fysisk kan gjøre.
- Phase 13 beskriver hvilke operations en reaction krever.
- Phase 14 mapper requirement -> kompatibel RecipeType.
- Physical separators skal aldri bli universal bond-breakers.

## TD-0009: Foundry må ikke eie heater-type

Reference-foundryen har eldre direkte heat-source knowledge som ikke skal bli permanent Industron-arkitektur.

- Phase 07 introduserer separate full-footprint heater-multiblocks.
- Foundry mottar heat gjennom et felles contract.
- Steam/Solid Fuel/Liquid Fuel heater implementeres uavhengig av Foundry melting-runtime.
- Structure/heat links caches og invalideres på relevante structure events i stedet for full lookup per tick.

## TD-0010: Dagens cycle-validator er konservativ

`ProcessSafetyValidator` bevarer flattened mass, avviser chance chemistry outputs og blokkerer alle directed cycles. Det er sikkert for dagens generated chemistry, men kan ikke direkte representere Foundryens eksakt reversible mixing/casting/grinding/separation.

- Før Foundry publiserer reversible routes må validatoren bruke stoichiometric/vector cycle analysis.
- Bare zero-net-material cycles kan tillates; chance-bonus eller positiv reachable vector forblir hard feil.
- Form- og thermal-state transitions må ikke forveksles med materialproduksjon.

## TD-0011: Hot forms er ufullstendige

Dagens registry har `HOT_INGOT`, `HOT_NUGGET` og hot molds, men ikke en generell hot state for alle castable forms.

- Mål: typed thermal data component/bands med compatibility bridge for eksisterende hot forms.
- Ikke legg til én permanent registry enum-form per temperatur og form hvis data kan representere tilstanden trygt.

## TD-0008: Automated timed-tool progress over full server restart

`AssemblyRuntime` lagrer Workbench recipe step, wait deadline, refund-items og materialbindings. Pågående timed tool state (`activeTool`, `toolProgressTicks`, `toolAutomated`) serialiseres ikke i dagens `ActiveAssembly.save/load`.

- Normal Deployer/FakePlayer pulse-work fungerer mellom vanlige aktiveringer mens runtime-state lever.
- Full server restart midt i et uferdig automatisert tool-step mister denne delprogressen.
- Enten serialiser tool-work senere eller behold dette eksplisitt som forventet reset-semantikk; ikke anta persistens som koden ikke har.


## TD-PERF-01: Spilletidsprofil og modellminne er fortsatt ikke målt her

JEI-oppslag, material/væske-oppslag, ingredient-cacher, energirute-cacher og eksakte døgnfaktorer er optimalisert i 2026-10-08-patchen. Kontrollerte sammenligninger bevarer resultater, rekkefølge og beregningsverdier.

- Ingen full Minecraft-klient eller brukerens PC var tilgjengelig; FPS, tick-percentiler, GC-pauser og stabil heap er ikke dokumentert som forbedret.
- Modell-/blockstate-mengden og geometrideling er ikke endret. Ressursinnlastingens heap-topp kan fortsatt være stor.
- Shelter-geometrien, worldgen og plantenes persisted-data/oppdateringsbudsjetter beholder dagens regler; kostnaden i en faktisk verden er fortsatt ukjent.
- Material-/væskecacher følger append-registrering gjennom katalogstørrelser. Ved framtidig erstatning eller endring av eksisterende definisjoner uten størrelsesendring må eieren kalle `MaterialLookup.clearCaches()` og `IndustrialFluidLookup.clearCache()`. Dagens registrering gjør tillegg før vanlig runtime.
- Ikke flytt world-access, data-component/tag-predikater eller energi-/inventarmutasjoner til workers.

Se `TODO_Performance.md` og `docs/performance-2026-10-08.md` for status og dokumenterte kontrollmålinger.
