# Industron Development Docs

Denne mappen beskriver **målarkitekturen, gjenstående arbeid og implementeringsrekkefølgen** for Industron. Den skal kunne gis til en ny utvikler/chat sammen med siste prosjekt-ZIP uten at tidligere samtalehistorikk er nødvendig.

Dokumentasjonen skal primært svare på:

1. Hva skal modden ende opp som?
2. Hvilke designbeslutninger er allerede fastsatt?
3. Hva er neste tekniske steg?
4. Hvilke systemer skal bygges senere, og i hvilken rekkefølge?
5. Hvilke ting skal ikke implementeres ennå?

## Prosjekt

- Minecraft: **1.21.1**
- Loader: **NeoForge**
- Java: **21**
- Mod ID: **industron**
- Base package: **`net.mads.industron`**
- Create: **6.0.10**, brukt som integrasjon/dependency der det passer

## Mapper

- `preferences/`: arbeidsmåte, leveranseformat, kode- og dokumentasjonsregler.
- `code/`: målarkitektur for materialer, chemistry, recipes, machines, casings, multiblocks, energy og Create-integrasjon.
- `to do/`: gjenværende arbeid i anbefalt rekkefølge.
- `templates/`: maler for nye designbeslutninger og systemspecs.

## Hovedretning

Industron skal være et generativt, data-drevet industrisystem bygget rundt omtrent 100 fiktive grunnstoffer. Et grunnstoff defineres minimalt med identitet, atomnummer og tier. Materialegenskaper, forms, wires, fluid transport, ores og senere chemistry/gameplay skal så langt som praktisk mulig utledes fra felles regler fremfor manuelle per-material spesialtilfeller.

Wood og stone skal være fullverdige substances/materialer i samme composition-system og bruke gjenbrukbare grayscale `structure_sets`. Metal structure templates skal være generiske `metal_N`-design, ikke copper/iron/gold-familier.

## Gjeldende implementasjon vs plan

Docs skal fortsatt beskrive målarkitektur og roadmap, men implementerte systemer skal dokumenteres som implementerte. Assembly er et slikt system. Ved konflikt gjelder faktisk kode og `code/recipes/assembly-recipes.md` foran eldre Phase 15–17-eksempler. Eldre designnotater beholdes når de fortsatt er nyttige som framtidige utvidelser, men skal merkes som historiske/fremtidige.

## Dokumentstatus

Bruk disse merkene når nødvendig:

- **FASTSATT**: beslutning som ikke skal endres uten uttrykkelig ny beslutning.
- **PLANLAGT**: ønsket retning, detaljer kan fortsatt justeres.
- **ÅPEN**: faktisk uavklart.
- **NESTE**: arbeid som bør gjøres før senere faser.

Unngå å fylle dokumentasjonen med historikk over hva som tidligere ble implementert. Git/prosjektkoden er kilden for nåværende implementasjonsstatus; docs skal først og fremst styre veien videre.


## API-konvensjoner

Se `API_CONVENTIONS.md`. Foreslåtte Java-API-er skal bruke typed references og typed property-metoder; ikke string-lookups når et ekte objekt/property-API kan brukes.
