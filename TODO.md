# Industron TODO

## Ferdig i denne oppryddingen

- Datagen RAM-oppsett er hevet til `-Xmx6G`, og genererte material-block models bruker nå delte tinted cube/cutout/layer templates i stedet for å skrive full kubegeometri per materiale.
- Prosjektet er renavnet til Industron.
- Mod-id er `industron`.
- Java-pakken er `net.mads.industron`.
- Konkrete materialdefinisjoner er flyttet til `net.mads.industron.material.defenitions`.
- `CompoundMaterials.TEST_ALLOY` er lagt til som en 1:1 metallic-lattice alloy av `ORLUNE` og `PRAXEL`.
- Gammelt konkret gameplay er fjernet fra registries, datagen og integrasjoner.
- Det minimale testinnholdet er beholdt:
  - `TEST_MATERIAL`
  - `TEST_COMPONENT`
  - `TEST_FLUID`
  - `test_processing`
  - `test_machine`
  - `test_multiblock`
  - `test_processing/dust_to_ingot.json`
- JEI og Jade er beholdt som tomme/generiske integrasjonspunkter.
- Datagen, ressursprosessering og build er verifisert.
- Mold texture-navn er ryddet: dedikerte mold-PNG-er ligger i formen/itemets mappe og heter `mold.png`; `cast_*_mold` registry-ID-er og recipes er beholdt.
- Terracotta texture-navn er ryddet: dedikerte terracotta-PNG-er ligger i formen/itemets mappe og heter `terracotta.png`; `terracotta_*` item-ID-er og display-navn er beholdt.

## Neste programmeringssteg

- Kjoer `compileJava -x createMinecraftArtifacts --no-daemon` med Java 21 og deretter `runData -x createMinecraftArtifacts --no-daemon` for aa regenerere de mindre material-block modellene; denne sandboxen fant ikke lokal `java`/`javac`.
- Kjoer `runData -x createMinecraftArtifacts --no-daemon` og kontroller `build/reports/industron/chemistry/test_alloy.*`; forrige kjoering ble avbrutt etter brukerbeskjed foer chemistry-rapporten kunne kontrolleres.
- Ved nye mold-art assets: legg filen som `mold.png` i riktig `material_sets/...`-mappe og oppdater bare `CastingDefinitions.moldTexture(...)`; ikke lag unike `cast_x_mold.png` texture-navn.
- Ved nye terracotta-art assets: legg filen som `terracotta.png` i riktig `material_sets/...`-mappe; ikke legg item-/formnavnet inn i PNG-filnavnet.
- Bytt ut testinnholdet med de foerste ekte Industron-materialene og maskinene.
- Vurder aa rydde hardkodet lokal Registrate-jar i `build.gradle` naar riktig Maven-koordinat er bekreftet.
- Legg til fokuserte tester eller GameTests naar de foerste ekte maskinreglene er stabile.
- Hold `IndustrialMaterials`, `CERecipeTypes`, maskinlistene og datagen-katalogene smaa til ekte innhold trengs.
