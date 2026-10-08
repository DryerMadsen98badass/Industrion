# START HERE - next developer/chat

The active source of truth for current work is `CURRENT_TASK.md` and `to do/07-foundry-and-heater-multiblocks.md`. Phase 05 and Phase 06 are complete. Phase 06 remains the canonical process-semantics contract that Phase 07 must consume. Phase 23 is retained as the historical material/geology execution plan; its old baseline/status statements are not current.

## First rules

- Inspect current project code before stating an API/status.
- Never claim something is implemented merely because an earlier chat described it.
- Use current `main` as baseline; the earlier geology changed-files archive is unfinished reference only.
- Implement one milestone at a time and pass its compile/runData/test gate before moving on.
- Deliver actual requested files/ZIP and integrity-test them before saying the task is finished.

## Active priority

1. Canonical payload/signatures for defined and runtime metal mixtures.
2. Exact alloy matching and generic unclassified mixture carriers.
3. Dupe-safe stoichiometric graph validation for reversible metallurgy.
4. Foundry/Heater multiblocks, thermal state, molds, casting and cooling.
5. UI and performance validation for up to 20 constituents.

Read Phase 07 for all locked boundaries, especially composite-dust chemistry, exact ratios, no closest-alloy UI and no profitable loops.

## Important existing system status

Assembly Workbench, recursive `ComponentDefinition`, free/fixed material binding, generated Frame/Casing Assembly recipes, JEI Assembly Products/Components, timed tools and Create Deployer/FakePlayer tool progress exist in the current project. See `code/recipes/assembly-recipes.md` before changing Assembly.

Do not refactor unrelated Assembly/Create/geology systems while implementing Foundry.

## API convention

Use `API_CONVENTIONS.md`. Typed project concepts use typed references/constants. External registry resource IDs may use their real ResourceLocation strings where appropriate.
