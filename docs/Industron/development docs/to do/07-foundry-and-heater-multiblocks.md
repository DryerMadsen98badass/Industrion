# Phase 07 – Foundry, heater, støping og dynamiske metallblandinger

**Status: AKTIV FASE.** Dagens prosjekt har recipe-typene `MELTING`, `SMELTING`, `ALLOYING`, `CASTING` og `COOLING`, `MOLTEN_FLUID`, `HOT_INGOT`, `HOT_NUGGET`, kalde/varme støpeformer, materialtemperaturer og et generelt multiblock-grunnlag. Det finnes derimot ingen ferdig Foundry-controller eller Heater-multiblock. Dette dokumentet er den komplette implementeringsspesifikasjonen.

Foundryen er et tidlig-spill metallurgisystem. Den skal gjøre oppvarming, fysisk smelting, reduksjon der det er gyldig, blanding, støping og termisk behandling forståelig uten å bli en universell chemistry-maskin. Den skal aldri omgå den eksisterende sammensatt-dust-prosesseringen.

## 1. Fastsatte grenser

- [ ] Foundry håndterer metallurgiske tilstander og operasjoner, ikke vilkårlig chemistry.
- [ ] Rent, elementært metalldust kan smeltes når materialet har gyldig molten form og riktig temperatur.
- [ ] Sammensatt dust kan ikke automatisk bli molten metall bare fordi det har `DUST`.
- [ ] Ore/mineral/compound dust må først følge den deterministiske ruten fra `SeparationClassifier`/`ProcessPlanner`, eller en eksplisitt metallurgisk reduksjonsrute når strukturen tilsier det.
- [ ] Direkte generell `DUST -> INGOT` skal fortsatt ikke genereres. Dagens `ProcessPlanner` fjerner denne snarveien med vilje.
- [ ] Tørr dust kan ikke destilleres. En distillasjonsrute må først lage en gyldig fluid intermediate, for eksempel automatisk `<material> slurry`, `<material> solution` eller `<material> reaction mixture`.
- [ ] Ore-preprocessing fram til ore-materialets eget `DUST` forblir separat fra Foundry og chemistry.
- [ ] Stone/gravel/host-stone dust forblir separat.

## 2. Struktur og størrelser

Foundryen har odd inner width/depth. Heateren er en egen multiblock direkte under og dekker hele outer footprint.

| Variant | Inner side | Foundry outer side | Heater side |
| ---: | ---: | ---: | ---: |
| 1 | 1 | 3 | 3 |
| 2 | 3 | 5 | 5 |
| 3 | 5 | 7 | 7 |
| 4 | 7 | 9 | 9 |

```text
inner side  = 2n - 1
outer side  = 2n + 1
heater side = 2n + 1
```

- [ ] Controlleren finner variant og innvendig volum deterministisk.
- [ ] Heater må ligge direkte under, være formet og matche footprint eksakt.
- [ ] For liten, for stor, forskjøvet eller delvis heater gir ingen varmeoverføring.
- [ ] Foundry height, minimum/maximum size og capacity-per-volume kalibreres før implementasjon.
- [ ] Structure- og heater-link caches og invalideres ved blokkendring, reforming, chunk unload/load og controller removal.
- [ ] Ingen full structure scan per tick.

## 3. Heat-provider contract

Foundryen eier ikke fuel- eller kraftlogikk. Steam Heater, Solid Fuel Heater og Liquid Fuel Heater kan komme som separate providers, men Foundryen ser bare et typed heat-interface med minst:

```text
formed
footprint/variant
availableHeatPerTick
targetTemperature
maximumTemperature
providerRevision
```

- [ ] Foundry har thermal mass, current temperature, heat loss og mottatt varme.
- [ ] Temperatur beveger seg gradvis; den teleporterer ikke til target temperature.
- [ ] Stor masse/volum varmes saktere enn liten ved samme heat input.
- [ ] Når heater stopper, kjøles Foundry gradvis mot ambient.
- [ ] Serveren er autoritativ. Klienten mottar bare nødvendig visningsstate.
- [ ] Foundry kan ikke bruke høyere temperatur enn casing/refractory/coil/port tåler.

