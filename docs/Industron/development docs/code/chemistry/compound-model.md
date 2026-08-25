# Compound model

## Foreslått modell – ikke implementert

```java
record ChemicalFormula(Map<ElementId, Integer> atoms, int charge) {}

record Compound(
    CompoundId id,
    ChemicalFormula formula,
    BondModel bonds,
    CompoundProperties properties
) {}
```

## Opprettelse

Et compound kan oppstå fordi:

1. en recipe request ber om det;
2. en reaction rule krever det som mellomstoff eller byproduct;
3. et genereringsmål eksplisitt inkluderer det;
4. en natural-generation rule tillater det.

## Validering

- Alle elementer må finnes.
- Atomkoeffisienter må være positive heltall.
- Formula og charge må være kompatibel med binding-/ionregler.
- ID- og navnekollisjoner skal avvises.
