# Phase 11 – Compounds og ionic chemistry

Denne fasen bygger faktiske compounds oppå composition, Phase 01 ions og Phase 10 bond/structure model.

## 1. Chemical formula og compound identity

- [ ] Implementer typed `ChemicalFormula` med canonical ordering og net charge.
- [ ] Implementer `Compound` med composition, structure/bond model og derived `MaterialProperties`.
- [ ] Samme composition uten samme structure skal ikke nødvendigvis være samme compound.
- [ ] Canonical compound identity må være deterministisk.

## 2. Ionic compounds og salts

Et salt skal ikke genereres bare fordi to elements er valgt. Charges må faktisk passe sammen.

Eksempel:

```text
A supports A²⁺
B supports B⁻

charge neutrality:
1 x A²⁺ + 2 x B⁻ -> AB2
```

- [ ] Bruk allowed `IonState` fra Phase 01.
- [ ] Finn minste canonical integer ratio som gir total charge 0 for neutral salts.
- [ ] Tillat mindre preferred, men fortsatt allowed, ion state når det er nødvendig og energetically plausible.
- [ ] Ta formation cost/stability med i ranking når flere charge-combinations er mulige.
- [ ] Ikke generer charged bulk item variants for hvert ion; charge tilhører chemical species/compound state.

## 3. IonState vs oxidation state

- [ ] Skill konseptuelt mellom free/dissolved `IonState` og oxidation state for atom inne i en compound.
- [ ] De kan dele atomic/electron rules, men skal ikke behandles som identiske gameplay-objekter.
- [ ] Bulk elemental ingot/dust/block forblir nøytralt.

## 4. Covalent compounds

- [ ] Bruk Phase 10 structure graph for covalent compounds.
- [ ] Deriver properties fra bond strength/order, polarity, molecular structure og composition i stedet for bare parent-average.
- [ ] Validate total charge og electron feasibility.

## 5. Acidity/basicity og chemical compatibility

- [ ] Implementer compound-level acidity/chemical-balance behavior basert på compound/ion/structural data og den eksisterende generated atom/material-modellen.
- [ ] Koble chemistry-resultater til typed chemical-range/corrosion properties fra Phase 02–03 og CB-conventions fra Phase 05–06.
- [ ] Dette skal senere avgjøre om pipes, plates, seals og machine internals faktisk tåler stoffet.

## 6. Solutions og molten ionic media

- [ ] Definer hvordan dissolved ions representeres uten å registrere egne `+1`/`+2` material-form families.
- [ ] Definer charge-neutrality for bulk solution/molten salt state.
- [ ] Gjør dataene tilgjengelige for electrolysis og reaction-systemet senere.

## Ferdig når

Systemet kan lage og validere ionic/covalent compounds fra atomic/bonding rules, charge-balanse er korrekt, og salts/solutions kan representeres uten registry-eksplosjon av charged ingots/blocks.
