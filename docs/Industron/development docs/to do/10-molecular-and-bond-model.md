# Phase 10 – Molecular structure og bond model

Composition forteller **hvilke atomer** som finnes. Denne fasen forteller **hvordan de er bundet sammen**. Det er nødvendig fordi samme chemical formula kan ha forskjellige structures og properties, og fordi chemistry ikke må lage molecules som bryter electron/valence-reglene fra Phase 01.

## 1. Molecular graph

- [ ] Definer canonical molecule/structure graph med typed atoms/nodes og bonds/edges.
- [ ] Nodes peker på element identity og eventuell relevant formal charge/oxidation context.
- [ ] Structure identity må være deterministisk og uavhengig av insertion order.
- [ ] Skill chemical formula fra structure identity; samme formula kan representere flere structures/isomers.

## 2. Bond types og bond order

- [ ] Definer typed bond families minst for covalent, ionic relation/context og metallic bonding der det er relevant.
- [ ] Covalent bonds skal støtte:

```text
SINGLE
DOUBLE
TRIPLE
```

- [ ] Bond order skal konsumere riktig bonding/valence capacity.
- [ ] Ikke tillat tilfeldig double/triple bond bare fordi to atoms finnes i samme composition.
- [ ] Aromatic/resonance-lignende modell kan komme senere hvis første chemistry-sett ikke trenger det, men API-et må ikke blokkere utvidelse.

## 3. Valence og electron feasibility

- [ ] Bruk Phase 01 atomic/ion data som source of truth for plausible bonding.
- [ ] Validate atom valence/electron budgets i hele molecule graph.
- [ ] Tillat alternative plausible structures bare når de faktisk oppfyller bonding-reglene.
- [ ] Ranger structures deterministisk etter stability/formation cost når flere er gyldige.

## 4. Structural classification

Systemet skal kunne **utlede structural families fra graph**, ikke fra hardkodede real-element-navn eller formulas.

Fiktiv eksempelretning:

```text
A-B    -> single bond structure
A=B    -> double bond structure
A≡B    -> triple bond structure
```

- [ ] Structural family/classification må bruke actual bond graph.
- [ ] En family som krever double/triple bond skal bevise det fra graphen.
- [ ] Ikke hardkod bestemte ekte chemical formulas som sannhetskilde.

## 5. Functional-group foundation

- [ ] Gjør structure queries mulig for structural motifs/groups som senere chemistry faktisk trenger, uttrykt over de fiktive atom- og bond-typene.
- [ ] Første implementasjon trenger bare grupper som faktisk brukes av de første chemistry/processene.
- [ ] Structural groups skal kunne påvirke acidity/chemical behavior, polarity, boiling behavior, polymerization-like reactions og derived properties senere.

## Ferdig når

Systemet kan avgjøre om en proposed molecular structure er electron/valence-messig gyldig, skille single/double/triple bonds, og representere samme formula med forskjellig structure uten identity-kollisjon.
