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

## Nåværende anbefalte implementeringsrekkefølge

1. Få Wood/Stone-systemet til å kompilere på NeoForge 1.21.1 og verifiser `runData`.
2. Verifiser genererte Test Wood/Test Stone assets, registry IDs, models, loot, tags og textures i klient.
3. Fullfør `existing(...)`-oppførsel og composition references med tester.
4. Koble alle registrerte `StoneMaterial`-hosts automatisk inn i ore-generatoren, normal + small.
5. Stabiliser wire + fluid transport-formler og compatibility-regler.
6. Fjern gjenværende legacy/hardkodede materialbroer.
7. Bygg chemistry/composition flattening og reaction-system.
8. Koble reactions til recipes og energy tiers.
9. Utvid machines/casings/multiblocks/maintenance.
10. Gjør kontrollert cleanup av virkelige metaller/materialer og alle avhengigheter.
11. Lag TreeDefinition/tree-growth etter at wood-materialsystemet er stabilt.

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


## Active material/geology integration

Current canonical architecture/order: `CURRENT_TASK.md` and `to do/23-material-geology-autorecipe-integration.md`. Natural resources use composed ore-source materials, registered StoneMaterial hosts, tier-driven dimensions and deterministic deposit worldgen. Automatic composition processing is currently scoped to stone, wood and raw ore-source materials.
