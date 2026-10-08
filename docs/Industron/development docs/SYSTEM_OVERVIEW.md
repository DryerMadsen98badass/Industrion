# Systemoversikt og målarkitektur

## Overordnet flyt

```text
ElementDefinition
    -> Atomic / Electron Model
    -> MaterialPropertyCalculator
    -> MaterialProperties
    -> Classification / Capabilities
    -> Generated Forms / Blocks / Fluids
    -> Wires / Pipes / Pumps / Tanks / Ores

WoodMaterial / StoneMaterial
    -> IndustrialSubstance
    -> contains(...) / existing(...)
    -> typed Structure Model
    -> grayscale structure_sets
    -> tinted/generated block families
    -> material forms (wood pulp / stone dust)

All IndustrialSubstances
    -> Composition Graph
    -> recursive elemental flattening
    -> Compound / Chemistry Rules
    -> Balanced Reactions
    -> Generated Recipes
    -> Machines / Casings / Multiblocks / Maintenance
```

## Grunnprinsipper

### 1. Minimal material-input
Et fiktivt element skal i utgangspunktet bare kreve:

- id
- display name
- symbol
- atomic number
- tier

Gameplay-egenskaper skal ikke håndskrives per element når de kan utledes.

### 2. Én property-kilde
`MaterialPropertyCalculator` skal være den sentrale kilden for materialfysikk. Andre systemer skal lese resultatene fra `MaterialProperties`, ikke lage egne konkurrerende formler for samme property.

### 3. Universell substance/composition-modell
Elementer, alloys/compounds, wood, stone, fluids, gases og relevante material forms skal kunne delta i samme composition graph.

Mål-API:

```java
.contains(
    component(VERNIUM, 5),
    component(XYLORA, 2),
    component(XENOLITE, 1)
)
```

Composition skal lagres strukturert og senere kunne flattenes rekursivt til elemental composition med cycle detection.

### 4. Generated content er data-driven
Nye forms, texture variants, stone families og wood families skal så langt som mulig oppdages/genereres uten å vedlikeholde parallelle hardkodede lister.

### 5. Static registry vs dynamic rules
Minecraft registry entries må bestemmes før registry freeze. Datapack/reload kan senere endre rules, reactions og recipes, men ikke vilkårlig opprette nye blocks/items etter registrering. Arkitekturen må bevare dette skillet.

## Structure material pipeline

```text
WoodMaterial XYLORA
    -> WoodModel.SPRUCE
    -> structure_sets/wood/spruce/
    -> tint with XYLORA color
    -> log/planks/door/trapdoor/window/etc.
    -> tiny/small/normal wood pulp

StoneMaterial XENOLITE
    -> StoneModel.DIORITE
    -> structure_sets/stone/diorite/
    -> tint with XENOLITE color
    -> full decorative stone family
    -> tiny/small/normal dust
    -> later: automatic ore-host participation
```

`existing(...)` skal overstyre auto-generation for den konkrete rollen/formen slik at eksisterende registry objects kan gjenbrukes uten duplikater.

## Metal structure templates

Metal bruker ikke materialnavn som texture-family. Hver block-type velger design uavhengig:

```text
structure_sets/metal/door/metal_1/
structure_sets/metal/door/metal_2/
structure_sets/metal/trapdoor/metal_1/
structure_sets/metal/block/metal_5/
```

Dette gjør at samme metall kan ha f.eks. `door = metal_2` og `trapdoor = metal_1`.

## Nåværende implementeringsstatus og neste rekkefølge

Dagens kode har common `MaterialPart`, komplette Minecraft/Create stone-/wood-definisjoner, dynamic ore hosts, runtime geology, mekanisk ore-processing, chemistry analysis, deterministic sammensatt-dust routes, automatic slurry/solution/reaction-mixture, recipe emission og første graph-/mass-validation.

Neste anbefalte rekkefølge:

1. Canonical dynamic composition payload/signatures og exact units.
2. Exact defined-alloy indexes og generic unclassified mixture carriers.
3. Stoichiometric graph validation som kan tillate bare eksakt balanserte reversible metallurgy cycles.
4. Generell thermal item/fluid state og kompatibilitet med eksisterende hot forms/molds.
5. Variable Foundry + full-footprint Heater contract og cached thermal runtime.
6. Molten bath, mixing, casting, cooling og deterministic separation integration.
7. UI/JEI/diagnostics og 20-component performance/dupe tests.
8. Videre chemistry, casing, maintenance og TreeDefinition-hardening etter aktiv prioritet.

## Gjeldende Assembly-flyt

```text
AssemblyRecipeDefinition
    -> AssemblyPlan (precompiled/flattened runtime steps)
    -> AssemblyRuntime

Component.X
    -> ComponentDefinitions
    -> recursive expansion
    -> Material leaves / exact items / tools / waits

Material.X
    -> fysisk form/leaf

Metal.X
    -> fixed materialbinding når eksplisitt angitt
```

En free Component-root uten `Metal.X` gir uavhengig materialvalg per material-leaf. En fixed root med `Metal.X` arver samme material gjennom normale nested branches. `inputAny(...)` kan bryte en fixed binding lokalt og parent-relative stat-krav kan hindre at erstatningsmaterialet går under/over parentens capability. Generated Frame/Casing recipes bruker den samme `AssemblyPlan`-resolusjonen som runtime for feasibility.


## Active Foundry integration

Current canonical architecture/order: `CURRENT_TASK.md`, completed `to do/06-process-rules.md`, active `to do/07-foundry-and-heater-multiblocks.md`, then `to do/09-alloys.md` and `to do/14-automatic-process-recipe-generation.md`. Material/geology Phase 23 is historical implementation context, not the current baseline.

```text
declared/dynamic composition
  -> canonical top-level signature + flattened ledger
  -> structure/phase/property classification
  -> compiled Foundry or chemistry process plan
  -> graph/mass validation
  -> indexed runtime operation
```
