# Phase 04 – Content generation og material-derived casings – FERDIG

Phase 04 er ferdig implementert. Fasen etablerer robust, deterministisk content-generation og et generisk casing-system der materialets typed stats avgjør om en casing-variant kan eksistere.

Det finnes ingen navnebaserte materialunntak i qualification-logikken. Et nytt materiale som senere legges inn i `IndustrialMaterials.ALL`, blir automatisk evaluert mot de samme casing-definisjonene.

## 1. Ferdig content-generation-fundament

- [x] Material-, structure- og casing-definisjoner valideres av den felles validation-pipelinen.
- [x] Validation kjøres både ved oppstart og før datagenerering.
- [x] Ugyldige og dupliserte ID-er blir hard feil før registrering/output.
- [x] `.existing(...)` hindrer generatoren i å registrere den samme materialformen på nytt.
- [x] Existing/generated casing-kollisjoner kontrolleres mot materialblokker, ore-host-blokker, structure-blokker og tier-casings.
- [x] Manglende source-texture for en casing eller structure-definition blir hard feil.
- [x] Generatorresultater er stabile og deterministisk sortert.
- [x] Manglende varianttekstur fjerner ikke en ellers gyldig materialform; eksisterende variant-resolusjon beholder fallback-reglene sine.

## 2. Dynamic ore hosts

- [x] Registrerte stone-materialer oppdages dynamisk som mulige ore hosts.
- [x] Normal og small ore genereres for alle kompatible hosts.
- [x] Host appearance kommer fra stone-materialet, mens ore overlay/tint kommer fra ore-materialet.
- [x] Dynamic hosts bruker stabile, avledede registry-ID-er.
- [x] Dupliserte host/ore-ID-er og manglende host-assets stoppes av validation/generator checks.

Det finnes ingen per-ore hardkodet liste over stone-hosts. Nye kompatible stone-materialer kan derfor kobles inn uten endringer i hver ore-definition.

## 3. Typed casing-definition

`CasingDefinition` beskriver hva en casing-type trenger:

- stabil ID og display name
- start-tier
- texture
- base block-form
- typed material requirements
- assembly inputs, components og tools

Requirements bruker Phase 03-systemet og kan uttrykke `atLeast`, `atMost`, `exactly`, ranges og typed enum/bool-krav der capability-typen tillater det.

`MaterialCasingGenerator` evaluerer alle casing-definisjoner mot alle materialer. En variant genereres bare når:

- materialet er på eller over casingens start-tier
- alle typed material requirements er oppfylt
- nødvendig base block-form finnes
- alle material-, component- og tool-inputs kan løses

Ett materiale kan kvalifisere til flere casing-typer. Qualification bruker ikke materialnavn, symbolske spesialregler eller en håndskrevet tillatelsesliste.

## 4. Permanent casing-katalog

Phase 04 har én permanent, generell startprofil:

- `machine_casing`

Dette er ikke en testprofil. Den er den generelle strukturelle material-casingen og krever tilstrekkelig structural strength, frame som base og plate-inputs i assembly.

Spesialiserte casings for varme, kjemi, trykk eller bestemte multiblocks skal legges til av fasen som eier det gameplay-systemet. De skal bruke samme `CasingDefinition`-API og trenger ikke endringer i generatoren.

## 5. Stabil identity og naming

Generated casing identity er fast:

```text
registry ID: <material_id>_<casing_definition_id>
display name: <Material Name> <Casing Name>
```

- [x] Definisjoner og materialer sorteres etter ID før qualification.
- [x] Samme definitionsdata gir samme ordnede resultat.
- [x] Dupliserte generated IDs blir hard feil.
- [x] Display name valideres mot materialnavn + casing-navn.
- [x] Hver casing-definition må generere minst én gyldig variant.

## 6. Komplett generated casing-content

For hver kvalifiserte variant genereres hele content-settet:

- [x] block registration
- [x] block item registration
- [x] block model
- [x] blockstate
- [x] item model
- [x] casing texture/template med material-tint
- [x] lang/display name
- [x] loot table
- [x] assembly recipe
- [x] mining tool/tier tags
- [x] common block/item casing tags
- [x] per-definition tags
- [x] per-material tags

Følgende nye tag-familier er en del av kontrakten:

```text
c:casings
c:machine_casings
industron:material_machine_casings
industron:material_machine_casings/<casing_definition_id>
industron:material_machine_casings/material/<material_id>
```

Block- og item-tags genereres fra den samme `MaterialCasingGenerator.ALL`-listen som registreringen. Manglende registry holder blir hard feil under datagenerering i stedet for stille manglende innhold.

## 7. Casing-validator

`MaterialCasingDefinitionValidator` er den egne validatoren for casing-definisjoner og generated casing-content. Den kontrollerer:

- gyldig og unik definition-ID
- unik og ikke-tom display name
- gyldig electric start-tier
- ikke-tomme typed requirements og assembly inputs
- at casing-level requirements ikke bruker part-spesifikke practical stats
- at base input er en block-form
- at source-texture faktisk finnes
- at generatoren gir samme resultat ved identiske kjøringer
- korrekt avledet registry ID og display name
- dupliserte generated casing-ID-er
- kollisjon med andre generated block-familier
- at kvalifisert materiale faktisk har baseformen
- at hver registrerte casing-definition gir minst én variant

Feil rapporteres under `ValidationSubsystem.CASING` og stopper registrering/datagen gjennom den eksisterende validation-pipelinen.

## 8. Future material support

- [x] Alle dagens `IndustrialMaterials.ALL` evalueres automatisk.
- [x] Generatoren mottar typed materialdata og er ikke knyttet til elementnavn.
- [x] Nye alloys, compounds eller polymers kan bruke samme vei når de senere blir representert som `IndustrialMaterial` med forms og derived stats.
- [x] Et framtidig substance får ikke casing bare fordi det finnes; alle requirements må fortsatt bestås.
- [x] Nye casing-profiler kan legges til katalogen uten ny registry-, model-, loot-, lang-, tag- eller recipe-kode.

## 9. Bevisst utenfor Phase 04

Følgende er ikke uferdige Phase 04-punkter:

- generated alloy definitions og alloy chemistry
- molecular/compound generation
- polymer chemistry
- automatic chemistry og composite-dust separation
- Foundry-runtime, varme molds og alloy-mixtures
- heater- og processing-multiblocks
- tree/sapling growth

Disse systemene skal gjenbruke fundamentet fra Phase 04, men eies av senere faser.

## 10. Ferdigkriterier

- [x] Existing content-generation har validation rundt IDs og source-assets.
- [x] Ores bruker registrerte stone hosts dynamisk.
- [x] Typed casing-definitions uttrykker materialkrav gjennom Phase 03 capabilities/requirements.
- [x] Alle registrerte materialer testes automatisk mot alle casing-definisjoner.
- [x] Kun kvalifiserte materialer får generated casing-content.
- [x] Generated identity, rekkefølge og assets er stabile og deterministiske.
- [x] Registrering, models, blockstates, item models, lang, loot, tags og assembly recipes bruker samme generated casing-liste.
- [x] Missing references og ID-kollisjoner stoppes før ugyldig output kan brukes.
- [x] Senere materialfamilier kan kobles inn uten per-material casing-kode.

**Status: FERDIG.**
