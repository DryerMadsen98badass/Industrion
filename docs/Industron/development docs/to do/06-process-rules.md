# Phase 06 – Process Rules og fysiske transformasjoner

RecipeType sier **hvilken prosess en machine utfører**. Process Rules sier **hva den prosessen fysisk har lov til å gjøre**. Dette må finnes før automatic chemistry generation, ellers kan systemet generere transformasjoner som ikke passer prosessen.

Én konkret machine bruker én RecipeType. Process Rules skal derfor beskrive semantics for den ene typen, ikke la én machine bli en samling av mange RecipeTypes.

## 1. Skill mixture fra bonded substance

- [ ] Definer typed structure/state som kan skille minst mellom fysisk mixture, solution/suspension, molten mixture, alloy/solid phase, molecular compound og ionic compound.
- [ ] Ikke anta at «samme elemental composition» betyr at materialene kan separeres mekanisk.
- [ ] En process vurderer substance structure, phases og bonds, ikke bare input/output-listen.

Fiktivt eksempel:

```text
Material A particles + Material B particles
-> physical mixture
-> kan potensielt separeres fysisk

Bonded Compound C
-> bonded chemical substance
-> kan ikke splittes til parent-atomer med en ren physical separator
```

## 2. Typed process operations

- [ ] Definer typed process-operation/capability keys, ikke strings.
- [ ] Eksempelretning:

```text
MIX_PHASES
SEPARATE_PHYSICAL_PHASES
SEPARATE_BY_DENSITY
FRACTIONATE_BY_BOILING_POINT
BREAK_OR_FORM_BONDS
TRANSFER_ELECTRONS
CHANGE_OXIDATION_STATE
FORM_IONIC_STRUCTURE
FORM_METALLIC_PHASE
CRYSTALLIZE
DISSOLVE
PRECIPITATE
```

Hver RecipeType har ett tydelig fysisk process-semantikk-sett som senere automatic generation kan targete.

## 3. Physical separation

- [ ] `CENTRIFUGING`, `FILTRATION`, `SIFTING`, `PHASE_SEPARATION` og beslektede typer skal bare separere states de fysisk kan skille.
- [ ] De kan ikke bryte bonds bare fordi output-materialene er kjent.
- [ ] En ferdig bonded alloy/compound er ikke automatisk reversibel til parent materials med en mechanical separator.

## 4. Distillation og fractionation

- [ ] `DISTILLATION` separerer en egnet fluid mixture etter boiling/volatility behavior.
- [ ] `FRACTIONATION` brukes når prosessen er definert som fractionation og skal ikke blandes inn som en ekstra type på samme machine.
- [ ] Distillation/fractionation bryter ikke chemical bonds i en bonded substance.
- [ ] Boiling-point proximity kan senere påvirke difficulty/duration uten å bli hardkodet i RecipeType.

## 5. Electrochemical processes

- [ ] `ELECTROLYSIS`, `ELECTROREFINING` og `ELECTROWINNING` får hvert sitt tydelige process-rule-sett.
- [ ] Electron transfer/oxidation-state changes skal bruke ion/bond/reaction-data når chemistry finnes.
- [ ] Recipe/machine kan kreve temperature, CB, tier, duration eller catalyst etter behov; RecipeType eier ikke slike konkrete values.

## 6. Mixing og alloying

- [ ] `MIXING` kan lage physical mixture uten å late som bonds eller metallic phase automatisk oppstår.
- [ ] `ALLOYING` kan senere danne en metallic/alloy phase når Phase 09 alloy-reglene sier at composition/state er gyldig.
- [ ] Powder mixture, molten mixture og finished alloy er forskjellige states.
- [ ] En ferdig alloy er ikke automatisk fysisk separerbar til parent materials.

## 7. Chemical reaction

- [ ] `CHEMICAL_REACTION` utfører et allerede validert reaction plan fra senere chemistry-system.
- [ ] Processen balanserer eller oppfinner ikke compounds selv.
- [ ] Temperature, CB, catalyst, state og andre eksisterende recipe/machine conditions kan avgjøre om en bestemt reaction path er tilgjengelig.
- [ ] Pressure brukes ikke som process-condition.

## 8. Chemical Balance

Legacy pH skal ikke være process-begrepet.

```text
CB < 0 = basic
CB = 0 = neutral
CB > 0 = acidic
```

- [x] Recipe/machine kan uttrykke nødvendig `chemicalBalanceRange(min, max)`.
- [x] CB er en condition på recipe/machine, ikke en egenskap hardkodet i RecipeType.
- [ ] Senere compounds/fluids kan få CB fra deres generated chemistry/material model uten at process-reglene trenger hardkodede stoffnavn.

## 9. Process selection contract

Senere chemistry skal kunne produsere et abstrakt behov som:

```text
requires electron transfer
requires bond breaking
reactants are in a compatible fluid/molten state
```

Process resolver velger bare en RecipeType hvis dens process rules dekker behovet.

- [ ] Definer typed `ProcessIntent`/operation-set.
- [ ] Resolver skal bare returnere process types som støtter alle nødvendige operations og input states.
- [ ] Dersom ingen process passer skal generation avvises med konkret diagnostic, ikke «nærmeste» fallback.
- [ ] Resolver returnerer RecipeType/process identity, ikke controller-navn eller power source.

## Ferdig når

En process kan forklare hvorfor en transformasjon er gyldig eller ugyldig, automatic chemistry kan velge én passende RecipeType uten machine-special-cases, og physical separators aldri brukes som universal bond-breakers.
