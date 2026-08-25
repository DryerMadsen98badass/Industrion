# Recipes

Recipes er Minecraft-representasjoner av validerte prosesser og reactions.

## FASTSATT

- Vanlige recipe-definisjoner skal angi tier.
- De skal ikke angi et manuelt energitall.
- Tierens `EnergyProfile` gir maskinens faste energy input/rate.
- Kjemiske recipes må være balanserte og validerte før de registreres.
- Registrerte materials/substances skal refereres med typed constants/objects, ikke med string-lookups i det foreslåtte Java-API-et.

## Foreslått typed request – ikke implementert

```java
recipeRequest("varelium_salt")
    .process(ProcessFamily.CHEMICAL_REACTION)
    .tier(Tier.MV)
    .inputs(VERNIUM, BASE_REAGENT)
    .target(VARELIUM_SALT);
```

Dette er foretrukket fremfor API-er som `element("varelium")` eller `compound("base_reagent")`. Når objektet allerede finnes som en registrert constant, skal constanten brukes direkte.

Generatoren kan legge til nødvendige co-reactants og byproducts, men bare når rules og registrerte formulas krever dem.

## Assembly recipes

Den kanoniske guiden for dagens Assembly API, casing-genererte Assembly recipes, `Part`, `MaterialPart`, `Material`, `Component`, `Metal`, block/item base/output og detaljerte eksempler ligger i:

- `assembly-recipes.md`

Når eldre forslag i `to do/16-assembly-recipe-language.md` avviker fra denne guiden, er `code/recipes/assembly-recipes.md` og faktisk kode fasit.
