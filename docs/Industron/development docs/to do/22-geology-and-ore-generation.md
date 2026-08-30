# Phase 22 - Geology and ore generation

This phase is now governed by the integrated plan in `23-material-geology-autorecipe-integration.md` and the architecture guide in `../code/geology/README.md`.

## Goal

Generate large, deterministic, learnable deposits from Industron's fictional material data rather than random one-element ore blobs or real-world family-name shortcuts.

## Core rules

- Elements normally do not worldgen as one ore block per element.
- Natural resources are manually named composed ore-source materials.
- One element may occur in many source materials.
- One source material may contain many useful substances.
- Source composition uses `.contains(component(...), ...)`.
- Source names/IDs are manually owned by the user.
- New source-less elements get deterministic composition suggestions in `runData`.
- Highest tier among source/deposit contents chooses dimension.
- ULV-HV -> Overworld; EV-LuV -> Nether; ZPM+ -> End.
- Registered `StoneMaterial` definitions are the automatic host catalog.
- Adding a new stone must not require adding an ore-host enum/switch.
- Ore block identity is source + StoneMaterial host + size.
- Host availability is independent from the dimension in which a deposit is selected.

## Implementation checklist

### A. Material/form migration

- [ ] Move stone/wood to common `MaterialPart`.
- [ ] Remove host-specific ore-part dependence.
- [ ] Complete stone/wood external mappings.

### B. Ore-source definitions

- [ ] Add dedicated source registry under `material/defenitions`.
- [ ] Add source coverage graph.
- [ ] Add recursive tier resolution/cycle detection.

### C. Missing-source report

- [ ] Analyze every registered element.
- [ ] Rank compatible components using fictional properties.
- [ ] Normalize deterministic integer ratios.
- [ ] Emit exact copyable `.contains(...)` Java.
- [ ] Never invent source name/ID.

### D. Deposit planning

- [ ] Deterministic 9x9 chunk region planner.
- [ ] Primary + optional secondary sources.
- [ ] Property-based host compatibility.
- [ ] Geometry/depth/grade selection.
- [ ] Surface indicators where justified.
- [ ] Report every decision.

### E. Worldgen runtime

- [ ] Register actual worldgen adapter.
- [ ] Place only the current chunk's slice.
- [ ] No forced chunk loading/order dependence.
- [ ] Fixed-seed tests in every dimension band.

## Processing boundary

Geology generates the source in host stone. The separate future ore-processing chain turns mined source ore into dust. Automatic composition processing then begins from that dust according to Phase 14.

Do not implement ore-to-dust processing as part of Phase 22.

## Done when

- [ ] a new source definition enters reports/worldgen without geology switch edits;
- [ ] a new stone definition enters host candidates without geology switch edits;
- [ ] mixed-tier deposits use the highest-tier dimension;
- [ ] fixed seeds are stable across chunk generation order;
- [ ] worldgen actually places blocks, not only reports plans;
- [ ] missing-source `runData` workflow works end-to-end.
