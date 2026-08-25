# Multiblocks

## Tierbegrensning

En multiblock skal ikke kunne kjøre en recipe over sin effective tier.

```text
effectiveCasingTier = laveste gyldige tier blant required structural casing positions
effectiveTier = min(installedEnergyTier, effectiveCasingTier)
canRun = recipeTier <= effectiveTier
```

Patternet skal angi hvilken casing profile som kreves på hver strukturell posisjon. Ability blocks og controller valideres separat og bestemmer ikke casing-tier bare fordi de har egne tiers.

## Flere casing profiles

Et pattern kan kreve ulike profiles i ulike soner, for eksempel High Pressure rundt reaction chamber og Heat Resistant rundt heating zone. Hver sone skal valideres mot sin profile og minimum tier.

## Feilrapportering

Ved ugyldig struktur skal controlleren kunne rapportere:

- manglende block-posisjon;
- feil casing profile;
- casing-tier under minimum;
- energy-tier under recipe requirement;
- ugyldig ability count.
