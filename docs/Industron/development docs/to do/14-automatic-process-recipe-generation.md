# Phase 14 – Automatic process recipe generation

**Status: FØRSTE GENERELLE PIPELINE IMPLEMENTERT, HARDENING GJENSTÅR.**

Dagens kode er ikke lenger begrenset til bare stone/wood/raw ore-source. `ChemistryBootstrap` analyserer registrerte materials, `ProcessPlanner` lager planer, `ProcessSafetyValidator` validerer dem og `AutomaticChemistryRecipes`/`ReflectiveRecipeEmitter` publiserer gyldige CE-recipes. Ore-preprocessing bygges separat av `OreProcessingRecipes`.

## Implementert grunnlag

- [x] Sammensatte materials med `DUST` kan få deterministic post-dust separation route.
- [x] Topology/properties velger mellom centrifuging, magnetic separation, direct/molten electrolysis, molten electrorefining, acid leaching og chemical reaction chains.
- [x] Automatisk navngitte/register-backed intermediates: `<material> slurry`, `<material> solution`, `<material> reaction mixture`.
- [x] Tørr dust-distillasjon og andre ugyldige phase transitions avvises av `ProcessSemantics`.
- [x] Elementært `DUST -> INGOT` genereres ikke.
- [x] Ore-preprocessing ender i ore-materialets eget dust og er separat fra chemistry.
- [x] Flattened elemental mass balance, guaranteed-output requirement og directed-cycle block kjøres før emission.
- [x] Generated plans/diagnostics lagres i `GeneratedProcessRegistry` og rapporteres fra `runData`.
- [x] Recipe emission bruker eksisterende CE recipe infrastructure.

## Gjenstående hardening

- [ ] Samle canonical composition/signature/flattening i én delt tjeneste i stedet for flere interne implementasjoner.
- [ ] Verifiser alle emitted process IO limits og deterministic multi-step splitting for mange outputs.
- [ ] Gjør requirement resolution og manglende compatible materials mer presist uten materialnavn-hardkoding.
- [ ] Test gems generisk: chemistry gir gem-elementets dust; separat gem-processing håndterer rough/normal gem.
- [ ] Test nested composite outputs og top-level-vs-elemental recovery policy.
- [ ] Utvid graph validator til stoichiometric cycle analysis før Foundry får reversible dynamic mixtures. Dagens validator blokkerer alle directed cycles.
- [ ] Støtt dynamic carrier payloads/concentrates uten nye registry IDs per runtime mixture.
- [ ] Kalibrer route thresholds fra representative generated properties.
- [ ] Verifiser byte-stabile reports/recipes over gjentatte `runData`-kjøringer.
- [ ] Legg til graph-wide regression for ore forms, dust units, chemistry intermediates og Foundry forms.

## Faste regler

- Recipe route kommer fra composition + structure/topology + beregnede material-/atomegenskaper.
- Samme inputdata gir samme plan; iteration order og random brukes ikke.
- Machine slot layout bestemmer ikke hvilken fysikk materialet trenger.
- `.contains(...)` er composition, ikke automatisk bevis på physical mixture.
- Physical separators bryter ikke bonds.
- Dry dust destilleres ikke; en gyldig liquid intermediate må finnes først.
- Ingen output truncates for å passe slots.
- Ingen materialnavn-special cases.
- Ingen chance main outputs i massebalanserte chemistry chains.
- Energi/tid kan aldri gjøre materialduping trygg.

## Foundry-integrasjon

Foundryen skal konsumere compiled/validated metallurgical planer, ikke lage en parallell recipe-generator. Exact defined alloys og uklassifiserte dynamic mixtures beskrives i `07-foundry-and-heater-multiblocks.md` og `09-alloys.md`.

## Ferdig når

Alle registrerte composite dusts og senere dynamic mixture payloads får en fysisk gyldig, deterministisk, IO-kompatibel og graph-validert route – eller en konkret blocking diagnostic – før recipes/plans blir tilgjengelige i runtime.
