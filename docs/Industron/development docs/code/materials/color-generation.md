# Color generation

## FASTSATT

Farge skal beregnes fra atomsystemet og skal ikke være et påkrevd elementfelt.

## PLANLAGT

Fargen skal være deterministisk og bygge på flere signaler, for eksempel:

- atomnummer;
- shell/valence-signatur;
- metallicity;
- stabilitet og ionetilstand;
- bindingstype for compounds.

Atomnummeret skal ikke bare brukes direkte som tilfeldig RGB. Algoritmen bør generere en stabil base hue og justere saturation/value fra materialegenskapene. Compounds skal få farge fra binding og elektronisk struktur, ikke bare et naivt gjennomsnitt.

Generated casings bruker casing-archetypens texture og materialets beregnede tint.