## 4. Materialtemperatur og tier-bånd

Dagens kode bruker `MaterialPropertyCalculator.METAL_MELTING_TIER_BAND_C = 500`. For tier-banded solide metaller blir melt point:

```text
meltingPoint = normalizedBase(1..500) + tierIndex * 500
```

| Tier index | Temperaturbånd |
| ---: | --- |
| ULV / 0 | 1–500 °C |
| LV / 1 | 501–1000 °C |
| MV / 2 | 1001–1500 °C |
| HV / 3 | 1501–2000 °C |
| videre | +500 °C per tier |

Dette er implementert property-logikk, men fortsatt en gameplaykurve som må balanseres med heater og refractory tiers.

- [ ] Melt kan starte når hele batchen kan nå minst materialets `meltingPoint`.
- [ ] Pour/cast krever minst materialets beregnede `castTemperature` og riktig viscosity/castability-vindu.
- [ ] Overtemperature kan øke smeltehastigheten etter en capped formel, men kan også øke heat loss, mold wear eller oxidation risk.
- [ ] Temperatur må aldri brukes til å legitimere materialduping eller en fysisk ugyldig transformasjon.

## 5. Intern Foundry-state

Foundryen skal ha fire tydelige lag:

1. solid feed inventory;
2. thermal/process jobs;
3. molten bath med canonical composition;
4. tap/pour interfaces.

Hver molten entry trenger minst:

```text
canonical top-level constituents
flattened elemental conservation vector
exact amount
temperature
phase/structure state
defined material identity, hvis exact match finnes
revision
```

- [ ] Ingen anonymous fluid som mister identitet.
- [ ] Ingen flyttall som sannhetskilde for materialmengde eller ratio.
- [ ] Bruk heltall/fixed-point/rational amounts og safe overflow checks.
- [ ] Capacity avledes av innvendig volum og faktisk molten volume.
- [ ] Incompatible baths må enten være separate entries/layers eller kreve eksplisitt mixing; de skal ikke automatisk slås sammen.

## 6. Melting, smelting og reduction

`MELTING` betyr phase change uten at elemental composition endres. `SMELTING`/reduction betyr at en bundet kilde reagerer med nødvendige ekstra inputs og danner metall + balanserte byproducts.

| Input | Tillatt Foundry-operasjon |
| --- | --- |
| Elementært metalldust / metal item | Melting til samme materials molten state |
| Definert alloy item/dust | Melting til samme alloy hvis metallic lattice og molten form er gyldig |
| Fysisk metallpulverblanding | Melting/mixing til molten mixture |
| Ore/mineral compound dust | Bare reduction/smelting når classifier/reaction plan sier det og co-reactants/byproducts balanseres |
| Ionic/covalent dust med chemistry-route | Avvis Foundry-snarvei; bruk planlagt electrolysis/leaching/reaction chain |
| Fluid mixture | Bare processer som faktisk gjelder fluid state; tørr-dust-regler gjelder fortsatt |

- [ ] Foundry spør et plan-/semantikk-lag om operasjonen er tillatt; den velger ikke prosess fra slot-layout.
- [ ] En reduction-route må angi reagent, slag/gass/residue og eksakt massevektor.
- [ ] Foundry skal ikke oppfinne manglende reagent eller byproduct.
- [ ] `MELTING` skal aldri bli skjult `DUST -> INGOT`; output er molten state og må støpes/kjøles.

## 7. Definerte alloys og exact ratio-matching

`.contains(...)` er autoritativ composition for en definert alloy/material. Matching skal være deterministisk og navneuavhengig.

```text
Defined alloy: 1 Vernium + 2 Ilyra

1 + 2  -> exact match
2 + 4  -> exact match etter GCD-normalisering
2 + 2  -> ingen match; forblir uklassifisert blanding
```

