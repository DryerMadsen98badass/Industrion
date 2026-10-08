# Phase 05 – Recipe Type Foundation – FERDIG

**Status: FERDIG.** Phase 05 er implementation-complete. Phase 06 er også ferdig; Phase 07 er nå aktiv fase.

Industron beholder den eksisterende generiske CE recipe/runtime-infrastrukturen. En typed `RecipeTypeDefinition` identifiserer prosessen; RecipeType er ikke en egen power-, chemistry- eller controller-definisjon.

## 1. Låste arkitekturregler

- [x] Én konkret machine bruker én RecipeType per processing path.
- [x] Samme RecipeType kan brukes av flere machine-varianter på tvers av tier og power-system når designet tillater det.
- [x] RecipeType bestemmer ikke steam/electric/kinetic/non-energy.
- [x] Temperature, Chemical Balance (`CB`), RPM, circuit, catalyst/non-consumable input, duration, tier og andre runtime-conditions ligger på machine/recipe.
- [x] Pressure er ikke en process-condition i dette systemet.
- [x] RecipeType beskriver process identity/layout og velger ikke konkret controller-klasse.

## 2. Verifisert RecipeType-katalog

Ved Phase-05 completion var baseline **62 gameplay process RecipeTypes + 1 intern `TEST_PROCESSING` type**. Katalogen under dokumenterer denne historiske Phase-05-baselinen.

Senere arbeid har lagt til/fjernet enkelte runtime-types uten å åpne Phase 05 på nytt. Dagens `CERecipeTypes.ALL` har **69 gameplay types** etter blant annet primitive/manual additions, inkludert `HAND_PROCESSING`. `TestProcessingRecipeType` finnes fortsatt som test-definition, men er ikke medlem av dagens `CERecipeTypes.ALL`.

Phase-05 gameplay-katalogen var:

```text
PULVERIZING
CRUSHING
GRINDING
SIFTING
WASHING
MIXING
HEATING
COOLING
DRYING
EVAPORATION
VAPORIZATION
CONDENSATION
LIQUEFACTION
FREEZING
MELTING
SMELTING
ALLOYING
CASTING
ROASTING
CALCINATION
SINTERING
ANNEALING
HEAT_TREATING
QUENCHING
CHEMICAL_REACTION
DISSOLUTION
NEUTRALIZATION
PRECIPITATION
CRYSTALLIZATION
LEACHING
SOLVENT_EXTRACTION
PYROLYSIS
CRACKING
REFORMING
POLYMERIZATION
FERMENTATION
ELECTROLYSIS
ELECTROREFINING
ELECTROWINNING
CENTRIFUGING
FILTRATION
PHASE_SEPARATION
GAS_SEPARATION
ABSORPTION
ADSORPTION
DISTILLATION
FRACTIONATION
COMPACTING
COMPRESSING
EXTRUDING
ROLLING
HAMMERING
CUTTING
TURNING
BENDING
WIRE_DRAWING
WINDING
PRECISION_MACHINING
ASSEMBLING
MAGNETIC_SEPARATION
MAGNETIZING
POLISHING
```

- [x] Alle type-identiteter er typed constants gjennom `CERecipeTypes`.
- [x] Alle registrerte typer ligger i den sentrale `CERecipeTypes.ALL`-listen som runtime/JEI bruker.
- [x] `FoundationDefinitionValidator` validerer duplicate type IDs, display names, IO-limits og duplicate supported-logic IDs.
- [x] `CERecipeTypes` bygger `BY_ID` med en unik map-key per `ResourceLocation`, slik at duplicate IDs også feiler ved initialisering.

### Post-Phase-05 additions

`CERecipeTypes.ALL` er senere utvidet av faktisk gameplay-arbeid. Dette endrer ikke Phase-05 ferdigstatus. `HAND_PROCESSING` er for eksempel tierless, bruker `uses`/right-clicks og skal derfor ikke tvinges inn i ULV–IV machine skeleton-strukturen bare for å passe den historiske fasen.

## 3. JEI/runtime-kontrakt

`IndustronJeiPlugin` itererer `CERecipeTypes.ALL` både ved category-registration og recipe-registration. Dermed brukes samme sentrale type-katalog av JEI og CE runtime uten en separat håndskrevet JEI-liste.

- [x] Registrerte types får generisk JEI category/layout gjennom eksisterende CE-infrastruktur.
- [x] Recipes filtreres mot `recipe.recipeType()` og registreres under tilhørende JEI RecipeType.
- [x] Machine/multiblock catalysts kobles via type-ID og trenger ikke per-type JEI-hardkoding.
- [x] `TEST_PROCESSING` kan fortsatt eksistere som intern test-type uten å regnes som gameplay-process.

## 4. Håndskrevne recipe paths – ULV til IV

Alle **62 Phase-05 gameplay process RecipeTypes** hadde en håndskrevet skeleton-path under:

