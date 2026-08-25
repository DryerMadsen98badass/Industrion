# Phase 15 – Typed component definitions

**Status: KJERNE IMPLEMENTERT.** `ComponentDefinition` finnes i dagens kode. Seksjonene lenger ned om `expose`, `dependentInput`, `onePer` og component capability exposure er beholdt som framtidige designmål; de er ikke en beskrivelse av dagens Java-signatur.

## Gjeldende ComponentDefinition-modell

Typed steps er `MATERIAL`, `COMPONENT`, `ITEM`, `TOOL` og `WAIT`. `Material.X` er fysisk leaf; `Component.X` ekspanderes rekursivt. Dagens library inneholder `SCREW`, `PLATE` og `VERY_LONG_ROD`. `VERY_LONG_ROD` består av fysisk Very Long Rod + 8 × `Component.SCREW`.

Normal `.input(...)` arver callerens materialmodus: fixed `Metal.X` forblir fixed, mens en free caller forblir free per material-leaf. `inputAny(...)` bryter en inherited fixed binding for én branch. Etter `inputAny(...)` kan `.stat(Stats.X).atLeastParent()` eller `.atMostParent()` sammenligne alle descendant material-leaves mot fixed parent-materialets tilsvarende form. Relative range støttes ikke.

Missing nested `ComponentDefinition` og cycles er programmeringsfeil. At et konkret material mangler en fysisk form er derimot en feasibility-feil og kan gjøre at en materialgenerert recipe bare ikke opprettes. Full guide: `code/recipes/assembly-recipes.md`.

---

## Beholdte framtidige/historiske Phase 15-designmål

Machine components er reusable assemblies. `WIRE`, `RING`, `PLATE`, `ROD`, `SCREW` osv. er material forms/parts; `ELECTRIC_MOTOR`, `PUMP`, `CONVEYOR`, `ROBOT_ARM` osv. er components bygget av parts og eventuelt andre components.

## 1. Typed components

- [ ] Definer typed component constants med IntelliJ autocomplete.
- [ ] Definer `ComponentDefinition` som en DAG av subcomponents, material parts, tools, waits og byproducts.
- [ ] Ingen fritekst for kjente components, parts, properties, classes, tools eller tiers.
- [ ] Validate cycles, missing component, impossible requirement og duplicate ID ved load/datagen.
- [ ] La tier normalt arves fra parent assembly; bruk eksplisitt tier override bare når en component faktisk krever det.

## 2. Internal part relations

Et component skal kunne uttrykke at én faktisk valgt part skaper requirements for en annen part.

Eksempelretning:

```java
ComponentDefinition.component(INSULATED_WIRE)
    .input(WIRE, 1)
        .expose(ELECTRICAL_CAPACITY)
    .dependentInput(RING, 1)
        .onePer(WIRE)
        .materialClass(ELASTOMER)
        .require(INSULATION_CAPACITY.atLeastFrom(WIRE, ELECTRICAL_CAPACITY));
```

Meaning:

- Componentet bruker én wire.
- Den valgte wirens faktiske `ELECTRICAL_CAPACITY` eksponeres som componentets relevante capacity.
- Hver wire trenger én insulation ring.
- Ringen må være `ELASTOMER`.
- Ringens `INSULATION_CAPACITY` må være minst den **faktisk valgte wirens** capacity.

Hvis recipe minimum er 150, men spilleren velger en wire på 320, blir insulation requirement 320 – ikke 150.

## 3. Dependent counts

- [ ] Støtt `onePer(parent)`.
- [ ] Støtt faste multipliers som `twoPer(parent)`/`countPer(parent, n)` uten string expressions.
- [ ] Resolveren skal evaluere hver faktisk parent separat hvis inputs har forskjellige materials/ratings.
- [ ] Gjør samme mekanisme generell for:
  - wire -> insulation
  - shaft -> bearing
  - pipe -> seal
  - plate -> fastener
  - rotor -> shaft

## 4. Component capability exposure

For at en recipe skal kunne stille krav til et component uten å kjenne internstrukturen, må componentet kunne eksponere capabilities fra sine faktiske deler.

- [ ] Definer typed mapping fra component capability til én eller flere interne parts.
- [ ] Eksempel: `INSULATED_WIRE.ELECTRICAL_CAPACITY` kan komme fra den valgte `WIRE`.
- [ ] Eksempel: en `PUMP` sin `CHEMICAL_RANGE` kan være intersection av alle wetted parts.
- [ ] Eksempel: en motor sin max load kan være minimumet av shaft/bearing/winding-limits.
- [ ] Ikke hardkod en tilfeldig component-stat hvis den kan avledes fra de faktiske delene spilleren valgte.

## 5. Forward feasibility

- [ ] Når et parent-input skaper downstream requirements, sjekk at det finnes minst én registrert/reachable løsning før parenten konsumeres.
- [ ] Hvis wire 1_000_000_000 krever insulation >= 1_000_000_000 og ingen gyldig ring finnes, skal wiren avvises før consumption.
- [ ] Dette er en feasibility check på definitions/material-space, ikke et krav om at spilleren allerede har dependent item i inventory.

## Ferdig når

Et reusable component kan beskrive parts, dependency-counts og capability propagation uten hardkodede materialnavn, og sterkere faktiske inputs automatisk kan gjøre dependent requirements strengere.
