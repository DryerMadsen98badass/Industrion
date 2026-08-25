# Phase 01 – Atomic model og ions – FERDIG

**Status: FERDIG.** Neutral atom model og ion model bruker nå samme atom-/electron engine. Ingen recipe types eller chemistry recipes hører hjemme i denne fasen.

## Implementert

- [x] Dynamisk neutral atom structure for positive Java `int` atomnumre.
- [x] Felles `AtomicModel`/atomic state som eier electron configuration i stedet for duplisert logikk.
- [x] Typed `IonState` for positive og negative ioner.
- [x] Ion charge påvirker electron count, men ikke proton/neutron identity.
- [x] Flere kjemisk tillatte ion states kan eksistere for samme element.
- [x] `preferredIonState` er bare beste/default state; chemistry kan senere bruke andre tillatte states når charge/bonding krever det.
- [x] Ion state inneholder blant annet charge, electron count/configuration, stability, formation cost og viability.
- [x] Neutral atoms og ions bruker samme electron-configuration source of truth.
- [x] Tier påvirker ikke atomidentitet, electron configuration eller ion identity.
- [x] Determinism/invariant validation for representative elementer, ioner og svært høye atomnumre.
- [x] Electron arithmetic håndterer edge case der negativ ion på `Integer.MAX_VALUE` krever mer enn signed-int electron count.

## Fast designregel

Vanlige material forms er bulk/nøytrale materials. Phase 01 genererer **ikke** egne `+1`/`+2` ingots, nuggets, ores eller blocks.

Eksempel:

```text
Vernium Ingot   -> neutral bulk Vernium
Vernium Dust    -> neutral bulk Vernium
Vernium Block   -> neutral bulk Vernium

Vernium²⁺       -> chemical species/state brukt senere av chemistry
```

`IonState` skal senere brukes av compounds, solutions, molten salts, electrolysis, redox og reaction balancing. Det er chemistry-data, ikke en ny `MaterialPart`-variant.

## Ferdig-kriterium

Oppfylt: samme atomic identity gir deterministisk neutral/ion model, flere ions kan vurderes uten å duplisere atomlogikk, og tier kan ikke endre hva selve atomet er.
