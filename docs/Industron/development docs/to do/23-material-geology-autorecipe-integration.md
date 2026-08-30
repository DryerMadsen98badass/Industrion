# Active integration plan - materials, geology and automatic processing

This is the canonical implementation plan for the current Industron material/geology task.

It is intentionally written as an execution sequence. Implement one milestone at a time and do not claim a milestone is complete until its acceptance gate has passed.

## 0. Source of truth and current status

### Source of truth

Use the current project from `main (2).zip` as the code/API baseline.

The earlier `industron_geology_changed_files(2).zip` is only an unfinished reference. It is not safe to merge as-is and must not be treated as implemented functionality. The archive does not contain the complete work that the previous chat later described.

Existing docs are design context, but when docs and current code disagree, inspect the current code before proposing Java syntax or implementation details.

### Current baseline facts that were verified

- `StoneMaterial` and `WoodMaterial` currently use `StructureMaterialPart`, not the common `MaterialPart` model.
- There are still multiple Java references to `StructureMaterialPart`; the migration is not done.
- `StoneMaterials.java` currently contains only the test stone.
- `WoodMaterials.java` currently contains only the test wood.
- `StoneModel` currently contains the Minecraft/Create visual families already copied into Industron resources, but it is missing a plain `STONE` family.
- `WoodModel` currently contains the vanilla wood families used by Minecraft 1.21.1, including bamboo, crimson and warped.
- `MaterialPart` still contains host-specific ore constants such as deepslate/andesite/granite/etc. ore forms.
- `IndustrialMaterial` still has the parallel `MaterialStoneSource` host mechanism.
- `BlockRegistry`, `ItemRegistry`, form generators, loot generation and assembly aliases still know about host-specific ore parts.
- `StoneMaterial.contains(...)` and `WoodMaterial.contains(...)` already exist, but they must survive the common-part migration and remain trusted declarative composition.
- The current chemistry `ProcessPlanner` is not yet the required stone/wood/raw-ore automatic processing planner.
- The current process catalog and CE recipe infrastructure are broad enough to reuse; do not create a second recipe engine.

## 1. Locked domain rules

These are decisions, not suggestions.

### 1.1 Fictional chemistry only

Industron does not assume real-world elements that are absent from its fictional element table. Do not classify ore sources by real-world mineral-family names derived from absent elements.

Chemical and processing behavior must instead be derived from Industron data that actually exists, for example:

- atomic number and generated atomic behavior;
- tier;
- phase;
- generated material properties;
- charge/ion behavior where available;
- electronegativity-like behavior where available;
- bond type and bond strength;
- `ChemicalStructure` topology;
- declared composition;
- density, volatility, solubility-like, magnetic, thermal and chemical properties where the model exposes them.

The code must never choose a route because a material name contains a real geology/chemistry word.

### 1.2 Element != ore source

An element is not automatically a worldgen ore.

Normal worldgen resource content is an ore-source material made from one or more registered substances. It is the fictional equivalent of a naturally occurring mineral source: a material with its own ID/name, forms, composition and geology behavior.

One element may occur in several different ore-source materials. One ore-source material may contain several useful elements/substances. Rare, high-tier resources may occur together in complex source materials instead of receiving one elemental ore block each.

Direct elemental ore blocks are legacy/special-case content only and must not be the default generation model.

### 1.3 Canonical source-definition location

Raw ore-source definitions belong under the existing package/directory:

```text
C:\Users\Madsf\Documents\Minecraft Modding\1.21.1\NeoForge\Industrion\src\main\java\net\mads\industron\material\defenitions
```

Keep the project's existing package spelling `defenitions` unless the whole project is deliberately migrated later.

Create a dedicated `OreMaterials.java` (or equivalently named dedicated registry file) in that package rather than mixing permanent ore-source content into test-only compound definitions.

### 1.4 Exact composition API

Never invent `.contains(...)` syntax. The current project uses one call containing one or more `component(...)` values, for example the verified pattern:

```java
.contains(component(VERNIUM, 1), component(ORLUNE, 3))
```

Generated build suggestions must emit copyable Java in this exact API shape.

### 1.5 `.contains(...)` is authoritative on stone and wood

`StoneMaterial` and `WoodMaterial` may contain any registered `IndustrialSubstance` combination that the user declares.

Do not reject a declaration because it looks unusual. A stone or wood definition may contain solids, metals, fluids, gases, other structure materials, or any other valid substance references in the same composition.

The framework may diagnose what processing route follows from that composition, but it must not silently rewrite or reject the declared composition.

Stone/wood keep their structure-family identity. Having `.contains(...)` does not turn them into ordinary generated chemical compounds.

### 1.6 Tier resolution

