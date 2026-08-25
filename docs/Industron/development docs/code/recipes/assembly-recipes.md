# Assembly recipes – canonical guide

**Status: IMPLEMENTERT / gjeldende API.**

Dette dokumentet er den kanoniske beskrivelsen av Assembly-systemet i dagens Industron-kode. Hvis eldre planfiler eller chat-notater sier noe annet, er dette dokumentet og faktisk kode fasit.

De viktigste klassene er:

```text
net.mads.industron.recipe.recipetypes.AssemblyRecipeDefinition
net.mads.industron.recipe.recipetypes.AssemblyPlan
net.mads.industron.recipe.recipetypes.ComponentDefinition
net.mads.industron.recipe.recipetypes.Component
net.mads.industron.recipe.recipetypes.Material
net.mads.industron.recipe.recipetypes.Metal
net.mads.industron.recipe.recipetypes.Stats
net.mads.industron.recipe.recipes.assembly.ComponentDefinitions
net.mads.industron.recipe.recipes.assembly.AssemblyRecipes
net.mads.industron.recipe.recipetypes.AssemblyRuntime
```

---

## 1. De tre navnene som ikke må blandes

Assembly-systemet skiller bevisst mellom **fysisk form**, **semantic component tree** og **materialidentitet**.

### 1.1 `Material.X` = fysisk materialform / leaf

Eksempler:

```java
Material.PLATE
Material.SCREW
Material.RING
Material.VERY_LONG_ROD
Material.FRAME
```

`Material.X` peker på en `MaterialPart` og er alltid en leaf.

Det betyr:

```java
.input(Material.SCREW, Metal.VERNIUM)
```

krever én fysisk Vernium Screw. Den ekspanderer **ikke** til Ring, Tool eller andre underkomponenter.

Det gamle `MaterialDefinition`-systemet finnes ikke i den gjeldende Assembly-arkitekturen. `Material.X` bygger aldri et tree.

### 1.2 `Component.X` = semantic assembly tree

Eksempler:

```java
Component.SCREW
Component.PLATE
Component.VERY_LONG_ROD
```

`Component.X` slås opp i `ComponentDefinitions` og ekspanderes rekursivt.

Gjeldende definitions:

```java
SCREW =
    Material.SCREW
    Material.RING
    Tool.PICKAXE

PLATE =
    Material.PLATE
    4 x Component.SCREW

VERY_LONG_ROD =
    Material.VERY_LONG_ROD
    8 x Component.SCREW
```

`Component.SCREW` betyr derfor ikke bare Screw-itemet. Det betyr hele det definerte assembly-treet.

### 1.3 `Metal.X` = materialidentitet / binding

Eksempler:

```java
Metal.VERNIUM
Metal.TITENITE
Metal.XAVREN
Metal.ANY
```

`Metal.X` bestemmer **hvilket IndustrialMaterial** en materialbundet branch skal bruke.

Dette må ikke forveksles med `Material.X`:

```text
Material.SCREW = formen Screw
Metal.VERNIUM  = materialet Vernium
```

---

## 2. Root input uten `Metal.X`: helt fri branch

Dette er en fast regel.

```java
.input(Component.PLATE, 2)
```

har implicit `Metal.ANY`.

Det betyr **ikke** at systemet velger ett tilfeldig metall og bruker det gjennom hele treet.

Det betyr at hver material-leaf i det ekspanderte component-treet kan velges uavhengig.

Hvis `Component.PLATE` er:

```text
PLATE
├─ Material.PLATE
└─ 4 x Component.SCREW
   ├─ Material.SCREW
   ├─ Material.RING
   └─ Tool.PICKAXE
```

kan en fri recipe i prinsippet godta:

```text
Vernium Plate
Yskel Screw
Zorixa Ring
```

så lenge hver fysisk leaf finnes og oppfyller kravene som gjelder den leafen.

