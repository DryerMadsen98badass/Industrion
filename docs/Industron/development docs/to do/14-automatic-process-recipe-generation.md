# Phase 14 - Automatic process recipe generation

For the current implementation slice, this phase is scoped to `StoneMaterial`, `WoodMaterial` and raw ore-source materials. The detailed canonical design is `../code/recipes/material-autorecipes.md`; execution order is in `23-material-geology-autorecipe-integration.md`.

## Goal

Turn an already-declared composition into a physically justified CE process route and generated recipe without hardcoding a machine per material name.

## Feed forms

```text
StoneMaterial -> DUST
WoodMaterial -> WOOD_PULP
Raw ore-source -> DUST
```

Ore block -> dust processing is a later system and is not part of this phase.

## Composition API

Use the real project syntax:

```java
.contains(component(VERNIUM, 1), component(ORLUNE, 3))
```

Do not invent a second composition format.

## Rules

- `.contains(...)` is composition, not proof of topology.
- Stone/wood composition is authoritative even for unusual cross-type components.
- Prefer explicit `ChemicalStructure` when inference is ambiguous.
- Physical separators only perform physical separation.
- Bonded material requires a process that can actually alter bonds.
- Process selection uses phase/properties/structure and registered process semantics, never material-name families.
- Multiple valid routes are ranked deterministically.
- Complex/large-output sources may use justified multi-step routes.
- Never silently drop an output to fit recipe slots.
- Generated recipe tier is one tier below source tier, clamped at ULV.
- Generated recipes reuse the existing CE recipe infrastructure and do not live in hand-written tier classes.

## Current implementation tasks

- [ ] Add normalized source adapter for stone/wood/raw ore-source.
- [ ] Add central previous-electric-tier utility.
- [ ] Add form resolver for DUST/WOOD_PULP/component outputs.
- [ ] Replace simplistic separation choice with topology/property-aware selection.
- [ ] Add ambiguity diagnostics.
- [ ] Add multi-step route representation.
- [ ] Add ratio-to-item/fluid amount normalization.
- [ ] Read/enforce each selected RecipeType's actual max IO.
- [ ] Emit deterministic generated recipes.
- [ ] Emit route report explaining every selected step.
- [ ] Test all three source families and invalid/ambiguous cases.

## Not in this phase

- universal recipe generation for every gem/alloy/compound;
- ore mining/crushing/washing chain;
- source name generation;
- automatic Java source edits.

## Done when

- [ ] representative stone, wood and ore-source compositions generate stable valid routes;
- [ ] invalid/ambiguous definitions produce diagnostics rather than nonsense recipes;
- [ ] all emitted recipes fit actual IO;
- [ ] two unchanged `runData` runs are byte-stable;
- [ ] generated recipes load in runtime/JEI;
- [ ] one-tier-below behavior is tested at boundaries.
