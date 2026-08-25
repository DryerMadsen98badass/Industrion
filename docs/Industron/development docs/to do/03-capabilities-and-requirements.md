# Phase 03 – Capabilities and requirements ✅ FERDIG

Phase 03 etablerer det typed requirement-systemet som resten av assembly-systemet bygger på.

Målet er at recipes og component definitions aldri skal hardkode hvilke konkrete materialer som er «gode nok». De skal i stedet uttrykke **hvilke egenskaper eller praktiske capabilities som kreves**, og material-/form-systemet avgjør hvilke faktiske deler som oppfyller kravene.

Denne fasen er ferdig. Konkrete component definitions, som spesielle ringer, motorer, bearings eller andre sammensatte deler, lages senere når de faktisk trengs av recipes.

## Nåværende Assembly-bruk av Phase 03

Assembly bruker det typed `Stats`/requirement-systemet direkte. Et recipe-level `.stat(...)` etter en Material/Component-input propageres til material-leaves i det ekspanderte treet. `ComponentDefinition.inputAny(...)` har i tillegg parent-relative scalar requirements med `atLeastParent()`/`atMostParent()`. Disse relative kravene er implementert senere enn den opprinnelige Phase 03-planen og dokumenteres fullt i `code/recipes/assembly-recipes.md`.

---

## 1. Typed `Stats`-API ✅

Alle kjente material-properties og assembly-capabilities skal nås gjennom typed constants med IntelliJ autocomplete.

Eksempel:

```java
Stats.ELECTRICAL_CAPACITY.atLeast(150)
Stats.MELTING_POINT.atLeast(2000)
Stats.BOILING_POINT.atLeast(3000)
Stats.OUTER_SHELL_ELECTRONS.exactly(8)
```

Ikke:

```java
.require("electrical_capacity", 150)
.require("melting_point", 2000)
```

### Numeriske stats

Numeriske stats støtter:

```java
.atLeast(value)
.atMost(value)
.exactly(value)
.range(min, max)
```

Eksempel:

```java
Stats.HARDNESS.atLeast(80)
Stats.DENSITY.atMost(120)
Stats.OUTER_SHELL_ELECTRONS.exactly(8)
```

`atLeast(150)` betyr 150 eller høyere. Systemet velger ikke automatisk den minste gyldige verdien.

### Range-stats

Egenskaper som representerer et intervall bruker eksplisitt range-semantikk.

Eksempel:

```java
Stats.CHEMICAL_RANGE.covers(-32, 120)
```

Et materiale med range `[materialMin, materialMax]` er gyldig når:

```text
materialMin <= requiredMin
materialMax >= requiredMax
```

Eksempel:

```text
[-40, 150]  -> valid
[-32, 120]  -> valid
[-20, 150]  -> invalid
[-50, 100]  -> invalid
```

### Typed enum- og boolean-stats

Ikke-numeriske materialegenskaper er også tilgjengelige gjennom `Stats`.

Eksempel:

```java
Stats.PHYSICAL_STATE.is(...)
Stats.ELECTRICAL_BEHAVIOR.is(...)
Stats.CRYSTAL_STRUCTURE.is(...)
Stats.FRACTURE_BEHAVIOR.is(...)
Stats.ELECTRONIC_FAMILY.is(...)

Stats.METAL.is(true)
Stats.IS_MAGNETIC.is(true)
Stats.CRYSTALLINE.is(true)
Stats.GEM_CANDIDATE.is(true)
Stats.HEAT_RESISTANT.is(true)
Stats.PRESSURE_RESISTANT.is(true)
Stats.FURNACE_FUEL.is(true)
```

Dermed kan både recipes og component definitions bruke samme typed API for materialegenskaper.

---

## 2. Intrinsic properties og practical capabilities ✅

Et materiale beholder de samme **intrinsic properties** uansett hvilken form det har.

En konkret part kan samtidig ha en praktisk kapasitet som avhenger av:

- materialets intrinsic properties
- part-type
- geometri
- tykkelse
- størrelse
- lasttype

Dette håndteres av `PracticalCapabilityResolver`.

Eksempler på practical capabilities:

```text
WIRE
└─ ELECTRICAL_CAPACITY

RING
└─ INSULATION_CAPACITY

PLATE
└─ STRUCTURAL_LOAD

PIPE
└─ PRESSURE_CAPACITY

ROD
└─ SHAFT_LOAD

SCREW / BOLT / RIVET
└─ FASTENER_LOAD
```

Practical capabilities skal **deriveres** fra material + form/geometri. De skal ikke være en separat håndskrevet materialtabell og skal aldri skrive nye form-spesifikke verdier tilbake til materialets intrinsic properties.

---

## 3. Requirements i recipes ✅

Recipes kan legge requirements direkte på en input.

Eksempel:

```java
.input(WIRE_1X, 4)
    .require(Stats.ELECTRICAL_CAPACITY.atLeast(150))
```

Requirements gjelder den konkrete inputen de er satt på, med mindre de eksplisitt er definert som environment requirements.

En recipe kan bruke både:

- intrinsic material properties
- practical capabilities
- scalar requirements
- exact requirements
- range requirements
- typed enum/bool requirements

