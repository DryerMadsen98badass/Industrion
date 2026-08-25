# Naming and formatting

## IDs

- Registry/data IDs: lowercase `snake_case`.
- Element-ID: kort, stabil og språk-uavhengig.
- Symbol: 1–3 tegn, unikt, med definert case.
- Generated casing ID: `<profile>_<material>_casing`.
- Generated part ID: `<material>_<part>` dersom separate registry items brukes.
- Rule ID: `<namespace>:<domain>/<name>`.

## Java

- Classes: `PascalCase`.
- Methods/fields: `camelCase`.
- Constants/enums: `UPPER_SNAKE_CASE`.
- Korte chained builder-kall skal holdes kompakte og ikke brytes unødvendig over mange linjer.
- Ikke bruk forkortelser som gjør kjemi- eller fysikklogikk uklar.

## Terminologi

Bruk disse ordene konsekvent:

- `ElementDefinition`: brukerdefinert grunnstoffdata.
- `AtomicModel`: beregnet intern atomstruktur.
- `IonState`: en tillatt ladet variant.
- `MaterialProperties`: observerbare fysiske egenskaper.
- `MaterialForm`: dust, ingot, plate, screw, wire osv.
- `Compound`: stoff med to eller flere elementer, eller en definert molekylær struktur.
- `Reaction`: domenemodell for kjemisk transformasjon.
- `Recipe`: Minecraft-representasjon av en valid reaction/prosess.
- `CasingProfile`: funksjonell archetype, som High Pressure.
- `CasingVariant`: en casing generert fra profil + materiale.