Hver fri material-leaf får sin egen runtime binding. Valget av materiale for én fri leaf låser derfor ikke søsken eller descendants til samme materiale.

Det samme gjelder en fysisk Material-input uten eksplisitt metal:

```java
.input(Material.PLATE, 2)
```

Den inputen kan bruke hvilken som helst gyldig Plate.

---

## 3. Root input med `Metal.X`: hele normale treet låses

Når recipe eksplisitt angir et metal:

```java
.input(Component.PLATE, Metal.VERNIUM, 2)
```

blir hele den normale component-branchen materialbundet til Vernium.

Resultat:

```text
Vernium Plate
└─ Vernium Screw
   ├─ Vernium Screw
   └─ Vernium Ring
```

Alle nested `input(...)` uten override arver samme binding.

Hvis Vernium mangler en nødvendig fysisk form, kan ikke branchen løses.

Eksempel:

```text
Vernium PLATE  = finnes
Vernium SCREW  = mangler
```

Da er en fixed Vernium `Component.PLATE` ugyldig.

Dette er også mekanismen casing- og materialgenererte recipes bruker når hele assemblyen skal bruke samme materiale.

---

## 4. `ComponentDefinition`: normal inheritance

En normal definition skrives slik:

```java
public static final ComponentDefinition PLATE = component(Component.PLATE)
        .input(Material.PLATE)
        .input(Component.SCREW, 4)
        .build();
```

Ingen av disse linjene bestemmer et konkret metall selv.

De **arver materialmodusen fra caller**:

- fixed caller -> samme `Metal.X` nedover,
- free caller -> fortsatt fri materialvalg per leaf.

Det er derfor samme `ComponentDefinition` kan brukes både i en materiallåst casing og i en helt fri recipe.

---

## 5. `inputAny(...)`: eksplisitt brudd på en fixed branch

`inputAny(...)` brukes inne i `ComponentDefinition` når en branch med vilje skal få lov til å bryte en arvet fixed `Metal.X`-binding.

Eksempel:

```java
public static final ComponentDefinition SOME_PART = component(Component.SOME_PART)
        .input(Material.PLATE)
        .inputAny(Component.SCREW, 4)
        .build();
```

Hvis `SOME_PART` brukes med:

```java
.input(Component.SOME_PART, Metal.VERNIUM)
```

blir `Material.PLATE` fortsatt Vernium, men Screw-branchen blir fri:

```text
Vernium Plate
└─ Screw branch = free
   ├─ Screw kan være et annet materiale
   └─ Ring kan være enda et annet materiale
```

`inputAny` finnes både for material leaf og nested component:

```java
.inputAny(Material.RING)
.inputAny(Material.RING, 2)
.inputAny(Component.SCREW)
.inputAny(Component.SCREW, 4)
```

### Viktig

På recipe-root trenger vi normalt **ikke** en egen `inputAny`-metode. Dette er allerede free:

```java
.input(Component.SCREW, 4)
```

`inputAny` er først og fremst nyttig **inne i et tree som ellers er låst til `Metal.X`**.

---

## 6. Vanlige stat-krav på recipe inputs

Assembly recipe builder støtter typed requirements etter en `Material`- eller `Component`-input.

Eksempel:

```java
.input(Component.PLATE, 2)
        .stat(Stats.STRUCTURAL_STRENGTH).atLeast(50)
```

Kravet propageres til **alle material-leaves** i det ekspanderte component-treet.

Hvis PLATE inneholder Screw og Ring, må alle relevante material-leaves klare kravet.

Det gjelder også i en free branch. Materialene kan være forskjellige, men hver kandidat må tilfredsstille samme requirement.

Støttede numeriske former i vanlig recipe-stat API:

```java
.atLeast(value)
.atMost(value)
.exactly(value)
.range(min, max)
.covers(min, max)
```

`covers(...)` brukes for range-capabilities som `Stats.CHEMICAL`.

Property-requirements kan også brukes gjennom det eksisterende typed property-API-et.

