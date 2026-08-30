# Geology and ore worldgen architecture

The current geology work is composition-driven and fictional. Read `../../to do/23-material-geology-autorecipe-integration.md` for the implementation sequence.

## Vocabulary

- **element/substance**: atomic/material identity that may be contained in another material;
- **ore-source material**: manually named composed material that is naturally available as a resource;
- **ore block variant**: one ore-source rendered in one registered stone host;
- **deposit**: spatial body containing one or more ore-source materials;
- **geology region**: deterministic large-scale planning cell.

This vocabulary prevents the word "ore" from meaning an element, block and whole deposit at the same time.

## No elemental-ore default

The normal resource model does not create one ore block per element. Elements are discovered through one or more composed ore-source materials.

## Source definitions

Permanent ore-source definitions live under:

```text
src/main/java/net/mads/industron/material/defenitions/
```

Names and IDs are manually chosen. `runData` may propose composition but never invent the source name/ID.

## Missing source workflow

`runData` checks every registered element for natural source coverage. Missing elements get deterministic, property-based component/ratio suggestions and a copyable `.contains(component(...), ...)` line.

The user adds the actual source definition, reruns data generation, and the source automatically enters processing and geology.

## Tier to dimension

Highest relevant tier wins:

```text
ULV-HV -> Overworld
EV-LuV -> Nether
ZPM+ -> End
```

A deposit may mix tiers. Host stone does not choose the dimension.

## Stone hosts

`StoneMaterials.ALL` (or its eventual registry abstraction) is the host source of truth.

A registered stone with a valid `MaterialPart.STONE` base automatically participates. No `MaterialStoneSource` edit and no host-specific ore enum constant is allowed for a new stone.

Ore block identity is source + host + size.

## Region/deposit generation

Use deterministic 9x9 chunk regions as the planning layer unless profiling later proves another size is better.

A region plan may choose zero or more deposits. Each deposit describes:

- primary source material;
- optional secondary source materials;
- host compatibility;
- center/extent;
- depth/height range;
- geometry;
- grade/density variation;
- optional indicators.

Generate only the current chunk's intersection with the plan.

## Formation and geometry

Choose geometry from physical/geological properties and explicit geology data, not material names. Valid physical shapes can include vein, layer, lens, disseminated body, pipe/chimney, contact body, intrusive body, hydrothermal body, magmatic body, placer-like body and fluid/gas reservoir.

## Indicators

Where justified, deterministic indicators may include outcrop, float fragments, altered host, material-derived color changes, seepage, bubbles and vents.

## Determinism

Region/deposit random streams must derive from:

```text
world seed + dimension id + region coordinates + stable domain separator per decision stage
```

Do not share mutable RNG state between chunks. Chunk generation order must not change results.

## Debugging

Provide a report/debug command or data report that can explain for a region:

- selected dimension band;
- candidate sources and resolved tiers;
- chosen source(s);
- compatible/rejected hosts with reasons;
- geometry/depth decision;
- indicator decision;
- deterministic seed components.

## Worldgen completion definition

Planner classes and reports are not enough. Geology is implemented only when a registered runtime/worldgen adapter places the planned variants in chunks and fixed-seed tests pass.
