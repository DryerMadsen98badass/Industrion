# START HERE – neste utvikler/chat

Dette dokumentet er inngangen til Industron-planen. Les også `DECISIONS.md`, `SYSTEM_OVERVIEW.md` og `to do/README.md`.

## Hva du skal optimalisere for

- Sannhet og nøyaktighet fremfor å være enig.
- Inspiser faktisk prosjektkode før du antar API/status.
- Data-driven/generative systemer fremfor hardkodede lister.
- Små, kontrollerte patches; normalt bare endrede/nye filer i ZIP.
- Ikke implementer ting brukeren eksplisitt sier skal vente.

## Gjeldende Assembly-status

Assembly Workbench, recursive `ComponentDefinition`, free/fixed materialbinding, generated Frame/Casing Assembly recipes, JEI `Assembly Products`/`Assembly Components`, timed tools og Create Deployer/FakePlayer tool-progress finnes i dagens kode. Se `code/recipes/assembly-recipes.md` før Assembly-endringer.

## Historisk prioritetssnapshot – verifiser mot siste prosjekt før bruk

> Denne seksjonen er beholdt fordi den forklarer tidligere Wood/Stone-arbeid, men den er **ikke** en autoritativ beskrivelse av dagens aktive oppgave. Assembly/casing-kode er implementert senere og må vurderes fra siste prosjekt.


Wood/Stone structure-material patchen må først bygges riktig på Minecraft 1.21.1 / NeoForge.

Siste kjente compile blocker var i:

```text
src/main/java/net/mads/industron/registry/BlockRegistry.java
```

Feiltypen var:

- `net.minecraft.world.level.block.GlassBlock` finnes ikke i dette prosjektets 1.21.1 mappings/API.
- `properties.noCollision()` finnes ikke på den aktuelle `Properties`-typen slik patchen brukte den.
- window-konstruksjonen som brukte `new GlassBlock(...)` må erstattes med en faktisk tilgjengelig 1.21.1-løsning.

**Ikke gjett rettelsen.** Inspiser eksisterende imports/API/dependencies i siste prosjekt-ZIP og rett deretter. Kjør `runData` igjen og følg de neste faktiske feilene hvis noen.

## Når Wood/Stone bygger

1. Verifiser Test Wood (`WoodModel.SPRUCE`) og Test Stone (`StoneModel.DIORITE`) i klient.
2. Kontroller generated textures/models/blockstates/loot/tags/lang.
3. Test `.existing(...)` og `.contains(component(...))`.
4. Koble `StoneMaterial` registry til ore-host generator slik at alle nye stones automatisk får normal + small ore for ore-eligible materials.

## Ikke gjør ennå

- Ikke lag tree growth/TreeDefinition før wood-systemet er stabilt.
- Ikke bygg full chemistry engine før composition/substance-modellen er stabil.
- Ikke slett alle real-world legacy metals uten dependency-scan.
- Ikke endre Create-integrasjon bredt bare for å rydde arkitektur.

## Viktige designankre

- Element gameplay-properties kommer fra `atomicNumber + tier`.
- Metal og gem er mutually exclusive.
- Ingen gameplay `electricalResistance` for wires.
- Pump flow og pump SU/RPM er separate calculations.
- Wood/stone er `IndustrialSubstance` og kan brukes direkte i `component(...)`.
- Visual family er typed: `WoodModel.SPRUCE`, `StoneModel.DIORITE`.
- Metal structure assets er generiske `metal_N` per block role.
- Nye custom stones skal senere bli ore hosts automatisk.


## API-konvensjoner

Se `API_CONVENTIONS.md`. Foreslåtte Java-API-er skal bruke typed references og typed property-metoder; ikke string-lookups når et ekte objekt/property-API kan brukes.
