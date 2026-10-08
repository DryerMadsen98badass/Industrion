# Plant material + early fiber/string

## Nåværende implementasjon

Plant-koden har foreløpig bare det lille fundamentet som early-game fiber/string trenger.

### `PlantMaterial`

`PlantMaterial` implementerer `IndustrialSubstance` og eier:

- `id`
- `displayName`
- `color`
- `.contains(MaterialComponent...)`
- optional `.existing(PlantPart, ResourceLocation)` mappings

Det finnes bevisst ingen growth, placement, environment, propagation eller genetics runtime ennå.

### `PlantPart`

Første forms:

```text
PLANT
SEEDS
FIBER
STRING
```

`PlantMaterialGenerator` genererer foreløpig bare eksplisitt requested `FIBER`/`STRING` forms fra `.parts(...)`, og validerer at materialets direkte composition inneholder `Sylvara`. `PLANT`/`SEEDS` er for definitions/existing mappings og senere plant content.

### Tags

```text
#industron:plant_fibers
#industron:strings
```

`#industron:strings` inkluderer også `minecraft:string`, slik at early tool/assembly recipes senere kan flyttes fra hardkodet vanilla string til én felles tag uten strength-stat.

### Textures/models

Generated fiber/string bruker delte grayscale base-textures som tintes med plant-materialets color. Det lager ikke species-spesifikke texture-filer.

## Hand Processing

`HAND_PROCESSING` er en tierless CE recipe type med maksimum 1 item input og 1 item output. Recipe-builderen har `Option.uses(int)` som serialiseres som `uses`.

Runtime-kontrakt:

1. spilleren holder input i main hand;
2. Ctrl + right-click starter en matching recipe;
3. første click teller som første use;
4. videre right-clicks teller videre så lenge input fortsatt matcher;
5. ved siste use konsumeres recipe-count og output gis;
6. ingen tool/durability/tier brukes.

JEI viser `Right-clicks: N` og `Hold Ctrl + right-click to start`.

## Ingen konkrete plants ennå

`PlantMaterials.ALL` er foreløpig tom. Dette unngår å gjette composition/yields. Første species legges inn først når vi har bestemt:

- existing/generated PLANT;
- eventuell SEEDS;
- exact `.contains(...)`;
- plant -> fiber recipe/yield;
- fiber -> string input/output/uses.

Se `../../to do/plants/` for senere growth/environment/genetics roadmap.
