# Organic system

## 1. Formål

Denne mappen samler dokumentasjon for organiske materialer og senere organiske systemer i Industron.

Akkurat nå dokumenteres de fire organiske mellomstoffene som brukes som byggesteiner for wood/bamboo, samt det minimale `PlantMaterial` + fiber/string-fundamentet som nå finnes i kode. Se `plants.md`.

Full crop growth, environment, genetics, food, nutrition, farming og animals er ikke implementert. Plant-roadmapen ligger separat under `../../to do/plants/`, slik at framtidsdesignet ikke blandes sammen med faktisk runtime.

## 2. Fastsatte krav

### Organiske basisstoffer

Industron har foreløpig fire organiske compound-materialer:

- **Lignara** – wood-fiber-lignende strukturell fraksjon.
  - `.contains(REDSTONE 6, DRAXIL 5)`
- **Sylvara** – plant-fiber-lignende fraksjon med Cevora-komponent.
  - `.contains(REDSTONE 6, DRAXIL 5, CEVORA 1)`
- **Dulcara** – sugar-lignende organisk compound.
  - `.contains(REDSTONE 1, DRAXIL 1)`
- **Resyra** – carbon-rich resin-lignende fraksjon.
  - `.contains(REDSTONE 8, DRAXIL 2, CEVORA 1)`

`.contains(...)` skal alltid skrives som minste hele tall-forhold når forholdet kan reduseres.

### Wood og bamboo

Hver `WoodMaterial`-definition eier sin egen `.contains(...)`. Composition skal ikke hardkodes i den felles `wood(...)`-helperen.

Dagens wood- og bamboo-definisjoner bruker:

```text
Lignara 14
Sylvara 3
Resyra 2
Dulcara 1
```

Det vil si forholdet `14:3:2:1`.

At flere wood-typer foreløpig har samme forhold betyr ikke at systemet krever det. En bestemt wood kan senere få en annen composition direkte i sin egen definition dersom det er ønsket.

Ikke legg inn sjeldne metaller eller andre spesielle komponenter i wood uten at det er en eksplisitt materialbeslutning.

### PlantMaterial og fiber/string

`PlantMaterial.contains(...)` følger samme canonical composition-prinsipp. En direkte `Sylvara`-fraksjon er foreløpig bootstrap-valideringen for eksplisitt requested `FIBER`/`STRING` forms via `.parts(...)`. Det er ikke en permanent erstatning for senere full process analysis.

String har ingen strength-stat. Plant-string og vanilla string samles gjennom `#industron:strings`. Første konkrete plant-definition er ikke valgt ennå, så systemet gjetter ikke species-composition eller yields.

### Animal/organism parts

`OrganismDefinition` beskriver mobs som fysiske organismer med `part(...)`-deler. Hver part eier sin egen `.contains(...)`. Dette er composition-data, ikke bare loot-metadata.

Eksempel:

```java
public static final OrganismDefinition CREEPER = organism("creeper", "Creeper")
        .existing("minecraft:creeper")
        .rewriteExistingLoot()
        .part(MEAT).color(0x7F9A43)
            .contains(component(MUSCLE_PROTEIN,3), component(COLLAGEN,1), component(WATER,6), component(TOXIN,1))
            .loot(drop(RAW).count(1,3).lootingBonus(0,1))
        .part(BONE).color(0xD4C470)
            .contains(component(AnimalMaterials.BONE,1))
            .loot(drop(RAW).count(0,2).lootingBonus(0,1))
        .part(POWDER).color(0x4A4A4A)
            .contains(component(CHARCOAL,1), component(TOXIN,1), component(BONE_MINERAL,1))
            .existing(RAW, "minecraft:gunpowder")
        .build();
```

`POWDER` er en raw organism part for vanilla drops som allerede er pulver/residue/compound i sin dropped form. Det skal ikke defineres som `existing(DUST, "minecraft:gunpowder")`, fordi gunpowder ikke er "dust-formen av creeper"; det er creeperens raw powder part.

