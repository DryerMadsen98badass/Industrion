# Phase 06 – Process Rules og fysiske transformasjoner

**Status: FERDIG.** Phase 06 er implementert som et typed process-semantics-lag over RecipeType-foundationen. Neste aktive fase er **Phase 07 – Foundry, Heater, støping og dynamiske metallblandinger**.

RecipeType sier **hvilken prosess en machine utfører**. Process Rules sier **hva den prosessen fysisk har lov til å gjøre**. Power source, controller-navn og konkrete recipe-conditions er fortsatt utenfor RecipeType/process identity.

## Implementert kjerne

Følgende er nå den kanoniske Phase 06-kjeden:

```text
MaterialAnalysis / runtime composition state
    -> ProcessSubstanceState
    -> ProcessOperation-set
    -> ProcessIntent
    -> ProcessRecipeResolver
    -> ProcessKind + RecipeTypeDefinition
    -> ProcessSemantics
    -> ProcessSafetyValidator
    -> recipe emission/runtime caller
```

Viktige implementerte typer:

- `ProcessSubstanceState` – skiller bonded substance fra powder/liquid/solution/suspension/phase-separated/molten/gas/reaction mixtures.
- `ProcessOperation` – typed physical/chemical capabilities; ingen string-capability keys.
- `ProcessRuleSet` – capability-set per `ProcessKind`.
- `ProcessIntent` – abstrakt behov med operations, inputs, outputs, requirements og temperature-data uten controller/power-source.
- `ProcessRecipeResolver` – velger/validerer RecipeType/process identity og returnerer konkrete rejection diagnostics når ingen prosess passer.
- `ProcessSemantics` – state/topology-regler for physical separation, distillation/fractionation, mixing/alloying, phase changes og chemistry routes.
- `ElectrochemistrySemantics` – electron/oxidation-state signal validation for electrolysis/electrorefining/electrowinning.
- `ProcessSafetyValidator` – semantics + resolver + atom-/material-conservation + graph-cycle checks før automatic chemistry emitteres.

## 1. Skill mixture fra bonded substance

- [x] Typed structure/state skiller physical mixture, metallic/ionic/molecular/network topology og fluid/molten phases.
- [x] Runtime-state kan representere dynamic Foundry mixtures, solutions, suspensions, phase-separated baths, molten mixtures og reaction mixtures uten å registrere en ny bonded substance.
- [x] Samme elemental composition brukes ikke som bevis på mekanisk separerbarhet.
- [x] Process semantics vurderer topology/state/phase og ikke bare input/output-slottene.

`ProcessSubstanceState` er runtime-kontrakten. `PHYSICAL_MIXTURE` er separat fra `METALLIC_LATTICE`, `IONIC_LATTICE`, molecular/network topology osv.

## 2. Typed process operations

- [x] Process capabilities er typed `ProcessOperation`-verdier, ikke strings.
- [x] Capability-set dekker blant annet mixing, physical separation, density/magnetism/filtering, distillation/fractionation, bond change, electron transfer, oxidation-state change, metallic/ionic formation, crystallization, dissolution, leaching og precipitation.
- [x] `ProcessKind -> ProcessRuleSet -> RecipeTypeDefinition` er den eksplisitte mappingen mellom fysisk semantics og registrert process identity.
- [x] Alle `ProcessKind`-verdier i Phase 06 har unik capability-signatur og minst én registrert RecipeType-ID.

## 3. Physical separation

- [x] Generated composite-dust planner velger physical separation bare for `PHYSICAL_MIXTURE`.
- [x] Samme regel kan brukes av håndskrevne/runtime callers gjennom `ProcessRecipeResolver.validateRecipeType(...)` og `ProcessSemantics`.
- [x] Centrifuging krever physical mixture + phase-compatible outputs + reell density contrast.
- [x] Magnetic separation krever physical solid mixture med magnetisk og ikke-magnetisk fraksjon.
- [x] Filtration krever eksplisitt `SUSPENSION`, ikke bare «en eller annen liquid».
- [x] Phase separation krever eksplisitt `MIXED`/`PHASE_SEPARATED` state.
- [x] En bonded alloy/compound kan ikke åpnes av physical separators.
- [x] Ingen fallback velger centrifuge når ingen fysisk regel passer; generation avvises med diagnostic.

## 4. Distillation og fractionation

- [x] Tørr dust-distillasjon avvises.
- [x] `DISTILLATION` krever en physical condensed-fluid mixture og separerbare fluid/gas fractions.
- [x] `FRACTIONATION` er en egen process identity og velges ved close-but-distinguishable boiling/volatility behavior.
- [x] Planner bruker generated `boilingpoint`/`volatility` data når disse finnes.
- [x] Runtime/generated `ProcessMaterial.processProperties` kan brukes uten hardkodet backing-material.
- [x] Distillation/fractionation kan ikke brukes som skjult bond breaker fordi input må være `PHYSICAL_MIXTURE`.
- [x] Boiling proximity ligger i process selection/semantics og ikke som hardkodet konkret RecipeType-temperature/duration value.

## 5. Electrochemical processes

