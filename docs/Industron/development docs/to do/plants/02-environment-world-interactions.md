# 02 – Environment og world interactions – senere

Dette skal **ikke implementeres i første fiber/string-leveranse**.

## Hovedidé

Plant environment skal følge samme deklarative mønster som multiblock world interactions der det passer. De eksisterende konseptene rundt relative positions, named areas og `BlockRequirement` er modellen som bør generaliseres/gjenbrukes fremfor å lage plant-spesifikke hardkodede checks.

## Relative positions

Plantens origin er referansepunkt:

```text
.at(0, -1, 0) = block direkte under planten
.at(1, 0, 0)  = block ved siden av planten
```

Krav skal kunne bruke konkrete blocks, tags, fluids og kombinasjoner som `anyOf/allOf/not`.

## Named areas

Planter skal kunne definere og gjenbruke områder som:

```text
soil
roots
water_zone
nearby
canopy
spread_area
```

Regler kan deretter bruke `.inArea("roots")` osv. Dette gjør det mulig å kreve eller telle flere blocks uten species-hardkodet radiuskode.

## Requirement vs modifier

To forskjellige semantics er nødvendige:

- **requirement**: planten kan ikke plantes/vokse når regelen ikke matcher;
- **modifier**: planten kan fortsatt vokse, men growth/yield/etc. påvirkes.

Eksempler som skal støttes:

- en bestemt block/tag direkte under planten;
- minst én water-block rundt soil;
- flere compost-blocks i root area gir større growth bonus opp til cap;
- avstandsbasert eller count-basert bonus;
- flere forskjellige block-effekter samtidig.

## World interactions

Multiblock-språket er også relevant for senere plant-events:

```text
REQUIRE
CONSUME
CONVERT
PLACE
```

Eksempler:

- `REQUIRE`: water/soil må finnes;
- `CONSUME`: en faktisk expendable nutrient resource brukes opp;
- `CONVERT`: rich soil kan gradvis bli depleted soil/dirt;
- `PLACE`: runners/shoots/spread kan lage nye plant-blocks.

Vanlig dirt skal ikke forsvinne bare fordi en crop vokser. `consume/convert` brukes bare når den konkrete plant-rule faktisk krever det.

## Plant events/phases

Senere rules må kunne bindes til tydelige plant-events, for eksempel:

```text
ON_PLANT
ON_GROWTH_CHECK
ON_GROWTH
ON_MATURE
ON_HARVEST
ON_BREAK
```

Dette skal bygges som generalisert world-interaction-infrastruktur, ikke kopieres blindt fra machine-kode.
