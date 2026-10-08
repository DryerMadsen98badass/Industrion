# Docs package validation notes

Denne pakken er en komplett korrigert kopi av docs-arkivet, sammenlignet mot den vedlagte current project-koden.

Kontroller ved pakking:

- hele docs-treet og `Industron Checklist.xlsx` er med;
- `FILE_INDEX.md` stemmer med filsettet;
- Foundry-TODO beskriver exact alloys, uklassifiserte mixtures, opptil 20 constituents, hot molds/cooling, heater, ytelse og dupe-sikkerhet;
- current-status docs skiller implementert chemistry/geology/ore-processing fra planlagt Foundry runtime;
- gamle påstander om test-only stone/wood, `StructureMaterialPart`, utsatt ore-processing og chemistry begrenset til tre familier er fjernet eller erstattet;
- ingen Java/prosjektfiler er endret;
- ZIP-integritet testes før levering.

Faktisk kode er fortsatt sannhetskilden for hva som er implementert. TODO-er merket planlagt skal ikke leses som ferdig runtime.
