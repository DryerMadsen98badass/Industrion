# Maintenance state and repair

## Data som må lagres

- active runtime accumulator;
- maintenance event index;
- liste over nåværende maintenance positions;
- current/max durability per maintenance block;
- eventuelt repair history.

## Persistens – ÅPEN

To aktuelle modeller:

1. Hver maintenance block har BlockEntity med egen durability.
2. Controlleren eier et position-to-durability map.

Valget må vurderes mot ytelse, chunk unloading, flytting og hva som skjer når multiblocken demonteres.

## Repair

Repair skal gjenopprette durability uten å erstatte hele blokken. Materialkostnad kan senere avledes fra casingmaterialets plate/bolt/screw-forms. Eksakt repair UI, item og kostnad er åpen.

## Catastrophic failure

Ved failure bygges candidate-settet kun fra blokker som faktisk inngår i siste gyldige multiblock-structure. Fjern controller og ability blocks, trekk 2–5 unike tilfeldige posisjoner og ødelegg dem server-side. Etterpå skal multiblocken bli invalid.
