# Phase 17 – Assembly runtime, tools og UX

**Status: KJERNE IMPLEMENTERT.** Checklista under er beholdt fordi den inneholder videre hardening/UX-idéer; unchecked gamle linjer er ikke i seg selv bevis på at dagens runtime mangler funksjonen.

## Gjeldende runtime

`AssemblyPlan` pre-ekspanderer recursive `ComponentDefinition`-trees. Free material-leaves får separate runtime binding IDs; fixed `Metal.X` lagres direkte i planen. Recipe-level requirements propageres til descendant material-leaves, og parent-relative `inputAny`-requirements resolves mot fixed parent før matching.

`AssemblyRuntime` er server-authoritative og støtter world-block base, Assembly Workbench item-base, material/exact-item/tool/wait steps, materialbindings, refund/result state og next-step overlay. Ekte spiller må holde timed tool kontinuerlig. Create Deployer/FakePlayer kan ikke holde client use-state, så hver faktisk interaksjon gir 2 ticks = 0,1 s tool-work. Et 10-ticks tool-step krever derfor 5 Deployer-aktiveringer. Durability brukes én gang ved completion.

JEI har `Assembly Products` for root recipe og `Assembly Components` for ett direkte component-level om gangen; hele tree-et flates ikke ut i product-visningen.

**Kjent hardening:** materialbindings/waits/refund persisteres for Workbench, men pågående fake-player tool-progress (`activeTool`/`toolProgressTicks`) serialiseres ikke gjennom full server restart i dagens runtime.

---

## Beholdt Phase 17-backlog og tidligere designmål

Recipe language beskriver hva som skal skje. Runtime skal utføre planen effektivt, sikkert og forståelig i verden.

## 1. Precompiled AssemblyPlan

- [ ] Pre-expand nested `ComponentDefinition` til immutable `AssemblyPlan` ved load.
- [ ] Ikke traverser hele component tree eller gjør global recipe search på hvert høyreklikk.
- [ ] Index assemblies etter start block/item.
- [ ] Runtime state skal i hovedsak være plan ID, current step, selected actual inputs/material bindings, dependent requirement context og wait/tool-work state.

## 2. Tool system

Recipe skal bare skrive:

```java
.tool(SCREWDRIVER)
```

Actual tool item avgjør speed og durability.

- [ ] Definer typed `ToolType`: `SCREWDRIVER`, `WRENCH`, `HAMMER`, `SAW`, osv.
- [ ] Definer tool capability/stats for faktiske tool items: supported tool type, `speed`, max/remaining durability og eventuelt tool tier.
- [ ] Recipe/component definition skal **ikke** skrive tool speed eller durability.
- [ ] En bedre screwdriver kan derfor gjøre samme `SCREWDRIVER`-step raskere og ha høyere durability uten egen recipe.
- [ ] Tool speed påvirker bare tool-work, ikke `waitTicks(...)`.
- [ ] Tools arver ikke chemical/temperature requirements fra sluttproduktet.
- [ ] Tool damage skjer bare for faktisk fullført/successful work; invalid interaction skal ikke skade tool.

### Foreslått timingmodell

Hver `ToolType` har standard `baseWork`. Actual tool har `speed`.

Konseptuelt:

```text
tool step ticks = ceil(baseWork / actualToolSpeed)
```

Eksakt skala/balanse bestemmes senere. Recipe trenger fortsatt bare `.tool(SCREWDRIVER)`.

- [ ] Sett minimum 1 tick for tool action.
- [ ] Bestem om durability koster 1 per fullført tool-step eller skaleres med baseWork; ikke la speed direkte doble durability cost uten eksplisitt design.

## 3. Other step duration

- [ ] Exact item/tag/material/component insertion er normalt ett interaction-step og trenger ikke egen arbitrary duration.
- [ ] Nested component assembly bruker sine egne interne steps.
- [ ] `waitTicks(n)` er eksplisitt fixed time.
- [ ] Wait lagrer `readyAt = gameTime + n`; ingen heavy per-assembly ticking.
- [ ] Machine/process recipe duration hører til Phase 14 og må ikke blandes med hand-assembly tool speed.

## 4. Safe actual-input handling

- [ ] Resolver current step mot den **faktiske stacken/materialet/toolen** spilleren bruker.
- [ ] Når et sterkt input skjerper downstream requirements, lagre actual rating/context i assembly state.
- [ ] Kjør forward-feasibility før consumption når inputet kan gjøre resten av planen umulig.
- [ ] Feil input gjør ingenting og konsumeres ikke.
- [ ] Definer sikker cancel/disassemble/recovery policy for abandoned assemblies slik at items ikke permanent låses ved chunk/restart.

## 5. Server authority

- [ ] Serveren eier consumption, tool damage, current step, bindings, actual capability values og waits.
- [ ] Client viser bare progress/prompt/animation.
- [ ] Test concurrent interactions fra flere spillere mot samme assembly.
- [ ] Test chunk unload/reload og server restart midt i assembly.

## 6. UX

Vis minst:

```text
LV Electric Motor
Step 5 / 13

Required: 1x Insulation Ring
Class: ELASTOMER
Insulation Capacity: >= 320
Reason: selected Wire = 320
```

For range:

```text
Required: Plate
Chemical Range must cover: -32 .. 120
```

For tool:

```text
Required Tool: Screwdriver
Current tool speed: 4.0x
```

- [ ] JEI skal vise både total component tree og assembly order.
- [ ] JEI skal vise minimumsrequirements, men forklare at sterkere values er tillatt.
- [ ] Når requirement kommer fra faktisk tidligere input, vis den resolved value under assembly.
- [ ] Ikke kreve et stort GUI for block-start assembly; bruk tooltip/Jade/actionbar/custom overlay der det er nok.

## Ferdig når

Assembly runtime kan håndtere dynamiske materialvalg, dependent requirements og tools med forskjellig speed/durability uten recursive runtime-search, desync eller item-loss.
