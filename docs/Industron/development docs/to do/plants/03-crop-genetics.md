# 03 – Crop genetics – senere

Dette skal **ikke implementeres nå**.

## Species vs individual

`PlantMaterial` beskriver arten/materialet og beholder canonical `.contains(...)`.

Et senere `PlantGenome` beskriver egenskapene til ett seed/plant-individ. To individer av samme `PlantMaterial` kan derfor ha forskjellige genes uten at materialidentity blir forskjellig.

## Første gene-kategorier

Aktuelle senere genes:

```text
growth
yield
fiber yield
seed yield
environment tolerance
quality / andre konkrete gameplay traits
```

Kun genes som får en faktisk gameplay-funksjon skal implementeres.

## Determinisme

Genes skal ikke tilfeldig omskrive artens chemistry-composition. Eksempel: en høy `fiber yield` gene kan gi mer høstbar biomass/fiber eller bedre extraction efficiency innen definerte grenser; den skal ikke gjøre samme PlantMaterial til en tilfeldig ny `.contains(...)` hver generation.

Senere modell:

```text
species base behavior
× environment modifiers
× genome modifiers
= faktisk growth/yield
```

Inheritance, mutation, crossing og gene storage bestemmes først når genetics blir en aktiv feature.
