# API conventions

Dette dokumentet gjelder både **gjeldende typed API-er** og foreslåtte framtidige Java-API-er i developer docs. Når et system allerede er implementert, skal faktisk kode og systemets kanoniske guide brukes fremfor eldre pseudokode.

## 1. Typed references framfor string-lookups

Når et konsept allerede finnes som et Java-objekt, enum eller registrert constant, skal det refereres direkte.

Foretrukket:

```java
component(VERNIUM, 5)
.model(StoneModel.DIORITE)
.model(WoodModel.SPRUCE)
.inputs(VERNIUM, BASE_REAGENT)
```

Ikke bruk string-lookups for `VERNIUM`, `StoneModel.DIORITE` eller andre objekter som allerede finnes som typed Java-referanser.

Strings er fortsatt riktige for ekte identitets-/resource-data som selve registry-ID-en, display name eller resource location når API-et faktisk trenger dette, for eksempel:

```java
wood("xylora", "Xylora", color, WoodModel.SPRUCE)
casingProfile("high_pressure")
.baseTexture("industron:block/casing/high_pressure")
```

## 2. Material properties skal være typed methods

Property-navn skal ikke være string keys.

Foretrukket stil:

```java
properties
    .structuralStrength(0.45)
    .fractureToughness(0.35)
    .pressureResistance(0.50)
    .corrosionResistance(0.40);
```

Ikke:

```java
// Ikke bruk fritekstnøkler for properties.
// Property requirements skal uttrykkes med de typed metodene over.
```

Navn skal følge den kanoniske Java-modellen, normalt camelCase, eksempelvis `pressureResistance`, ikke `pressure_resistance`.

## 3. Ikke dikt opp et compatibility-API

Hvis docs beskriver et API som ennå ikke eksisterer, er det et designforslag. Før implementering skal siste prosjektkode inspiseres. Hvis et tilsvarende typed API allerede finnes, skal dokumentasjonen og implementeringen bruke det fremfor å legge til parallelle aliases.

Hvis brukeren skriver et foreslått API-navn med en skrivefeil, skal dokumentasjonen bruke det korrekte og konsistente Java-navnet.

## 4. Constants for material identity

Wood, stone, elements, alloys, compounds og andre substances skal kunne være ekte references:

```java
public static final WoodMaterial XYLORA = wood(
    "xylora",
    "Xylora",
    color,
    WoodModel.SPRUCE
);
```

Deretter:

```java
.contains(
    component(XYLORA, 5),
    component(VERNIUM, 1)
)
```

Ikke slå opp `XYLORA` igjen via en string når objektet allerede er tilgjengelig.

## 5. Assembly – gjeldende typed skille

Assembly har tre separate concepts som ikke skal aliases:

```java
Material.X   // fysisk MaterialPart leaf
Component.X  // recursive semantic component tree
Metal.X      // materialidentitet/binding
```

Root uten `Metal.X` er fri per material-leaf:

```java
.input(Component.PLATE, 2)
```

Eksplisitt metal låser normale descendants:

```java
.input(Component.PLATE, Metal.VERNIUM, 2)
```

Base input/output bruker fysiske `Material.X`-former, ikke `Component.X`. Full semantikk, relative parent-krav og `inputAny(...)` er dokumentert i `code/recipes/assembly-recipes.md`.
