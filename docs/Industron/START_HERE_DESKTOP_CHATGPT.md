# Start here – Desktop ChatGPT

Du arbeider på Minecraft-modprosjektet **Industron**.

## Før du skriver kode

1. Les `development docs/DECISIONS.md`.
2. Les `development docs/preferences/`.
3. Les relevant systemdokument i `development docs/code/`.
4. Kontroller `development docs/to do/open-decisions.md`.
5. Ikke implementer noe som krever en åpen beslutning uten at prosjekteieren har valgt retning.

## Viktigste krav

- Elementdefinisjonen skal bare kreve ID, display name, symbol, atomnummer og tier.
- State, metallisk karakter, farge, forms/parts, magnetisme, tools, ores, cables og casing-egnethet skal beregnes.
- Kjemiske reactions må balanseres og valideres. Manglende nødvendige compounds/byproducts skal gi feil.
- Recipe energy kommer fra tierens sentrale energiprofil; recipes skal ikke hardkode energy.
- Casing-tier er material-tier. Material properties bestemmer hvilke casing profiles materialet kvalifiserer til.
- Multiblock effective tier begrenses av både energy-tier og casing-tier.
- Maintenance skjer etter omtrent 60 aktive driftsminutter: én tilfeldig maintenance block mister 1 durability. Ved 0 ødelegges den og 2–5 ekstra tilfeldige structure blocks, men aldri controller eller ability blocks.

## Ikke lat som kode finnes

Kodeeksemplene i docs er mål-API/pseudokode. Første oppgave er å fastsette foundation-valgene i `development docs/to do/00-foundation.md`, deretter implementere fasene i rekkefølge.


## Assembly – les dette før endringer

- `Material.X` = fysisk materialform/leaf.
- `Component.X` = rekursivt semantic assembly-tree.
- `Metal.X` = materialidentitet/binding.
- `.input(Component.X, amount)` er fri nedover; hver material-leaf kan velge uavhengig.
- `.input(Component.X, Metal.Y, amount)` låser normale descendants til `Metal.Y`.
- `ComponentDefinition.inputAny(...)` kan eksplisitt bryte en arvet fixed binding for én branch og kan bruke `atLeastParent()`/`atMostParent()`.
- Create Deployer/FakePlayer bidrar 2 ticks (0,1 s) tool-work per interaksjon; total tool-tid hoppes ikke over.

## Leveringsform

- Hold endringer smale.
- Lever komplette endrede filer.
- Ved ZIP-levering: inkluder kun endrede/nye filer og nødvendig dokumentasjon, ikke hele prosjektet uten grunn.
- Oppdater docs og tester sammen med kode.
