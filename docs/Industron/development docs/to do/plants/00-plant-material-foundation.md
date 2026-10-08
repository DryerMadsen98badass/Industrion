# 00 – Plant material foundation

## Mål

Gjør nye planter enkle å legge til uten species-hardkoding i recipe-systemene. Én `PlantMaterial` skal være sannhetskilden for materialidentity og organisk composition.

## Nåværende kontrakt

En plant kan defineres konseptuelt som:

```java
plant("id", "Display Name", color)
    .contains(
        component(SYLVARA, ...),
        component(LIGNARA, ...),
        component(DULCARA, ...),
        component(RESYRA, ...)
    )
    .parts(PlantPart.FIBER, PlantPart.STRING)
    .existing(PlantPart.PLANT, "namespace:item_or_block_item");
```

`.contains(...)` skal bruke registrerte `IndustrialSubstance`-definitions. Det skal ikke finnes separate booleans som `fiberPlant`, `canCompost` eller `canMakeString` når dette kan avgjøres fra composition/process-rules.

## Forms

Første `PlantPart`-sett er:

```text
PLANT
SEEDS
FIBER
STRING
```

`PLANT` og `SEEDS` kan peke til eksisterende content. `FIBER` og `STRING` kan genereres når en faktisk processing-route trenger dem.

Dagens minimale generator bruker en direkte `Sylvara`-komponent som deterministic validering av at en eksplisitt forespurt `FIBER`/`STRING` form gir mening. Forms opprettes ikke bare fordi de teoretisk kan eksistere: definitionen må også be om dem med `.parts(...)`. Senere kan full composition/process-analyse avgjøre yield og route.

String har **ingen strength-stat**.

## Hand Processing

`HAND_PROCESSING` brukes for svært enkelt arbeid som kan gjøres med hendene før Workbench/maskiner finnes.

Recipe-eksempel, når en konkret plante senere er bestemt:

```java
RecipeDefinition.recipe()
    .recipeDefinition(Option.id("..."))
    .recipeDefinition(Option.recipeType(CERecipeTypes.HAND_PROCESSING))
    .recipeDefinition(Option.inputItem(..., 4))
    .recipeDefinition(Option.outputItem(..., 1))
    .recipeDefinition(Option.uses(6));
```

Runtime-regler:

- main-hand input;
- Ctrl + right-click kreves for å **starte**;
- første Ctrl-click teller som use 1;
- etter start fortsetter separate right-clicks dersom samme recipe/input fortsatt matcher;
- bytte/miste input stopper aktiv operation;
- ingen tier;
- ingen tool;
- ingen durability;
- JEI viser uses/right-clicks og Ctrl-startkravet.

## Ikke bestem ennå

- første konkrete species;
- exact `.contains(...)` for grass/flax/reed/cactus eller andre planter;
- exact plant -> fiber yield;
- exact fiber -> string count/uses;
- farming/growth/genetics.
