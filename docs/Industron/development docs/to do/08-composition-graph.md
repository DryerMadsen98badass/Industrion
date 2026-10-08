# Phase 08 – Composition graph og substance-identitet

`IndustrialSubstance` og `MaterialComponent` finnes. Dagens chemistry adapter analyserer composition, `ProcessSafetyValidator` flater rekursivt med cycle detection og bruker eksakte rational vectors for massebalanse. Foundry/heat-runtime finnes ikke ennå. Gjenstående arbeid er å gjøre canonical identity/signature til et gjenbrukbart offentlig domain-lag og støtte runtime dynamic mixtures.

## Gjenstår

- [ ] Definer canonical composition representation med positive heltallsforhold og normaliserte fractions ved property calculations.
- [x] Første recursive flattening og cycle detection finnes i chemistry/safety-pipelinen.
- [ ] Trekk flattening/cycle-path/signature ut som én delt canonical tjeneste for chemistry, Foundry og validation.
- [ ] Skill typed mellom `ELEMENT`, `MIXTURE`, `ALLOY`, `COMPOUND` og andre nødvendige substance kinds uten string-lookups.
- [ ] Skill substance-kind fra phase/structure: en alloy kan f.eks. være en metallic solid solution og er ikke bare «ALLOY = physical class».
- [ ] Sørg for at wood, stone, fluids/gases og generated substances fortsatt kan delta som `IndustrialSubstance` der det gir mening.
- [ ] Definer stable canonical identity/signature slik at samme composition + relevant structure ikke registreres flere ganger under forskjellige tilfeldige IDs.
- [ ] Hold visual name/color/texture atskilt fra chemical/composition identity.

## Mixture vs bonded substance

- [ ] En physical mixture beholder identities til komponentene og kan være kandidat for fysisk separation.
- [ ] En compound/alloy structure er ikke automatisk separerbar bare fordi flattened composition er kjent.
- [ ] Canonical identity må derfor etter hvert inkludere relevant structure/bond/phase-signature, ikke bare atomtall og ratio.

## Registry-strategi – må løses tidlig

Minecraft registry freeze betyr at vilkårlige nye item/block registry IDs ikke kan opprettes sent runtime.

- [ ] Ikke pre-generer alle matematiske kombinasjoner av elementer og ratios.
- [ ] Første anbefalte løsning: registrer eksplisitt requested/reachable substances før registration/datagen.
- [ ] Lag dependency/reachability pass fra definitions/reactions/recipes slik at bare nødvendige substances blir registry-backed.
- [x] Generic item/fluid carriers med data components er valgt retning for Foundryens udefinerte runtime mixtures.
- [ ] Implementer carrier-codec, payload limits, networking, tooltip og exact mass ledger.

## Ferdig når

Enhver substance har stabil composition-identitet, kan flattenes uten cycles, og systemet skiller fysisk mixture fra en faktisk bonded/structured substance.

## Geology integration

Geology note: ore minerals use the same top-level composition graph. Their `.contains(...)` ratios are authoritative for recovery and waste-stream balance. See `22-geology-and-ore-generation.md`.