A composed material's progression tier is derived from its contents unless explicitly overridden by a future API:

```text
material tier = highest tier reachable from the relevant top-level composition
```

The resolver must be deterministic and cycle-safe.

For geology/deposits, the highest tier among all ore-source materials/resources participating in the deposit determines the dimension.

### 1.7 Dimension policy

```text
ULV through HV  -> Overworld
EV through LuV  -> Nether
ZPM and above   -> End
```

This rule is based on the highest resolved tier. Lower-tier and higher-tier substances may occur together; the highest tier wins.

Do not infer the dimension from host rock names. Host variants can exist regardless of the dimension in which a particular deposit is selected.

### 1.8 Autorecipe scope for this task

Automatic material processing in this integration applies to:

1. `StoneMaterial`;
2. `WoodMaterial`;
3. declared raw ore-source materials.

Do not expand this milestone into a universal automatic-recipe system for every gem, alloy and arbitrary compound.

The process-selection framework may be reusable later, but the current generation entry points must be scoped to those three families.

### 1.9 Ore-to-dust is explicitly deferred

Do not implement the mined-ore-to-dust processing chain in this task. That ore-processing chain will be designed later.

This task starts automatic composition processing at the canonical powder/feed form:

- stone -> `DUST`;
- raw ore-source material -> `DUST`;
- wood -> `WOOD_PULP` through a common feed-form resolver, because the current wood model already owns pulp forms rather than a duplicate wood dust family.

The future ore-processing chain will terminate at the raw ore-source dust and then hand over to the automatic processing described here.

### 1.10 Recipe tier

For these generated composition-processing recipes:

```text
recipe tier = one electric tier below the resolved source-material tier
```

Clamp at ULV. Do not use list-index arithmetic without a central tier utility because `MachineTier` also contains steam aliases and special values.

## 2. Common `MaterialPart` migration

This must happen before filling the stone and wood definition registries.

### Goal

`MaterialPart` becomes the shared form identity for normal materials, stone and wood. Remove the parallel `StructureMaterialPart` model after all consumers are migrated.

### Required minimum roles

The common enum must be able to represent every real registry form needed by the supported stone/wood families. Minimum required roles include:

```text
STONE
COBBLED_STONE
LOG
STRIPPED_LOG
WOOD
STRIPPED_WOOD
PLANKS
SLAB
STAIRS
WALL
COBBLED_SLAB
COBBLED_STAIRS
COBBLED_WALL
FENCE
FENCE_GATE
BUTTON
PRESSURE_PLATE
DOOR
TRAPDOOR
LEAVES
SAPLING
SIGN
WALL_SIGN
HANGING_SIGN
WALL_HANGING_SIGN
WINDOW
WINDOW_PANE
TINY_DUST
SMALL_DUST
DUST
TINY_WOOD_PULP
SMALL_WOOD_PULP
WOOD_PULP
```

Additional stone decorative roles must be added only for actual block registry forms, not because a PNG exists. Typical real roles that need explicit inventory include polished, brick, tile, smooth, chiseled, pillar and Create cut/decorative block variants.

Bamboo mosaic forms need their own real roles if they are represented as family-specific blocks. Crimson/warped stems/hyphae map to generic log/wood semantics rather than requiring a second parallel enum.

### Critical texture rule

A texture sprite is not automatically a block form.

The current resource folders contain helper/connected/decorative sprites such as connected variants and pattern textures. The implementation must inventory actual Minecraft/Create block registry entries and map those logical block roles. Do not generate fake blocks simply because a texture filename exists.

### Migration work

1. Add all needed common roles to `MaterialPart`.
2. Give `MaterialPart` any registry/display-name helpers currently owned only by `StructureMaterialPart`.
3. Change `StructureMaterial`, `StoneMaterial`, `WoodMaterial`, `GemMaterial`, `MetalMaterial`, structure generator/data providers/registries/colors/items to use `MaterialPart`.
4. Change existing-part maps to `Map<MaterialPart, ResourceLocation>`.
5. Change generated-form sets to `Set<MaterialPart>`.
6. Update all switch statements and helper signatures.
7. Remove or fully deprecate `StructureMaterialPart` only after repository search reports zero runtime/source references.
8. Compile before continuing.

### Acceptance gate

- zero Java references to `StructureMaterialPart` except an intentionally retained compatibility shim, if one is temporarily necessary;
- common `.existing(MaterialPart.X, "namespace:id")` works for stone and wood;
- common generated forms work for stone and wood;
- existing gem/metal structure behavior is not broken;
- compile passes;
- `runData` passes.

## 3. Complete `StoneMaterials.java`

### Family inventory