- [x] `ELECTROLYSIS`, `ELECTROREFINING` og `ELECTROWINNING` har separate typed operation-set.
- [x] Electrolysis krever liquid/molten ionic/solution-compatible feed.
- [x] Electrorefining krever molten metallic lattice/molten metal mixture.
- [x] Electrowinning krever metal-bearing liquid physical mixture/solution og metallic guaranteed products.
- [x] Electron-transfer/oxidation-state validation bruker `processProperties` først og backing `MaterialProperties` som fallback.
- [x] Generated chemistry kan derfor levere electrochemical potential, donation/acceptance og preferred ion charge uten named-material special cases.
- [x] Temperature, CB, tier, duration og catalyst ligger fortsatt på recipe/machine/requirements, ikke på RecipeType.

## 6. Mixing og alloying

- [x] `MIXING` må produsere en eksplisitt physical-mixture state.
- [x] `ALLOYING` må produsere en eksplisitt `METALLIC_LATTICE`/alloy state.
- [x] Powder mixture, liquid/solution/suspension, molten mixture og finished metallic lattice er forskjellige process states.
- [x] En ferdig metallic lattice er ikke automatisk fysisk separerbar til parent materials.
- [x] Phase 09 beholder ansvaret for exact alloy composition/matching og hvilke alloy-ratios som er gyldige.

## 7. Chemical reaction

- [x] `CHEMICAL_REACTION` er en executor for et allerede planlagt `ProcessStep`/`ProcessPlan`.
- [x] Recipe-emitteren oppfinner ikke nye compounds og balanserer ikke reaksjoner selv.
- [x] `ProcessSafetyValidator` kontrollerer flattened conservation vector før emission.
- [x] Temperature, CB, catalyst, state og andre conditions kan ligge på recipe/process requirements.
- [x] Pressure brukes ikke som process-condition i Phase 06 process-systemet.

## 8. Chemical Balance

Canonical sign:

```text
CB < 0 = basic
CB = 0 = neutral
CB > 0 = acidic
```

- [x] Recipe/machine kan uttrykke `chemicalBalanceRange(min, max)`.
- [x] CB er en recipe/machine condition, ikke en RecipeType-property.
- [x] Generated compound/material fluids får sin beregnede `MaterialProperties.acidity()` propagert til `IndustrialFluid.chemicalBalanceHundredths`.
- [x] Tooltip og Chemical Balance hatch leser dermed samme faktiske fluid-data; CB er ikke bare displaytekst.
- [x] Ingen hardkodede stoffnavn kreves for generated fluid CB.

## 9. Process selection contract

Et caller kan beskrive et abstrakt behov, for eksempel:

```text
TRANSFER_ELECTRONS
CHANGE_OXIDATION_STATE
BREAK_OR_FORM_BONDS
+ compatible liquid/molten ProcessSubstanceState
```

- [x] Komplett typed `ProcessIntent`/operation-set finnes for Phase 06 og kan også brukes av runtime Foundry payloads.
- [x] Resolver returnerer bare process types som støtter alle nødvendige operations og validerer input/output states.
- [x] Resolver returnerer `ProcessKind + RecipeTypeDefinition`, aldri controller-navn eller power source.
- [x] Ingen passende process gir failure med candidate/rejection diagnostics i stedet for «nærmeste» fallback.
- [x] Automatic chemistry går gjennom samme resolver/semantics contract i safety/emission-pipelinen.
- [x] Håndskrevne/runtime routes har et offentlig `validateRecipeType(...)`-entry point for samme contract.

## 10. Validation

- [x] `ChemistrySelfTest` dekker bonded-vs-mixture separation, explicit suspension, mixing-state, distillation/fractionation, generated electrochemistry properties og typed operation capabilities.
- [x] Separate Java 21 compile/self-test checks er kjørt på Phase 06 process-kjernen.
- [x] Separate CB-propagation checks bekrefter at generated acidity `40` blir fluid-data `4000` hundredths / `40.00 CB`, og at hatch-readable data finnes.
- [x] Source-structure validation bekrefter at alle Phase 06 `ProcessKind`-verdier mapper til registrert RecipeType og at capability-signaturene er unike.
- [x] Ingen default/unconditional centrifuge fallback finnes.

Den opplastede source-pakken inneholder ikke prosjektets Gradle-wrapper/build-filer, så full NeoForge `gradlew build/runData` kan ikke kjøres fra chat-arkivet alene. Phase 06-koden er derfor validert med Java 21 compile harness, self-tests og strukturelle source checks; full prosjektbuild kan kjøres direkte i den lokale Industrion-roten.

## Ferdigkriterium

- [x] En process kan forklare hvorfor en transformasjon er gyldig eller ugyldig.
- [x] Automatic chemistry kan resolve én passende process/RecipeType gjennom typed operations uten machine-name/power-source special cases.
- [x] Physical separators brukes ikke som universal bond-breakers.
- [x] Generated/runtime mixtures har eksplisitt state i stedet for å bli behandlet som bonded compounds.
- [x] Phase 07 kan bygge Foundry runtime oppå `ProcessSubstanceState`, `ProcessIntent` og `ProcessRecipeResolver` uten å lage et parallelt process-system.

**Status: FERDIG. Neste aktive fase: Phase 07 – Foundry, Heater, støping og dynamiske metallblandinger.**
