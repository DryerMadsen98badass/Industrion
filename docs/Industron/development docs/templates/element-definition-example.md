# Element definition example

> **Foreslått API – ikke implementert.**

```java
elements.register(element(
    "varelium",      // id
    "Varelium",      // display name
    "Vr",            // symbol
    37,              // atomic number / proton count
    Tier.MV          // progression tier
));
```

Ikke legg til:

```java
.state(...)
.metal(...)
.color(...)
.parts(...)
.magnetic(...)
.toolMaterial(...)
```

Disse verdiene skal beregnes av systemet.