At minimum, the current project resources/models already identify these families. Add the missing plain stone family and register all supported Minecraft/Create families as actual `StoneMaterial` definitions rather than keeping only `TEST_STONE`.

Minecraft-side families represented by the current design/resources:

```text
STONE
ANDESITE
BASALT
BLACKSTONE
CALCITE
DEEPSLATE
DIORITE
DRIPSTONE
END_STONE
GRANITE
NETHERRACK
RED_SANDSTONE
SANDSTONE
TUFF
```

Create palette families already represented by project resources:

```text
ASURINE
CRIMSITE
LIMESTONE
OCHRUM
SCORCHIA
SCORIA
VERIDIUM
```

Do not create a new "Create wood species" list; Create adds blocks such as windows for existing wood families rather than a separate species model here.

### Per-stone definition requirements

Every stone definition must have:

- stable ID and display name;
- explicit color;
- typed `StoneModel` visual family;
- a base `MaterialPart.STONE` form, mapped with `.existing(...)` when it exists in Minecraft/Create;
- an explicit `.existing(...)` mapping for every other real Minecraft/Create block form in that family;
- generated forms only for roles that do not already exist;
- tiny/small/normal dust;
- a cobbled family.

### Cobbled rule

Every registered `StoneMaterial` must expose `COBBLED_STONE`, `COBBLED_SLAB`, `COBBLED_STAIRS` and `COBBLED_WALL`.

If Minecraft/Create already provides a corresponding block, map it with `.existing(...)`.

If it does not exist, generate it from the grayscale cobbled templates and tint it using the stone material color. Do not hardcode per-stone Java texture exceptions.

### Existing forms rule

If a form already exists in Minecraft or Create, the definition must point at it declaratively. Do not register a duplicate Industron block for an existing form.

During implementation, build an explicit registry-ID inventory for each family. Verify IDs against the loaded dependency/version before writing the Java definitions. Do not derive an ID from a texture filename and assume it is a block.

### Acceptance gate

- all supported stone families are in `StoneMaterials.ALL`;
- every family has an explicit color;
- base stone is represented by `MaterialPart.STONE`;
- every family has the four required cobbled roles;
- real existing blocks are `.existing(...)` mappings;
- no helper sprite becomes a fake block;
- generated assets exist only for truly missing forms;
- compile and `runData` pass.

## 4. Complete `WoodMaterials.java`

### Family inventory

Register the current Minecraft 1.21.1 wood families already represented by `WoodModel`:

```text
ACACIA
BAMBOO
BIRCH
CHERRY
CRIMSON
DARK_OAK
JUNGLE
MANGROVE
OAK
SPRUCE
WARPED
```

### Existing forms

Each definition must use `.existing(MaterialPart.X, "namespace:id")` for every real family-specific Minecraft/Create block it already owns, including where applicable:

- log/stem;
- stripped log/stem;
- wood/hyphae;
- stripped wood/hyphae;
- planks;
- slab;
- stairs;
- fence;
- fence gate;
- button;
- pressure plate;
- door;
- trapdoor;
- leaves;
- sapling/growth-start block where the family actually has one;
- sign and wall sign;
- hanging sign and wall hanging sign;
- Create window and window pane;
- bamboo mosaic family forms where they actually exist.

Do not invent leaves or saplings for crimson/warped. Do not force mangrove into an ID pattern that assumes a normal sapling; map its actual propagation block semantics deliberately. Bamboo's log-like blocks and mosaic blocks must be mapped to the common role model rather than special-cased throughout generators.

### Generated wood forms

Only generate a form if the family does not already provide it and if the form makes sense for that family. `WOOD_PULP`, `SMALL_WOOD_PULP` and `TINY_WOOD_PULP` remain generated material forms.

Tree growth/TreeDefinition is not part of this task.

### Acceptance gate

- all `WoodModel` families have registered `WoodMaterial` definitions;
- all real vanilla/Create family blocks are mapped, not duplicated;
- special families are semantically correct;
- no fake blocks are created from helper textures;
- compile and `runData` pass.

## 5. Stone mining and dust loot

The current stone loot implementation does not fully match the requested target and must be reviewed rather than silently preserved.

### Locked behavior

- Silk Touch must not produce dust.
- Normal mining uses the same dust chance progression as vanilla gravel-to-flint style fortune behavior.
- The dust is the dust belonging to the actual `StoneMaterial` host: stone dust, netherrack dust, diorite dust, etc.
- Do not keep a special hardcoded switch for stone/deepslate/netherrack/diorite family IDs.
- The loot provider must work for any new `StoneMaterial` registered later.

Current intended chance curve:

```text
Fortune 0: 10%
Fortune 1: about 14.2857%
Fortune 2: 25%
Fortune 3+: 100%
```

