# Datadrevet rule – schemautkast

> **Foreslått schema – ikke implementert.**

```json
{
  "type": "industron:reaction_threshold",
  "id": "industron:chemistry/acid_formation",
  "priority": 100,
  "conditions": {
    "requires_polar_bonding": true,
    "minimum_reactivity": 0.65,
    "allowed_charge_range": [-3, 3]
  },
  "result": {
    "reaction_family": "acid_formation",
    "derive_stoichiometry": true,
    "derive_byproducts": true
  }
}
```

En data rule skal ikke inneholde vilkårlig Java-kode. Den velger og konfigurerer en trygg, registrert rule type.
