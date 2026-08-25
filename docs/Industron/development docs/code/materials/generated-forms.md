# Generated material forms

## FASTSATT

Forms skal i størst mulig grad bestemmes av materialtype + capabilities/rules, ikke en lang manuell parts-liste per element.

## Element/material forms

Eksempler på rule-baserte forms:

- dust / tiny dust / small dust
- ingot / hot ingot
- plate
- rod-varianter
- screw / bolt
- ring / spring
- gear / bearing / ball bearing
- wire thickness 1x/2x/4x/8x/16x
- magnetic variants når materialet faktisk er magnetiserbart
- gems og rough gems når gem classification tillater det
- fluid/gas/molten når material state/capability tillater det

Manglende form kan være en gyldig konsekvens av properties.

## Wood forms

Alle `WoodMaterial` skal kunne ha:

- Tiny Wood Pulp
- Small Wood Pulp
- Wood Pulp

Structure blocks genereres fra valgt `WoodModel` family. `existing(...)` kan erstatte enkeltroller med eksisterende registry objects.

## Stone forms

Alle `StoneMaterial` skal kunne ha:

- Tiny Dust
- Small Dust
- Dust

Molten stone er ikke automatisk obligatorisk; aktivering/API må avgjøres senere.

Structure blocks genereres fra valgt `StoneModel` family. `existing(...)` kan erstatte enkeltroller.

## Texture/system-regel

Registry existence og texture existence er separate problemer. En form som er gyldig etter materialreglene skal ikke forsvinne bare fordi en variant-folder mangler.
