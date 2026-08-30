# Stone, wood and ore-source material model

This document defines the target material/form architecture for the current geology integration. The execution order is in `../../to do/23-material-geology-autorecipe-integration.md`.

## One form enum

Use `MaterialPart` as the common form/role identity. `StructureMaterialPart` is migration debt and should disappear after consumers are ported.

The reason is practical: `.existing(...)`, generated forms, registries, data generation and recipes must all speak the same typed form language. A second enum guarantees duplicated switches and makes automatic ore hosting harder.

## `.existing(...)`

Existing external forms are declarative mappings. The target stone/wood API is the same typed shape used elsewhere:

```java
.existing(MaterialPart.STONE, "minecraft:stone")
```

Only use registry IDs that have been verified in the actual project/dependency version.

If Minecraft/Create already owns a form, map it. Do not create a duplicate Industron block.

## `.contains(...)`

Composition syntax is the verified project syntax:

```java
.contains(component(VERNIUM, 1), component(ORLUNE, 3))
```

Stone and wood accept arbitrary registered substance components. The composition is trusted user data. Validation checks nulls, invalid amounts, unknown references and cycles; it does not reject unusual substance-type combinations.

## Stone family contract

Every registered stone has:

- ID/display name;
- explicit RGB color;
- `StoneModel`;
- base `STONE` role;
- `COBBLED_STONE`;
- `COBBLED_SLAB`;
- `COBBLED_STAIRS`;
- `COBBLED_WALL`;
- tiny/small/normal dust;
- explicit mappings for real Minecraft/Create forms;
- generated blocks only for missing supported forms.

The current family inventory includes vanilla-like families plus Create palette families listed in the active plan. Add plain `STONE`, which is missing from the current `StoneModel`.

### Stone role inventory

Do not define roles by scanning PNG names. Build the list from actual block forms. In addition to the base/cobbled roles, the common part model may need typed roles for real forms such as polished, smooth, bricks, tiles, chiseled, pillar and Create cut variants.

Add a part only if it represents a real logical form the generators need to identify.

Connected-texture sprites, side/top sprites and decorative pattern helper textures are not separate material parts by themselves.

## Wood family contract

Every supported `WoodModel` receives a `WoodMaterial` definition and maps all actual family forms with `.existing(...)`.

Common roles include log, stripped log, wood, stripped wood, planks, slab, stairs, fence, gate, button, pressure plate, door, trapdoor, leaves, propagation block, signs, hanging signs and Create window/window pane where applicable.

Special semantics:

- Crimson/warped stems and hyphae use generic log/wood roles; do not invent leaves/saplings.
- Mangrove uses its actual propagation block rather than a guessed `<wood>_sapling` ID.
- Bamboo maps bamboo-block forms as log-like roles and maps mosaic forms explicitly.
- Create windows/panes belong to the existing wood family; they do not create a new species.

Wood keeps tiny/small/normal wood pulp forms. Tree growth is separate work.

## Ore-source material

An ore-source material is a composed natural resource definition, not an element and not a deposit.

Definitions live under:

```text
src/main/java/net/mads/industron/material/defenitions/
```

Use a dedicated registry file for permanent ore-source definitions.

A source may contain multiple elements/substances, and an element may occur in multiple sources. Names/IDs are manually owned by the user.

## Ore block identity

Host-specific ore enum constants are the wrong abstraction.

Target identity:

```text
OreBlockVariant
  source: ore-source material
  host: StoneMaterial
  size: normal | small
```

Host base texture/model comes from the `StoneMaterial`. Overlay/tint comes from the ore-source material. This makes every newly registered stone an automatic host without adding a new enum constant.

## Form resolution

Autorecipe processing needs a family-to-feed adapter:

```text
StoneMaterial       -> DUST
WoodMaterial        -> WOOD_PULP
Raw ore-source      -> DUST
```

The adapter only resolves the physical feed form. It does not choose the processing machine. Process selection uses composition/structure/properties.

## Invariants

- Existing and generated IDs may not collide.
- A real external block is never registered twice.
- A helper texture never creates a block by itself.
- Component amounts must be positive integers.
- Recursive composition must be cycle-safe.
- Stable input yields stable forms, reports and IDs.
- Stone/wood identity remains stone/wood even when composition is present.