- [ ] Normaliser sorted component IDs + positive exact amounts med GCD.
- [ ] Bygg index `canonical direct signature -> defined material` én gang ved load/reload.
- [ ] Bevar både top-level signature og recursively flattened elemental vector.
- [ ] Top-level exact match brukes først, fordi `.contains(...)` også kan inneholde alloys og andre sammensatte materialer.
- [ ] Flattened vector er conservation ledger, ikke automatisk bevis på samme structure.
- [ ] Dersom flere definisjoner har samme flattened vector men forskjellig structure/topology, skal systemet ikke gjette.
- [ ] Hele den valgte bath-entryen klassifiseres bare som defined alloy når hele ratioen matcher. Ikke konverter en skjult delmengde automatisk.
- [ ] Alloy resolution kjøres ved innholdsendring, ikke hvert tick.
- [ ] Ingen materialnavn eller eksempelmaterialer hardkodes i matching.

## 8. Udefinerte blandinger, også med opptil 20 komponenter

Spilleren kan blande to eller mange metaller selv om ingen definert alloy har den ratioen. Blandingen må fortsatt være et virkelig, bevarbart og separerbart materiale.

- [ ] Ikke registrer et nytt item/fluid-ID for hver runtime-kombinasjon.
- [ ] Bruk et lite sett generiske carriers med data component, for eksempel `molten_metal_mixture`, `unclassified_metal_ingot`, `unclassified_metal_dust`, `separation_concentrate` og `residual_mixture`.
- [ ] Carrier-data inneholder canonical constituents, flattened ledger, amount, temperature/thermal band og structure/state.
- [ ] Udefinert molten mixture kan støpes til generic hot form, kjøles og eventuelt males til generic dust uten å miste composition.
- [ ] Stack merging tillates bare når canonical payload og relevant thermal/state data er identisk.
- [ ] Payload størrelse og component count får eksplisitte grenser; målet er minst 20 komponenter.
- [ ] Invalid/ukjent material-ID eller ødelagt payload skal avvises med diagnostic, ikke silently slettes.

## 9. Separasjon av udefinerte blandinger

Alle udefinerte blandinger skal kunne gjenvinnes, men ikke nødvendigvis med én centrifuge-recipe. Ruten kommer fra structure, phase og materialegenskaper:

| Tilstand | Typisk rute |
| --- | --- |
| Distinkte solide/pulverfraksjoner | magnetic separation, density/centrifuging eller annen fysisk separation |
| Phase-separated molten bath | tapping/phase separation etter density/immiscibility |
| Homogeneous metallic lattice | melting ved behov, deretter electrorefining |
| Electrochemically separable solution/molten | electrolysis/electrowinning |
| Acid-egnet bundet blanding | leaching -> solution/slurry -> recovery |
| Stabil/kompleks struktur | deterministic multi-step reaction/intermediate chain |

- [ ] `SeparationClassifier`-prinsippet gjenbrukes/utvides for dynamic payloads.
- [ ] Opptil 20 outputs håndteres gjennom flere concentrates/residual mixtures; ingen outputs truncates for å passe slots.
- [ ] Hvert steg reduserer en eksplisitt measure, for eksempel antall uavklarte constituents eller process complexity, slik at planlegging terminerer.
- [ ] Systemet foretrekker top-level constituent recovery når identiteten er bevart; flattened elements brukes som sikkerhetsledger og fallback når strukturdata tilsier full decomposition.
- [ ] Defined alloys kan også separeres, men bare gjennom fysisk/kjemisk riktig route – aldri universell centrifuging.

## 10. Molds, hot states, støping og kjøling

Dagens materialegrunnlag har kalde og varme molds samt `HOT_INGOT`/`HOT_NUGGET`. Foundry-designet skal gjøre hele termiske kjeden eksplisitt:

```text
cold empty mold
-> mold preheating
-> hot empty mold
-> pour molten material
-> filled hot mold
-> solidification
-> hot cast part + hot empty mold
-> controlled/ambient cooling
-> normal part + reusable cold mold
```

