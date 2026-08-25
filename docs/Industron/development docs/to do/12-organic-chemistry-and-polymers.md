# Phase 12 – Structural chemistry, plastics og polymers

Denne fasen bygger videre på molecular graph og compounds. Målet er ikke å kopiere et virkelig periodesystem eller ekte organisk kjemi. Målet er å ha nok structural rules til at polymer-/plastic-/elastomer-lignende generated substances får properties fra de fiktive elementenes bond structure i stedet for hardkodede materialnavn.

## 1. Chain/network families

- [ ] Klassifiser chain/network skeleton fra actual bond graph over de fiktive elementene.
- [ ] Støtt relevante single/double/triple-bond chain families uten å hardkode virkelige elementfamilier.
- [ ] Deriver plausible terminal/side bonding fra Phase 10 valence/bond rules.
- [ ] Ikke generer alle mulige structural variants ukontrollert; bruk requested/reachable generation og safety limits.

## 2. Functional/structural groups

- [ ] Implementer første typed functional-group recognizers som chemistry faktisk trenger.
- [ ] Første group-sett bestemmes fra hvilke fiktive structural motifs gameplay faktisk trenger.
- [ ] Functional groups kan påvirke polarity, acidity/basicity, boiling point, reactivity og reaction paths.

## 3. Polymer/repeating-unit model

- [ ] Definer polymer/repeating-unit representation som bygger på molecular/bond-systemet.
- [ ] Definer polymerization sites/bonds slik at monomers faktisk må kunne kobles kjemisk.
- [ ] Ikke hardkod «to organiske stoffer -> plastic».
- [ ] Begrens registry-backed polymers til requested/reachable definitions.

## 4. Plastic/elastomer classification

- [ ] Deriver `POLYMER`, `ELASTOMER` og eventuelle thermoplastic/thermoset classes fra structure/properties.
- [ ] Deriver insulation, flexibility, thermal limit, chemical resistance og mechanical properties fra samme property pipeline.
- [ ] En generated polymer skal automatisk kunne bli gyldig assembly-isolasjon hvis den faktisk oppfyller Phase 03 requirements.
- [ ] Ikke hardkod bestemte materialnavn som «good insulator»; qualification kommer fra generated properties/capabilities.
- [ ] Tillat tradeoffs: god isolator kan være for myk, for sprø, for varmefølsom eller chemically incompatible.

## Ferdig når

Generated organic compounds/polymers får structure-derived properties og kan automatisk kvalifisere eller feile som insulation, seals og andre components uten name-based special cases.
