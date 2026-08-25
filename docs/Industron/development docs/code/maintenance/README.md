# Multiblock maintenance

## FASTSATT oppførsel

- Wear akkumuleres bare mens multiblocken kjører en recipe eller utfører aktivt arbeid.
- Omtrent hver 60. aktive driftsminutt utløses ett maintenance event.
- Ett event velger én tilfeldig maintenance block i den aktive multiblocken.
- Den valgte blokken mister nøyaktig 1 durability.
- Blokken kan repareres før den når 0.
- Ved 0 bryter den valgte maintenance blocken sammen.
- Sammenbruddet ødelegger også 2–5 tilfeldige ekstra blokker fra den validerte multiblock-strukturen.
- Ekstra blokker trenger ikke være maintenance blocks.
- Controller og alle ability blocks er ekskludert fra collateral destruction.

## Active time

Bruk akkumulert server-side active ticks, ikke veggklokke. Ved 20 TPS tilsvarer 60 minutter nominelt 72 000 aktive ticks. Lag skal derfor ikke gjøre maskinen raskere slitt i realtid.

## Tilfeldighet

Random selection skal skje server-side. Bruk en stabil seed-strategi basert på world, controller position og maintenance event index, eller en sikker server RNG som lagres korrekt. Klienten skal aldri bestemme failure.