- [ ] Mold må være kompatibel med form, capacity, materialtemperatur og thermal-shock limits.
- [ ] Kald mold kan avvise for varm pour eller gi dårligere kvalitet/wear; den skal ikke instant bli et kaldt ferdig item.
- [ ] Alle støpte former trenger en representerbar hot/thermal state, ikke bare ingot og nugget.
- [ ] Foretrukket langsiktig løsning er en generell thermal data component/bands (`WARM`, `HOT`, `RED_HOT`) på støpbare forms, slik at registry ikke eksploderer.
- [ ] Eksisterende `HOT_INGOT`, `HOT_NUGGET` og hot molds beholdes som kompatibilitetsgrunnlag inntil generell thermal state er implementert/migrert.
- [ ] Hot parts kan ikke brukes i recipes som krever kald/normal form med mindre recipe uttrykkelig aksepterer temperaturen.
- [ ] Cooling kan være ambient, water/quench eller maskinell, men materialegenskaper avgjør om thermal shock/skade er relevant.
- [ ] Mold returneres eksakt én gang og kan ha wear/durability uten å påvirke massebalansen til det støpte materialet.

## 11. Runtime-operasjoner og RecipeType-grensen

Den gamle teksten «Foundry = bare MELTING RecipeType» er for enkel for dette stateful multiblock-systemet. Samtidig skal Foundry ikke late som den er enhver chemistry-maskin.

- [ ] Foundry-controlleren er én stateful metallurgisk maskin med et begrenset sett typed Foundry operations.
- [ ] Hver ekstern transformation bruker eksisterende process identity (`MELTING`, `SMELTING`, `ALLOYING`, `CASTING`, `COOLING`) eller en eksplisitt Foundry-internal state transition.
- [ ] Chemistry types som `DISTILLATION`, `ELECTROLYSIS` og `CENTRIFUGING` kjøres ikke skjult i Foundry.
- [ ] Separat tap/drain/casting/cooling blocks kan eie sine egne operation contexts uten at Foundry-controlleren globalt søker alle recipe types.
- [ ] Et compiled `FoundryProcessPlan` beskriver steg, temperaturvinduer, inputs, outputs og transitions før materialer konsumeres.
- [ ] Recipe lookup er indeksert per operation/signature; aldri scan alle recipes per tick.

## 12. Spillerinformasjon

Foundry-UI skal vise faktiske data, ikke gjette spillerens mål.

Vis:

- innhold og exact amount per constituent;
- total amount/capacity;
- temperatur, heat input og thermal limit;
- current phase/structure og om innholdet er en exact defined alloy;
- aktiv operation, blokkering og nødvendig neste fysisk prosess;
- mold state, fill og cooling progress.

Ikke vis:

- «closest alloy»;
- hvor mye som mangler for en annen alloy;
- tilfeldig foreslått materialnavn;
- maskinvalg bare fordi IO passer.

JEI kan vise definerte exact alloy-ratios og statiske process chains. Dynamic mixture-stackens tooltip/analysis view viser dens faktiske composition og validerte separation route.

## 13. Ytelse og threading

En bath med 20 komponenter er liten dersom dataene normaliseres og caches riktig.

- [ ] Canonicalization: `O(k log k)` ved endring, der `k <= 20`.
- [ ] Pairwise compatibility kan være opptil `O(k²)` = 400 sammenligninger, men bare ved content revision.
- [ ] Defined-alloy lookup er hash/index lookup, ikke full scan.
- [ ] Heat tick er `O(1)` per formed Foundry pluss aktivt job count med en hard grense.
- [ ] Ingen composition flattening, route planning, multiblock scan eller recipe scan hvert tick.
- [ ] Cache plan etter canonical signature + structure + relevant temperature band + registry/rule revision.
- [ ] Cache er bounded og invalidates ved datapack/material reload.

Vanlig exact matching skal kjøres synkront; det er billigere enn thread scheduling. Bare tung, immutable planlegging/global validering kan flyttes til en bounded shared executor:

```text
server thread: lag immutable snapshot + revision
worker: ren beregning uten world/registry/ItemStack-tilgang
server.execute: revision check + apply
```

- [ ] Ingen egen tråd per Foundry.
- [ ] Ingen world, BlockEntity, Registry, ItemStack eller network access fra worker.
- [ ] Bounded queue, deduplication av samme signature og fallback når køen er full.

## 14. Massebalanse, loops og sikkerhetsvalidator

Hver transition må bevare exact flattened elemental vector, inkludert slag, gass, residue, mold og reagenter på riktig ledger.

