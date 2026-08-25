# Industron TODO

## Ferdig i denne oppryddingen

- Prosjektet er renavnet til Industron.
- Mod-id er `industron`.
- Java-pakken er `net.mads.industron`.
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

## Neste programmeringssteg

- Bytt ut testinnholdet med de foerste ekte Industron-materialene og maskinene.
- Vurder aa rydde hardkodet lokal Registrate-jar i `build.gradle` naar riktig Maven-koordinat er bekreftet.
- Legg til fokuserte tester eller GameTests naar de foerste ekte maskinreglene er stabile.
- Hold `IndustrialMaterials`, `CERecipeTypes`, maskinlistene og datagen-katalogene smaa til ekte innhold trengs.
