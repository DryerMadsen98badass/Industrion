# Casings

## FASTSATT modell

1. Materialets tier blir casingens tier.
2. Systemet beregner materialets properties.
3. Materialet testes mot alle casing profiles på sin egen tier.
4. Hver bestått profil genererer en egen casing variant.
5. Varianten bruker profilens base texture/model og materialets tint.

Et materiale kan altså generere null, én eller flere casing-typer.

## Eksempler på profiles

- High Pressure
- Heat Resistant
- Corrosion Resistant
- Cryogenic
- Structural
- Electrically Shielded

Den endelige listen er åpen.

## Gjeldende implementasjon

Dagens code path bruker `CasingDefinition`, `MaterialCasingRecipes`, `MaterialCasingGenerator` og `MaterialCasingAssemblyRecipes`. Casing-inputs bruker samme `Material`/`Component`/`Metal`- og requirement-semantikk som Assembly. Se `profile-definition.md` og `../recipes/assembly-recipes.md` for faktiske signaturer og materialbinding.