- [ ] Main material outputs er guaranteed; chance outputs brukes ikke i reversible metallurgy chains.
- [ ] Tiny/small/normal units konverteres med exact rational units.
- [ ] Casting, cooling og grinding endrer form/temperatur, ikke mengde.
- [ ] Mixing og full separation kan være en matematisk reversibel syklus. Det er tillatt bare når syklusen er eksakt massebalansert og aldri gir positiv expected/worst-case materialgevinst.
- [ ] Energi, tid eller høy tier gjør ikke en materialskapende loop trygg.
- [ ] Ekstra output krever ekstra material-input eller en eksplisitt balansert kilde.

Dagens `ProcessSafetyValidator` gjør per-step flattened elemental balance, avviser non-guaranteed chemistry outputs og blokkerer alle directed cycles. Dette er trygt for dagens generated chemistry, men for strengt for framtidige reversible Foundry-transformasjoner. Foundry-arbeidet må derfor utvide validatoren til stoichiometric/vector cycle analysis:

- [ ] behold hard block for positiv materialgevinst i enhver reachable cycle;
- [ ] behold hard block for chance-bonus i tapsfri/reversibel cycle;
- [ ] tillat kun cycles der netto materialvektor er nøyaktig null under alle tillatte konverteringer;
- [ ] skill form/temperature edges fra substance-creation edges;
- [ ] valider hele graphen før recipes/plans publiseres;
- [ ] ved usikkerhet: avvis plan med konkret diagnostic.

## 15. Implementeringsrekkefølge

1. [ ] Definer canonical `CompositionPayload`, exact unit ledger og stable signature.
2. [ ] Lag defined-alloy indexes og ambiguity diagnostics.
3. [ ] Lag generic dynamic molten/solid/dust/concentrate carriers med codec, limits og tooltip.
4. [ ] Utvid process planner til dynamic mixture separation og terminating concentrates.
5. [ ] Utvid graph validator fra «avvis alle cycles» til vector-balansert cycle-safety.
6. [ ] Definer thermal state component og migreringsregel for eksisterende hot forms/molds.
7. [ ] Definer Foundry operation plan og process semantics.
8. [ ] Implementer variable Foundry + cached multiblock state.
9. [ ] Implementer heat-provider contract og første heater.
10. [ ] Implementer molten bath, exact alloy resolution og unclassified mixtures.
11. [ ] Implementer mold preheat, pouring, solidification, hot output og cooling.
12. [ ] Implementer UI/JEI/Jade/diagnostics uten closest-alloy hints.
13. [ ] Koble valid reduction routes uten å omgå chemistry.
14. [ ] Kjør determinism-, restart-, dupe-, performance- og multiplayer-tests.

## 16. Obligatoriske tester

- [ ] Alle size variants, feil heater-size og cache invalidation.
- [ ] ULV/LV/MV tier-band boundaries: 500/501/1000/1001 °C.
- [ ] Pure metal melting endrer bare phase.
- [ ] Composite dust med chemistry-route kan ikke bruke melting som snarvei.
- [ ] Exact ratio 1:2 og scaled 2:4 matcher samme alloy; 2:2 forblir mixture.
- [ ] Nested `.contains(...)` bevarer top-level og flattened ledgers.
- [ ] Ambiguous flattened signatures gjettes ikke.
- [ ] 20-component payload canonicalizes deterministisk og holder tick-budget.
- [ ] Undefined mixture kan støpes, kjøles, males og separeres uten tap/gevinst.
- [ ] Ingen closest-alloy/missing-amount UI.
- [ ] Hot mold/hot part kan ikke bli kald uten cooling transition.
- [ ] Chunk unload/reload og server restart bevarer temperature/composition/job state trygt.
- [ ] Ingen `A -> B -> C -> A`-syklus kan gi positiv materialvektor, inkludert chance/tiny/small conversions.
- [ ] To identiske loads/runs gir identiske signatures, plans og recipe IDs.

## Ferdig når

Foundryen er ferdig når den kan varme, smelte gyldige materialer, utføre balansert reduction der planlagt, blande opptil 20 constituents, finne exact definerte alloys, bevare udefinerte blandinger, støpe gjennom hot molds, kjøle hot outputs og gi en sikker separasjonsvei tilbake – uten materialnavn-hardkoding, tick-scans, tilfeldig recipe-valg eller profitable loops.
