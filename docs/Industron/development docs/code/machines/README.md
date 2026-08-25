# Machines

Det finnes ikke et ferdig machine API ennå. Dette dokumentet fastsetter ansvar og ønsket definisjonsnivå.

## En machine definition må beskrive

- ID og display name;
- hvilke process/recipe families den støtter;
- tilgjengelige tiers;
- item/fluid/data capabilities;
- om den er single block eller multiblock;
- UI/animation hooks;
- eventuelle Create-integrasjonspunkter.

Den skal ikke duplisere tierens energitall.

## Foreslått API – ikke implementert

```java
machine("chemical_reactor")
    .displayName("Chemical Reactor")
    .recipeFamily(ProcessFamily.CHEMICAL_REACTION)
    .tiers(Tier.ULV, Tier.LV, Tier.MV, Tier.HV)
    .inputs(ItemPort.class, FluidPort.class)
    .outputs(ItemPort.class, FluidPort.class);
```

## Runtime

Machine runtime leser den validerte recipe, kontrollerer effective tier og capabilities, trekker energy fra tier-profilen og oppdaterer progress. Kjemilogikken skal ikke ligge i block entity tick-metoden.
