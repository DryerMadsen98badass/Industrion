# Ion model

## Formål

Generere tillatte positive og negative ion states for hvert element.

## Foreslått datastruktur – ikke implementert

```java
record IonState(
    ElementId element,
    int charge,
    int electronCount,
    double formationEnergy,
    double stability,
    ValenceSignature valence
) {}
```

## Regler

- `charge > 0`: elektroner er fjernet.
- `charge < 0`: elektroner er lagt til.
- Et ion må passere stabilitets- og energikrav.
- Et compound må ha total tillatt ladning, normalt 0 med mindre systemet støtter polyatomiske ioner/salter eksplisitt.
- Ladning skal brukes av formula builder og reaction balancer.