### One unresolved loot detail

The user explicitly said the stones should no longer simply drop their original stone/netherrack/diorite block in the old way. The exact result on a failed non-Silk dust roll must be made explicit during this milestone instead of accidentally retaining today's self/cobbled fallback.

Do not mark this step complete until the failure branch is deliberately chosen and tested.

## 6. Replace hardcoded ore hosts with `StoneMaterial`

### Goal

A stone becomes an ore host because it is a registered `StoneMaterial` with a valid `MaterialPart.STONE` base form. There must be no second hardcoded host catalog to maintain.

### Remove the parallel model

Migrate away from:

- `MaterialStoneSource` as the authoritative host list;
- `IndustrialMaterial.stoneSources()` as the normal host-selection mechanism;
- host-specific `MaterialPart` ore constants such as deepslate/andesite/granite/etc. ore forms;
- hardcoded ore-part sets in form generators and loot providers.

### Target identity

Represent a generated ore block by data equivalent to:

```text
ore-source material + StoneMaterial host + ore size
```

The host determines base block/model/texture. The ore-source material determines overlay/tint/resource identity. Ore size distinguishes normal/small ore where needed.

`MaterialPart.ORE` and `MaterialPart.SMALL_ORE` may remain as generic ore-overlay/form roles, but the host stone must not be encoded into the enum.

### Automatic host behavior

Adding a new definition such as:

```java
.existing(MaterialPart.STONE, "minecraft:stone")
```

must automatically make that `StoneMaterial` available to the ore-host system without editing a geology host enum, ore form enum, block registry switch or loot list.

### Existing ore blocks

Existing vanilla/Create ore blocks are mappings, not proof that the element itself should remain the worldgen source. Where legacy element definitions currently point directly at ore blocks, migrate worldgen ownership to an ore-source definition when the new source model is introduced. Preserve external block compatibility through explicit mappings/adapters rather than preserving the wrong domain model.

### Acceptance gate

Add one temporary custom stone in a test and prove that:

- it becomes a host without editing geology code;
- normal/small ore variants can be produced for eligible ore-source materials;
- blockstate/model/loot/tag generation works;
- deleting the test stone removes the host cleanly;
- no host-specific ore `MaterialPart` was added.

## 7. Raw ore-source definition model

### Definition ownership

Raw ore-source materials are manually named and declared by the user. The framework must never silently create a Java source definition or invent the registry name.

A definition owns at least:

- stable ID/display name/color;
- composition through `.contains(component(...), ...)`;
- optional explicit `ChemicalStructure` when composition/properties do not uniquely describe how components are arranged/bonded;
- source classification declaring that it is naturally acquired/world-deposit content;
- generated material forms needed by the source-processing boundary, especially dust;
- optional existing external forms/blocks where compatibility requires them.

### Multiple sources are normal

The source analyzer must support:

- multiple ore-source materials containing the same element;
- one ore-source material containing many useful substances;
- low- and high-tier substances occurring together;
- one element having no source yet, one source, or many sources.

Do not stop supporting an element after the first source is found. "Has at least one source" is only the condition for suppressing the missing-source report.

## 8. Automatic missing-source suggestions from `runData`

### Trigger

For every registered element, especially newly added atomic numbers, determine whether at least one declared natural ore-source material reaches that element through valid composition.

If an element has no valid source, `runData` emits a deterministic suggestion report.

### Output contract

The report must include:

- target element ID/display name/atomic number/tier;
- why it has no registered source;
- candidate compatible registered components ranked deterministically;
- proposed integer component ratios;
- the properties/compatibility signals used;
- the exact copyable Java `.contains(...)` call using the project's real API;
- resolved highest tier and predicted dimension if the proposed composition were used;
- warnings when the chemistry model cannot justify a unique structure.

The report must NOT invent:

- ore-source material name;
- registry ID;
- Java constant name;
- arbitrary real-world family classification.

### Deterministic candidate selection

Candidate scoring must use only stable project data. Sort ties by stable material/substance ID. Normalize ratios by greatest common divisor. Cap complexity with documented deterministic limits.

A repeated `runData` on unchanged inputs must produce byte-stable suggestions.

### Workflow for adding atoms 101-110

1. Add the element definitions with atomic number/tier and normal required identity fields.
2. Run `runData`.
3. Read the missing-source report.
4. Choose a source name/ID manually.
5. Add a source definition under `material/defenitions` and paste/adapt the proposed `.contains(component(...), ...)` composition.
6. Add explicit structure only if diagnostics say the route cannot be derived safely.
7. Run `runData` again.
8. The source warning should disappear and geology/autorecipe reports should now include the new source automatically.

## 9. Shared tier and dimension resolver

Create one central resolver used by:

