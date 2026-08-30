# START HERE - next developer/chat

The active source of truth for current work is `CURRENT_TASK.md` and `to do/23-material-geology-autorecipe-integration.md`.

## First rules

- Inspect current project code before stating an API/status.
- Never claim something is implemented merely because an earlier chat described it.
- Use current `main` as baseline; the earlier geology changed-files archive is unfinished reference only.
- Implement one milestone at a time and pass its compile/runData/test gate before moving on.
- Deliver actual requested files/ZIP and integrity-test them before saying the task is finished.

## Active priority

1. Common `MaterialPart` migration for stone/wood.
2. Complete Minecraft/Create stone and wood definitions through `.existing(...)`.
3. Data-driven stone dust loot and automatic stone hosts.
4. Composed raw ore-source definitions and missing-source suggestions.
5. Tier-driven geology/worldgen.
6. Automatic processing from stone dust, wood pulp and raw ore-source dust.

Read the integrated plan for all locked details, especially the composition API, dimension policy and out-of-scope boundaries.

## Important existing system status

Assembly Workbench, recursive `ComponentDefinition`, free/fixed material binding, generated Frame/Casing Assembly recipes, JEI Assembly Products/Components, timed tools and Create Deployer/FakePlayer tool progress exist in the current project. See `code/recipes/assembly-recipes.md` before changing Assembly.

Do not refactor unrelated Assembly/Create systems while doing the material/geology integration.

## API convention

Use `API_CONVENTIONS.md`. Typed project concepts use typed references/constants. External registry resource IDs may use their real ResourceLocation strings where appropriate.
