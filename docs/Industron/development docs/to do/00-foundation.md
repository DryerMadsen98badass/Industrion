# Phase 00 – Foundation – FERDIG

**Status: FERDIG.** Denne fasen ble implementert før Phase 01 og skal bare gjenåpnes hvis selve validation-/diagnostics-arkitekturen endres.

## Implementert

- [x] Felles typed diagnostics/error-model med subsystem, severity og konkrete validation codes.
- [x] Felles `ValidationPipeline` som kan brukes av materialer, structures, recipes, chemistry og senere assembly.
- [x] Registry/domain validation for duplicate IDs og andre identity-kollisjoner.
- [x] Graph validation med unknown-reference og cycle-detection som kan gjenbrukes av composition og component/assembly graphs.
- [x] Pure-Java domain invariant tests uten world/server-runtime.
- [x] Determinism checks for sentrale material-/generation-paths.
- [x] Representative atomic boundary-tests, inkludert positive atomnumre helt opp til `Integer.MAX_VALUE`.
- [x] Create-boundary er definert slik at ren material/chemistry/assembly-domainkode ikke skal eie Create-regler; konkrete adapters/blocks kan integrere med Create.
- [x] Developer docs skal oppdateres når en FASTSATT beslutning endres. Dette er en vedvarende prosjektregel, ikke en egen blocker.

## Videre bruk

Alle nye faser skal registrere sine validatorer i samme pipeline i stedet for å lage parallelle validation-systemer. Errors som gjør data ugyldige skal være fatal; content som tilhører en senere fase kan rapporteres som warning når det er riktig.

## Ferdig-kriterium

Oppfylt: nye domain-systemer kan valideres tidlig og deterministisk med konkrete diagnostics uten å kreve Minecraft world/runtime for ren logikk.
