# Materials - current canonical note

For current stone/wood/ore-source work, read `stone-wood-ore-model.md` and `../../to do/23-material-geology-autorecipe-integration.md` first.

Locked current rules:

- `MaterialPart` becomes the common form model for stone/wood.
- All existing Minecraft/Create family forms are mapped with `.existing(...)`.
- Stone and wood `.contains(...)` accept arbitrary registered substances as authoritative definitions.
- Every stone has a cobbled family and dust forms.
- Registered stones become ore hosts automatically.
- Natural resource worldgen is based on composed ore-source materials, not one elemental ore per element.

---

# Materials – målarkitektur

Et material/substance er en spillrepresentasjon som kan ha identity, derived properties, composition og generated forms.

## Element materials

Et fiktivt element defineres minimalt med:

- ID
- display name
- symbol
- atomic number
- tier

`atomicNumber + tier` er gameplay-input til material-property-systemet. Navn/symbol er identitet og skal ikke påvirke fysikken.

## Property pipeline

```text
ElementDefinition
  -> atomic/electron model
  -> MaterialPropertyCalculator
  -> MaterialProperties
  -> classifications/capabilities
  -> gameplay consumers
```

Consumers som wires, pipes, pumps, tanks, casings og tools skal lese `MaterialProperties` fremfor å definere egne parallelle materialstats.

## IndustrialSubstance og composition

Wood, stone, elementer, compounds/alloys, fluids/gases og relevante forms skal kunne representeres/refereres gjennom samme substance-abstraksjon.

Mål:

```java
.contains(
    component(VERNIUM, 5),
    component(XYLORA, 2),
    component(XENOLITE, 1)
)
```

Composition graph skal senere støtte rekursiv flattening til elements med cycle detection.

## WoodMaterial

- typed visual family, f.eks. `WoodModel.SPRUCE`
- `.contains(...)`
- `.existing(...)`
- generated wood structure family
- Tiny/Small/Normal Wood Pulp
- ingen faktisk tree growth før TreeDefinition finnes

## StoneMaterial

- typed visual family, f.eks. `StoneModel.DIORITE`
- `.contains(...)`
- `.existing(...)`
- generated full decorative stone family
- Tiny/Small/Normal Dust
- senere automatisk ore-host

## Structure templates

- `structure_sets/wood/<family>/`
- `structure_sets/stone/<family>/`
- `structure_sets/metal/<role>/metal_N/`

Templates er grayscale og tintes per material. Wood/stone holder coherent family; metal-design velges uavhengig per block role.

## Viktige regler

- Missing texture må ikke fjerne en ellers gyldig form fra registry.
- Metal og gem classification er mutually exclusive.
- `electricalResistance` skal ikke brukes som wire gameplay-property.
- Material tier kan brukes av progression/energy, men fluid transport stats skal komme fra relevante properties fremfor direkte tier-oppslag.

## Canonical property API

Andre systems skal lese material-properties gjennom den typed `MaterialProperties`-modellen. Eksempel:

```java
material.properties().structuralStrength()
material.properties().fractureToughness()
material.properties().pressureResistance()
material.properties().corrosionResistance()
material.properties().maxOperatingTemperature()
material.properties().electricalConductivity()
material.properties().fatigueResistance()
material.properties().frictionCoefficient()
material.properties().pipeThroughput()
material.properties().pumpFlowRate()
material.properties().pumpStressImpact()
material.properties().tankCapacity()
```

Ikke bruk string property keys som et parallelt API. Hvis `MaterialProperties` endrer navn, skal consumers og developer docs bruke det nye typed navnet.