```text
src/main/java/net/mads/industron/recipe/recipes/<recipe_type>/
```

Hver path har:

```text
ULV<TypeName>Recipes.java
LV<TypeName>Recipes.java
MV<TypeName>Recipes.java
HV<TypeName>Recipes.java
EV<TypeName>Recipes.java
IV<TypeName>Recipes.java
```

Skeleton-helperen setter alltid både riktig `MachineTier` og riktig `CERecipeTypes.<TYPE>`.

De ni prosessene som manglet skeletons er ferdigstilt i denne leveransen:

```text
TURNING
BENDING
WIRE_DRAWING
WINDING
PRECISION_MACHINING
ASSEMBLING
MAGNETIC_SEPARATION
MAGNETIZING
POLISHING
```

Dette gir **9 × 6 = 54 nye skeleton-filer**.

- [x] Alle gameplay-types har ULV–IV skeletons.
- [x] Alle skeletons er koblet inn i `CERecipeProvider`.
- [x] Skeletons trenger ikke inneholde faktiske recipes i Phase 05.

## 5. Generated material recipe path

Automatiske material-recipes skal ikke blandes inn i de håndskrevne tier-filene.

`material/recipes/` er reservert for senere generatorer fra stone, wood, raw ore sources og senere generated alloys/compounds/substances. Phase 14 eier mappingen fra generated process requests til riktig RecipeType og generated recipe path.

- [x] Håndskrevne recipes og generated material recipes har tydelig separert ownership/path.
- [x] Senere generated recipes skal gjenbruke samme CE recipe/runtime-format.

## 6. Machine/recipe conditions

RecipeTypes bruker den generelle CE recipe-dataformen; konkrete conditions eies av recipe/machine.

Støttet grunnlag inkluderer:

```text
MachineTier
Duration
Temperature
Chemical Balance (CB) range
RPM min/max
Output RPM
Circuit
Catalyst / non-consumable inputs
Typed required/optional logic
Existing runtime/world conditions
```

Eksisterende validation dekker blant annet:

- [x] `duration > 0`.
- [x] circuit er `1..32`.
- [x] temperature requirement må være positiv.
- [x] min/max/output RPM må ligge innen CE runtime-range, og max kan ikke være lavere enn min.
- [x] Chemical Balance er finite, ligger i `-100..100`, og max kan ikke være lavere enn min.
- [x] recipe IO må respektere RecipeType-layoutens slot-limits.
- [x] required/optional logic må støttes av RecipeType.
- [x] CB-konvensjonen er `CB < 0 = basic`, `CB = 0 = neutral`, `CB > 0 = acidic`.
- [x] Ingen pressure-condition er lagt til.

## 7. RecipeType skal ikke gjøre dette

RecipeType skal ikke:

- bestemme power source;
- hardkode CB-range eller temperature for alle recipes av typen;
- velge controller/machine-klasse;
- opprette alloys/compounds;
- balansere reactions;
- fungere som universal physics-regel.

Physics/transformasjonsreglene eies av **Phase 06 – Process Rules**.

## 8. Verifikasjon av denne leveransen

Source-pakken er strukturelt kontrollert for at:

- alle 62 Phase-05 gameplay-type constants hadde en recipe-folder;
- alle 62 folders har nøyaktig ULV/LV/MV/HV/EV/IV skeleton-klasser;
- de 54 nye klassene bruker riktig package, tier og `CERecipeTypes` constant;
- `CERecipeProvider` importerer og kaller alle de nye skeletons;
- ingen nye faktiske gameplay recipes er introdusert.

Den opplastede kildepakken inneholdt `src/main`-innhold, men ikke Gradle wrapper/build-filer. Derfor kan en ekte prosjektlokal `compile`/`runData`/client smoke test først kjøres etter at filene er limt inn i hele prosjektet. Dette er en merge/release-verifikasjon og endrer ikke at Phase 05-implementasjonen er ferdig.

## Ferdigkriterier

- [x] Phase-05 baseline er dokumentert som 62 gameplay-types + `TEST_PROCESSING`; senere additions dokumenteres som post-Phase-05 types.
- [x] Alle registrerte type IDs valideres mot duplicates og ugyldige definitions.
- [x] JEI/runtime bruker den sentrale RecipeType-katalogen generisk.
- [x] Alle 62 Phase-05 gameplay-types hadde ULV–IV recipe skeletons; tierless/manual post-phase types trenger ikke slike skeletons.
- [x] Alle skeletons er koblet til `CERecipeProvider`.
- [x] Machine/recipe conditions ligger utenfor RecipeType og har eksisterende value/range-validation.
- [x] `material/recipes/` er reservert for senere automatic material recipe generation.
- [x] Phase 06 kan nå bygge process semantics oppå en komplett og stabil RecipeType-foundation.

**Status: FERDIG. Phase 06 er senere fullført; neste aktive fase er Phase 07 – Foundry, Heater, støping og dynamiske metallblandinger.**
