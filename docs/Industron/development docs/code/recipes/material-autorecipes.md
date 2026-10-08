# Automatic material processing – current composite-dust pipeline

This file originally described the pre-implementation stone/wood/raw-source slice. Current code has moved beyond that slice: registered composite materials with `DUST` are analyzed generically by `ProcessPlanner`, while ore preprocessing is handled separately.

## Scope

Generate post-dust routes for registered composite materials that own `DUST`, based on composition, topology and calculated properties. Automatic process intermediates are system-owned slurry, solution or reaction mixture materials. Dynamic runtime Foundry mixtures are future scope.

## Feed boundary

The mined ore-processing chain is implemented separately by `OreProcessingRecipes`.

```text
RAW_ORE -> CRUSHED/WASHED/REFINED/IMPURE/PURIFIED -> ore material DUST
ore/composite DUST -> automatic chemistry processing -> component forms
```

Wood/stone processing remains separately governed by their material/form recipes and must not be confused with ore chemistry.

## Current safety boundary

```text
candidate plans
-> ProcessSemantics validation
-> exact flattened elemental balance
-> guaranteed-output validation
-> directed-cycle rejection
-> recipe emission
```

The current directed-cycle rule is deliberately conservative. Foundry's future reversible but exactly balanced transformations require stoichiometric cycle analysis before that restriction may be relaxed.

## Planner input

Normalize the three supported families into the same planner data:

- source ID;
- source family;
- feed form;
- top-level composition;
- phase;
- material properties;
- optional explicit `ChemicalStructure`;
- resolved tier;
- component form availability.

## Composition is not topology

`.contains(...)` only establishes what is present and in what ratio. It does not always prove whether the material is a physical mixture or a bonded structure.

Use explicit `ChemicalStructure` when available. If the model cannot infer a physically valid route from existing data, generate a diagnostic instead of a fake recipe.

## Decision order

### 1. Validate

Reject recipe emission (with diagnostic) for:

- missing/unknown component;
- zero/negative amount;
- composition cycle;
- missing feed form;
- output component with no legal form;
- impossible recipe IO;
- ambiguous transformation that needs explicit structure.

### 2. Physical mixture route

Use physical separation only if the source is actually a physical mixture.

Candidate logic:

```text
all gas-like
  -> gas separation or fractionation if the registered process semantics fit

multiple fluid-like components
  -> distillation when volatility differences justify it
  -> otherwise phase separation only when phase behavior justifies it

insoluble solid + fluid
  -> filtration when a filterable phase exists

fine suspension / density-driven mixed phases
  -> centrifuging when density and phase data justify it

solid physical mixture
  -> a registered solid-separation process only when a distinguishing property exists
```

A centrifuge never breaks a chemical bond merely because `.contains(...)` has several entries.

### 3. Electrochemical route

For a suitable ionic/electrochemical topology and valid process phase, electrolysis may split/recover components. Electrowinning/electrorefining are used only when their actual input/output semantics match a recovery/refining step.

### 4. Bonded chemical route

If bonds must be changed, use a chemical transformation that can represent the change. A direct chemical-reaction route is valid only when reaction/bond validation can account for reactants and products.

Multi-step routes may use existing typed CE processes where justified, including dissolution/leaching, reaction, solvent extraction, precipitation/crystallization, electrochemical recovery, thermal decomposition and phase-management steps.

Every step must have a diagnostic explanation.

## Top-level outputs

By default, decompose to the declared top-level components.

Do not recursively explode a source into every leaf element in one recipe. A top-level component that is itself processable can have a later route. This preserves definition ownership and avoids slot explosion.

Recursive flattening is still used for source coverage/tier reports where needed.

## Amount conversion

`component(X, n)` values are ratios, not direct item-stack counts.

Recipe emission must:

1. reduce ratios by GCD;
2. determine a legal batch size;
3. map solid units to item forms and fluid/gas units to fluid amounts;
4. preserve total material ratio exactly;
5. avoid stack/fluid overflow;
6. never round away a component.

If exact representation is impossible with available units, report it.

## IO limits

Verified current limits for important processes:

| Process | Item in | Item out | Fluid in | Fluid out |
| --- | ---: | ---: | ---: | ---: |
| Centrifuging | 9 | 9 | 3 | 3 |
| Electrolysis | 9 | 9 | 3 | 3 |
| Chemical reaction | 9 | 9 | 3 | 3 |
| Filtration | 4 | 6 | 3 | 3 |
| Gas separation | 0 | 0 | 3 | 6 |
| Phase separation | 2 | 2 | 3 | 6 |

Read the actual recipe type for every additional process before using it.

Too many outputs must trigger another route, deterministic multi-step fractioning or a clear diagnostic. Never truncate.

## Multi-step route constraints

A multi-step route is acceptable when:

- the direct route is physically invalid;
- a required phase must first be created;
- the output count exceeds one machine's legal IO;
- purification/recovery needs a real intermediate step;
- explicit structure indicates staged bond changes.

It is not acceptable merely to increase gameplay length.

Intermediate states should be derived, deterministic process states where possible rather than arbitrary permanent registry materials.

## Determinism

Stable definitions must produce stable:

- route selection;
- process ordering;
- recipe IDs;
- ratios/batch sizes;
- tier;
- report text/order.

Tie-break with stable IDs, never random iteration order.

## Report format

For every supported material with composition, emit a plan containing:

```text
source
family
feed form
resolved tier
recipe tier
composition
structure/topology decision
selected process step(s)
why each step is valid
input/output forms and amounts
IO usage
warnings/requirements
```

## Acceptance tests

Cover at least:

- stone physical mixture;
- wood with mixed substance types;
- raw ore source with several useful components;
- all-gas top-level outputs;
- fluid mixture with large/small volatility contrast;
- filterable solid/fluid mixture;
- density-driven centrifuge case;
- explicitly bonded material that must not centrifuge;
- valid electrolysis case;
- ambiguous composition requiring structure;
- >slot-count composition requiring split/diagnostic;
- missing output form;
- recursive composition cycle;
- source tier ULV and one-tier-below clamp;
- HV -> MV recipe tier;
- deterministic repeat generation.
