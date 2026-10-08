# 01 – Growth, placement og propagation – senere

Dette skal **ikke implementeres i første fiber/string-leveranse**.

## Mål

`PlantMaterial` skal senere kunne kobles til svært custom world-behavior uten én spesialklasse per species.

Plantesystemet skal støtte både vanlige presets og custom behaviors, blant annet:

- crop-lignende staged growth;
- cactus/sugar-cane-lignende vertical/column growth;
- bush/ground/vine/aquatic/custom growth;
- max age/height og hvilke deler som faktisk vokser;
- harvest-self kontra separat produce;
- regrowth etter harvest;
- bonemeal-policy og lignende growth triggers.

## Propagation

Propagation skal være separat fra growth shape. Eksempler:

```text
SEEDS
SELF
CUTTING
SPORES
NONE
CUSTOM
```

`SELF` betyr at plant-itemet selv brukes som planting material, slik cactus/sugar cane kan gjøre. Det betyr ikke automatisk world-spreading.

Seeds skal være optional; en plant skal ikke få et unødvendig seed-item hvis propagation ikke bruker seeds.

## Arkitektur

Unngå én gigantisk `PlantMaterial` med mange booleans. Foretrukne deler:

```text
PlantMaterial = identity + .contains(...)
GrowthBehavior
PlacementBehavior
PropagationBehavior
HarvestBehavior
```

Vanlige presets kan være convenience builders. Custom behavior må være mulig uten å endre alle andre plants.