---

## 4. Requirements i component definitions ✅

En `ComponentDefinition` kan selv definere hvilke inputs den består av og hvilke requirements de enkelte delene må oppfylle.

Det betyr at en component senere kan beskrives som en sammensatt struktur:

```text
COMPONENT
├─ PART
│  └─ requirement
├─ PART x4
│  └─ requirement
└─ nested COMPONENT
```

Den samme componenten kan deretter brukes:

- direkte i en recipe
- inni en annen component definition

Konkrete component definitions er **ikke en del av Phase 03**. Systemet er ferdig; de faktiske komponentene legges til senere etter behov.

---

## 5. Nested components ✅

Components kan inneholde andre components.

Assembly-systemet kan derfor ekspandere et component-tre rekursivt:

```text
RECIPE
└─ COMPONENT
   ├─ PART
   └─ NESTED_COMPONENT
      ├─ PART
      └─ PART
```

Requirements på de konkrete inputene beholdes på riktig nivå.

Dette gjør at en framtidig semantic component kan representere mer enn én fysisk part uten at recipes trenger å kjenne hele den interne oppbygningen.

---

## 6. Environment requirements og propagation ✅

Requirements som beskriver sluttproduktets miljø kan legges inn med:

```java
.environment(...)
```

Dette kan gjøres både på:

- recipe
- component definition

Eksempel:

```java
.environment(Stats.CHEMICAL_RANGE.covers(-32, 120))
.environment(Stats.TEMPERATURE.atLeast(800))
```

Environment requirements propageres automatisk ned til relevante consumed material-bearing inputs og nested components.

Eksempel:

```text
COMPONENT
│
├─ environment: chemical range
├─ environment: temperature
│
├─ PLATE
│  └─ arver environment
│
├─ SCREW
│  └─ arver environment
│
└─ NESTED_COMPONENT
   └─ arver environment
```

Vanlige `.require(...)`-requirements er fortsatt lokale. Det er bare eksplisitte `.environment(...)`-requirements som propageres på denne måten.

---

## 7. Scope og `ignoreEnvironment()` ✅

En input kan eksplisitt skjermes fra inherited environment requirements.

Eksempel:

```java
.input(...)
    .ignoreEnvironment()
```

Dette brukes når en intern del ikke faktisk eksponeres for det samme miljøet som resten av komponenten.

Eksempel:

```text
COMPONENT
├─ exposed PART
│  └─ arver environment
│
└─ protected internal PART
   └─ ignoreEnvironment()
```

Dette gjør environment-systemet eksplisitt uten å tvinge alle nested deler til å ha samme rating.

---

## 8. Tools og waits ✅

Tools er ikke en del av sluttproduktet.

Derfor:

```text
TOOLS
└─ arver ikke material/environment requirements
```

Timed tool steps påvirker assembly-prosessen, men ikke materialkravene til sluttproduktet.

Wait-steps har heller ingen material-capabilities og mottar ikke propagated requirements.

---

## 9. Material classes er bevisst fjernet ✅

Phase 03 bruker **ikke** et eget `MaterialClass`-system som:

```text
METAL
CERAMIC
POLYMER
ELASTOMER
GEM
```

Dette ble bevisst valgt bort.

I stedet uttrykkes gameplay-krav gjennom:

- konkrete parts/components
- intrinsic material properties
- practical capabilities
- component definitions
- typed requirements

Dermed kan framtidige components få egne betydninger uten at recipes må begrenses av en generell materialklasse.

---

## 10. Component-identitet og framtidige definitions ✅

En component-identitet kan senere representere:

- én fysisk part
- en bestemt variant av en part
- en semantic component
- en sammensatt component med flere underdeler

Det betyr at framtidige recipes ikke er låst til generiske navn som `RING`.

De kan bruke den component-identiteten som faktisk beskriver delen recipe-en trenger.

Selve Phase 03 definerer **mekanismen**, ikke listen over alle framtidige components.

---

# Ferdig når ✅

Phase 03 regnes som ferdig når:

- [x] Kjente stats/capabilities har typed keys og IntelliJ autocomplete.
- [x] Numeriske stats støtter `atLeast`, `atMost`, `exactly` og range-semantikk.
- [x] Range-properties kan uttrykke coverage med eksplisitte min/max-grenser.
- [x] Typed enum- og boolean-properties kan brukes som requirements.
- [x] Material-properties kan brukes gjennom det felles `Stats`-API-et.
- [x] Practical capabilities kan deriveres fra material + part/geometri.
- [x] Recipes kan legge requirements på konkrete inputs.
- [x] Component definitions kan ha egne requirements.
- [x] Components kan inneholde nested components.
- [x] Environment requirements kan propagere gjennom component-treet.
- [x] Tools og waits arver ikke sluttproduktets environment requirements.
- [x] `ignoreEnvironment()` kan stoppe propagation på en skjermet gren.
- [x] Concrete component definitions kan legges til senere uten å endre requirement-systemet.
- [x] `MaterialClass` er ikke nødvendig og er bevisst fjernet fra designet.

**Phase 03 – Capabilities and requirements: FERDIG.**
