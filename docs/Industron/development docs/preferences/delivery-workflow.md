# Delivery workflow

## Før implementering

- Les `START_HERE.md`, `DECISIONS.md` og relevant `to do/`-fil.
- Inspiser siste prosjekt-ZIP/kode før du antar at docs og kode har identisk status.
- Ikke gjenåpne FASTSATTE beslutninger uten uttrykkelig beskjed.
- Hvis brukeren sier at vi bare diskuterer/design­er, ikke programmer.

## Under implementering

- Begrens endringen til avtalt scope.
- Bevar data-driven/generative prinsipper og unngå nye per-material hardkodede lister.
- Skill domain calculations fra registry/datagen/runtime-lag.
- Bruk faktiske Minecraft 1.21.1/NeoForge/Create 6.0.10 API-er fra prosjektet; ikke gjett class/method names.
- Når `.existing(...)` finnes, skal det være tydelig om generated registration undertrykkes.

## Før levering

- Kjør compile/tests/runData når komplett build-oppsett er tilgjengelig.
- Ikke påstå at build er verifisert hvis den ikke faktisk er kjørt.
- Lever normalt bare endrede/nye filer i ZIP.
- Hvis gamle filer må slettes, list eksakte paths separat.
- Oppsummer hva som ble endret og hva neste blocker/step er.
