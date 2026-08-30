# Phase 08 – Composition graph og substance-identitet

`IndustrialSubstance` og `MaterialComponent` finnes allerede. Før alloys og compounds bygges må composition være canonical, rekursiv og sikker. Recipe types/process rules er definert i Phase 05–06, og Foundry/heat-grunnlaget ligger i Phase 07, slik at senere generated substances kan kobles til virkelige processer.

## Gjenstår

- [ ] Definer canonical composition representation med positive heltallsforhold og normaliserte fractions ved property calculations.
- [ ] Implementer recursive flattening fra substance -> elemental composition.
- [ ] Implementer cycle detection med konkret path i error-meldingen.
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
- [ ] Vurder senere generic item/fluid carriers med data components hvis spilleren skal kunne oppdage reelt vilkårlige substances runtime.

## Ferdig når

Enhver substance har stabil composition-identitet, kan flattenes uten cycles, og systemet skiller fysisk mixture fra en faktisk bonded/structured substance.

## Geology integration

Geology note: ore minerals use the same top-level composition graph. Their `.contains(...)` ratios are authoritative for recovery and waste-stream balance. See `22-geology-and-ore-generation.md`.
