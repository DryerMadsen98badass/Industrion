# Implementation rules

## Fastsatte prinsipper

- Ikke implementer manuelle elementfelt for state, metal, color, parts, magnetic, tool material eller cable support.
- Ikke hardkod spesialtilfeller for ett bestemt grunnstoff når en generell regel kan uttrykke samme oppførsel.
- Ikke implementer framtidige systems i material-property-fasen: pickaxes, tools, motors, machine components, rods som gameplay-component, screws, bolts, wires, cables, wrenches, pumps, rotors, circuits, advanced machine parts, maintenance components, casing gameplay eller multiblock gameplay.
- Ikke lag material textures, texture sets, generated sprites, tinting, fluid textures, gas textures eller emissive rendering i material-property-fasen.
- Ikke generer material-recipes fra `IndustrialMaterials` foer recipe types og recipe-regler er eksplisitt designet.
- Placeholder-verdier som block strength, radioactivity eller midlertidig casting-formel kan eksistere mens systemet ikke bruker dem, men de skal vaere tydelige placeholders og ikke bli permanent materiallogikk.
- Ikke la tier alene gjøre hvert høyere materiale bedre på alle egenskaper. Tier gir progresjonsramme; atommodellen gir identitet og tradeoffs.
- Ikke generer recipes som bryter elementbevaring, ladningsbalanse eller registrerte kjemiregler.
- Ikke inventer manglende byproducts. Manglende definisjoner skal gi en tydelig feil.
- Ikke legg energitall direkte på vanlige recipe-definisjoner. Bruk tierens sentrale energiprofil.
- Ikke la høy energy-tier omgå en lav casing-tier i multiblocks.
- Ikke la tilfeldig maintenance failure ødelegge controller eller ability blocks.

## Utviklingspraksis

- Hold hver endring smal og sporbar.
- Oppdater relevante docs sammen med kode.
- Skriv tester for deterministiske beregninger.
- Bruk stabile seeds og sortering der generering ellers kunne variere mellom oppstarter.
- Feil ved datagenerering skal inneholde ID, regel, fase og konkret årsak.
- Midlertidige hacks skal merkes og føres i `to do/technical-debt.md`.

## Kodeleveranser

Når en eksisterende fil endres og hele filen er tilgjengelig, skal den komplette ferdige filen leveres, ikke bare fragmenter. Ved ZIP-leveranser skal ZIP-en inneholde de endrede filene og nødvendig dokumentasjon, ikke en unødvendig kopi av hele prosjektet.

## Assembly-regler for dagens implementasjon

- Ikke gjeninnfør `MaterialDefinition` som skjult tree-lag; `Material.X` er fysisk leaf og `Component.X` eier tree-strukturen.
- Ikke gjør free `.input(Component.X, amount)` om til én felles `Metal.ANY`-binding; frie material-leaves skal kunne løses uavhengig.
- Ikke omgå fixed `Metal.X` på en normal nested branch. Bruk bare eksplisitt `inputAny(...)` når designet faktisk tillater materialbytte.
- Parent-relative krav skal uttrykkes typed med `atLeastParent()`/`atMostParent()` etter `inputAny(...)` og må ha en entydig fixed parent.
- Create Deployer/FakePlayer skal ikke få instant tool completion. Den bidrar 2 ticks tool-work per interaksjon.