Tools og waits er ikke material-leaves og testes ikke mot materialstats.

---

## 7. Relative stat-krav etter `inputAny(...)`

Når en fixed branch brytes med `inputAny(...)`, kan den frie branchen kreves å være minst eller maksimalt like god som parent-materialet.

Eksempel:

```java
.inputAny(Component.SCREW, 8)
        .stat(Stats.STRUCTURAL_STRENGTH).atLeastParent()
```

Hvis parent-bindingen er Vernium, betyr dette:

> Hver material-leaf under denne frie Screw-branchen må ha `STRUCTURAL_STRENGTH` minst tilsvarende Vernium for den samme fysiske formen.

For Screw-leafen sammenlignes kandidatens Screw-capability mot Vernium Screw.
For en descendant Ring-leaf sammenlignes kandidatens Ring-capability mot Vernium Ring.

Dette gjør at branchen kan bruke andre materialer uten at kvaliteten faller under parent-materialet på den angitte staten.

Støttet:

```java
.atLeastParent()
.atMostParent()
```

Ikke støttet for relative parent comparisons:

```text
range
covers
exactlyParent
```

Range er bevisst ikke definert for denne relative mekanismen.

### Relative krav trenger en fixed parent

Dette er gyldig:

```java
.input(Component.SOME_PART, Metal.VERNIUM)
```

hvor `SOME_PART` senere har:

```java
.inputAny(Component.SCREW)
        .stat(Stats.STRUCTURAL_STRENGTH).atLeastParent()
```

Caller gir da en konkret parent (`Vernium`).

Hvis hele parent-componenten allerede er free, finnes det ikke ett entydig parent-materiale å sammenligne mot. En parent-relative `inputAny`-regel i den konteksten er derfor ugyldig og compilation skal feile i stedet for å gjette.

---

## 8. Practical stats og fysisk form

Noen stats er form-spesifikke practical capabilities:

```text
Stats.STRUCTURAL_LOAD
Stats.SHAFT_LOAD
Stats.FASTENER_LOAD
Stats.ELECTRICAL_CAPACITY
Stats.INSULATION_CAPACITY
Stats.PRESSURE_CAPACITY
```

De beregnes fra både materialegenskapene og den konkrete `MaterialPart`-formen.

Eksempel:

```text
FASTENER_LOAD på SCREW != rå STRUCTURAL_STRENGTH
SHAFT_LOAD på VERY_LONG_ROD != SHAFT_LOAD på SHORT_ROD
```

Hvis en practical stat ikke er definert for den formen den brukes på, skal kravet ikke silently passere. Den kombinasjonen er ugyldig.

Derfor er dette et godt leaf-spesifikt eksempel:

```java
.inputAny(Material.SCREW)
        .stat(Stats.FASTENER_LOAD).atLeastParent()
```

Mens `inputAny(Component.SCREW).stat(Stats.FASTENER_LOAD)...` er ugyldig med dagens `Component.SCREW`, fordi requirementen også ville nå `Material.RING`, som ikke har `FASTENER_LOAD`. Bruk en stat som er definert for alle descendant forms, for eksempel `STRUCTURAL_STRENGTH`, når kravet skal gjelde hele Screw-componenten.

---

## 9. Base input og output ekspanderer aldri Components

Baseverdier er fysiske start-/sluttobjekter.

For materialgenererte recipes finnes typed overloads:

```java
.baseItemInput(Material.VERY_LONG_ROD, material)
.baseBlockInput(Material.FRAME, material)
.baseItemOutput(Material.SCREW, material)
.baseBlockOutput(Material.FRAME, material)
```

Her er `material` et konkret `IndustrialMaterial` fra generatoren.

Base input/output bruker `Material.X`, ikke `Component.X`.

Dette er bevisst. En base er det fysiske workpiece-et og skal ikke automatisk ekspandere et component tree.

---

## 10. Frame-recipen – gjeldende konkrete eksempel

