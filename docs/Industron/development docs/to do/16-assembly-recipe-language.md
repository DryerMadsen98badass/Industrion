# OBS: kanonisk Assembly API

Dette dokumentet inneholder eldre designnotater fra Phase 16. Den gjeldende, detaljerte API-retningen er dokumentert i:

`code/recipes/assembly-recipes.md`

Ved konflikt mellom eksemplene under og `code/recipes/assembly-recipes.md`, gjelder den nye Assembly-guiden.

## Gjeldende implementert kjerne

```java
.baseBlockInput(...)
.baseItemInput(...)
.baseBlockOutput(...)
.baseItemOutput(...)

.input(Material.PLATE, count)
.input(Material.PLATE, Metal.VERNIUM, count)
.input(Component.PLATE, count)
.input(Component.PLATE, Metal.VERNIUM, count)
.input("minecraft:redstone", count)
.input(Tool.PICKAXE)
.waitTicks(...)
```

`.input(Component.A, amount)` er free per material-leaf. `.input(Component.A, Metal.X, amount)` er fixed til `Metal.X` gjennom normale descendants. Vanlige `.stat(Stats.X).atLeast/atMost/exactly/range/...` requirements propageres til material-leaves i root-inputens tree. Fixed nested materialbinding kan brytes i `ComponentDefinition` med `inputAny(...)` + eventuelt `atLeastParent()`/`atMostParent()`.

Gjeldende generated Frame-eksempel:

```java
AssemblyRecipeDefinition.recipe("material/recipes/" + material.id() + "_frame")
        .baseItemInput(Material.VERY_LONG_ROD, material)
        .input(Component.VERY_LONG_ROD, material, 11)
        .baseBlockOutput(Material.FRAME, material)
        .build();
```

---

# Phase 16 – Assembly recipe language

Denne fasen definerer **hvordan vi skriver assembly recipes**. API-et under er målretning, ikke låst Java-signatur. Prinsippene er det viktige.

## 1. Start og finish

En assembly starter enten på en plassert block eller på et item/workpiece i Assembly Bench.

```java
.startBlock("industron:lv_machine_hull")
```

eller:

```java
.startItem("minecraft:some_item")
```

Final output er én eksplisitt final output:

```java
.finishBlock("industron:lv_wiremill")
```

eller:

```java
.finishItem("industron:electric_motor")
```

Strings er akseptable her fordi de faktisk er ResourceLocation/item/block IDs.

## 2. Alle input-typene assembly skal støtte

### A. Exact item ID

Når en bestemt item må brukes:

```java
.item("minecraft:redstone", 2)
```

### B. Item tag

Når alle items i en bestemt tag er gyldige, skal API-et ta en typed `TagKey<Item>` constant:

```java
.tag(REDSTONE_DUSTS, 2)
```

`REDSTONE_DUSTS` er en definert tag constant. Vi skal ikke skrive tag-navnet som fritekst overalt.

### C. Material part + requirements

Når formen er viktig, men materialet kan variere:

```java
.input(WIRE, 4)
    .require(ELECTRICAL_CAPACITY.atLeast(150))
```

Dette betyr enhver generated/existing `WIRE` med rating >= 150.

### D. Material part + class + requirements

```java
.input(RING, 4)
    .materialClass(ELASTOMER)
    .require(INSULATION_CAPACITY.atLeast(150))
```

### E. Reusable component

```java
.component(INSULATED_WIRE, 4)
    .require(ELECTRICAL_CAPACITY.atLeast(150))
```

Component-definitionen avgjør hvilke interne parts som trengs og hvordan requirementen propageres.

### F. Tool action

```java
.tool(SCREWDRIVER)
```

Recipe skriver bare hvilken tool type som kreves. Den skriver **ikke** tool speed eller durability.

### G. Fixed wait

```java
.waitTicks(20)
```

Dette er fysisk/process wait og påvirkes ikke av tool speed.

### H. Byproduct

```java
.byproduct("minecraft:string", 1)
```

Byproduct er intermediate output og må ikke forveksles med final result.

## 3. Requirement semantics – minimum betyr minimum

