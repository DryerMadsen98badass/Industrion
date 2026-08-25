# Phase 07 – Foundry og heater-multiblocks

Denne fasen ligger **etter RecipeType/Process Rules (Phase 05–06), men før Composition/Alloys/Chemistry (Phase 08+)**.

Målet er å flytte foundry-konseptet fra reference-projectet til Industron senere, men først som en tydelig spesifikasjon. Reference-foundryen skal ikke kopieres blindt: den skal optimaliseres og tilpasses Industron når implementasjonen starter.

**Ingen Foundry/heater-kode implementeres mens denne TODO-en skrives.**

## 1. Foundry – grunnidé

Foundryen skal fungere svært likt reference-foundryen:

- [ ] Variable square structure sizes.
- [ ] Variable høyde der designet tillater det.
- [ ] Intern kapasitet avledes fra faktisk innvendig volum.
- [ ] Material-input kan smeltes til molten state.
- [ ] Flere material-inputs kan prosesseres parallelt der runtime/design tillater det.
- [ ] Required melting temperature kommer fra materialets generated `Melting Point`/relevante materialstats.
- [ ] Høyere faktisk Foundry-temperature enn minimum kan redusere melt duration etter en eksplisitt, testbar formel.
- [ ] Intern molten storage kan inneholde flere material/substance entries uten å miste identity eller amount.
- [ ] Input/output/drain/casting-interface skal tilpasses Industron abilities/ports i stedet for å kopiere gamle klasser direkte.
- [ ] Structure validation skal være cached/event-driven der mulig; ikke full rescan av hele Foundry hvert tick.

## 2. Foundry size variants

Foundryen bruker odd inner width/depth. Heater-varianten bestemmes direkte av Foundryens inner footprint.

```text
Variant 1
Foundry inner: 1x1
Foundry outer footprint: 3x3
Heater footprint under Foundry: 3x3

Variant 2
Foundry inner: 3x3
Foundry outer footprint: 5x5
Heater footprint under Foundry: 5x5

Variant 3
Foundry inner: 5x5
Foundry outer footprint: 7x7
Heater footprint under Foundry: 7x7

Variant 4
Foundry inner: 7x7
Foundry outer footprint: 9x9
Heater footprint under Foundry: 9x9
```

Generell regel:

```text
variant n
inner side  = 2n - 1
outer side  = 2n + 1
heater side = 2n + 1
```

- [ ] Foundry-controlleren må kunne avgjøre hvilken size variant strukturen er.
- [ ] Heater må matche Foundryens variant eksakt.
- [ ] Heater som er for liten, for stor eller feilplassert skal ikke varme Foundryen.
- [ ] Varianten skal ikke være hardkodet til bare 1–4 dersom structure-systemet senere tillater større odd sizes.

## 3. Heater skal fylle hele arealet under Foundryen

Heater er en **egen multiblock direkte under Foundryen** og dekker hele Foundryens outer footprint.

Konseptuelt:

```text
┌─────────────────────────┐
│         FOUNDRY         │
│ outer footprint NxN     │
└─────────────────────────┘
███████████████████████████
│ HEATER MULTIBLOCK NxN   │
│ samme variant/footprint │
███████████████████████████
```

- [ ] Hele required heater footprint må være formed før varmeoverføring er aktiv.
- [ ] Foundry må bare koble til heateren direkte under sin base og med samme horizontal footprint/variant.
- [ ] Ingen delvis heater skal gi delvis tilfeldig temperatur; formation/variant-regelen skal være entydig.
- [ ] Heat-transfer-link skal caches og invalidere ved structure changes/chunk unload/rebuild.

## 4. Foundry eier ikke fuel eller power

Foundryen skal ikke selv forstå steam, solid fuel, liquid fuel eller CE.

Den skal bare kjenne noe i retning av:

```text
currentTemperature
receivedHeat / availableHeat
heaterTargetTemperature
```

- [ ] Foundry spør heater-interface etter tilgjengelig heat/target temperature.
- [ ] Foundry har ingen egen fuel slot/tank bare for varmeproduksjon.
- [ ] Foundry har ingen hardkodet `if steam`, `if coal`, `if liquid fuel`-logikk.
- [ ] Heat source kan byttes senere uten å omskrive Foundryens melting/runtime-logikk.

## 5. Heater families – senere implementasjon

Følgende separate heater-multiblocks er planlagt, men lages ikke nå:

```text
Steam Heater
Solid Fuel Heater
Liquid Fuel Heater
```

De kan ha ulike:

- maximum/target temperature.
- heat output / warm-up rate.
- fuel/resource consumption.
- efficiency og thermal stability.
- minimum material/casing requirements.

Foundryen skal se alle gjennom samme heat-provider contract.

## 6. Temperature behavior

