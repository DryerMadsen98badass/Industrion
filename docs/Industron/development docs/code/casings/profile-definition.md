# Casing definition – current API

**Status: IMPLEMENTERT.**

Material-derived casings bruker dagens `CasingDefinition` + `MaterialCasingGenerator`. Det gamle foreslåtte `casingProfile(...)`-API-et er erstattet og skal ikke gjeninnføres.

## Canonical paths

```text
src/main/java/net/mads/industron/material/recipes/CasingDefinition.java
src/main/java/net/mads/industron/material/recipes/MaterialCasingRecipes.java
src/main/java/net/mads/industron/material/recipes/MaterialCasingGenerator.java
src/main/java/net/mads/industron/material/recipes/MaterialCasingAssemblyRecipes.java
```

## Definition

En casing-family beskrives med typed Assembly requirements og assembly-inputs.

Gjeldende eksempel:

```java
public static final CasingDefinition MACHINE_CASING = casing("machine_casing")
        .displayName("Machine Casing")
        .texture("industron:block/structure_sets/casing/casings/variant_6")
        .tier(MachineTier.ULV)
        .materialStats(Stats.STRUCTURAL_STRENGTH).atLeast(78)
        .baseBlockInput(Material.FRAME)
        .input(Component.PLATE, 6)
        .build();
```

`materialStats(...)` er krav på selve casing-materialet. `input(...).stat(...)` er input-lokale krav og bruker samme typed requirement-system som vanlig Assembly.

## Qualification

`MaterialCasingGenerator` evaluerer alle registrerte `IndustrialMaterial` mot definitionen.

En materialvariant genereres bare når:

- materialets tier er minst casingens start-tier,
- casingens raw material requirements består,
- baseformen finnes,
- alle Material/Component-inputs kan løses,
- nødvendige Tool-typer finnes.

Manglende fysisk materialform gir **skip av den varianten**, ikke en strukturell programmeringsfeil.

Missing `ComponentDefinition`, cycles, ukjent fixed `Metal.X` eller annen ugyldig definition er fortsatt hard feil.

## Materialbinding i casing inputs

Normal casing-input uten override arver casing-materialet:

```java
.input(Component.PLATE, 6)
```

For en Vernium casing betyr det et fixed Vernium `Component.PLATE`-tree.

En eksplisitt override kan brukes:

```java
.input(Component.SOMETHING, Metal.X, count)
.input(Component.SOMETHING, Metal.ANY, count)
```

`Metal.ANY` følger dagens Assembly-semantikk: material-leaves i den frie branchen kan velges uavhengig.

## Assembly recipe conversion

`MaterialCasingAssemblyRecipes` konverterer hver kvalifisert generated casing til en vanlig `AssemblyRecipeDefinition`. Runtime og JEI trenger derfor ikke et separat casing-recipe-system.

Casing output er den eksakte generated casing-blokken for materialet.

## Generated content

Kvalifiserte casings brukes av eksisterende registry/datagen paths for block, item, lang, model og loot. Casing identity er deterministisk:

```text
<material-id>_<casing-definition-id>
```

Eksempel:

```text
vernium_machine_casing
```

## Source of truth

Ikke vedlikehold en separat liste over «materialer som får casing». `CasingDefinition` + `MaterialCasingGenerator` + Assembly component resolver er source of truth.
