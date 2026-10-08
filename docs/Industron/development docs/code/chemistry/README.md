# Chemistry

Dagens kode har en første implementert pipeline: material snapshots analyseres av `ChemistryEngine`, sammensatte dust routes klassifiseres/planlegges, safety-valideres og emitteres som CE-recipes. Full molecular/reaction/alloy-runtime er fortsatt roadmap.

## Prinsipper

- Ingen reaction får bryte bevaring av elementer.
- Charge skal balanseres der det er relevant.
- Reaksjonsprodukter skal være registrerte eller gyldig genererbare compounds.
- Manglende nødvendig byproduct eller co-reactant skal gi feil.
- Systemet skal være deterministisk.
- Ikke generer alle matematiske kombinasjoner av 100 elementer. Generer compounds som er etterspurt, reachable eller nødvendige for validerte reaction paths.

## Implementert nå

- `ChemistryBootstrap` og reports;
- `SeparationClassifier` og `ProcessPlanner`;
- automatic slurry/solution/reaction-mixture intermediates;
- `ProcessSemantics` phase checks;
- flattened elemental mass balance og conservative cycle rejection;
- `AutomaticChemistryRecipes` + `ReflectiveRecipeEmitter`.

## Gjenstår

- full bond-/reaction balancing for alle chemistry families;
- dynamic Foundry mixture payloads og separation concentrates;
- stoichiometric cycle validation som kan tillate kun eksakt balansert reversibilitet;
- threshold-, IO-, determinism- og dupe-hardening.
