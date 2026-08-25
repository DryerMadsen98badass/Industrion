# Phase 04 – Content generation og material-derived casings – DELVIS IMPLEMENTERT

Phase 03 er ferdig. Denne fasen gjør content-generatorene robuste og flytter **material-derived casing generation** fram fra den gamle Phase 17 til nåværende aktive fase.

Casings skal genereres fra materialets faktiske stats/capabilities. Det skal ikke finnes en håndskrevet liste over hvilke materialer som «får lov» til å være casing.

Denne fasen bygger bare generatoren og casing-systemet for materialer som finnes. **Generated alloys, compounds og polymers kommer senere** og skal da kunne gå gjennom samme casing-generator uten spesialkode.

## Gjeldende casing-status

Kjernen for material-derived casings finnes nå i `CasingDefinition`, `MaterialCasingRecipes`, `MaterialCasingGenerator` og `MaterialCasingAssemblyRecipes`. Qualification bruker typed stats/requirements og Assembly-resolusjon; kvalifiserte variants konverteres til vanlige `AssemblyRecipeDefinition`-recipes. Manglende fysisk part kan gjøre at en materialvariant ikke genereres, mens ugyldig component-definition/cycle er hard feil.

Den opprinnelige checklista under beholdes fordi den også beskriver content-generation, assets, future substances og hardening som fortsatt kan være relevant. En ukrysset gammel casing-linje betyr derfor ikke automatisk at funksjonen mangler i dagens kode. Se `code/casings/profile-definition.md` for faktisk current API.

## 1. Structure/material regression

- [ ] Kjør full `runData` + client regression etter større generatorendringer og verifiser stable IDs, models, blockstates, loot, lang og tags.
- [ ] Legg automated validation rundt `.existing(...)` slik at eksisterende registry objects aldri får generated duplikater.
- [ ] Legg automated validation for generated model -> texture references og existing/generated collisions.
- [ ] Behold regelen om at missing texture ikke skal fjerne en ellers gyldig registry form.
- [ ] Behold dynamic variant discovery der variant-mapper brukes.
- [ ] Sørg for at generatorresultatet er deterministisk mellom identiske `runData`-kjøringer.

## 2. Ores

- [ ] Koble registrerte `StoneMaterial` dynamisk til ore-host generator.
- [ ] Generer normal + small ore for hver gyldig host uten per-material hardkodet hostliste.
- [ ] Host-texture skal komme fra stone-materialets genererte block/family, mens ore overlay/tint kommer fra ore-materialet.
- [ ] Validation skal oppdage duplicate host/ore IDs og manglende generated assets før output skrives.

## 3. Material-derived casing definitions

Casing-typen beskriver **hva en casing krever**. Materialet bestemmer om en variant faktisk kan eksistere.

Konseptuelt:

```text
CasingDefinition
├─ stable casing ID/name
├─ typed material requirements
├─ texture/model template
├─ optional gameplay metadata
└─ generated variants
    ├─ Material A -> kvalifiserer -> casing genereres
    ├─ Material B -> feiler requirement -> ingen casing
    └─ Material C -> kvalifiserer -> casing genereres
```

- [ ] Definer typed casing-definition/profile API som bruker `Stats`/typed requirements fra Phase 03.
- [ ] En casing-definition skal kunne kreve flere stats samtidig med `atLeast`, `atMost`, `exactly`, ranges og typed enum/bool requirements der det gir mening.
- [ ] Et materiale får **bare** en casing-variant dersom alle requirements for casing-typen er oppfylt.
- [ ] Ett materiale kan kvalifisere til flere forskjellige casing-typer dersom statsene tillater det.
- [ ] Ikke bruk navn, tier-spesifikke materiallister eller `if (material == ...)` som permanent qualification-logikk.
- [ ] Exact casing-profiler og thresholds bestemmes i neste designrunde; generatoren skal ikke låses til en bestemt liste før dette er bestemt.
- [ ] Ingen pressure-condition/profile legges inn som prosesskrav. Materialets eksisterende materialstats kan fortsatt eksistere som materialdata uten å bli et machine/recipe pressure-system.

## 4. Generated casing identity og naming

Hver generated casing skal kombinere casing-identiteten og material-identiteten på en stabil måte.

Eksempelretning:

```text
<Casing Name> + <Material Name>
```

- [ ] Stable registry ID skal avledes deterministisk fra `casingDefinition + materialId`.
- [ ] Display name skal alltid inneholde både casing-navnet og material-navnet.
- [ ] Exact rekkefølge/format for display name og registry ID avgjøres når casing-profilene diskuteres ferdig.
- [ ] Ingen kollisjon mellom to casing-typer for samme materiale eller samme casing-type for to materialer.

## 5. Generated casing content

Når et materiale kvalifiserer skal generatoren kunne lage hele content-settet som trengs:

- [ ] Block registration.
- [ ] Item/block item.
- [ ] Model.
- [ ] Blockstate.
- [ ] Texture/template + material appearance/tint etter eksisterende material-generatorregler.
- [ ] Lang/display name.
- [ ] Loot.
- [ ] Relevante block/item tags.
- [ ] Validation for missing template/model/texture references.
- [ ] Existing/generated collision protection.

Recipes for generated material-content skal **ikke** spres i casing-generatoren. `material/recipes/` er reservert for de senere automatiske recipe-generatorene for blant annet `StoneMaterial`, `GemMaterial`, `WoodMaterial`, `IndustrialMaterial` og senere generated substances.

## 6. Fremtidig generated substance support

Denne fasen skal gjøre casing-generatoren substance-agnostic nok til at senere materialer kan kobles inn uten ny casing-spesialkode.

- [ ] Dagens registrerte materialer kan evalueres umiddelbart.
- [ ] Når generated alloys kommer i Phase 09 skal de automatisk kunne evalueres mot de samme casing-definisjonene.
- [ ] Når compounds/polymers senere får material-form/content support skal de bare få casing-varianter dersom deres derived stats faktisk oppfyller kravene.
- [ ] Ikke generer casing-varianter bare fordi et substance finnes; qualification er alltid requirement-basert.

## Ikke i Phase 04

Følgende utsettes bevisst:

- Generated alloy definitions og alloy chemistry.
- Molecular/compound generation.
- Polymer chemistry.
- Automatic chemistry recipes.
- Foundry runtime og heater-multiblocks; de er spesifisert i Phase 07 etter recipe/process-fundamentet.
- `TreeDefinition` og faktisk sapling/tree-growth.

## Ferdig når

Phase 04 er ferdig når:

- [ ] Existing content generation har regression/validation rundt IDs og assets.
- [ ] Ores kan bruke registrerte stone hosts dynamisk.
- [ ] En typed casing-definition kan uttrykke materialkrav gjennom Phase 03 `Stats`/requirements.
- [ ] Alle registrerte materialer kan testes automatisk mot alle casing-definisjoner.
- [ ] Kun qualifying materials får generated casing blocks/items/assets.
- [ ] Generated casing identity og assets er stabile og deterministiske.
- [ ] Senere alloys/compounds kan kobles inn i samme qualification/generation path uten per-material casing-kode.