- ore-source analysis;
- deposits;
- worldgen dimension selection;
- automatic recipe tier selection;
- reports/validation.

It must handle nested composition, detect cycles, memoize resolved tiers and emit a diagnostic instead of recursing forever.

Do not duplicate `if tier == ...` ladders in geology and recipe code.

## 10. Geology planning model

### Separation of concepts

Keep these identities separate:

```text
Element/Substance
    -> what can be contained

Ore-source material
    -> naturally occurring composed source material

Ore block variant
    -> ore-source material rendered in a StoneMaterial host

Deposit
    -> spatial body containing one or more ore-source materials

Geology region
    -> deterministic large-scale worldgen context
```

### 9x9 region strategy

Preserve the 9x9 chunk region idea as the large-scale deterministic planning unit unless profiling proves a different size is necessary. The region seed must derive from world seed + dimension + region coordinates, so chunk generation order does not change deposits.

Plan at region/deposit level; place only the slice intersecting the currently generated chunk. Never scan/generate all 81 chunks synchronously when one chunk is requested.

### Deposit contents

A deposit may contain:

- one primary ore-source material;
- zero or more secondary ore-source materials;
- gangue/host alteration represented by registered stone/substance data where useful;
- deterministic grade/size/shape variation.

The highest tier of all included source materials determines the allowed dimension.

### Deposit geometry

Physical/geological body shapes may include, when supported by properties and formation rules:

- vein;
- disseminated body;
- band/layer;
- lens;
- pipe/chimney;
- intrusive/porphyry-like body;
- coarse crystal/pegmatitic-like body;
- contact-reaction body;
- hydrothermal body;
- magmatic body;
- placer-like body;
- ocean-floor massive body;
- fluid or gas reservoir.

These are geometry/formation descriptions, not real-world chemical family labels.

### Host compatibility

Host choice comes from registered `StoneMaterial` data and properties, not a fixed list of vanilla tags. A new stone therefore participates automatically once it has valid host data.

Compatibility may depend on properties such as hardness, thermal history proxies, porosity/permeability, fracture tendency, phase stability and declared composition. If a required property does not exist yet, use a typed explicit geology property/override rather than material-name checks.

### Surface indicators

Deposits may produce deterministic indicators where appropriate:

- float fragments;
- exposed outcrop;
- altered host rock;
- ground color/staining effects derived from material color/properties;
- bubbles;
- fluid/gas seep;
- vent;
- vegetation/environment response if a later system supports it.

Indicators are hints, not a replacement for the deposit.

## 11. Actual worldgen runtime

Planning/reporting alone is not worldgen. This milestone is not complete until an adapter is registered in the NeoForge/Minecraft worldgen path and places blocks in generated chunks.

### Runtime requirements

- deterministic across chunk generation order;
- dimension-aware by the locked tier policy;
- no global mutable random state;
- no forced chunk loads;
- no cross-thread unsafe registry mutation during worldgen;
- chunk-local placement from precomputed region/deposit descriptors;
- host replacement based on the registered `StoneMaterial` block mapping;
- normal/small ore overlays use the same dynamic host model;
- worldgen reports can reproduce the decision for a coordinate/region.

### Acceptance gate

Use fixed seeds and verify:

- same seed + same inputs => same deposits;
- different chunk generation order => same deposits;
- mixed-tier deposit appears only in the dimension selected by its highest tier;
- a newly registered stone can be selected as host without geology code edits;
- a newly declared ore-source material reaches worldgen without adding a switch case;
- region borders do not create discontinuity/duplication bugs.

## 12. Automatic processing architecture

### Entry point

