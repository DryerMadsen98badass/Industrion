# Industron Developer Guide

## Prosjekt

Industron kjører på Minecraft 1.21.1, NeoForge og Java 21. Create 6.0.10 brukes som integration/dependency der det passer.

Koden ligger under:

```text
net.mads.industron
```

## Materialer

`IndustrialMaterials`/material-systemet er kilden for registrerte materialer og forms. `MaterialProperties` og practical capability-resolvere skal brukes i stedet for lokale hardkodede strength/pressure/electrical-tabeller.

## Assembly – viktigste modell

Det finnes tre forskjellige concepts:

```text
Material.X  = fysisk MaterialPart leaf
Component.X = recursive semantic assembly tree
Metal.X     = materialidentitet/binding
```

Eksempel fysisk leaf:

```java
.input(Material.SCREW, Metal.VERNIUM)
```

Eksempel recursive component:

```java
.input(Component.PLATE, Metal.VERNIUM, 2)
```

### Free component

```java
.input(Component.PLATE, 2)
```

har ikke én felles hidden metal-binding. Hver material-leaf kan velges uavhengig, så lenge den finnes og klarer requirements.

### Fixed component

```java
.input(Component.PLATE, Metal.VERNIUM, 2)
```

låser hele normale Component-tree-et til Vernium.

### Nested binding break

I `ComponentDefinitions` kan en fixed branch brytes eksplisitt:

```java
.inputAny(Component.SCREW, 4)
        .stat(Stats.STRUCTURAL_STRENGTH).atLeastParent()
```

Den nested branchen blir free, men candidates må minst matche fixed parent-materialet på den valgte staten. `atMostParent()` finnes også. Relative range støttes ikke.

Full guide:

```text
Industron/development docs/code/recipes/assembly-recipes.md
```

## Frame

Gjeldende generated Frame recipe:

```java
AssemblyRecipeDefinition.recipe("material/recipes/" + material.id() + "_frame")
        .baseItemInput(Material.VERY_LONG_ROD, material)
        .input(Component.VERY_LONG_ROD, material, 11)
        .baseBlockOutput(Material.FRAME, material)
        .build();
```

`Component.VERY_LONG_ROD` = physical Very Long Rod + 8 Screw-components.

Generatoren ignorerer et materiale når hele fixed tree-et ikke kan resolve. Missing physical form er ikke en structural definition error.

## Assembly tools

Timed tools:

- ekte spiller: hold høyreklikk kontinuerlig,
- FakePlayer/Create Deployer: 2 ticks (0.1 s) work per faktisk interaction,
- total `useTimeTicks` beholdes,
- durability brukes én gang ved completion.

## JEI

Assembly har to kategorier:

- `Assembly Products` – sluttprodukt og direkte recipe-inputs,
- `Assembly Components` – én nested ComponentDefinition-level om gangen.

Ikke flatten hele component-treet inn i product-kategorien.

## Recipe types og machines

Vanlige process recipes og machine execution er separate fra hand/world Assembly. Ikke bland Assembly tool duration med machine process duration eller energy logic.

## Create

Create er integration provider. Industron eier material-/assembly-reglene. Create Deployer skal bare levere fake-player interaction pulses; den skal ikke omgå stat requirements, materialbinding eller tool total time.

## Om resten av docs-pakken

`Industron/development docs/to do/` beholder også eldre roadmap- og designnotater. De er nyttige for framtidige features, men implementert Assembly skal alltid leses fra `code/recipes/assembly-recipes.md` og siste prosjektkode.
