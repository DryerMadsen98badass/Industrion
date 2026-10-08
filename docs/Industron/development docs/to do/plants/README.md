# Plants – separat roadmap

Denne mappen er en egen plant-roadmap ved siden av hovedfasene `00`–`23`. Den skal ikke presses inn i fase-nummereringen før plantesystemet faktisk blir en aktiv hovedfase.

## Implementert nå

Første minimumsfundament er laget for early-game fiber/string:

- `PlantMaterial` er en materialdefinition med canonical `.contains(...)`.
- `PlantPart` har foreløpig `PLANT`, `SEEDS`, `FIBER` og `STRING`.
- En plant-definition kan eksplisitt be om `FIBER`/`STRING` forms med `.parts(...)`; generatoren godtar disse bare når `.contains(...)` faktisk har en direkte `Sylvara`-fraksjon.
- `#industron:plant_fibers` samler plantefiber.
- `#industron:strings` samler plant-string og inkluderer `minecraft:string`.
- `HAND_PROCESSING` er en tierless recipe type for arbeid som gjøres direkte i hånden.
- Hand Processing starter bare når spilleren holder **Ctrl og høyreklikker**. Første klikk teller som første use. Etter start kan samme operation fortsette med høyreklikk så lenge input fortsatt matcher.
- Hand Processing bruker `uses`, ikke duration eller tool durability.

Ingen konkret plante er definert ennå. Det er med vilje: første plant-material skal først få en eksplisitt existing/generated plant-form, eventuell seed-form, eksakt `.contains(...)` og eksakte fiber/string-yields.

## Senere arbeid

- `00-plant-material-foundation.md` – canonical species/material data og forms.
- `01-growth-placement-propagation.md` – custom growth, seed/self/cutting propagation, harvesting.
- `02-environment-world-interactions.md` – `.at(...)`, named areas, nearby block requirements/modifiers og consume/convert/place.
- `03-crop-genetics.md` – individuelle genes for growth/yield/tolerance uten å gjøre material-composition tilfeldig.
- `04-organic-soil-processing.md` – composition-driven compost/dirt/organic routes med mass conservation.

## Låst designregel

`PlantMaterial.contains(...)` beskriver hva plantebiomassen **er**. Growth, placement, environment, propagation og genes skal senere bygges oppå denne identiteten og skal ikke erstatte eller randomisere canonical composition.
