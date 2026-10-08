# Fastsatte og åpne beslutninger

## FASTSATT – prosjekt

1. Modden heter **Industron**.
2. Minecraft er **1.21.1**, loader er **NeoForge**, Java er **21**.
3. Mod ID er `industron`, base package er `net.mads.industron`.
4. Create **6.0.10** brukes der integrasjon gir mening, men kjernefysikk/kjemi skal ikke være Create-avhengig.
5. Leveranser skal normalt være ZIP med bare endrede/nye filer. Eventuelle filer som må slettes skal listes eksplisitt.

## FASTSATT – elements og properties

1. Modden skal ha omtrent 100 helt fiktive grunnstoffer.
2. Et grunnstoff defineres minimalt med ID, display name, symbol, atomnummer og tier.
3. State, metal/gem classification, color, magnetic behavior og material capabilities skal utledes fra felles regler, ikke skrives som manuelle sannhetsfelt per element.
4. Gameplay-property-input skal i siste instans komme fra `atomicNumber + tier`.
5. Tier multiplier følger tier order: ULV=1, LV=2, MV=3, HV=4 osv.
6. Ikke alle properties skal være tier-monotone; atom/identity-relaterte egenskaper skal kunne gi tradeoffs.
7. Referansetemperatur for state classification er **20 °C**.
8. Metal og gem er mutually exclusive classifications.
9. `MaterialPropertyCalculator` skal være sentral eier av material-property-beregningene.
10. `electricalResistance` skal **ikke** være et gameplay-property/system for wires.

## FASTSATT – generated material content

1. Forms/items/blocks skal kunne eksistere selv om texture mangler; manglende texture skal gi missing texture, ikke manglende registry entry.
2. Texture variants skal oppdages dynamisk fra resource-mapper der systemet er variant-basert.
3. Hot ingot bruker samme ingot-design som normal ingot med hot overlay over; magnetic overlay ligger over hot overlay når relevant.
4. Magnetic variants genereres bare for magnetiserbare materialer.

## FASTSATT – ores

1. Ore består av host stone + material ore overlay/tint.
2. Alle gyldige ore hosts skal ha både **normal ore og small ore**.
3. Nye registrerte `StoneMaterial` skal senere bli ore hosts automatisk uten per-metal redigering.

## FASTSATT – wires

1. Material tier bestemmer wire voltage tier.
2. Amp-capacity utledes fra `electricalConductivity`.
3. Thickness finnes som 1x, 2x, 4x, 8x og 16x.
4. Insulated variant har 1.25x amp-capacity og avrundes til nærmeste heltall (.5 opp).
5. Wire tooltip skal bare vise Tier, CE og Amps.
6. Det skal ikke simuleres gameplay-resistance/loss i wire-nettet med en material `electricalResistance` property.

## FASTSATT – pipes, pumps og tanks

1. Pipe/pump/tank stats kommer fra material properties, ikke en separat hardkodet material-tier-tabell.
2. Pipe color kommer fra materialets farge.
3. Pipe throughput og pump throughput er forskjellige størrelser.
4. Pump flow og pump kinetic stress skal beregnes **uavhengig**; ingen felles efficiency multiplier.
5. `pumpFlowRate` bruker mB/t/RPM og maks én desimal.
6. `pumpStressImpact` bruker heltallig SU/RPM.
7. Pipes skal kunne avvise fluids utenfor materialets temperatur- og chemical range.
8. Chemical range bruker negativt for base, 0 neutral, positivt for acid; tooltip viser f.eks. `-28 to 61`, ikke `-28 to +61`.
9. Max pressure kan finnes internt, men skal ikke vises i pipe tooltip.
10. Pump tooltip skal ikke duplisere Create sin SU-visning.
11. Tank capacity skal utledes fra structural/mechanical properties, ikke hardkodes per materiale.

## FASTSATT – WoodMaterial / StoneMaterial

