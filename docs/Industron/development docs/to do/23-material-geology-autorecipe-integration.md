# Phase 23 – Material/geology/autorecipe integration status

**Status: den opprinnelige integrasjonsplanen er gjennomført i dagens prosjekt og er ikke lenger aktiv arbeidskø.** Denne filen erstatter den utdaterte 1000-linjers baseline-planen som beskrev test-only stone/wood, `StructureMaterialPart` og utsatt ore-processing.

## Verifisert implementert

- [x] Common `MaterialPart`; ingen Java `StructureMaterialPart` references.
- [x] Populerte Minecraft/Create `StoneMaterials` og vanilla wood families.
- [x] `.existing(...)` og Create host textures/blocks gjennom typed stone definitions.
- [x] Registrerte stone materials kan brukes som ore hosts.
- [x] Sammensatte ore materials med `.contains(component(...), ...)`.
- [x] Tier-/property-drevet geology planning og runtime `GeologyDepositFeature`.
- [x] Mechanical `OreProcessingRecipes` fra raw/crushed/washed/refined/impure/purified til ore-materialets dust.
- [x] Separat post-dust chemistry gjennom `ProcessPlanner` og automatic recipe emission.
- [x] Host-stone dust forblir separat fra ore-material chemistry.

## Faste grenser som fortsatt gjelder

- Fiktive material-/atomegenskaper styrer geology og chemistry; ingen real-material name rules.
- `.contains(...)` er autoritativ composition.
- Highest relevant tier styrer dimension policy etter prosjektets regler.
- Geology, ore preprocessing og composite-dust chemistry er separate lag.
- Physical separators bryter ikke chemical bonds.
- Generated outputs skal ikke truncates for å passe slots.
- Determinism og mass balance må valideres før emission/runtime.

## Nåværende videre arbeid

Foundry/alloy-mixture-arbeidet er spesifisert i:

- `07-foundry-and-heater-multiblocks.md`;
- `09-alloys.md`;
- `14-automatic-process-recipe-generation.md`;
- `21-validation-and-balance.md`.

Bruk `../CURRENT_TASK.md` som aktiv handoff.
