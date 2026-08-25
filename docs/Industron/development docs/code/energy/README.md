# Energy and tiers

## FASTSATT

Recipe-forfatteren skriver tier, ikke energi.

## Foreslått modell – ikke implementert

```java
record EnergyProfile(
    Tier tier,
    long machineInputPerTick,
    int defaultAmps,
    long capacityBaseline
) {}
```

```java
EnergyProfiles.register(Tier.MV, new EnergyProfile(/* TBD */));
```

Eksakte tall er ikke bestemt. Det skal finnes én sentral kilde for tier-energi, slik at balansering kan endres uten å redigere hver recipe.

## Effective machine tier

For multiblocks er øvre recipe-tier minst begrenset av:

```text
effectiveTier = min(installedEnergyTier, effectiveCasingTier)
```

En eksplisitt controller/machine cap kan senere inngå dersom designet krever det.