Frame genereres dynamisk per kompatibelt materiale.

```java
AssemblyRecipeDefinition.recipe("material/recipes/" + material.id() + "_frame")
        .baseItemInput(Material.VERY_LONG_ROD, material)
        .input(Component.VERY_LONG_ROD, material, 11)
        .baseBlockOutput(Material.FRAME, material)
        .build();
```

`Component.VERY_LONG_ROD` er:

```java
public static final ComponentDefinition VERY_LONG_ROD = component(Component.VERY_LONG_ROD)
        .input(Material.VERY_LONG_ROD)
        .input(Component.SCREW, 8)
        .build();
```

Dermed bruker en frame:

```text
1 x Very Long Rod som base
11 x Component.VERY_LONG_ROD

Totalt:
12 x Very Long Rod
88 x Component.SCREW
```

Base-roden ekspanderer ikke og gir derfor ikke 8 Screws.

Siden generatoren binder de 11 component-rods til samme `IndustrialMaterial`, må alle normale nested parts være samme material.

Eksempel Vernium:

```text
Vernium Frame
├─ 12 x Vernium Very Long Rod
└─ 88 x Vernium Screw-components
   ├─ Vernium Screw
   ├─ Vernium Ring
   └─ Tool.PICKAXE
```

Hvis Vernium mangler `VERY_LONG_ROD`, `SCREW`, `RING` eller annen materialform som tree-et senere trenger, genereres **ingen Vernium frame assembly recipe**.

Det er ikke en hard programmeringsfeil at et materiale mangler en form. Det betyr bare at akkurat den materialbundne recipen ikke kan lages.

---

## 11. Generated recipe qualification

Generated material recipes skal ikke ha parallelle håndskrevne dependency-lister.

Bruk `ComponentDefinitions.canResolve(...)` / den kompilerte `AssemblyPlan` som sannhetskilde.

Det gir denne oppførselen:

```text
material har FRAME              -> nødvendig
material har baseform           -> nødvendig
hele fixed component tree løses -> nødvendig
```

Hvis en definition senere får en ny nested materialdel, følger feasibility-checken tree-et automatisk.

### Hard feil vs skip

**Skip recipe:**

```text
Vernium mangler Material.SCREW
```

**Hard programmeringsfeil:**

```text
Component.SCREW brukes, men ingen ComponentDefinition finnes
Component A -> B -> A cycle
ukjent fixed Metal.X
ugyldig parent-relative requirement
```

Silent failure på strukturelle definition-feil er ikke tillatt.

---

## 12. Exact item, tool og wait

### Exact item

```java
.input("minecraft:redstone", 2)
```

Dette matcher akkurat registry item-ID-en.

### Tool

```java
.input(Tool.PICKAXE)
```

Tool er en action/catalyst, ikke en consumed materialdel.

Tool type løses gjennom `AssemblyTools` til registrerte `ToolDefinition`-entries.

### Wait

```java
.waitTicks(20)
.waitSeconds(1.0)
```

Wait er en egen tidsoperasjon og har ingen materialbinding eller materialstats.

---

## 13. Timed tools – ekte spiller

Et `ToolDefinition` har `useTimeTicks`.

For en ekte `ServerPlayer` må spilleren:

1. bruke riktig tool,
2. holde høyreklikk kontinuerlig,
3. holde samme tool,
4. være nær assembly-posisjonen,
5. gjøre dette til `useTimeTicks` er nådd.

Slipp av høyreklikk, bytt tool eller gå for langt unna -> tool-progress avbrytes/resettes.

Durability brukes først når hele tool-steget er fullført.

---

## 14. Timed tools – Create Deployer / FakePlayer

Create Deployer bruker fake player. En fake player kan ikke levere samme kontinuerlige client-side hold-state som en ekte spiller.

Derfor har automasjon en eksplisitt pulse-modell:

```text
1 faktisk fake-player/deployer interaction = 2 tool ticks = 0.1 s tool work
```