Dette lar vanilla loot-table fortsette å droppe `minecraft:gunpowder`, samtidig som Industron vet hva gunpowder består av via `.contains(...)`.

Regler:

- `.part(...)` velger fysisk del.
- `.contains(...)` beskriver hva den fysiske delen består av.
- `.existing(RAW, "...")` kobler en part til et vanilla item.
- `.loot(...)` legger til nye drops; `.existing(...)` alene sier ikke at en ny drop skal legges til.
- `.replaceDrop(...)` brukes når vanilla drop skal byttes til en annen part/form.
- Raw-only parts som `POWDER`, `SLIME`, `INK_SAC`, `EYE`, `MEMBRANE` og lignende skal ikke automatisk få dust/small dust/tiny dust-former.
- Powder-generering som `BONE -> BONE_DUST` skal bare brukes for powder-family parts som faktisk støtter det.

### Composition og videre prosessering

`.contains(...)` er funksjonell chemistry-data, ikke bare beskrivende metadata.

Når et compound inneholder andre compounds, gjelder compositionen ett nivå av gangen. Eksempel:

```text
Wood -> Lignara / Sylvara / Resyra / Dulcara
```

Lignaras egen `.contains(...)` brukes først når Lignara selv prosesseres videre. Nested composition skal ikke endre parent-forholdet `14:3:2:1`.

Automatiske process-intermediates skal bare registreres når en faktisk valgt processing-route trenger dem. Det skal for eksempel ikke finnes `Oak Slurry`, `Oak Pyrolysate` eller andre automatiske intermediates hvis ingen recipe-kjede bruker dem.

## 3. Input og output

### Input

Organic-systemet bruker registrerte materialdefinitions og deres `.contains(...)` som sannhetskilde.

Dagens organiske compounds bygges fra:

- Redstone
- Draxil
- Cevora

### Output

De fire organiske compounds kan brukes som komponenter i andre materials, blant annet `WoodMaterial`.

Når chemistry-systemet prosesserer wood, skal toppnivå-separasjon bevare definitionens ratio. For dagens standard-composition betyr 20 material units:

```text
14 Lignara
3 Sylvara
2 Resyra
1 Dulcara
```

Videre nedbrytning av disse fire stoffene skjer i egne senere steg ut fra deres egne `.contains(...)`-definitions.

## 4. Validering og feilmeldinger

- `.contains(...)` må referere til registrerte substances/materials.
- Ratios må være positive og bør være redusert til minste hele tall.
- Parent-ratio må ikke multipliseres med størrelsen på nested compositions.
- Total materialmengde skal bevares gjennom genererte process-recipes.
- Auto-intermediates skal ikke eksistere dersom ingen valgt route bruker dem.
- Nye organiske stoffer skal ikke legges til bare for å fylle ut systemet; de skal ha en faktisk rolle i et implementert system.

## 5. Oppstarts-/reload-fase

De organiske compound-definisjonene registreres sammen med øvrige materialdefinitions under normal material-bootstrap.

Wood- og bamboo-composition leses fra hver enkelt `WoodMaterial`-definition. Chemistry- og recipe-generation bruker deretter disse registrerte definitions som input.

## 6. Testkrav

- Verifiser at dagens wood-definisjoner beholder `14:3:2:1` på toppnivå.
- Verifiser at nested `.contains(...)` ikke endrer dette forholdet.
- Verifiser mass conservation gjennom automatisk processing.
- Verifiser at ubrukte slurry/solution/pyrolysate/fraction-intermediates ikke blir registrert.
- Verifiser at en ny `WoodMaterial` med en annen `.contains(...)` kan få sin processing beregnet fra den nye compositionen uten hardkodet species-logikk.

## 7. Åpne beslutninger

Ingen flere organiske basisstoffer er nødvendige for dagens wood/bamboo-scope.

Neste konkrete organic-arbeid er å definere første faktiske `PlantMaterial` med exact `.contains(...)` og fiber/string-yields. Full growth/environment/genetics følger roadmapen i `../../to do/plants/`. Nye organiske basisstoffer vurderes fortsatt bare når et konkret system trenger dem.
