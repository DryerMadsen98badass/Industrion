# How to add a machine

> Prosedyren er et mål for fremtidig API. Ingen av klassene er implementert ennå.

1. Opprett machine ID og dokumenter formål.
2. Velg én eller flere eksisterende `ProcessFamily`-verdier.
3. Angi støttede tiers, ikke energitall.
4. Angi inputs/outputs og inventory/fluid constraints.
5. For multiblock: angi pattern, nødvendige casing profiles og ability slots.
6. Koble renderer/UI uten å legge prosesslogikk i klientkode.
7. Registrer machine definition gjennom sentralt register.
8. Legg til valideringstester.
9. Legg til data generation for blockstate/model/lang når registry-modellen er bestemt.
10. Oppdater denne dokumentasjonen dersom API-et endres.

En ny machine skal ikke få egne spesialregler for chemistry dersom samme prosess kan uttrykkes som en gjenbrukbar rule eller process family.
