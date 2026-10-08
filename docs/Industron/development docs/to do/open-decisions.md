# Faktisk åpne beslutninger

Beslutninger som allerede er låst/implementert skal ikke stå som åpne her.

## Casings/content generation – Phase 04

- [ ] Endelig liste over casing-definisjoner/profiler som faktisk skal finnes.
- [ ] Exact `Stats` requirements/thresholds per casing-type.
- [ ] Exact naming convention: registry ID og display-name ordering for `<Casing Name> + <Material Name>`.
- [ ] Hvilke casing-typer som trenger egne model/texture templates vs shared template + material appearance.
- [ ] Om casing qualification skal være tier-sensitive eller bare property-sensitive for første version.
- [ ] Hvordan generated casing tags organiseres for multiblock predicates og later recipe use.

## Recipe types/process rules – Phase 05–06

- [x] Én concrete machine bruker én RecipeType.
- [x] RecipeType eier ikke power source.
- [x] Pressure brukes ikke som process-condition.
- [x] CB/temperature/RPM/circuit/catalyst/tier/duration er machine/recipe-level data etter behov.
- [x] Dagens `CERecipeTypes.ALL` er den canonical runtime/JEI-katalogen. Etter senere primitive/manual additions inneholder den nå 69 gameplay types; tierless `HAND_PROCESSING` er en av dem.
- [ ] Maks item/fluid/gas input/output counts per type der dette faktisk er en layout/runtime-limit.
- [ ] Endelig representation av catalyst/non-consumed process inputs.
- [ ] Typed process-operation-sett for separation, distillation, electrolysis, mixing, alloying, crystallization og chemical reaction.
- [ ] Hvor detaljert phase/density/volatility-model må være før automatic separation/distillation selection.

## Foundry/heaters – Phase 07

- [x] Heater fyller hele outer footprint under Foundry og matcher Foundry size variant.
- [x] Variant-regel: inner `2n-1`, outer/heater `2n+1`.
- [x] Foundry eier ikke steam/solid/liquid fuel; den mottar heat gjennom et heater contract.
- [ ] Exact Foundry min/max size og max height.
- [ ] Exact capacity-per-inner-volume formula i Industron.
- [ ] Exact heat capacity / warm-up / cool-down formula.
- [ ] Exact over-temperature melt-speed scaling.
- [ ] Exact heater output curves for Steam Heater, Solid Fuel Heater og Liquid Fuel Heater.
- [ ] Hvilke generated casing profiles Foundry og hver heater krever.
- [ ] Exact drain/mold/casting interaction ved porting fra reference-projectet.
- [x] Defined alloy matching bruker exact normalized ratio; ingen closest-alloy eller missing-amount UI.
- [x] Udefinerte mixtures bruker generic data-bearing carriers og må kunne separeres igjen.
- [x] Målet er minst 20 constituents uten scan per tick.
- [ ] Exact generic carrier IDs/data-component schema og maximum serialized payload size.
- [ ] Exact thermal bands/thresholds og migrering fra `HOT_INGOT`/`HOT_NUGGET` til generell thermal state.
- [ ] Exact metallurgisk reduction-model/byproduct families for Foundry-eligible composite dust.
- [ ] Exact stoichiometric cycle algorithm som erstatter dagens «block every directed cycle» for reversible Foundry edges.

## Composition/registry – Phase 08

- [ ] Registry-strategi for generated substances: requested/reachable registry entries vs senere generic data-component carrier.
- [ ] Canonical signature som inkluderer nok structure/phase-data til å skille reelt forskjellige substances uten å duplisere identiske.

## Alloys – Phase 09

- [ ] Alloy property mixing/synergy-formel og bounds.
- [ ] Hvordan processing state (cast, annealed, hardened osv.) eventuelt påvirker properties senere.
- [ ] Hvor mye phase/crystal modeling som trengs for første alloy-system.
- [x] Foundry resolution bruker canonical direct signature først og flattened ledger til conservation/ambiguity checks.

