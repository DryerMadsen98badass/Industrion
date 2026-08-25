# Phase 20 – Create integration – gjenværende arbeid

Create 6.0.10 er en required dependency og brukes allerede for kinetic machines, structures/doors, Blaze Burner og fluid integration.

- [ ] Dokumenter alle aktive Create touchpoints og hvilken Industron-domain abstraction de kobler til.
- [ ] Hold nye chemistry/material/assembly rules ute av Create-spesifikke klasser; bruk adapters/integration-lag.
- [ ] Map relevante `ProcessFamily`-operasjoner til Create recipe types bare når Create faktisk er riktig machine/runtime.
- [ ] Ikke dupliser chemistry/balancing-regler inne i Create recipe generators.
- [ ] Regression-test Create 6.0.10-integrasjon ved endringer i pipes, pumps, doors/trapdoors, kinetic stress og Blaze Burner.
- [ ] Verifiser at suppression av Create sine gamle material/transport recipes ikke skjuler funksjonelle exceptions som fortsatt skal være tilgjengelige, f.eks. Brass Funnel/Tunnel.

## Assembly Deployer/FakePlayer – implementert

Create Deployer bruker FakePlayer mot Assembly-runtime. Riktig Tool-item kreves fortsatt. FakePlayer trenger ikke ekte kontinuerlig hold-state, men får heller ikke gratis completion:

```text
1 Deployer/FakePlayer interaction = 2 ticks = 0.1 s tool-work
```

Toolens totale `useTimeTicks()` er uendret. Et 0,5 s / 10 tick step krever 5 faktiske deployer-aktiveringer. Progress akkumuleres mellom aktiveringer i aktiv runtime state, og durability trekkes én gang ved completion. Vanlige statkrav, materialbinding og recipe-order omgås ikke av Create.

## Ferdig når

Create fungerer som runtime/integration-provider, mens Industron fortsatt eier material-, chemistry-, recipe- og assembly-reglene.
