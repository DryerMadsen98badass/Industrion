# Create integration

Create 6.0.10 er en runtime/integration provider for Industron på Minecraft 1.21.1. Domain-regler skal fortsatt eies av Industron.

## Boundary

Create-spesifikke adapters/hooks skal isoleres fra atomic/material/chemistry-kjernen.

Følgende eies av Industron:

- material properties,
- practical capabilities,
- ComponentDefinition trees,
- free/fixed Metal binding,
- Assembly requirements,
- Assembly tool total work time,
- recipe qualification.

Create leverer kinetic/runtime/automation der det er relevant.

## Kinetic/fluid integration

- Pump flow og pump kinetic stress er separate calculations.
- Create stress/RPM kan brukes av pumps/machines der gameplay krever det.
- Fluid transport-regler og material compatibility skal ikke flyttes inn i Create-klasser.

## Assembly Deployer integration

Create Deployer interagerer som FakePlayer.

Ekte spiller må holde et timed Assembly Tool kontinuerlig. FakePlayer kan ikke levere denne client hold-staten, så automasjon bruker pulse-work:

```text
1 FakePlayer/Deployer interaction = 2 Assembly tool ticks = 0.1 s
```

Toolens `useTimeTicks()` er fortsatt total required work.

Eksempel:

```text
10 ticks required / 2 ticks per activation = 5 Deployer activations
```

FakePlayer må ha riktig tool. Progress akkumuleres mellom aktiveringer, og durability brukes én gang ved full completion.

## Recipes og assets

Create recipes kan genereres fra samme validerte process/reaction-data som andre recipe types. Ikke dupliser chemistry logic i Create-specific generators.

Create assets kan brukes som source templates der lisens/prosjektstruktur tillater det, men generated Industron assets skal følge Industron sine egne structure/material conventions.

## Videre Create-arbeid beholdt fra roadmap

- Stabiliser version-specific block/model API rundt generated windows/structures.
- Hold Create dependency surface liten og dokumentert.
- Når chemistry recipes kobles på, unngå duplisert chemistry logic i Create recipe generators.
- Regression-test pipes, pumps, doors/trapdoors, kinetic stress og Blaze Burner ved Create-versjonsendringer.
