# Project structure

## Fast prosjektidentitet

- Minecraft 1.21.1
- NeoForge
- Java 21
- Mod ID: `industron`
- Base package: `net.mads.industron`

## Dokumentasjonsstruktur

```text
Industron/
  development docs/
    preferences/
    code/
    to do/
    templates/
```

## Ønsket domeneinndeling

Den faktiske prosjektstrukturen skal inspiseres før mapper flyttes. Ikke refactor bare for å matche denne skissen.

```text
src/main/java/net/mads/industron/
  registry/
  material/
    atomic/
    property/
    form/
    structure/
    ore/
    wire/
    fluidtransport/
  chemistry/
    compound/
    formula/
    rule/
    reaction/
    balancing/
  recipe/
  energy/
  machine/
  casing/
  multiblock/
  maintenance/
  integration/
    create/
  data/
  diagnostics/
```

## Ressurser

```text
src/main/resources/
  assets/industron/
    blockstates/
    models/
    textures/
      material_sets/
      structure_sets/
        wood/
        stone/
        metal/
    lang/
  data/industron/
    tags/
    ...
```

`structure_sets` er grayscale template assets. Wood/stone bruker coherent families; metal bruker generiske `metal_N` variants per block role.

## Avhengighetsretning

- Atomic/material property logic skal ikke avhenge av Minecraft recipe runtime.
- Chemistry skal kunne testes som domain logic uten world/block tick-kode.
- Recipe-laget konverterer validerte domain processes/reactions til Minecraft recipes.
- Machines bruker recipes; chemistry skal ikke ligge direkte i block entity ticks.
- `integration/create` skal ligge ytterst og ikke lekke Create-klasser inn i atomic/material/chemistry-kjernen.
