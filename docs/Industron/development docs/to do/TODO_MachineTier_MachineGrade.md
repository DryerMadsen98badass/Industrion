# TODO - MachineTier og MachineGrade system

## Mål

Behold `MachineTier` som systemet for de vanlige elektriske tierene:

- ULV
- LV
- MV
- HV
- EV
- IV
- LuV
- ZPM
- UV
- UHV
- UEV
- UIV
- UXV
- OpV
- MAX

Alternative maskintyper som Steam eller framtidige systemer skal ikke legges inn som elektriske `MachineTier`-verdier.

I stedet skal det lages et generelt `MachineGrade`-system.

---

## 1. MachineTier = elektrisk tier

`MachineTier` skal fortsatt representere elektrisk styrke/progresjon.

Eksempel:

```java
MachineTier.ULV
MachineTier.LV
MachineTier.MV
```

De elektriske tierene skal bruke følgende casing-grupper:

```text
ULV / LV / MV      -> variant_2
HV / EV / IV       -> variant_9
LuV / ZPM / UV     -> variant_21
UHV / UEV / UIV    -> variant_35
UXV / OpV / MAX    -> variant_41
```

Tre elektriske tiers bruker samme base-texture, men får forskjellige tier-farger.

---

## 2. Nytt generelt MachineGrade-system

Det skal lages et nytt system kalt for eksempel:

```java
MachineGrade
```

En `MachineGrade` representerer en alternativ variant av en maskin.

Eksempler kan være:

```text
Steam Copper
Steam Bronze
Phenomic
andre framtidige maskinvarianter
```

En grade skal IKKE være en elektrisk tier.

Den skal i stedet peke på hvilken elektrisk tier den tilsvarer.

---

## 3. MachineGrade skal inneholde

En grade skal kunne definere:

```text
id
displayName
baseTier
multiplier
casingTexture
optionalColor
```

Eksempel:

```java
public static final MachineGrade STEAM_COPPER =
        MachineGrade.builder("steam_copper", "Copper Steam")
                .baseTier(MachineTier.ULV)
                .multiplier(4.0)
                .casing("industron:block/casings/steam/copper")
                .build();
```

Og:

```java
public static final MachineGrade STEAM_BRONZE =
        MachineGrade.builder("steam_bronze", "Bronze Steam")
                .baseTier(MachineTier.ULV)
                .multiplier(2.0)
                .casing("industron:block/casings/steam/bronze")
                .build();
```

---

## 4. multiplier skal være generell

Det skal bare finnes:

```java
grade.multiplier()
```

Det skal IKKE finnes egne verdier som:

```text
durationMultiplier
steamMultiplier
euMultiplier
```

`MachineGrade` skal ikke vite hva multiplikatoren brukes til.

Det er selve maskinen som bestemmer hvordan `multiplier` påvirker den.

Eksempel:

```java
double multiplier = machine.grade().multiplier();
```

En Steam-maskin kan bruke denne verdien til for eksempel:

- processing
- steam input
- fluid consumption
- andre maskinspesifikke verdier

En annen maskintype kan bruke samme multiplier på en helt annen måte.

---

## 5. Steam input skal ligge i maskinen

Steam-systemet skal IKKE ligge inne i `MachineGrade`.

En Steam-machine skal selv definere:

```text
at den bruker steam
hvor steam kommer inn
hvordan steam consumption beregnes
hvordan grade.multiplier påvirker steam-bruk
hvordan grade.multiplier påvirker processing
```

`MachineGrade.STEAM_COPPER` skal bare si omtrent:

```text
baseTier = ULV
multiplier = 4
casing = copper steam casing
optional color
```

Selve maskinen bestemmer hva `4` betyr.

---

## 6. Steam-tiers / alternative tiers skal være eksplisitte

Steam skal IKKE automatisk ha én variant per elektrisk tier som ULV, LV, MV osv.

Vi skal bare definere de Steam-tierene som faktisk finnes.

Eksempel nå:

```text
Steam Copper
Steam Bronze
```

Begge kan for eksempel peke på `MachineTier.ULV` som base, men det betyr ikke at systemet automatisk skal lage:

```text
ULV Steam
LV Steam
MV Steam
HV Steam
...
```

Det skal ikke skje.

Hvis vi senere ønsker en ny Steam-tier, legger vi den eksplisitt til, for eksempel:

```java
public static final MachineGrade STEAM_ADVANCED =
        MachineGrade.builder("steam_advanced", "Advanced Steam")
                .baseTier(MachineTier.LV)
                .multiplier(...)
                .casing("industron:block/casings/steam/advanced")
                .build();
```

Poenget er:

```text
- Steam Copper er én definert tier/grade
- Steam Bronze er én definert tier/grade
- senere kan vi legge til flere
- det finnes ingen automatisk kobling der hver elektriske tier må få en Steam-casing
```

Når en Steam-maskin støtter flere av disse, kan den bruke de eksplisitt definerte gradene:

```java
SteamMachineDefinition.machine("macerator")
        .grades(
                MachineGrades.STEAM_COPPER,
                MachineGrades.STEAM_BRONZE
        );
```

Dette kan generere:

```text
Copper Steam Macerator
Bronze Steam Macerator
```

Hvis vi senere legger til enda en Steam-grade, kan den samme maskinen få den varianten uten at vi må bygge om systemet.

---

## 7. Casing og farge

Alle grades skal kunne ha egen casing.

Eksempel:

```java
.casing("industron:block/casings/steam/copper")
```

Farge skal være valgfri.

Eksempel:

```java
.color(0xAABBCC)
```

Hvis ingen farge er definert, skal texture kunne brukes uten ekstra tint.

Dette gjør det mulig å ha:

```text
Steam Copper -> egen casing
Steam Bronze -> egen casing
Phenomic -> egen casing
andre framtidige grades -> egne casings
```

Viktig: casingen tilhører den eksplisitt definerte graden. Vi skal ikke ha egne Steam-casings for ULV/LV/MV osv. med mindre vi faktisk lager slike Steam-grades senere.

---

## 8. MachineTier og MachineGrade skal være separate

Endelig struktur:

```text
MachineTier
= elektrisk tier

ULV
LV
MV
HV
...
MAX
```

Separat:

```text
MachineGrade
= alternativ maskinvariant

STEAM_COPPER
STEAM_BRONZE
PHENOMIC
...
```

En grade peker på en `baseTier`.

Eksempel:

```text
STEAM_COPPER
baseTier = ULV
multiplier = 4

STEAM_BRONZE
baseTier = ULV
multiplier = 2
```

---

## 9. Viktig designregel

Ikke hardkod logikk som:

```java
if (grade == STEAM_COPPER)
```

når det kan løses generelt.

Maskinen skal primært bruke:

```java
grade.baseTier()
grade.multiplier()
grade.casing()
grade.color()
```

Da kan nye grades legges til senere uten at hele maskinsystemet må bygges om.

---

## 10. Kort oppsummert

```text
MachineTier:
- kun elektriske tiers
- ULV -> MAX
- bestemmer elektrisk base/progresjon

MachineGrade:
- alternativ machine variant
- har baseTier
- har én generell multiplier
- har casing
- kan ha valgfri color

Machine:
- bestemmer selv hvordan multiplier brukes
- bestemmer selv steam/fluid/power input
- bestemmer processing-logikk
```

Målet er at systemet skal være generelt nok til at Steam, Phenomic og framtidige machine-varianter kan legges til uten egne hardkodede systemer.
