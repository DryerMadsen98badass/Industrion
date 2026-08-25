# Phase 13 – Reaction system og balancing

Denne fasen bestemmer **hva som reagerer til hva**. Den genererer fortsatt ikke CE recipes direkte. Resultatet skal være et validert reaction/process plan som Phase 14 kan mappe til RecipeTypes fra Phase 05–06.

## 1. Reaction representation

- [ ] Definer typed `ReactionDefinition`/`ReactionCandidate` med reactants, products, catalysts og conditions.
- [ ] Reactants/products peker på canonical substances, ions eller phases – ikke tilfeldige string material names.
- [ ] Representer bond changes/redox/phase changes eksplisitt nok til at Process Rules kan vurdere dem.

## 2. Stoichiometric balancing

- [ ] Implementer deterministic stoichiometric balancer.
- [ ] Bevar hvert element/atom count.
- [ ] Bevar total electric charge.
- [ ] Normaliser coefficients til minste positive heltallsforhold.
- [ ] Reject impossible/underdetermined candidates med konkrete diagnostics.

## 3. Bond og redox validation

- [ ] Reaction kan ikke lage en product structure som Phase 10/11 anser ugyldig.
- [ ] Electron transfer/oxidation-state changes skal samsvare med charge conservation.
- [ ] Bruk ion formation cost, bond breaking/forming og substance stability til feasibility/difficulty score.
- [ ] Skill «mulig» fra «favorabel»; en dyr reaction kan være mulig men kreve høyere process conditions.

## 4. Catalysts og conditions

- [ ] Catalyst kan senke process difficulty/activation requirement uten å endre mass/charge-balanse.
- [ ] Temperature, Chemical Balance (CB), solvent/state og atmosphere kan være feasibility conditions der gameplay trenger dem.
- [ ] Catalyst skal ikke konsumeres med mindre reaction-definition eksplisitt sier det.

## 5. Reaction graph og safety

- [ ] Implementer `ChemistryRule` registry med stabile phases/priorities.
- [ ] Implementer reaction graph over reachable substances.
- [ ] Beskytt mot combinatorial explosion og infinite dependency chains.
- [ ] Missing compound/co-reactant/byproduct skal gi diagnostic, ikke silent fallback.

## 6. Process intent output

Reaction-systemet skal returnere hva transformasjonen **krever**, ikke maskinnavnet.

Fiktiv eksempelretning:

```text
Bonded Compound X -> Product A + Product B
requires:
- break/form bonds
- electron transfer / redox dersom structure-planen krever det
```

Phase 06 kan da finne en kompatibel electrochemical RecipeType når operation-settet krever det. En physical separator matcher ikke bond-breaking/electron-transfer-settet og blir derfor aldri valgt.

## Ferdig når

Reactions er atom- og charge-balanced, products er structurally valid, og hver reaction kan beskrive nødvendige process operations/conditions uten å kjenne konkret machine/controller.