- [ ] Foundry starter fra sin aktuelle temperature-state og varmes gradvis mot heaterens target/available temperature.
- [ ] Når heateren stopper, går Foundry-temperature gradvis tilbake mot ambient temperature.
- [ ] Foundry skal ikke teleportere direkte til heaterens temperatur.
- [ ] Warm-up/cool-down skal være deterministisk og server-authoritative.
- [ ] Heating rate skal kunne ta hensyn til Foundry size/volume slik at en stor Foundry ikke nødvendigvis varmes like raskt som en liten.
- [ ] Exact heat-transfer formel balanseres når heaterne implementeres.

Material melting:

```text
currentTemperature < material.meltingPoint
-> material smelter ikke

currentTemperature >= material.meltingPoint
-> material kan smelte

currentTemperature godt over material.meltingPoint
-> kan smelte raskere etter eksplisitt formula
```

## 7. RecipeType-regel

Regelen **én concrete machine = én RecipeType** beholdes.

Foundryen skal ikke erklæres som en machine som samtidig «støtter» `MELTING`, `ALLOYING`, `CASTING`, osv.

Første retning:

- [ ] Foundryens processing RecipeType er `MELTING`.
- [ ] Casting håndteres gjennom separat drain/mold/casting-system der designet krever det, ikke som en ekstra RecipeType på Foundry-controlleren.
- [ ] Senere automatic alloy formation i molten storage behandles som material/substance-state logic, ikke som at Foundryen plutselig får en ekstra RecipeType.

Hvis dette designet endres senere skal det gjøres eksplisitt, ikke ved at samme controller skjult får flere process types.

## 8. Molten material storage

- [ ] Molten contents bruker canonical material/substance identity.
- [ ] Amounts må være eksakte og deterministiske.
- [ ] Capacity avledes fra Foundryens innvendige volum.
- [ ] Storage skal kunne inneholde flere molten entries.
- [ ] Input/output skal ikke tvinge alle molten materials inn i én anonym fluid.
- [ ] GUI/Jade/diagnostics skal senere kunne vise temperature, capacity og relevante molten contents uten å scanne recipes globalt.

## 9. Alloy hook – men alloys kommer senere

Phase 07 kommer før generated alloys. Derfor bygges ikke alloy-generatoren her.

- [ ] Foundry-storage/runtime skal ha et stabilt integration point for senere alloy resolution.
- [ ] Når Phase 09 definerer generated alloys kan compatible molten component ratios senere transformeres til en canonical alloy-state.
- [ ] Ingen hardkodede alloy-navn eller parent-material recipes legges inn i Foundry-fasen.
- [ ] Phase 14 automatic process recipe generation kan senere generere material/process recipes uten at Foundry-controlleren kjenner konkrete alloys.

## 10. Casing dependency

Foundryen bygges **etter Phase 04 casing-generation** slik at multiblocken kan bruke generated casing families/profiles i structure requirements.

- [ ] Foundry structure predicates skal bruke casing identity/profile/capability, ikke hardkodede materialnavn.
- [ ] Heater structures skal kunne bruke samme generated casing-system der designet krever det.
- [ ] Exact casing-profiler for Foundry/heaters bestemmes etter casing-designrunden.

## 11. Optimization ved porting

Når reference-projectets Foundry faktisk flyttes over:

- [ ] Kartlegg gammel controller/block entity/abilities før kode kopieres.
- [ ] Fjern gammel pH-logikk og bruk `Chemical Balance (CB)` der chemical environment faktisk er relevant.
- [ ] Fjern gammel direkte heater/fuel-detection fra Foundry-controlleren.
- [ ] Bruk separat cached heater-link.
- [ ] Unngå full structure/mold/alloy scan per tick.
- [ ] Indexer/cach molten lookups og active melting work.
- [ ] Behold server authority og sync bare state som klienten trenger.
- [ ] Benchmark stor Foundry med mange parallelle melts og flere molten entries.

## Ikke i denne fasen

- Generated alloy definitions/properties.
- Compound/molecular chemistry.
- Automatic alloy/chemistry recipe generation.
- Steam/Solid Fuel/Liquid Fuel heater implementations hvis vi fortsatt bare spesifiserer Foundryen.
- Pressure-system.

## Ferdig når

Phase 07 er ferdig når Foundry-design/runtime kan:

- [ ] forme variable odd-size Foundries.
- [ ] mappe inner size til riktig heater variant.
- [ ] kreve en full-size separat heater rett under Foundryen.
- [ ] motta heat uten å kjenne heaterens fuel/power type.
- [ ] varme/kjøle deterministisk.
- [ ] smelte eligible materials basert på generated melting stats.
- [ ] lagre molten materials med canonical identity.
- [ ] være klar for senere alloy integration uten hardkodede alloy-navn.