Toolens normale `useTimeTicks` beholdes.

Eksempel:

```text
tool time = 10 ticks = 0.5 s
Deployer contribution = 2 ticks per activation
=> 5 deployer activations
```

Fake player får altså **ikke gratis instant tool use**. Den slipper bare real-player hold-state.

Progress akkumuleres mellom deployer-aktiveringer så lenge samme tool-definition brukes. Hvis automasjonen bytter til et annet gyldig tool med annen definition, startes tool-progress på nytt.

Durability brukes én gang når det komplette tool-steget er ferdig, ikke én gang per pulse.

---

## 15. World-block assembly og workbench assembly

Assembly runtime støtter begge starttyper.

### Block base

```java
.baseBlockInput(...)
```

Assembly kjøres på den plasserte baseblokken.

Første world-block input krever Ctrl for å skille assembly-start fra normal block-interaction/placement.

### Item base

```java
.baseItemInput(...)
```

Itemet plasseres visuelt på Assembly Workbench. Workbench holder start-item/result-state og assembly kan fortsette med vanlige inputs, tools og waits.

Base item er fysisk workpiece og ekspanderer ikke components.

Block output kan plasseres over workbenchen; item output blir workbench-resultat.

---

## 16. JEI-modellen

Assembly bruker to separate JEI-kategorier.

### `Assembly Products`

Viser sluttprodukt-recipen.

Den viser bare root-level inputs slik recipen er skrevet. Den flater **ikke** hele component-treet ut i ett gigantisk recipe-bilde.

Eksempel casing:

```text
Base Frame
6 x Plate
Output Casing
```

Nested Screw/Ring/Tool vises ikke direkte her.

### `Assembly Components`

Viser én `ComponentDefinition`-level om gangen.

Eksempel:

```text
Plate
-> 4 x Screw
```

Klikk videre på Screw:

```text
Screw
-> Ring
-> Pickaxe tool
```

Dette bevarer quantity og gjør dype trees navigerbare.

Relative `inputAny`-requirements vises som tooltip på den direkte inputen.

### Kontekst

Runtime er fasit for free/fixed materialvalg. JEI skal ikke flattene en fixed casing til alle mulige frie nested materialkombinasjoner.

---

## 17. Runtime bindings

`AssemblyPlan` pre-ekspanderer components til deterministic steps.

En material-step er enten:

```text
fixedMaterial != null
```

eller:

```text
fixedMaterial == null + dynamic bindingId
```

For free trees får hver material-leaf en egen dynamic binding ID. Runtime binder den leafen til materialet spilleren faktisk la inn.

Bindings lagres i aktiv workbench assembly-state slik at det valgte materialet for den konkrete leafen er stabilt når steget er matchet.

Fixed steps trenger ingen runtime materialbinding fordi materialet allerede er bestemt av planen.

---

## 18. Requirement propagation i `AssemblyPlan`

Recipe-level requirements sendes med når et Component ekspanderes.

Eksempel:

```java
.input(Component.PLATE)
        .stat(Stats.CORROSION_RESISTANCE).atLeast(70)
```

kompileres konseptuelt til:

```text
Material.PLATE  -> corrosion >= 70
Material.SCREW  -> corrosion >= 70
Material.RING   -> corrosion >= 70
```

Tools/waits får ikke requirementen.

Ved `inputAny(...).stat(...).atLeastParent()` blir parent-relative requirement først oversatt til en vanlig absolutt requirement for hver konkrete descendant `MaterialPart`, der parent-materialets tilsvarende form er terskelen.

---

## 19. Current Component library

Per denne docs-versjonen er følgende semantic components registrert:

```text
Component.SCREW
Component.PLATE
Component.VERY_LONG_ROD
```

Definitions:

