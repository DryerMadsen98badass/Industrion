# Elements

## Formål

Representere omtrent 100 fiktive grunnstoffer med minimale definisjoner og deterministisk beregnede atommodeller.

## FASTSATT definisjon

Et grunnstoff skal bare kreve:

- `id`
- `displayName`
- `symbol`
- `atomicNumber`
- `tier`

State, metal, color, parts/forms, magnetic variant og lignende er ikke tillatte elementfelt.

## Foreslått API – ikke implementert

```java
elements.register(element(
    "varelium",
    "Varelium",
    "Vr",
    37,
    Tier.MV
));
```

## Validering

- ID må være unik og gyldig.
- Display name kan ikke være tomt.
- Symbol må være unikt.
- Atomnummer må være innenfor prosjektets tillatte område og unikt.
- Tier må finnes i tier-registeret.
- Ukjente eller dupliserte verdier stopper oppstart med konkret feil.