Create one adapter that turns a supported source family into a normalized `MaterialProcessSource` (name is illustrative; use the project's naming conventions during implementation).

It must expose:

- source material/substance;
- canonical process feed form (`DUST` or `WOOD_PULP`);
- top-level components and integer amounts;
- source phase/properties;
- optional explicit structure/topology;
- resolved source tier;
- actual item/fluid forms available for components;
- source family: stone, wood or raw ore-source.

The core route planner must not contain `if stone -> centrifuge` or `if wood -> chemical reactor` rules. Family chooses the feed form; chemistry/physics chooses the process.

### Top-level composition first

Generated decomposition normally outputs the declared top-level components, not a recursive flattening of the entire graph in one machine recipe.

If a top-level output is itself composed, it can receive its own generated processing route where that family/scope permits. This keeps recipes understandable and avoids impossible slot counts.

Recursive analysis is still required for tier calculation, source detection and diagnostics, with cycle detection.

## 13. Process-selection logic

The planner must choose a process because that process can physically perform the transformation represented by the available data.

### 13.1 Physical mixture

If `ChemicalStructure` explicitly marks a physical mixture, do not use a bond-breaking process.

Possible routes, depending on phase and properties:

- all gas-like outputs -> gas separation/fractionation when available;
- multiple fluid-like outputs -> distillation when volatility separation is justified, otherwise phase separation when immiscibility/phase behavior is justified;
- insoluble solid + fluid -> filtration when particle/phase behavior supports it;
- fine suspension or density-driven mixed phases -> centrifuging when density/phase behavior supports it;
- solid mixtures -> use an existing solid-separation process only if the project has a relevant typed process and the required distinguishing property exists.

Never use centrifuging as a generic chemical bond breaker.

### 13.2 Ionic/electrochemical structure

If the declared/inferred structure is ionic/electrochemically separable and a valid process phase exists, electrolysis may be a direct route.

Electrowinning/electrorefining are downstream purification/recovery routes and should be selected only when their input/output semantics match the source state. Do not select them merely because an output is metallic.

### 13.3 Bonded molecular/network material

If decomposition requires breaking/reforming chemical bonds, use a chemical transformation route. The direct chemical-reaction recipe is the fallback only when the reaction model can actually construct/validate the transformation.

Where properties and structure justify it, a multi-step route may instead use existing typed processes such as:

- dissolution or leaching;
- chemical reaction;
- precipitation or crystallization;
- solvent extraction;
- electrolysis/electrowinning;
- pyrolysis or thermal decomposition;
- roasting/calcination-type thermal transformations if the project's process semantics justify them;
- drying/evaporation/condensation as phase-management steps.

Do not add a step just to make a recipe longer. Every step needs a transformation/reason visible in diagnostics.

### 13.4 Ambiguous structure

`.contains(...)` gives composition, not necessarily bond topology.

If the available properties cannot distinguish physical mixture from bonded compound, or cannot justify which bonds are broken, do not invent a route. `runData` must emit a diagnostic asking for an explicit `ChemicalStructure` or future typed processing override.

This is preferable to generating a chemically nonsensical recipe.

### 13.5 Deterministic ranking

If several routes are valid:

1. filter out routes that violate process semantics, phase requirements, form availability or recipe IO limits;
2. prefer the simplest physically valid route;
3. prefer fewer transformations when output quality is equivalent;
4. use stable property-derived cost/difficulty scores;
5. use stable process ID as final tie-breaker.

No random route selection.

## 14. Recipe emission

### Use the existing CE recipe system

The automatic planner emits into the existing `RecipeTypeDefinition`/CE recipe infrastructure. It must not create a second recipe serializer/runtime.

Generated recipes live under the material-generated recipe path, separate from hand-written tier recipe classes.

### Current verified IO limits that must be respected

```text
Centrifuging:       9 item in, 9 item out, 3 fluid in, 3 fluid out
Electrolysis:       9 item in, 9 item out, 3 fluid in, 3 fluid out
Chemical reaction:  9 item in, 9 item out, 3 fluid in, 3 fluid out
Filtration:         4 item in, 6 item out, 3 fluid in, 3 fluid out
Gas separation:     0 item in, 0 item out, 3 fluid in, 6 fluid out
Phase separation:   2 item in, 2 item out, 3 fluid in, 6 fluid out
```

Read the actual `RecipeTypeDefinition` during implementation for every additional process used; do not copy assumed slot limits.

### Too many outputs

Never silently truncate components to fit a machine.

If a top-level composition exceeds a valid recipe's IO capacity:

- choose another physically valid process with sufficient capacity; or
- split the route into deterministic intermediate fractions/material states; or
- report that the route cannot currently be emitted.

Any intermediate identity must be deterministic and domain-valid; do not pollute the permanent material registry with arbitrary hidden compounds merely to bypass slot limits.

### Amount normalization

The integer values in `component(X, n)` are composition ratios. They are not automatically Minecraft item stack counts.

Add one unit-conversion layer that preserves stoichiometric ratios while mapping to:

- item counts for dust/pulp/solid forms;
- fluid amounts for liquid/gas forms;
- recipe batch size within stack/fluid limits.

Normalize ratios by GCD, then choose the smallest valid batch that produces integral output amounts. Detect overflow and Minecraft stack-limit problems. Never silently round away material.

### Component form resolution

For each output component, select a form compatible with its phase and registered content:

- solid -> normally dust for decomposition output when dust exists;
- liquid/gas -> matching fluid form;
- structure material -> its canonical process powder where supported;
- if no legal form exists -> diagnostic, no broken recipe.

Do not make the recipe generator register missing forms as a side effect.

## 15. Reports and diagnostics

`runData` must produce human-readable and machine-stable reports for this integration.

Minimum reports:

1. **source coverage** - every element and all ore-source materials that contain it;
2. **missing source suggestions** - copyable `.contains(...)` proposals;
3. **stone host inventory** - every registered stone, base block, generated/existing forms and host eligibility;
4. **wood inventory** - every family and existing/generated roles;
5. **ore-source analysis** - composition, resolved tier, dimension, available host count;
6. **deposit plan summary** - deterministic formation/geometry/host choices;
7. **autorecipe plan** - feed form, route steps, process reasons, tier, IO use and output forms;
8. **validation errors** - cycles, missing forms, invalid ratios, duplicate IDs, impossible routes, unmapped external blocks.

A report that merely says "unsupported" is insufficient. Include enough data to fix the definition.

## 16. Implementation order - one milestone at a time

Do not skip ahead. Each numbered step gets its own compile/runData gate.

### Step 0 - Baseline verification

- unpack clean main baseline;
- run compile/tests available in the project;
- run `runData` once;
- capture current generated-output diff;
- inventory `StructureMaterialPart`, host-specific ore parts, `MaterialStoneSource`, stone/wood definitions and process IO.

Gate: baseline failures are known and documented before editing.

### Step 1 - Common `MaterialPart`

- add needed stone/wood roles;
- migrate structure materials/generators/registries/providers;
- remove the parallel part enum after zero-reference check.

Gate: compile + `runData` + zero stale references.

### Step 2 - Stone/Wood API normalization

- keep immutable `.contains(...)` behavior;
- switch `.existing(...)` to common `MaterialPart`;
- keep arbitrary `IndustrialSubstance` components valid;
- add validation for duplicate role mappings only, not chemistry taste.

Gate: unit/self-tests with mixed-type components and external mappings.

### Step 3 - Complete stone definitions/forms

- add plain `STONE` model/resource mapping;
- register all supported Minecraft/Create stone families;
- explicit colors;
- explicit `.existing(...)` for all actual existing forms;
- generate only missing forms;
- guarantee cobbled block/slab/stairs/wall for every stone.

Gate: registry/assets/lang/loot and generated-file validation.

### Step 4 - Complete wood definitions/forms

- register all wood families;
- map all vanilla/Create forms;
- handle bamboo/crimson/warped/mangrove explicitly;
- keep tree growth out of scope.

Gate: registry/assets/lang/loot validation.

### Step 5 - Stone dust loot

- make loot data-driven by `StoneMaterial`;
- Fortune curve + no dust with Silk Touch;
- deliberately settle/test the failed dust-roll branch.

Gate: generated loot tables and gameplay test.

### Step 6 - Dynamic ore-host registry

- remove `MaterialStoneSource` as source of truth;
- remove host-specific ore `MaterialPart`s from generation paths;
- pair ore-source + stone host + size dynamically;
- refactor block/item/model/loot/tag providers.

Gate: add/remove a test stone with no geology edits.

### Step 7 - Ore-source material registry

- add dedicated definitions file under `material/defenitions`;
- move worldgen source ownership away from elemental ores;
- support multiple sources per element and multiple elements per source.

Gate: registry + composition + forms + source-coverage report.

### Step 8 - Tier/dimension resolver

- central recursive max-tier resolver;
- dimension policy;
- cycle detection/memoization.

Gate: tests at HV/EV/LuV/ZPM boundaries and mixed-tier compositions.

### Step 9 - Missing-source analyzer

- deterministic candidate scoring/ratios;
- exact copyable `.contains(...)` output;
- no automatic names/IDs/source-code edits.

Gate: add temporary atom with no source, verify report, add source, verify warning disappears.

### Step 10 - Geology planner

- region/deposit descriptors;
- property-based host/geometry/depth rules;
- primary/secondary source materials;
- deterministic indicators and debug report.

Gate: pure deterministic tests without world placement.

### Step 11 - Worldgen adapter

- register actual runtime/worldgen integration;
- chunk-local placement from region plans;
- no forced loads/order dependence.

Gate: fixed-seed integration tests in all three dimension bands.

### Step 12 - Autorecipe source adapter

- stone dust source;
- wood pulp source;
- raw ore-source dust source;
- no gem/all-material scope creep.

Gate: normalized planner inputs are identical in semantics across the three families.

### Step 13 - Process planner

- physical vs bonded vs ionic/electrochemical route selection;
- deterministic ranking;
- ambiguity diagnostics;
- multi-step path support;
- no process-name/material-name special cases.

Gate: table-driven tests for valid/invalid/ambiguous topologies.

### Step 14 - Recipe emitter

- one-tier-below resolver;
- form resolver;
- amount normalization;
- CE recipe emission;
- IO-limit splitting/reporting.

Gate: generated JSON parses and JEI/runtime can load representative recipes.

### Step 15 - Full validation/reporting

- source coverage;
- host inventory;
- geology report;
- autorecipe report;
- deterministic output hashes;
- duplicate/cycle/missing-form checks.

Gate: two consecutive unchanged `runData` runs produce no diff.

### Step 16 - Regression and delivery

Run at minimum:

- Java compile;
- automated tests/self-tests;
- `runData`;
- generated JSON parse validation;
- duplicate registry/resource scan;
- client startup;
- representative world creation/chunk generation;
- representative JEI recipe display;
- Silk/Fortune stone loot checks;
- fixed-seed geology checks;
- source-addition workflow check;
- new-stone automatic host check.

Only then package changed/new project files. Never claim implementation is complete without producing and integrity-testing the requested archive.

## 17. Known current-code mismatches to remove

The following are migration targets, not desired architecture:

- test-only `StoneMaterials.ALL`;
- test-only `WoodMaterials.ALL`;
- `StructureMaterialPart` parallel enum;
- host-specific ore constants in `MaterialPart`;
- `MaterialStoneSource` host list;
- element definitions directly owning normal worldgen ore blocks as the default model;
- ore loot provider hardcoded `ORE_PARTS` set;
- material form generators hardcoding every host-specific ore part;
- automatic process planner that treats composition without the new family/feed boundary;
- any geology/reporting code that uses real-world family-name shortcuts;
- any worldgen plan that has no registered runtime placer.

## 18. Explicitly out of scope for this integration

Do not implement these while completing the sequence above unless the user explicitly changes scope:

- the ore block -> crushed/washed/purified -> dust ore-processing chain;
- TreeDefinition/tree growth;
- a generated catalog of random ore-source material names;
- automatic Java source modification;
- a universal recipe generator for every material family;
- broad unrelated machine refactors;
- changing the fictional periodic table merely to imitate Earth chemistry.

## 19. Answered questions / decisions carried forward

- Stone and wood use the same `MaterialPart` model as other forms: **yes**.
- Existing Minecraft/Create forms use `.existing(...)`: **yes**.
- A new registered stone becomes an ore host automatically: **yes**.
- Every stone gets a cobbled block/slab/stairs/wall set: **yes**.
- Missing cobbled forms are generated from grayscale template + stone color: **yes**.
- Stone gets tiny/small/normal dust: **yes**.
- Wood uses pulp forms as its powder-processing feed in this phase: **yes**.
- Stone/wood may declare arbitrary `.contains(...)`: **yes; declaration is authoritative**.
- Stone/wood become ordinary chemical compounds because they have composition: **no**.
- Elements normally worldgen as their own elemental ores: **no**.
- One element can have several ore-source materials: **yes**.
- One ore source can contain several useful elements/substances: **yes**.
- A mixed-tier source/deposit can exist: **yes; highest tier selects dimension**.
- ULV-HV is Overworld, EV-LuV Nether, ZPM+ End: **locked**.
- New atom numbers can be added before sources exist: **yes; `runData` reports missing-source compositions**.
- `runData` invents source names/IDs: **no**.
- `runData` may emit exact `.contains(component(...), ...)` code: **yes**.
- Automatic processing begins from dust/pulp, not mined ore: **yes**.
- Generated processing recipe tier is one tier below source tier: **yes, clamped at ULV**.
- Centrifuge/electrolysis/chemical reaction selection is based on actual structure/properties: **yes**.
- Centrifuge may break arbitrary bonds because a material has several components: **no**.
- Multi-step routes are allowed: **yes, when each step is justified**.
- Recipe output may be silently dropped to fit slots: **no**.
- The unfinished geology ZIP is already implemented code: **no**.

## 20. Remaining open points

These are the only intentionally unresolved details in this integration plan. They should be resolved at the milestone where the actual code/data is visible, not guessed in advance.

1. **Failed non-Silk stone dust roll:** decide whether failure gives no material drop, a separate byproduct, or another explicitly designed result. Do not retain current self/cobbled fallback accidentally.
2. **Exact process scoring constants:** property thresholds for choosing among two equally valid physical/chemical routes must be calibrated from the current generated property ranges and tests.
3. **Exact recipe duration/energy formulas:** the process route and tier rule are fixed, but duration/power values should use the project's central recipe scaling rather than new hardcoded constants.
4. **Ambiguous composition override API:** prefer existing `ChemicalStructure`; only add a new typed override if real definitions prove structure alone is insufficient.
5. **Exact external block ID table:** verify every Minecraft/Create ID against the actual loaded 1.21.1/Create version during the definition milestone. Do not infer IDs from texture filenames.

Everything else above is treated as locked unless the user explicitly changes it.