```java
public static final ComponentDefinition SCREW = component(Component.SCREW)
        .input(Material.SCREW)
        .input(Material.RING)
        .input(Tool.PICKAXE)
        .build();

public static final ComponentDefinition PLATE = component(Component.PLATE)
        .input(Material.PLATE)
        .input(Component.SCREW, 4)
        .build();

public static final ComponentDefinition VERY_LONG_ROD = component(Component.VERY_LONG_ROD)
        .input(Material.VERY_LONG_ROD)
        .input(Component.SCREW, 8)
        .build();
```

Nye components skal legges i:

```text
src/main/java/net/mads/industron/recipe/recipes/assembly/ComponentDefinitions.java
```

Typed constants legges i:

```text
src/main/java/net/mads/industron/recipe/recipetypes/Component.java
```

---

## 20. Hvordan velge mellom `Material` og `Component`

Bruk:

```java
.input(Material.PLATE, ...)
```

når recipen krever **bare den fysiske platen**.

Bruk:

```java
.input(Component.PLATE, ...)
```

når recipen mener **Plate semantic assembly**, altså Plate + undercomponentene i `ComponentDefinitions.PLATE`.

Dette skillet er absolutt. Ikke legg hidden component-expansion på `Material.X`.

---

## 21. Hvordan velge mellom free og fixed metal

### Helt fri materialmix

```java
.input(Component.A, amount)
```

Hver material-leaf kan være forskjellig.

### Hele treet samme metal

```java
.input(Component.A, Metal.VERNIUM, amount)
```

Alle normale descendants bruker Vernium.

### Materialgenerert recipe

Generatoren har allerede et `IndustrialMaterial material`:

```java
.input(Component.A, material, amount)
```

Dette er convenience-overload for samme fixed semantics.

### Fixed tree med én fri underbranch

I `ComponentDefinitions`:

```java
.inputAny(Component.B)
        .stat(Stats.X).atLeastParent()
```

Parent-tree fortsetter fixed utenfor B, mens B blir fri under statkravet.

---

## 22. Anti-patterns

Ikke gjør dette:

```text
Material.X som hidden tree
MaterialDefinition
én shared Metal.ANY binding gjennom hele free component
hardkodet liste over descendant dependencies i hver generator
silent skip ved missing ComponentDefinition/cycle
fake player tool = instant complete
flatten hele nested tree i Assembly Products JEI
```

Bruk i stedet:

```text
Material = leaf
Component = tree
Metal = identity/binding
AssemblyPlan = canonical expansion
ComponentDefinitions.canResolve = generator feasibility
inputAny = explicit fixed-binding break
2 ticks per fake-player tool interaction
```

---

## 23. Frame paths

Frame recipe generator:

```text
src/main/java/net/mads/industron/material/recipes/MaterialFrameAssemblyRecipes.java
```

Component definitions:

```text
src/main/java/net/mads/industron/recipe/recipes/assembly/ComponentDefinitions.java
```

Assembly product aggregation:

```text
src/main/java/net/mads/industron/recipe/recipes/assembly/AssemblyRecipes.java
```

Runtime:

```text
src/main/java/net/mads/industron/recipe/recipetypes/AssemblyRuntime.java
```

JEI:

```text
src/main/java/net/mads/industron/integration/jei/assembly/
```

---

## 24. Kort mental modell

```text
Material.X
= fysisk leaf

Component.X
= recursive semantic tree

Metal.X
= fixed material identity

.input(Component.X, count)
= free tree; hver material-leaf kan velges uavhengig

.input(Component.X, Metal.Y, count)
= fixed tree; normal descendants arver Metal.Y

.inputAny(...) inne i ComponentDefinition
= bryt en inherited fixed binding for akkurat den branchen

.stat(...).atLeast(...)
= vanlig absolute requirement som propageres til material-leaves i recipe-inputens tree

inputAny(...).stat(...).atLeastParent()
= free branch, men alle descendant material-leaves må være minst parent-materialets tilsvarende form på staten

baseItem/baseBlock
= fysisk workpiece/result, aldri component-expansion
```
