# Current task - canonical handoff

Read this before continuing material/geology work.

## Active goal

Implement the full sequence in `to do/23-material-geology-autorecipe-integration.md`, one milestone at a time.

The active work combines:

- common `MaterialPart` migration;
- complete Minecraft/Create stone definitions;
- complete Minecraft/Create wood definitions;
- automatic stone ore-host discovery;
- composed raw ore-source definitions under `material/defenitions`;
- deterministic missing-source suggestions from `runData`;
- tier-driven geology/worldgen;
- automatic composition processing for stone, wood and raw ore-source materials.

## Source priority

1. Current `main` project code/API.
2. This docs package for locked design.
3. The earlier geology changed-files archive only as an unfinished reference.

Never claim a code change is implemented unless it exists in the delivered files and passes the relevant gate.

## Important boundaries

- Do not use real-world mineral-family naming as chemistry logic when the required elements do not exist in the fictional table.
- Do not generate elemental ores as the normal resource model.
- Do not invent `.contains(...)` syntax; verify it in current code.
- Do not implement ore-to-dust processing yet.
- Do not implement tree growth yet.
- Do not broaden current autorecipe generation beyond stone, wood and raw ore-source materials.

## Exact composition syntax verified in current project

```java
.contains(component(VERNIUM, 1), component(ORLUNE, 3))
```

## Dimension policy

```text
ULV-HV -> Overworld
EV-LuV -> Nether
ZPM+ -> End
```

Highest contained/deposit tier wins.

## Next action

Start at Step 0 of `to do/23-material-geology-autorecipe-integration.md`. If Step 0 is already proven against a newer project ZIP, continue with the first unpassed gate. Do not skip the `MaterialPart` migration and then hardcode stone/wood around the old enum.
