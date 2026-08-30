package net.mads.industron.material.chemistry.report;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.MaterialTierResolver;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.chemistry.geology.DepositDefinition;
import net.mads.industron.material.chemistry.geology.OreMineral;
import net.mads.industron.material.chemistry.geology.OreSourceAnalysis;
import net.mads.industron.material.chemistry.process.ProcessPlan;
import net.mads.industron.material.chemistry.process.ProcessStep;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Dedicated runData output for geology, source coverage and ore processing. */
public final class OreGenerationReportWriter {
    public void write(
            Path root,
            List<OreMineral> minerals,
            List<DepositDefinition> deposits,
            List<OreSourceAnalysis> sources,
            List<ProcessPlan> plans
    ) throws IOException {
        Files.createDirectories(root.resolve("materials"));
        Files.createDirectories(root.resolve("deposits"));

        writeSummary(root.resolve("summary.txt"), minerals, deposits, sources, plans);
        writeMinerals(root.resolve("minerals.txt"), minerals);
        writeDeposits(root.resolve("deposits.txt"), deposits);
        writeSources(root.resolve("element_sources.txt"), sources);
        writeMissingMinerals(root.resolve("missing_ore_minerals.txt"), sources);
        writeBlocked(root.resolve("blocked_worldgen.txt"), sources);
        writeProcessing(root.resolve("processing_routes.txt"), plans, minerals);
        writeIndicators(root.resolve("surface_indicators.txt"), deposits);
        writeRegionRules(root.resolve("region_rules.txt"));
        writeWorldgenEmission(root.resolve("worldgen_emission.txt"), minerals, deposits);
        writeStoneProfiles(root.resolve("stone_profiles.txt"));
        writeWarnings(root.resolve("warnings.txt"), sources, deposits);

        for (OreSourceAnalysis source : sources) {
            writeSource(root.resolve("materials").resolve(source.materialId() + ".txt"), source);
        }
        for (DepositDefinition deposit : deposits) {
            writeDeposit(root.resolve("deposits").resolve(deposit.id() + ".txt"), deposit);
        }
    }

    private static void writeSummary(
            Path path,
            List<OreMineral> minerals,
            List<DepositDefinition> deposits,
            List<OreSourceAnalysis> sources,
            List<ProcessPlan> plans
    ) throws IOException {
        long covered = sources.stream().filter(value -> value.status() == OreSourceAnalysis.Status.COVERED).count();
        long noOre = sources.stream().filter(value -> value.status() == OreSourceAnalysis.Status.NO_ORE_REQUIRED).count();
        long missing = sources.stream().filter(value -> value.status() == OreSourceAnalysis.Status.MISSING_ORE_MINERAL).count();
        long blocked = sources.stream().filter(value -> value.status() == OreSourceAnalysis.Status.BLOCKED).count();
        long mineralRoutes = plans.stream().filter(plan -> isMineralDustProcessingPlan(plan.targetMaterialId())).count();
        long dedicatedOres = IndustrialMaterials.ALL.stream().filter(IndustrialMaterial::isOreMaterial).count();
        long traceMinerals = IndustrialMaterials.ALL.stream().filter(IndustrialMaterial::isMineralDust).count();

        Files.write(path, List.of(
                "INDUSTRON ORE / GEOLOGY GENERATION",
                "",
                "Elements analyzed: " + sources.size(),
                "Geological minerals (dedicated + trace): " + minerals.size(),
                "Dedicated OreMaterials definitions: " + dedicatedOres,
                "MineralDustMaterials definitions: " + traceMinerals,
                "Dedicated deposits: " + deposits.size(),
                "Post-dust mineral processing plans: " + mineralRoutes,
                "Covered elements: " + covered,
                "No solid ore required: " + noOre,
                "Missing ore mineral: " + missing,
                "Blocked: " + blocked,
                "",
                "Dimension progression:",
                "  ULV-HV -> Overworld",
                "  EV-LuV -> Nether",
                "  ZPM+ -> End",
                "",
                "Worldgen region size: 9 x 9 chunks (144 x 144 blocks)",
                "Selection: deterministic from world seed + dimension + floorDiv(chunk, 9).",
                "Runtime: industron:geology_deposit places only the current chunk slice.",
                "Ore ownership: only OreMaterials definitions get generated compound ore blocks/worldgen; MineralDustMaterials are trace dusts only."
        ), StandardCharsets.UTF_8);
    }

