# Phase 02 – Material properties – FERDIG

**Status: FERDIG.** `MaterialPropertyCalculator` og `MaterialProperties` er låst som sentral sannhetskilde for elementenes intrinsiske materialegenskaper. Senere alloys, compounds, polymers, recipes, casings og assembly skal bygge videre på disse dataene i stedet for å opprette parallelle hardkodede materialstats.

## Implementert og stabilisert

- [x] Sentraliserte materialberegninger i `MaterialPropertyCalculator`; `IndustrialMaterial` skal primært lagre/eksponere data.
- [x] Stabil conductor/insulator-modell:
  - metal -> conductivity > 0 og insulation strength = 0
  - non-metal -> conductivity = 0 og insulation strength > 0
- [x] `metal` og `gemCandidate` er gjensidig eksklusive.
- [x] State-grenser rundt 20 °C er kontrollert mot melting/boiling transitions.
- [x] `.color(...)` er visual override og påvirker ikke materialfysikken.
- [x] `dielectricStrength()` er beholdt kun som deprecated compatibility-alias til den sentrale insulation-egenskapen.
- [x] Z=1–100 elementsettet er validert med samme property-engine og brukes som representativ regression-baseline.
- [x] Property-diversity er kalibrert slik at materialer på samme tier får tydelige styrker/svakheter i blant annet conductivity, hardness, strength, heat, corrosion, pressure og magnetism.
- [x] Tier er fortsatt relevant for progression, men materialegenskaper er ikke lenger låst til smale ikke-overlappende tier-bånd; sterke materialer kan slå svakere materialer i høyere tier på enkeltområder.
- [x] Radioactivity er en separat intrinsisk property og er ikke automatisk synonymt med fissile/fuel-quality; framtidig nuclear physics kan utlede egne reactor-relevante egenskaper fra atomic/nuclear data.
- [x] Wire base amps er tier-skalert slik at lave tiers ikke får urimelig høy strømkapasitet, samtidig som conductivity fortsatt gir variasjon innen tier.
- [x] Gameplay-facing pressure/tank/pump/chemical/tool-relaterte verdier har én property-kilde; framtidig finbalansering hører til Phase 21 og skal ikke lage parallelle materialtabeller.
- [x] Intrinsiske materialegenskaper er de samme uansett form. `WIRE`, `PLATE`, `ROD`, `RING`, `PIPE` osv. endrer ikke selve materialets conductivity/strength/insulation/heat properties.

## Fast designregel: material property vs praktisk kapasitet

Et materiale har én intrinsisk property-verdi. En konkret del kan senere få en **praktisk capability/capacity** fra materialegenskap + geometri + brukskontekst, men dette er ikke en ny materialproperty.

Eksempel:

```text
material conductivity       -> intrinsisk, samme for alle former
wire electrical capacity    -> material conductivity + wire-geometri/thickness
material tensile strength   -> intrinsisk, samme for alle former
shaft load capacity         -> tensile/shear-relevant properties + shaft-geometri
```

Phase 03 eier typed capability-/requirement-laget som bruker disse verdiene.

## Videre balansetuning

Eksakte gameplay-tall kan fortsatt finjusteres i **Phase 21 – Validation, performance og balance** når flere systems bruker properties samtidig. Slik rebalance gjenåpner ikke Phase 02 så lenge source-of-truth, invariants og property-semantikken forblir uendret.

## Ferdig-kriterium

Oppfylt: rå materialproperties er stabile og deterministiske nok til at senere domain-systemer kan bruke dem som felles sannhetskilde uten parallelle hardkodede stats eller form-spesifikke intrinsic materialegenskaper.
