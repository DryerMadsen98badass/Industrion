# Tools, ores, wires og fluid transport

## Tools

Tool eligibility/stats skal senere utledes fra relevante mekaniske properties og tier. Høy hardness alene er ikke nok; toughness, brittleness, wear og edge retention må gi tradeoffs.

## Ores

- Ore = host stone + material ore overlay/tint.
- Hver gyldig host får både normal og small ore.
- Vanilla/Create stone hosts kan eksistere side om side med custom `StoneMaterial` hosts.
- Nye `StoneMaterial` skal automatisk bli tilgjengelige for ore-generatoren uten å endre hvert `IndustrialMaterial`.
- Ore eligibility for selve materialet må fortsatt bestemmes av material/geochemical rules; metal classification alene er ikke nok.

## Wires

- Voltage tier = material tier.
- Ampacity = funksjon av `electricalConductivity` og thickness.
- Thickness: 1x, 2x, 4x, 8x, 16x.
- Insulated capacity = 1.25x med nearest-integer rounding (.5 opp).
- Tooltip: Tier, CE, Amps.
- Ingen materialbasert gameplay resistance/loss-system.

Amp-kurven skal kalibreres slik at representative materials ikke får for høye verdier; Vernium 1x er ønsket omtrent rundt 4 A.

## Pipes

Pipe throughput bruker en material score dominert av:

- pressureResistance
- structuralStrength
- fractureToughness
- fatigueResistance

Ønsket throughput curve:

```text
score 50  -> 512 mB/t
score 100 -> 1024 mB/t
score 150 -> 2048 mB/t
score 200 -> 4096 mB/t
score 250 -> 8192 mB/t
score 300 -> 16384 mB/t
```

Chemical compatibility og temperature compatibility skal kunne stoppe transport selv om throughput ellers er høy nok.

## Pumps

Flow og kinetic stress er separate beregninger.

Flow målkurve:

```text
score 50  -> 4.0 mB/t/RPM
score 100 -> 8.0
score 150 -> 16.0
score 200 -> 32.0
score 250 -> 64.0
score 300 -> 128.0
```

`pumpFlowRate`: maks én desimal.
`pumpStressImpact`: heltallig SU/RPM.

Friction/mechanical properties skal kunne gi et materiale både høy flow og lav SU dersom materialprofilen tilsier det.

## Tanks

Tank score:

```text
0.40 structuralStrength
0.35 pressureResistance
0.15 fractureToughness
0.10 fatigueResistance
```

Capacity:

```text
5500 * 2^((tankScore - 50) / 50)
```

avrundet til hele mB.
