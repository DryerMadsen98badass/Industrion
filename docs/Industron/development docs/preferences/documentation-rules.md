# Documentation rules

Alle systemdokumenter skal inneholde:

1. Formål.
2. Fastsatte krav.
3. Input og output.
4. Validering og feilmeldinger.
5. Oppstarts-/reload-fase.
6. Testkrav.
7. Åpne beslutninger.

Kodeeksempler som ikke finnes i prosjektet skal innledes med:

> **Foreslått API – ikke implementert.**

Når en beslutning endres, skal gamle beskrivelser fjernes eller tydelig merkes som erstattet. Det skal ikke ligge to motstridende «sannheter» i docs.

Store arkitekturvalg skal registreres med malen i `development docs/templates/decision-record.md`.

## Foreslåtte Java-API-er

- Bruk typed Java-referanser når objektet/property-en finnes som constant, enum eller metode.
- Ikke representer material-properties som string keys i foreslåtte API-er.
- Bruk canonical camelCase-navn fra faktisk prosjektkode, for eksempel `structuralStrength`, `fractureToughness` og `pressureResistance`.
- Strings er fortsatt tillatt når verdien faktisk er tekst/identity/resource data, som registry ID, display name eller resource location.
- Hvis et foreslått API-navn er skrevet feil i en diskusjon, dokumenter det med korrekt Java-navn.
