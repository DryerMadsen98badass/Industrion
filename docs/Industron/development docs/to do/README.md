# Industron TODO – anbefalt implementeringsrekkefølge

Denne TODO-en beholder den opprinnelige implementeringsrekkefølgen og langsiktige roadmapen. Senere arbeid er delvis implementert ute av den opprinnelige rekkefølgen, særlig casing- og Assembly-kjernen. Bruk derfor statusnotatet under sammen med faktisk kode; ikke tolk nummerrekkefølgen som en eksakt nåværende arbeidskø.

## Status akkurat nå

1. **Phase 00 – Foundation: FERDIG**
2. **Phase 01 – Atomic model og ions: FERDIG**
3. **Phase 02 – Material properties: FERDIG**
4. **Phase 03 – Capabilities and requirements: FERDIG**
5. **Phase 04 casing-kjerne: IMPLEMENTERT / øvrig content-generation fortsatt roadmap**
6. **Phase 15–17 Assembly-kjerne: IMPLEMENTERT** – ComponentDefinition, AssemblyPlan/runtime, Workbench, JEI, free/fixed materials, Frame/Casing assembly og Deployer tool-work finnes i dagens kode

Generated alloys/compounds/chemistry skal fortsatt vente til recipe/process-fundamentet og Foundry/casing-grunnlaget er på plass.

## Anbefalt rekkefølge

### 00–04: Atomic/material/content fundament

0. **00 Foundation – FERDIG** – diagnostics, validation, determinisme og domain tests.
1. **01 Atomic model + ions – FERDIG** – neutral atoms, multiple ion states og tier-uavhengig atomic identity.
2. **02 Material properties – FERDIG** – sentral property-kilde og generated intrinsic stats.
3. **03 Capabilities and requirements – FERDIG** – typed scalar/range/exact/enum/bool requirements, practical capabilities og environment propagation.
4. **04 Content generation + Casings – AKTIV** – robust content generation, dynamic ore hosts og material-derived casing generation fra `Stats`/requirements.

### 05–07: Process/machine fundament – før generated chemistry

5. **05 Recipe Type Foundation** – 53 gameplay process types, typed identities og ULV–IV recipe-folder skeletons. RecipeType eier ikke power source eller konkrete process conditions.
6. **06 Process Rules** – hva hver process fysisk har lov til å gjøre; separation, distillation, electrolysis, mixing, alloying, chemical reaction osv.
7. **07 Foundry + Heater Multiblocks** – variable Foundry, full-footprint heater variants og heat-provider contract før generated alloys kobles inn.

Faste regler:

```text
Én concrete machine = én RecipeType
RecipeType != power source
Conditions ligger på machine/recipe
Pressure brukes ikke som process-condition
```

### 08–13: Substances og chemistry

8. **08 Composition Graph** – canonical composition, mixtures, substance identity og registry/reachability.
9. **09 Alloys** – generated metallic/alloy phases med derived properties og tradeoffs.
10. **10 Molecular & Bond Model** – molecular graph, single/double/triple bonds, valence og structural identity for de fiktive elementene.
11. **11 Compounds & Ionic Chemistry** – ionic/covalent compounds, charge balance, solutions og compound-derived properties.
12. **12 Organic/Polymer-like Chemistry** – repeating units/functional structures og generated polymer/elastomer-like materials uten real-element hardcoding.
13. **13 Reaction System** – stoichiometric balancing, bond/redox validation, catalysts og process intent.

### 14: Automatic recipes

14. **14 Automatic Process Recipe Generation** – map validated reaction/process intent til riktig RecipeType og beregn tier/duration/conditions.

`material/recipes/` reserveres for automatiske recipes fra `StoneMaterial`, `GemMaterial`, `WoodMaterial`, `IndustrialMaterial` og senere generated substances.

### 15–17: Assembly crafting

15. **15 Component Definitions – KJERNE IMPLEMENTERT** – recursive ComponentDefinition med Material/Component/Item/Tool/Wait; advanced dependent-input/exposure-idéer er fortsatt roadmap.
16. **16 Assembly Recipe Language – KJERNE IMPLEMENTERT** – Material/Component/Metal, base block/item, requirements, waits/tools og fixed/free semantics; se kanonisk Assembly-guide for faktisk API.
17. **17 Assembly Runtime & Tools – KJERNE IMPLEMENTERT** – precompiled plans, Workbench/world runtime, JEI, timed tools og FakePlayer/Deployer pulse-work; videre hardening/tests gjenstår.

### 18–21: Integration og hardening

18. **18 Machines & Multiblocks** – integrer generated process/casing/substance data i den generelle existing runtime; Foundry er allerede spesifisert tidligere i Phase 07.
19. **19 Maintenance** – separat persistent maintenance-state.
20. **20 Create Integration** – adapters/regression; Create eier ikke domain-reglene.
21. **21 Validation & Balance** – full boundary/performance/system testing.

## Chemical Balance

Legacy pH skal erstattes av Chemical Balance:

```text
CB < 0 = basic
CB = 0 = neutral
CB > 0 = acidic
```

`Acidity` for materialer er fortsatt en existing generated materialstat. CB er runtime/recipe/machine chemical-environment-begrepet og skal ikke introdusere en parallell materialstat.

## Foundry/heater size rule

```text
Variant 1: inner 1x1 -> outer/heater 3x3
Variant 2: inner 3x3 -> outer/heater 5x5
Variant 3: inner 5x5 -> outer/heater 7x7
Variant 4: inner 7x7 -> outer/heater 9x9

variant n:
inner = 2n - 1
outer/heater = 2n + 1
```

Heateren fyller hele arealet direkte under Foundryen og må matche size variant eksakt.

## Faste prinsipper

- Kjente Industron-konsepter bruker typed constants/autocomplete, ikke fritekst.
- ResourceLocation item/block IDs kan være strings når de faktisk er external registry IDs.
- Tags gis som typed `TagKey` constants.
- Materialene og elementene i gameplay er fiktive; roadmap-eksempler skal ikke bygge designet rundt real-element special cases.
- Intrinsic materialstats kommer fra den sentrale material-property pipeline.
- Casings genereres bare når materialets stats oppfyller casing-definition requirements.
- Ett materiale kan kvalifisere til flere casing-typer; et materiale som ikke kvalifiserer får ingen variant.
- `atLeast(x)` betyr x og alt høyere; exact/range semantics skal være eksplisitte.
- Dependent requirements bruker faktisk valgt capability value.
- Environment requirements propageres bare etter Phase 03-reglene; tools/waits er ikke material-bearing sluttproduktdeler.
- RecipeTypes bestemmer prosessidentity, ikke steam/electric/kinetic/non-energy.
- Temperature, CB, RPM, circuit, catalyst, duration og tier ligger på recipe/machine etter behov.
- Pressure brukes ikke som process-condition.
- Composition er ikke det samme som structure.
- Physical separation bryter ikke chemical bonds uten en process som faktisk støtter det.
- Automatic chemistry recipes velges fra typed process rules, ikke hardkodede machine-navn.
- Runtime bruker precompiled/indexed/cached state der det er mulig, ikke recursive global search per tick/interaksjon.

## Hva vi gjør nå

Roadmapen over er fortsatt nyttig for gjenstående systemer, men aktiv prioritet skal bestemmes fra siste prosjekt og brukerens siste instruks. Casing- og Assembly-kjernen er allerede implementert senere enn den opprinnelige statuslinjen. For Assembly er `../code/recipes/assembly-recipes.md` fasit.