1. Wood og stone er ekte `IndustrialSubstance`-typer som kan refereres direkte fra `component(...)`.
2. Visual family velges med typed constants, f.eks. `WoodModel.SPRUCE` og `StoneModel.DIORITE`, ikke fritekst-strings.
3. Wood/stone skal støtte både `.contains(...)` og `.existing(...)`.
4. `existing(...)` skal gjenbruke eksisterende object/form i stedet for å generere en duplikat.
5. Wood bruker en sammenhengende named family på tvers av log/planks/door/trapdoor/window/etc.
6. Stone bruker en sammenhengende named family med hele tilgjengelige decorative archive.
7. Wood får Tiny Wood Pulp, Small Wood Pulp og Wood Pulp gjennom material-form-systemet.
8. Stone får Tiny Dust, Small Dust og Dust; molten kan tilføyes senere når det gir mening.
9. Generert sapling skal ikke få faktisk tree-growth før et separat TreeDefinition-system finnes.

## FASTSATT – structure_sets

1. Sluttbiblioteket skal ikke være sortert i `minecraft/` og `create/`.
2. Wood og stone beholder named families som `spruce` og `diorite`.
3. Metal bruker generiske, nummererte designs per block-type: `metal_1`, `metal_2`, osv.
4. Metal variants er uavhengige per block-type.
5. Copper templates bruker bare base/unoxidized/unwaxed copper.
6. Copper/brass casings og andre machine-specific casings skal ikke brukes som generic metal structure templates.
7. Grayscale templates skal bevare alpha, dimensjoner og shading.

## FASTSATT – composition/chemistry direction

1. `component(...)` skal kunne referere til alle relevante substances: elements, alloys, compounds, wood, stone, fluids, gases, molten og relevante forms.
2. Composition skal kunne ekspanderes rekursivt til elemental composition.
3. Cycle detection er obligatorisk.
4. Kjemiske reactions skal avvises når de ikke kan balanseres eller nødvendige substances/byproducts mangler.
5. Acid/base-skala: 0 neutral, positiv acid, negativ base.
6. Recipes skal angi tier, ikke håndskrevet energitall.

## FASTSATT – casings/multiblocks/maintenance

1. Casing tier følger material tier.
2. Casing profiles har property-thresholds; ett materiale kan kvalifisere til flere profiles.
3. Multiblock recipe tier begrenses av både casing tier og installert energy tier.
4. Maintenance wear akkumuleres bare under aktiv drift.
5. Omtrent hver 60. aktive driftsminutt mister en tilfeldig maintenance block durability.
6. Controller og ability blocks er beskyttet mot collateral destruction.

## ÅPEN / må kalibreres

- Eksakte property-enheter og endelige numeriske ranges.
- Endelig amp-curve for 1x wire; Vernium skal ligge omtrent rundt 4 A, men koeffisienten må valideres i kode/test.
- Endelig `frictionCoefficient`-formel og pump SU-kurve.
- Endelige chemistry orbital/isotope-detaljer der dagens atomic model fortsatt er forenklet.
- Compound naming-system og canonical formula ordering.
- Duration/overclocking-regler.
- Endelig første machine-sett for chemistry-fasen.
- Endelige casing-profile thresholds.
- Maintenance repair UX/persistensdetaljer.
- TreeDefinition API og worldgen/growth-regler.

## FASTSATT – Assembly API/runtime (implementert 2026-08-25)

