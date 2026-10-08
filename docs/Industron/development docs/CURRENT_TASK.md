# Current task – canonical handoff

## Active goal

**Phase 07 – Foundry, Heater, støping og dynamiske metallblandinger** er neste aktive fase. Phase 05 Recipe Type Foundation og Phase 06 Process Rules er ferdige og skal behandles som stabilt fundament.

Målet i Phase 07 er å bygge den stateful Foundry/Heater-runtime som bruker Phase 06-kontrakten i stedet for å lage egne machine-specific kjemiregler.

## Verified Phase 05 baseline

- Phase-05 sin opprinnelige baseline var 62 gameplay process types + `TEST_PROCESSING`. Etter senere primitive/manual additions har dagens `CERecipeTypes.ALL` **69 gameplay types**. `TestProcessingRecipeType` finnes fortsatt som test-definition, men ligger ikke i dagens `CERecipeTypes.ALL`.
- De opprinnelige machine/process-type skeletons beholder ULV–IV provider-kobling. Tierless/manual additions som `HAND_PROCESSING` har med vilje ikke tier-skeletons.
- RecipeType eier process identity/layout, ikke power source, controller eller konkrete conditions.

## Verified Phase 06 baseline

- `ProcessSubstanceState` skiller bonded substances fra physical powder/liquid/solution/suspension/phase-separated/molten mixtures.
- `ProcessOperation`, `ProcessRuleSet` og `ProcessIntent` er typed; ingen string-capability contract.
- `ProcessRecipeResolver` mapper process intent til `ProcessKind + RecipeTypeDefinition` og gir rejection diagnostics uten nearest-process fallback.
- Physical separation krever ekte physical mixture og kan ikke åpne bonded alloy/compound.
- Centrifuging krever density contrast; filtration krever suspension; phase separation krever explicit separated state.
- Distillation/fractionation bruker fluid-mixture state og boiling/volatility behavior.
- Electrolysis/electrorefining/electrowinning har separate electron/oxidation-state semantics og kan lese generated `processProperties`.
- `MIXING` produserer physical mixture; `ALLOYING` produserer metallic lattice.
- `ProcessSafetyValidator` kjører semantics/resolver + conservation + cycle checks før automatic recipe emission.
- Generated material fluids propagates calculated acidity til ekte `IndustrialFluid` Chemical Balance-data; tooltip og CB hatch leser samme verdi.
- Java 21 process self-tests/compile checks og separat CB-propagation check er passert. Full Gradle/NeoForge build må kjøres lokalt fordi build-wrapper ikke var med i chat-arkivet.

## Plant/fiber side-task

Et separat early-game sidearbeid er startet uten å endre Phase-07 hovedmålet: `PlantMaterial`, fiber/string forms og `HAND_PROCESSING`. Senere plant growth/environment/genetics ligger kun som roadmap i `to do/plants/`.

## Next implementation sequence

Følg `to do/07-foundry-and-heater-multiblocks.md` som canonical Phase 07-spec. Start med:

1. Canonical dynamic composition payload/signatures og exact integer/fixed-point units.
2. Defined-alloy exact ratio index + generic unclassified mixture carriers.
3. Foundry bath/state model som bruker `ProcessSubstanceState` og Phase 06 resolver.
4. Heat-provider contract + full-footprint Heater linking/cache.
5. Thermal mass/current temperature/heat loss uten full structure scan per tick.
6. Melting/mixing/alloying/casting/cooling som compiled Foundry operations.
7. Dupe-safe graph/conservation validation og deterministic separation routes.
8. UI/JEI/diagnostics og performance for opptil 20 constituents.

## Locked boundaries

- Én ekstern transformation bruker én eksplisitt process identity/RecipeType eller en dokumentert Foundry-internal state transition.
- RecipeType != power source.
- Conditions ligger på machine/recipe.
- Pressure brukes ikke som process-condition.
- Composition er ikke det samme som structure.
- Physical separation bryter ikke chemical bonds uten eksplisitt støttet process semantics.
- Foundry må spørre Phase 06 process/semantics-laget; den velger ikke prosess fra slot-layout.
- Resolver returnerer process/RecipeType identity, ikke controller-navn eller power source.
- Sammensatt ore/mineral dust skal ikke få en Foundry-snarvei rundt eksisterende deterministic chemistry route.

Do not reimplement Phase 06 as a parallel Foundry-specific rule system. Phase 07 must consume the existing process-state/intent/resolver contract.