```java
.input(WIRE, 1)
    .require(ELECTRICAL_CAPACITY.atLeast(150))
```

Gyldige eksempler:

```text
149          -> invalid
150          -> valid
320          -> valid
1_000_000_000 -> valid, hvis rating-systemet og downstream dependencies kan håndtere det
```

Det finnes ingen «closest tier only»-regel.

## 4. Actual-value dependency

Det er avgjørende at dependent requirements bruker **det spilleren faktisk valgte**, ikke bare recipe minimum.

Eksempel component:

```java
ComponentDefinition.component(INSULATED_WIRE)
    .input(WIRE, 1)
        .expose(ELECTRICAL_CAPACITY)
    .dependentInput(RING, 1)
        .onePer(WIRE)
        .materialClass(ELASTOMER)
        .require(INSULATION_CAPACITY.atLeastFrom(WIRE, ELECTRICAL_CAPACITY));
```

Recipe:

```java
.component(INSULATED_WIRE, 4)
    .require(ELECTRICAL_CAPACITY.atLeast(150))
```

Hvis spilleren velger:

```text
Wire = 150  -> insulation >= 150
Wire = 190  -> insulation >= 190
Wire = 320  -> insulation >= 320
Wire = 1_000_000_000 -> insulation >= 1_000_000_000
```

Hvis tilgjengelige ring-ratings er 50, 150, 320 og 1000:

```text
wire 190 -> ring 320 eller 1000 er valid
wire 320 -> ring 320 eller 1000 er valid
```

Systemet velger ikke automatisk 320. Spilleren kan bruke enhver valid sterkere ring.

## 5. Environment/range requirements

Et component kan kreve at alle relevante materialdeler tåler samme miljø.

```java
ComponentDefinition.component(ACID_PIPE_SECTION)
    .environment(CHEMICAL_RANGE.covers(-32, 120))
    .input(PLATE, 2)
    .input(SCREW, 4)
    .tool(SCREWDRIVER);
```

Da gjelder:

- Plate må dekke `-32..120`.
- Screws/fasteners som blir igjen i produktet må også dekke `-32..120`.
- Nested components som blir eksponert skal arve range.
- Screwdriver trenger **ikke** chemical range; den er bare et tool og blir ikke del av sluttproduktet.

Samme mønster skal kunne brukes for temperature range, Chemical Balance/chemical environment, radiation eller andre framtidige environment requirements.

## 6. Material bindings

Når flere parts eksplisitt skal være samme material:

```java
.input(LONG_ROD, 1)
    .bindMaterial(SHAFT_MATERIAL)
.input(RING, 2)
    .sameMaterialAs(SHAFT_MATERIAL)
.input(SCREW, 4)
    .sameMaterialAs(SHAFT_MATERIAL)
```

Bindings skal være typed constants, ikke `"shaft_material"` strings.

## 7. Fullt eksempel

```java
AssemblyRecipe.recipe(LV_WIREMILL)
    .tier(LV)
    .startBlock("industron:lv_machine_hull")

    .component(INSULATED_WIRE, 4)
        .require(ELECTRICAL_CAPACITY.atLeast(150))

    .input(PLATE, 4)
        .require(STRUCTURAL_LOAD.atLeast(220))

    .item("minecraft:redstone", 2)
    .tag(SOME_VALID_CONNECTOR_TAG, 1)

    .tool(SCREWDRIVER)
    .tool(WRENCH)
    .waitTicks(20)

    .finishBlock("industron:lv_wiremill");
```

## 8. Definition validation

- [ ] Exact item IDs må finnes.
- [ ] Tag constants må være gyldige og ikke tomme når recipe krever dem.
- [ ] MaterialPart requirements må ha minst én theoretically valid/reachable kandidat.
- [ ] Component requirements må kunne tilfredsstilles av componentets exposed capabilities.
- [ ] Dependent requirements må være acyclic.
- [ ] Final output må være entydig.
- [ ] Ingen input skal konsumeres hvis current step ikke er valid.

## Ferdig når

Vi kan uttrykke exact items, tags, generated material parts, nested components, tools, waits, byproducts, scalar requirements, ranges, bindings og actual-value dependencies i ett typed API med autocomplete.