1. `Material.X` er alltid fysisk `MaterialPart`/leaf og ekspanderer aldri et component-tree.
2. `Component.X` er et rekursivt semantic tree fra `ComponentDefinitions`.
3. `Metal.X` er materialidentitet/binding.
4. `.input(Component.X, amount)` har ingen felles hidden metal-binding; hver material-leaf kan velges uavhengig så lenge requirements består.
5. `.input(Component.X, Metal.Y, amount)` låser hele den normale branchen til `Metal.Y`.
6. Normal nested `.input(...)` arver callerens free/fixed materialmodus.
7. `ComponentDefinition.inputAny(...)` bryter eksplisitt en inherited fixed binding på bare den branchen.
8. Parent-relative stat-krav etter `inputAny(...)` støtter `atLeastParent()` og `atMostParent()`; relative range er bevisst ikke definert.
9. Vanlige recipe-statkrav propageres til material-leaves i det aktuelle component-treet.
10. Missing fysisk materialform gjør en materialgenerert variant uløselig og skal kunne gi skip; missing ComponentDefinition/cycle er programmeringsfeil.
11. Base item/block er fysisk workpiece/result og ekspanderer ikke components.
12. Create Deployer/FakePlayer må bruke korrekt tool og fullføre samme totale tool-work; hver fake-player interaction bidrar 2 ticks = 0,1 s.
13. JEI har `Assembly Products` for root recipe og `Assembly Components` for ett direkte component-level om gangen; root-visningen skal ikke flattene hele nested tree-et.

## 2026-08-27 - Material/geology/autorecipe integration decisions

These decisions override older geology/automatic-recipe text where it conflicts.

- Current main project code is the API/status source of truth. The earlier geology changed-files archive is incomplete reference only.
- `MaterialPart` is the target common part/form enum for stone and wood; remove the parallel structure-part model after migration.
- Existing Minecraft/Create stone/wood forms are expressed with `.existing(MaterialPart.X, "namespace:id")`.
- Every registered stone has explicit color, dust forms and a cobbled block/slab/stairs/wall set; missing cobbled forms are generated from grayscale templates tinted by the stone color.
- A registered stone with a valid base stone form automatically becomes an ore host. Host-specific ore enum constants and a separate hardcoded host catalog are migration debt.
- Stone/wood `.contains(...)` accepts arbitrary registered substances and is treated as authoritative user composition.
- Natural ore worldgen is normally based on manually named composed ore-source materials, not one elemental ore per element.
- An element may occur in multiple source materials; one source may contain multiple useful substances.
- Ore-source definitions live under `material/defenitions`.
- `runData` reports deterministic missing-source composition proposals and exact copyable `.contains(component(...), ...)` syntax, but never invents source names/IDs or edits Java source.
- Material/deposit dimension is selected from highest relevant tier: ULV-HV Overworld, EV-LuV Nether, ZPM+ End.
- The earlier stone/wood/raw-source scope has been superseded by the implemented generic composite-DUST planner. Ore preprocessing is implemented separately and hands off at the ore material's own `DUST`.
- Generated processing recipe tier is one electric tier below resolved source tier, clamped at ULV.
- Process selection is based on actual composition/phase/properties/structure. Physical separators do not break bonds. Ambiguous topology produces a diagnostic rather than a guessed recipe.
- Multi-step routes are allowed only when each step is physically/process-semantically justified.
- Recipe emission must respect actual IO limits and may never silently truncate outputs.
- Geology planning is not complete until registered runtime worldgen places deterministic deposits in chunks.

## 2026-09-02 – Foundry/alloy-mixture decisions

- Foundry is an early-game stateful metallurgical multiblock heated by a separate exact-footprint Heater provider.
- Exact normalized `.contains(...)` ratios identify defined alloys; non-matching ratios remain unclassified mixtures.
- The UI shows actual contents only: no closest-alloy or missing-amount suggestion.
- Undefined mixtures use generic data-bearing carriers, support at least 20 constituents and must always retain a physically valid separation path.
- Direct and flattened composition ledgers are both retained: direct identity for preferred recovery, flattened elements for conservation.
- Composite dust chemistry cannot be bypassed by generic melting. Dry dust cannot be distilled.
- Hot molds and cast outputs require explicit thermal/cooling transitions.
- Generic elemental `DUST -> INGOT` remains removed until a proper production/casting system owns it.
- Reversible Foundry transformations are allowed only after graph validation proves exact zero net material gain; energy and time never excuse a dupe.
- Exact matching is synchronous and cached; no global material/recipe/structure scan occurs per tick and no thread is created per Foundry.