    private static void writeMinerals(Path path, List<OreMineral> minerals) throws IOException {
        List<String> lines = new ArrayList<>();
        for (OreMineral mineral : minerals.stream().sorted(Comparator.comparing(OreMineral::id)).toList()) {
            lines.add(mineral.id() + " | material=" + mineral.materialId()
                    + " | recoverable=" + mineral.recoverableMaterials()
                    + " | magnetic=" + mineral.magnetic()
                    + " | processingDifficulty=" + mineral.processingDifficulty());
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static void writeDeposits(Path path, List<DepositDefinition> deposits) throws IOException {
        List<String> lines = new ArrayList<>();
        for (DepositDefinition deposit : deposits.stream().sorted(Comparator.comparing(DepositDefinition::id)).toList()) {
            lines.add(deposit.id()
                    + " | geometry=" + deposit.geometry()
                    + " | primary=" + deposit.primaryMinerals()
                    + " | secondary=" + deposit.secondaryMinerals()
                    + " | grade=" + deposit.minimumGrade() + ".." + deposit.maximumGrade()
                    + " (avg=" + deposit.grade() + ")"
                    + " | rarityWeight=" + deposit.rarity()
                    + " | size=" + deposit.minimumSize() + ".." + deposit.maximumSize()
                    + " | y=" + deposit.conditions().minimumY() + ".." + deposit.conditions().maximumY()
                    + " preferred=" + deposit.conditions().preferredY()
                    + " | dimensions=" + deposit.conditions().dimensions()
                    + " | hosts=" + deposit.conditions().hostRockTags()
                    + " | terrain=" + deposit.conditions().terrainProfiles()
                    + " | indicators=" + deposit.surfaceIndicators());
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static void writeSources(Path path, List<OreSourceAnalysis> sources) throws IOException {
        List<String> lines = new ArrayList<>();
        for (OreSourceAnalysis source : sources) {
            lines.add("#" + source.atomicNumber() + " " + source.displayName() + " (" + source.materialId() + ")"
                    + " | tier=" + source.tierName()
                    + " | dimension=" + source.dimensionBand()
                    + " | status=" + source.status()
                    + " | source=" + source.sourceKind()
                    + " | " + source.detail());
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static void writeMissingMinerals(Path path, List<OreSourceAnalysis> sources) throws IOException {
        List<String> lines = new ArrayList<>();
        for (OreSourceAnalysis source : sources) {
            if (source.status() != OreSourceAnalysis.Status.MISSING_ORE_MINERAL) continue;
            lines.add("[MISSING_ORE_MINERAL]");
            lines.add("Material: " + source.displayName() + " (" + source.materialId() + ")");
            lines.add("Atomic number: " + source.atomicNumber());
            lines.add("Calculated tier: " + source.tierName());
            lines.add("Worldgen dimension band: " + source.dimensionBand());
            lines.add("Reason: " + source.detail());
            lines.add("Projected bulk topology from the anonymous .contains(...) candidate: " + source.suggestedTopology());
            lines.add("Suggested compatible companion: " + source.suggestedCompanionId());
            lines.add("Suggested composition:");
            lines.add("  " + source.suggestedTargetAmount() + " " + source.materialId());
            lines.add("  " + source.suggestedCompanionAmount() + " " + source.suggestedCompanionId());
            lines.add("Suggested .contains(...):");
            for (String line : source.suggestedContains().split("\\R")) lines.add("  " + line);
            if (source.suggestedGeology() != null) {
                OreSourceAnalysis.SuggestedGeology geology = source.suggestedGeology();
                lines.add("Projected geology if this anonymous compound is accepted:");
                lines.add("  strongest component tier: " + geology.tierName());
                lines.add("  dimension: " + geology.dimensionBand());
                lines.add("  geometry: " + geology.geometry());
                lines.add("  Y: " + geology.minimumY() + ".." + geology.maximumY() + " preferred=" + geology.preferredY());
                lines.add("  biome affinity: " + geology.biomeTags());
                lines.add("  candidate host rocks: " + geology.hostBlocks());
            }
            lines.add("Name and ID: NOT GENERATED - define these manually in OreMaterials with ore(\"id\", \"Name\").contains(...).");
            lines.add("WORLD GENERATION: BLOCKED until a reviewed named OreMaterials definition exists.");
            lines.add("");
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static void writeBlocked(Path path, List<OreSourceAnalysis> sources) throws IOException {
        List<String> lines = new ArrayList<>();
        for (OreSourceAnalysis source : sources) {
            if (!source.missing()) continue;
            lines.add(source.materialId() + " | " + source.status() + " | " + source.dimensionBand() + " | " + source.detail());
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static void writeProcessing(
            Path path,
            List<ProcessPlan> plans,
            List<OreMineral> minerals
    ) throws IOException {
        java.util.Set<String> ids = minerals.stream().map(OreMineral::materialId).collect(java.util.stream.Collectors.toSet());
        List<String> lines = new ArrayList<>();
        for (ProcessPlan plan : plans) {
            String mineralId = mineralIdFromProcessingPlan(plan.targetMaterialId());
            if (mineralId == null || !ids.contains(mineralId)) continue;
            lines.add("TARGET " + mineralId + " DUST -> recoverable elemental forms");
            for (ProcessStep step : plan.steps()) {
                lines.add("  " + step.id() + " [" + step.kind() + "]");
                lines.add("    inputs=" + step.inputs());
                lines.add("    outputs=" + step.outputs());
                lines.add("    tier=" + step.recipeTierName());
                lines.add("    temperature=" + step.requiredTemperature() + " C");
                lines.add("    duration=" + step.durationTicks() + " ticks");
                lines.add("    balanced=" + step.balanced());
                if (!step.requirements().isEmpty()) lines.add("    requirements=" + step.requirements());
                lines.add("    note=" + step.explanation());
            }
            lines.add("");
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static boolean isMineralDustProcessingPlan(String id) {
        return id != null && (id.endsWith("_ore_dust_processing") || id.endsWith("_mineral_dust_processing"));
    }

    private static String mineralIdFromProcessingPlan(String id) {
        if (id == null) return null;
        if (id.endsWith("_ore_dust_processing")) {
            return id.substring(0, id.length() - "_ore_dust_processing".length());
        }
        if (id.endsWith("_mineral_dust_processing")) {
            return id.substring(0, id.length() - "_mineral_dust_processing".length());
        }
        return null;
    }

    private static void writeIndicators(Path path, List<DepositDefinition> deposits) throws IOException {
        List<String> lines = new ArrayList<>();
        for (DepositDefinition deposit : deposits) {
            lines.add(deposit.id() + " -> " + deposit.surfaceIndicators());
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static void writeRegionRules(Path path) throws IOException {
        Files.write(path, List.of(
                "INDUSTRON GEOLOGY REGION RULES",
                "",
                "Region size: 9 x 9 chunks = 144 x 144 blocks.",
                "regionX = Math.floorDiv(chunkX, 9)",
                "regionZ = Math.floorDiv(chunkZ, 9)",
                "Negative chunk coordinates must therefore use floor division, never truncating integer division.",
                "",
                "One primary eligible deposit plan is selected per geology region.",
                "Selection seed includes world seed, dimension ID, regionX and regionZ.",
                "Rarity values are relative weights among eligible deposits, not direct percentages.",
                "The complete body is deterministic and each chunk generates only its local body slice.",
                "No region generation may force-load all 81 chunks.",
                "",
                "Tier dimension policy:",
                "  ULV-HV: Overworld",
                "  EV-LuV: Nether",
                "  ZPM+: End"
        ), StandardCharsets.UTF_8);
    }

    private static void writeWorldgenEmission(
            Path path,
            List<OreMineral> minerals,
            List<DepositDefinition> deposits
    ) throws IOException {
        java.util.Map<String, OreMineral> mineralsById = minerals.stream()
                .collect(java.util.stream.Collectors.toMap(OreMineral::id, value -> value, (a, b) -> a, java.util.LinkedHashMap::new));
        java.util.Map<String, IndustrialMaterial> materialsById = IndustrialMaterials.ALL.stream()
                .collect(java.util.stream.Collectors.toMap(IndustrialMaterial::id, value -> value, (a, b) -> a, java.util.LinkedHashMap::new));

        List<String> lines = new ArrayList<>();
        lines.add("INDUSTRON GEOLOGY WORLDGEN EMISSION");
        lines.add("");
        lines.add("Only reviewed OreMaterials definitions own generated compound ore blocks and are emitted by the runtime placer.");
        lines.add("MineralDustMaterials may occur in StoneMaterial .contains(...) and get processing routes, but trace occurrence never creates a dedicated ore block/deposit.");
        lines.add("");

        for (DepositDefinition deposit : deposits.stream().sorted(Comparator.comparing(DepositDefinition::id)).toList()) {
            OreMineral primary = deposit.primaryMinerals().stream()
                    .map(value -> mineralsById.get(value.mineralId()))
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            IndustrialMaterial material = primary == null ? null : materialsById.get(primary.materialId());
            if (material == null) {
                lines.add(deposit.id() + " | BLOCKED | primary material not registered");
            } else if (!MaterialOreHost.hasNaturalOre(material)) {
                lines.add(deposit.id() + " | REPORT_ONLY | " + material.id()
                        + " is not an OreMaterials definition");
            } else {
                lines.add(deposit.id() + " | EMITTABLE | material=" + material.id()
                        + " | dimensions=" + deposit.conditions().dimensions()
                        + " | biomes=" + deposit.conditions().biomeTags()
                        + " | hosts=" + deposit.conditions().hostRockTags());
            }
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static void writeStoneProfiles(Path path) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("INDUSTRON STONE GEOLOGY PROFILES");
        lines.add("");
        lines.add("Stone .contains(...) amounts are independent centrifuge percentage points, not formula stoichiometry.");
        lines.add("The explicit .dimension(...) is a hard geology/worldgen boundary for the stone host.");
        lines.add("");
        for (StoneMaterial stone : StoneMaterials.ALL) {
            int totalChance = stone.components().stream().mapToInt(component -> component.amount()).sum();
            lines.add(stone.displayName() + " (" + stone.id() + ")");
            lines.add("  dimension=" + stone.dimension());
            lines.add("  strongestTraceTier=" + MaterialTierResolver.strongestComponentTier(stone).displayName());
            lines.add("  formula=" + stone.formula(false));
            lines.add("  traceOutputs=" + stone.components().size() + " totalChance=" + totalChance + "%");
            stone.components().forEach(component -> lines.add(
                    "    " + component.amount() + "% -> " + component.substance().displayName()
                            + " [" + component.substance().id() + "] formula=" + component.substance().formula()
            ));
            lines.add("");
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static void writeWarnings(
            Path path,
            List<OreSourceAnalysis> sources,
            List<DepositDefinition> deposits
    ) throws IOException {
        List<String> lines = new ArrayList<>();
        for (OreSourceAnalysis source : sources) {
            if (source.missing()) lines.add(source.materialId() + ": " + source.detail());
        }
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            if (!material.isOreMaterial()) continue;
            if (MaterialOreHost.compatibleHosts(material).isEmpty()) {
                lines.add(material.id() + ": OreMaterials definition has no compatible StoneMaterial host from StoneMaterial .contains(...); worldgen is blocked until a fitting stone exists.");
            }
        }
        if (deposits.isEmpty()) lines.add("No dedicated deposit definitions were generated.");
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static void writeSource(Path path, OreSourceAnalysis source) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("MATERIAL " + source.displayName() + " (" + source.materialId() + ")");
        lines.add("Atomic number: " + source.atomicNumber());
        lines.add("Tier: " + source.tierName());
        lines.add("Dimension: " + source.dimensionBand());
        lines.add("Status: " + source.status());
        lines.add("Source: " + source.sourceKind());
        lines.add("Detail: " + source.detail());
        if (!source.coveringMineralId().isBlank()) lines.add("Covered by mineral: " + source.coveringMineralId());
        if (!source.suggestedContains().isBlank()) {
            lines.add("");
            lines.add("SUGGESTED MINERAL COMPOSITION");
            lines.add(source.suggestedContains());
            lines.add("Name and ID are intentionally left to the developer.");
            if (source.suggestedGeology() != null) {
                OreSourceAnalysis.SuggestedGeology geology = source.suggestedGeology();
                lines.add("Projected tier/dimension: " + geology.tierName() + " / " + geology.dimensionBand());
                lines.add("Projected geometry: " + geology.geometry());
                lines.add("Projected Y: " + geology.minimumY() + ".." + geology.maximumY() + " preferred=" + geology.preferredY());
                lines.add("Projected biome affinity: " + geology.biomeTags());
                lines.add("Projected host rocks: " + geology.hostBlocks());
            }
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private static void writeDeposit(Path path, DepositDefinition deposit) throws IOException {
        Files.write(path, List.of(
                "DEPOSIT " + deposit.displayName() + " (" + deposit.id() + ")",
                "Geometry: " + deposit.geometry(),
                "Primary minerals: " + deposit.primaryMinerals(),
                "Secondary minerals: " + deposit.secondaryMinerals(),
                "Gangue: " + deposit.gangueWeights(),
                "Dimensions: " + deposit.conditions().dimensions(),
                "Biome tags: " + deposit.conditions().biomeTags(),
                "Terrain: " + deposit.conditions().terrainProfiles(),
                "Host rocks: " + deposit.conditions().hostRockTags(),
                "Y: " + deposit.conditions().minimumY() + ".." + deposit.conditions().maximumY(),
                "Preferred Y: " + deposit.conditions().preferredY(),
                "Vertical spread: " + deposit.conditions().verticalSpread(),
                "Size: " + deposit.minimumSize() + ".." + deposit.maximumSize(),
                "Grade: " + deposit.minimumGrade() + ".." + deposit.maximumGrade() + " (avg=" + deposit.grade() + ")",
                "High-grade core chance: " + deposit.highGradeCoreChance(),
                "Rarity weight: " + deposit.rarity(),
                "Surface indicators: " + deposit.surfaceIndicators()
        ), StandardCharsets.UTF_8);
    }
}
