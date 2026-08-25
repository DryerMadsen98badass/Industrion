# Phase 05 – Recipe Type Foundation

Denne fasen kommer før generated alloys og chemistry. Den eksisterende CE recipe/runtime-infrastrukturen beholdes; vi bygger ikke et nytt Minecraft `RecipeType`/serializer-system per prosess.

Industron bruker den eksisterende generiske CE-recipe-motoren, mens en typed `RecipeTypeDefinition` identifiserer selve prosessen.

## Faste arkitekturregler

- [x] Én konkret machine bruker **én RecipeType**.
- [x] Samme RecipeType kan brukes av flere machine-varianter på tvers av tier og power-system, f.eks. steam/electric/kinetic/non-energy der designet tillater det.
- [x] RecipeType bestemmer **ikke** power source.
- [x] Temperature, Chemical Balance (`CB`), RPM, circuit, catalyst/non-consumable input, duration, tier og andre runtime-conditions defineres på machine eller recipe – ikke hardkodet i RecipeType.
- [x] Pressure er ikke en process-condition i dette systemet.
- [x] RecipeType beskriver prosessen og dens identity/layout, ikke en konkret controller-klasse.

## 1. Recipe type catalogue

Det første komplette settet for material processing, alloys, chemistry og separation er:

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
```

Totalt: **53 process RecipeTypes**.

`TEST_PROCESSING` kan fortsatt eksistere som intern test-type så lenge testmaskinene trenger den, men er ikke en gameplay process i listen over.

## 2. Java path og ownership

Alle process RecipeTypes ligger under:

```text
src/main/java/net/mads/industron/recipe/recipetypes/
```

Hver type skal ha stable ID/display/layout-data gjennom det eksisterende `RecipeTypeDefinition`-systemet og registreres i den sentrale listen som JEI/runtime allerede bruker.

- [x] Typed constants med IntelliJ autocomplete.
- [x] Ingen chemistry-generator skal referere til process types med tilfeldige strings.
- [ ] Full project compile/runtime validation etter at type-settet er integrert lokalt.
- [ ] Verifiser at JEI automatisk oppretter category/layout for alle registrerte gameplay-types.

## 3. Håndskrevne recipe paths

Alle håndskrevne recipes organiseres etter RecipeType:

```text
recipe/recipes/<recipetypenavn>/
```

Hver mappe har tier-filer fra ULV til IV:

```text
ULV<TypeName>Recipes.java
LV<TypeName>Recipes.java
MV<TypeName>Recipes.java
HV<TypeName>Recipes.java
EV<TypeName>Recipes.java
IV<TypeName>Recipes.java
```

Tier-filen eier helperen som setter både `MachineTier` og `CERecipeTypes.<TYPE>`, slik at hver faktisk recipe bare trenger å beskrive sitt unike innhold.

Eksempelretning:

```text
recipe/recipes/distillation/
├─ ULVDistillationRecipes.java
├─ LVDistillationRecipes.java
├─ MVDistillationRecipes.java
├─ HVDistillationRecipes.java
├─ EVDistillationRecipes.java
└─ IVDistillationRecipes.java
```

- [x] ULV–IV skeleton-files er planlagt/opprettet for gameplay-type-settet.
- [x] Ingen faktiske chemistry/alloy recipes trenger å legges inn i denne fasen.

## 4. Generated material recipe path

Automatiske material-recipes skal **ikke** blandes inn i de håndskrevne tier-filene.

`material/recipes/` reserveres for senere generatorer fra blant annet:

```text
StoneMaterial
GemMaterial
WoodMaterial
IndustrialMaterial
```

og senere generated alloys/compounds/substances.

- [ ] Phase 14 kobler generated process requests til riktig RecipeType og generated recipe path.
- [ ] Generated material recipes skal bruke samme CE recipe/runtime-format som håndskrevne recipes.

## 5. Machine/recipe conditions

RecipeTypes støtter den generelle CE recipe-dataformen. Conditions eies av machine/recipe etter behov.

Aktuelle conditions/properties inkluderer:

```text
MachineTier
Duration
Temperature / temperature range
Chemical Balance (CB) range
RPM min/max for kinetic machines
Circuit
Catalyst / non-consumable process input
World/environment conditions der eksisterende runtime støtter det
```

- [x] CB erstatter legacy pH-konseptet: `CB = 0` neutral, positiv acidic, negativ basic.
- [x] Ingen pressure-condition skal legges til.
- [ ] Validation skal sikre gyldige values/ranges for de conditions som faktisk brukes.

## 6. RecipeType skal ikke gjøre dette

RecipeType skal ikke:

- bestemme steam/electric/kinetic/non-energy.
- inneholde hardkodet CB-range.
- inneholde hardkodet temperature for alle recipes av typen.
- velge konkret machine/controller.
- opprette alloy/compound chemistry.
- balansere reactions.

## Ferdig når

- [ ] Alle 53 gameplay RecipeTypes er registrert og kan lastes uten duplicate IDs.
- [ ] JEI/runtime ser alle typene gjennom eksisterende generiske system.
- [ ] ULV–IV recipe skeletons er koblet til provider uten at de trenger faktiske recipes.
- [ ] Machine/recipe conditions kan brukes uten at RecipeType eier power source eller process-specific hardcoding.
- [ ] `material/recipes/` er tydelig reservert for senere automatic material recipe generation.