## Molecular/compound chemistry – Phase 10–11

- [ ] Første supported bond families utover covalent single/double/triple og ionic/metallic context.
- [ ] Om aromatic/resonance-lignende behavior skal være med i første chemistry-version eller utsettes.
- [ ] Compound naming og canonical formula ordering for de fiktive elementene.
- [ ] Eksakt skille/API mellom free `IonState`, formal charge og oxidation state i compounds.
- [ ] Eventuelle polyatomic/composite ions og canonical identity.
- [ ] Hvordan compound-level acidity/CB-related behavior avledes fra den eksisterende generated atom/material-modellen + structure.

## Polymer-like chemistry – Phase 12

- [ ] Første structural/functional groups som faktisk trengs i gameplay.
- [ ] Repeating-unit model og eventuelle thermoplastic/thermoset-lignende classifications hvis de trengs.
- [ ] Hvor mange structural variants som kan genereres før reachability/safety cutoffs stopper søket.

## Reaction system – Phase 13

- [ ] Reaction feasibility/difficulty formula fra bonds, ions, temperature, CB/state og catalyst.
- [ ] Hvordan alternative reaction paths rangeres.
- [ ] Første catalyst-system og eventuell degradation/poisoning senere.

## Automatic recipes – Phase 14

- [ ] Base duration-formel og senere overclocking.
- [ ] Mapping fra reaction/process complexity til minimum machine tier.
- [ ] Om flere fysisk gyldige process paths skal genereres samtidig eller om én canonical default velges.
- [ ] Exact generated path/layout under `material/recipes/` for hver material-definition family.

## Assembly/components – Phase 15–17

### Låst/implementert – ikke åpne beslutninger lenger

- Java-kjernen for Material/Component/Metal inputs, block/item base, tools/waits og requirements er implementert; se `../code/recipes/assembly-recipes.md`.
- JEI er delt i `Assembly Products` og `Assembly Components`; product-visningen flater ikke ut hele nested tree-et.
- Free/fixed materialsemantikk, `inputAny(...)`, parent-relative `atLeastParent()`/`atMostParent()` og Deployer/FakePlayer tool-work er fastsatt.

### Fortsatt åpne

- [ ] Første permanente component library utover dagens `SCREW`, `PLATE` og `VERY_LONG_ROD` når Fluid Tank/Pipe/Pump, hatches, buses og machines får konkrete recipes.
- [ ] Permanent industrial tool-library som erstatter test-Pickaxe der passende.
- [ ] Videre UX for svært store component-trees utover dagens to JEI-kategorier/next-step overlay.

## Senere balance – Phase 21

- [ ] Finjuster property-/capability-skalaer mot faktisk gameplay når alloys, chemistry, casings, Foundry og assembly bruker dem samtidig.
- [ ] Rebalanser wire amp-kurve ved behov uten å bryte material-property invariants.
- [ ] Finjuster pump flow/friction/stress, tank/chemical thresholds og heater/foundry thermal rates mot representative builds.

## Andre systemer

- [ ] Om og hvordan stone molten forms aktiveres per stone.
- [ ] Maintenance persistence + repair UX.
- [ ] TreeDefinition API, growth algorithm og worldgen-regler.

## Current material/geology integration - only still-open details

The main architecture is locked in `23-material-geology-autorecipe-integration.md`. Do not reopen already-decided questions from older sections that conflict with it.

- [ ] Decide the explicit failed-roll result for non-Silk stone mining when the dust chance fails; do not retain current self/cobbled fallback accidentally.
- [ ] Calibrate deterministic process-selection thresholds from actual generated property ranges.
- [ ] Reuse/confirm the central duration/power scaling for generated routes rather than adding new constants.
- [ ] Add a new processing-override API only if `ChemicalStructure` proves insufficient for real source definitions.
- [ ] Verify the complete Minecraft/Create block ID mapping table against the actual loaded versions while implementing StoneMaterials/WoodMaterials.
