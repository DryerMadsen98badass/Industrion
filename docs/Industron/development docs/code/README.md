# Code design index

Dette området beskriver målarkitektur og videre plan, ikke en historikk over eksisterende kode.

- `elements/`: minimal elementdefinition, atomic/electron model og ioner.
- `materials/`: properties, generated forms, WoodMaterial/StoneMaterial, structure sets, ores, wires og fluid transport.
- `chemistry/`: universal composition, compounds, rules, reactions og balancing.
- `organic/`: organic compounds og senere crops, food, nutrition, farming og animals.
- `recipes/`: tier-baserte requests og generation fra validerte reactions/processes.
- `energy/`: sentrale tier-profiler; recipes skriver tier, ikke manuelt energitall.
- `machines/`: machine definitions/runtime uten chemistry-logikk i tick-koden.
- `casings/`: material property profiles og generated variants.
- `multiblocks/`: casing/energy caps og structure validation.
- `maintenance/`: active-time wear, repair og failure.
- `integration/`: avgrenset Create-integrasjon.
- `diagnostics/`: tydelige generator-, composition- og validation-feil.


## Assembly – implementert

`recipes/assembly-recipes.md` beskriver dagens faktiske Assembly API/runtime. `recipes/`, `casings/` og `integration/create.md` skal være konsistente med den guiden. Eldre roadmap-eksempler kan beskrive framtidige features som ikke er del av dagens API.

Nærmeste fokus ligger i `materials`: få Wood/Stone generation stabil på 1.21.1, deretter dynamiske stone ore-hosts.

## Current integration docs

- `materials/stone-wood-ore-model.md` - common parts, stone/wood forms and ore-source identity.
- `geology/README.md` - deposits, hosts, dimension rules and runtime worldgen.
- `recipes/material-autorecipes.md` - automatic processing for stone, wood and raw ore sources.
