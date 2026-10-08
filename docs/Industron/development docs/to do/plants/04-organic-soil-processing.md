# 04 – Organic soil/decomposition processing – senere

Dette skal **ikke implementeres nå**.

## Mål

Nye `PlantMaterial`-definitions skal senere kunne delta automatisk i compost/decomposition/soil-routes fordi `.contains(...)` beskriver plantens biomass, ikke fordi hver species får hardkodede recipes.

## Canonical rule

Systemet skal ikke anta:

```text
any plant -> dirt
```

En route må være fysisk/materialmessig definert og mass-conserving. Hvis dirt/soil/compost er materials med egen composition, må inputs, outputs, eventuelle mineral additions og byproducts balansere.

Mulig generell retning:

```text
plant biomass
-> organic/decomposed fraction
-> + passende mineral/soil material
-> compost/enriched soil/dirt-like material
```

Eksakt route bestemmes av process-systemet og registrerte materialdefinitions.

## Krav

- samme composition gir samme process choice;
- nye plants skal kunne følge route uten species-special-case;
- consumed material må ende i outputs/byproducts eller regenereres;
- auto-intermediates opprettes bare når en faktisk route bruker dem;
- nested `.contains(...)` beholdes ett nivå av gangen etter eksisterende organic-regel.
