package net.mads.industron.recipe.recipes.assembly;

import net.mads.industron.Industron;
import net.mads.industron.block.coils.CoilDefinitions;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.generated.Metal;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.material.recipes.ClayFireboxAssemblyRecipes;
import net.mads.industron.material.recipes.MaterialBrickAssemblyRecipes;
import net.mads.industron.material.recipes.MaterialCasingAssemblyRecipes;
import net.mads.industron.material.recipes.MaterialFrameAssemblyRecipes;
import net.mads.industron.material.recipes.MaterialPartAssemblyRecipes;
import net.mads.industron.material.recipes.MaterialRecipeHelper;
import net.mads.industron.material.recipes.StoneAssemblyRecipes;
import net.mads.industron.material.recipes.WoodAssemblyRecipes;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyMaterialSelector;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRecipeDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Central Assembly recipe registry.
 *
 * <p>Private/fixed-output Assembly recipes live directly in this file. Families whose output
 * identity is generated from material definitions stay in material/recipes and are registered here.</p>
 */
public final class AssemblyRecipes {
    public static final List<AssemblyRecipeDefinition> ALL = buildAll();

    private AssemblyRecipes() {
    }

    private static List<AssemblyRecipeDefinition> buildAll() {
        List<AssemblyRecipeDefinition> result = new ArrayList<>();

        // Material-derived families: adding a new material/wood/stone can add new output variants.
        result.addAll(MaterialPartAssemblyRecipes.ALL);
        result.addAll(MaterialFrameAssemblyRecipes.ALL);
        result.addAll(MaterialCasingAssemblyRecipes.ALL);
        result.addAll(MaterialBrickAssemblyRecipes.ALL);
        result.addAll(WoodAssemblyRecipes.ALL);
        result.addAll(StoneAssemblyRecipes.ALL);
        result.addAll(ClayFireboxAssemblyRecipes.ALL);
        result.addAll(ToolAssemblyRecipes.ALL);
        result.addAll(EquipmentAssemblyRecipes.ALL.stream()
                .filter(recipe -> !recipe.hasDynamicToolOutput()).toList());
        result.addAll(MineralArmourRecipes.ALL);

        // Private/fixed-output recipes belong here even when they accept many material alternatives.
        result.addAll(BasicAssemblyWorkbenchRecipes.ALL);
        result.addAll(VanillaEarlyGameAssemblyRecipes.ALL);
        result.addAll(ComponentAssemblyRecipes.ALL);
        result.addAll(CreateEarlyKineticAssemblyRecipes.ALL);
        result.addAll(HeatingCoilAssemblyRecipes.ALL);
        result.addAll(BlastFurnaceAssemblyRecipes.ALL);
        result.addAll(AnvilAssemblyRecipes.ALL);
        result.add(AssemblyRecipeDefinition.recipe("recipes/assembly/food/clean_wheat")
                .level(1).baseItemInput("minecraft:wheat").tool(Tool.MALLET,2)
                .baseItemOutput("industron:wheat_grain",1).build());
        for(String fish: new String[]{"cod","salmon","tropical_fish"})
            result.add(AssemblyRecipeDefinition.recipe("recipes/assembly/food/clean_"+fish)
                .level(1).baseItemInput("minecraft:"+fish).tool(Tool.KNIFE,3)
                .baseItemOutput("industron:cleaned_"+fish,1).build());
        addCampfireRecipes(result);
        addEarlyMachineRecipes(result);

        result.add(AssemblyRecipeDefinition.recipe("recipes/assembly/hopper")
                .level(1)
                .baseBlockInput(MaterialPart.CHEST, MaterialType.WOOD)
                .input(MaterialPart.PLATE, MaterialType.METAL, 5).tier(MachineTier.ULV)
                .tool(Tool.MALLET, 5)
                .baseBlockOutput("minecraft:hopper")
                .build());

        result.add(AssemblyRecipeDefinition.recipe("recipes/assembly/create_basin")
                .level(1)
                .baseItemInput(MaterialPart.PLATE, MaterialType.METAL).tier(MachineTier.ULV)
                .input(MaterialPart.PLATE, MaterialType.METAL, 4).tier(MachineTier.ULV)
                .tool(Tool.HAMMER, 5)
                .baseBlockOutput("create:basin")
                .build());

        result.add(AssemblyRecipeDefinition.recipe("recipes/assembly/portal_activator")
                .level(WorkbenchLevels.forTier(MachineTier.LV))
                .baseItemInput(MaterialPart.PLATE, MaterialType.METAL).tier(MachineTier.LV).capture("portal_metal")
                .input(Component.PLATE, MaterialType.METAL, 3).tier(MachineTier.LV).sameMaterialAs("portal_metal")
                .input(Component.ROD, MaterialType.METAL, 2).tier(MachineTier.LV).sameMaterialAs("portal_metal")
                .input(Component.SMALL_SCREW, MaterialType.METAL, 4).tier(MachineTier.LV).sameMaterialAs("portal_metal")
                .input("industron:portal_core", 1)
                .tool(Tool.SCREWDRIVER, 4)
                .tool(Tool.FILE, 2)
                .baseItemOutput("industron:portal_activator", 1)
                .build());

        result.add(
                AssemblyRecipeDefinition.recipe("primitive_machines/river_washer")
                        .level(1)
                        .baseBlockInput(MaterialPart.PLANKS, MaterialType.WOOD)
                        .input(MaterialPart.PLANKS, MaterialType.WOOD, 5)
                        .input(MaterialPart.STICK, MaterialType.WOOD, 4)
                        .input(Component.WOOD_PEG, MaterialType.WOOD, 8)
                        .input(PlantPart.STRING, 8)
                        .baseBlockOutput("industron:river_washer")
                        .build()
        );

        result.add(
                AssemblyRecipeDefinition.recipe("recipes/assembly/charcoal_pit")
                        .level(1)
                        .baseBlockInput("minecraft:dirt")
                        .tool(Tool.AXE, 1)
                        .baseBlockOutput("industron:charcoal_pit")
                        .build()
        );

        result.add(AssemblyRecipeDefinition.recipe("recipes/assembly/wool_hood")
            .level(1).baseItemInput("minecraft:white_wool").input("minecraft:white_wool",2)
            .input(PlantPart.STRING,4).tool(Tool.KNIFE,4).baseItemOutput("industron:wool_hood",1).build());
        result.add(AssemblyRecipeDefinition.recipe("recipes/assembly/wool_coat")
            .level(1).baseItemInput("minecraft:white_wool").input("minecraft:white_wool",7)
            .input(PlantPart.STRING,4).tool(Tool.KNIFE,4).baseItemOutput("industron:wool_coat",1).build());
        result.add(AssemblyRecipeDefinition.recipe("recipes/assembly/wool_trousers")
            .level(1).baseItemInput("minecraft:white_wool").input("minecraft:white_wool",5)
            .input(PlantPart.STRING,4).tool(Tool.KNIFE,4).baseItemOutput("industron:wool_trousers",1).build());
        result.add(AssemblyRecipeDefinition.recipe("recipes/assembly/wool_boots")
            .level(1).baseItemInput("minecraft:white_wool").input("minecraft:white_wool",1)
            .input(PlantPart.STRING,4).tool(Tool.KNIFE,4).baseItemOutput("industron:wool_boots",1).build());
        AssemblyRecipeCollisionValidator.validate(result);
        return List.copyOf(result);
    }

    /** Explicit private acquisition routes. A newly added machine tier gets no automatic recipe. */
    private static void addEarlyMachineRecipes(List<AssemblyRecipeDefinition> out) {
        addAndesiteConstructionRecipes(out);
        out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/fiber_gasket")
                .level(1).baseItemInput(PlantPart.FIBER)
                .input(PlantPart.FIBER, 3)
                .input(PlantPart.STRING, 2)
                .tool(Tool.KNIFE, 2)
                .tool(Tool.MALLET, 2)
                .baseItemOutput("industron:fiber_gasket", 2).build());
        out.add(kineticAssembly("lathe", 6, 4, 2, 2)
                .input(Component.CHUCK, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(Component.CUTTER, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(Component.BEARING, MaterialType.METAL, 2).tier(MachineTier.ULV)
                .tool(Tool.HAMMER, 4)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:lathe").build());
        out.add(kineticAssembly("mechanical_centrifuge", 8, 2, 4, 0)
                .input(Component.ROTOR, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(Component.BEARING, MaterialType.METAL, 2).tier(MachineTier.ULV)
                .input("minecraft:glass", 4)
                .tool(Tool.HAMMER, 4)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:mechanical_centrifuge").build());
        out.add(kineticAssembly("mechanical_sifter", 3, 2, 2, 0)
                .input(Component.CRANK_LINKAGE, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(PlantPart.STRING, 8)
                .tool(Tool.HAMMER, 4)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:mechanical_sifter").build());
        out.add(kineticAssembly("pulverizer", 10, 2, 2, 4)
                .input(Component.CRUSHING_DRUM, MaterialType.METAL, 2).tier(MachineTier.ULV)
                .tool(Tool.HAMMER, 4)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:pulverizer").build());
        out.add(kineticAssembly("wire_drawing_machine", 4, 3, 2, 2)
                .input(Component.EXTRUSION_HEAD, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(Component.BEARING, MaterialType.METAL, 2).tier(MachineTier.ULV)
                .tool(Tool.HAMMER, 4)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:wire_drawing_machine").build());
        out.add(kineticAssembly("winding_machine", 4, 2, 2, 0)
                .input(Component.CHUCK, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(Component.BEARING, MaterialType.METAL, 2).tier(MachineTier.ULV)
                .input(PlantPart.STRING, 4)
                .tool(Tool.HAMMER, 4)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:winding_machine").build());
        out.add(kineticAssembly("mechanical_bender", 6, 2, 3, 2)
                .input(Component.PRESS_SLIDE, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(Component.CRANK_LINKAGE, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .tool(Tool.HAMMER, 4)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:mechanical_bender").build());
        out.add(kineticAssembly("magnetic_separator", 8, 3, 2, 0)
                .input(Component.ROTOR, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(Component.BEARING, MaterialType.METAL, 2).tier(MachineTier.ULV)
                .input("minecraft:redstone", 2)
                .input(MaterialPart.WIRE_1X, MaterialType.METAL, 8).tier(MachineTier.ULV)
                .tool(Tool.HAMMER, 4)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:magnetic_separator").build());

        out.add(steamAssembly("steam_boiler", MachineTier.ULV, 8, 2, 2, 0)
                .input(Component.PRESSURE_VESSEL, MaterialType.METAL, 1).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .input(Component.HEAT_EXCHANGER_PLATE, MaterialType.METAL, 4).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .input("minecraft:bricks", 2)
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_boiler").build());
        out.add(steamAssembly("steam_solar_boiler", MachineTier.ULV, 4, 2, 2, 0)
                .input(Component.PRESSURE_VESSEL, MaterialType.METAL, 1).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .input(Component.HEAT_EXCHANGER_PLATE, MaterialType.METAL, 6).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .input("minecraft:glass", 4)
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_solar_boiler").build());
        out.add(steamAssembly("steam_liquid_fuel_boiler", MachineTier.ULV, 8, 2, 4, 0)
                .input(Component.PRESSURE_VESSEL, MaterialType.METAL, 1).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .input(Component.HEAT_EXCHANGER_PLATE, MaterialType.METAL, 4).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .input(Component.IMPELLER, MaterialType.METAL, 1).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_liquid_fuel_boiler").build());
        out.add(steamAssembly("steam_hammer", MachineTier.ULV, 6, 4, 2, 2)
                .input(Component.CYLINDER, MaterialType.METAL, 1).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .input(Component.PRESS_SLIDE, MaterialType.METAL, 1).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_hammer").build());
        out.add(steamAssembly("steam_compressor", MachineTier.ULV, 6, 2, 4, 2)
                .input(Component.CYLINDER, MaterialType.METAL, 2).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .input(Component.CRANK_LINKAGE, MaterialType.METAL, 1).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_compressor").build());
        out.add(steamAssembly("steam_extruder", MachineTier.ULV, 8, 3, 2, 3)
                .input(Component.CYLINDER, MaterialType.METAL, 1).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .input(Component.EXTRUSION_HEAD, MaterialType.METAL, 1).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .input(MaterialPart.GUIDE_RAIL, MaterialType.METAL, 2).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_extruder").build());
        out.add(steamAssembly("steam_autoclave", MachineTier.ULV, 10, 2, 4, 0)
                .input(Component.PRESSURE_VESSEL, MaterialType.METAL, 2).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .input(Component.HEAT_EXCHANGER_PLATE, MaterialType.METAL, 2).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .input("minecraft:glass", 2)
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_autoclave").build());
        out.add(steamAssembly("steam_extractor", MachineTier.ULV, 6, 3, 3, 2)
                .input(Component.CYLINDER, MaterialType.METAL, 1).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .input(Component.IMPELLER, MaterialType.METAL, 1).tier(MachineTier.ULV).sameMaterialAs("steam_frame")
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_extractor").build());
        out.add(steamAssembly("steam_lv_boiler", MachineTier.LV, 8, 2, 2, 0)
                .input(Component.PRESSURE_VESSEL, MaterialType.METAL, 1).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .input(Component.HEAT_EXCHANGER_PLATE, MaterialType.METAL, 4).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .input("minecraft:bricks", 2)
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_lv_boiler").build());
        out.add(steamAssembly("steam_lv_solar_boiler", MachineTier.LV, 4, 2, 2, 0)
                .input(Component.PRESSURE_VESSEL, MaterialType.METAL, 1).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .input(Component.HEAT_EXCHANGER_PLATE, MaterialType.METAL, 6).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .input("minecraft:glass", 4)
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_lv_solar_boiler").build());
        out.add(steamAssembly("steam_lv_liquid_fuel_boiler", MachineTier.LV, 8, 2, 4, 0)
                .input(Component.PRESSURE_VESSEL, MaterialType.METAL, 1).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .input(Component.HEAT_EXCHANGER_PLATE, MaterialType.METAL, 4).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .input(Component.IMPELLER, MaterialType.METAL, 1).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_lv_liquid_fuel_boiler").build());
        out.add(steamAssembly("steam_lv_hammer", MachineTier.LV, 6, 4, 2, 2)
                .input(Component.CYLINDER, MaterialType.METAL, 1).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .input(Component.PRESS_SLIDE, MaterialType.METAL, 1).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_lv_hammer").build());
        out.add(steamAssembly("steam_lv_compressor", MachineTier.LV, 6, 2, 4, 2)
                .input(Component.CYLINDER, MaterialType.METAL, 2).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .input(Component.CRANK_LINKAGE, MaterialType.METAL, 1).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_lv_compressor").build());
        out.add(steamAssembly("steam_lv_extruder", MachineTier.LV, 8, 3, 2, 3)
                .input(Component.CYLINDER, MaterialType.METAL, 1).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .input(Component.EXTRUSION_HEAD, MaterialType.METAL, 1).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .input(MaterialPart.GUIDE_RAIL, MaterialType.METAL, 2).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_lv_extruder").build());
        out.add(steamAssembly("steam_lv_autoclave", MachineTier.LV, 10, 2, 4, 0)
                .input(Component.PRESSURE_VESSEL, MaterialType.METAL, 2).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .input(Component.HEAT_EXCHANGER_PLATE, MaterialType.METAL, 2).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .input("minecraft:glass", 2)
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_lv_autoclave").build());
        out.add(steamAssembly("steam_lv_extractor", MachineTier.LV, 6, 3, 3, 2)
                .input(Component.CYLINDER, MaterialType.METAL, 1).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .input(Component.IMPELLER, MaterialType.METAL, 1).tier(MachineTier.LV).sameMaterialAs("steam_frame")
                .tool(Tool.HAMMER, 6)
                .tool(Tool.FILE, 2)
                .baseBlockOutput("industron:steam_lv_extractor").build());

        // Private Create machine construction, one recipe per actual block.
        out.add(kineticAssembly("create_mechanical_mixer", 6, 2, 2, 2)
                .input(Component.MIXING_HEAD, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(Component.GEAR, MaterialType.METAL, 2).tier(MachineTier.ULV)
                .tool(Tool.FILE, 4)
                .baseBlockOutput("create:mechanical_mixer").build());
        out.add(kineticAssembly("create_encased_fan", 6, 2, 2, 2)
                .input(Component.FAN_ASSEMBLY, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .tool(Tool.FILE, 4)
                .baseBlockOutput("create:encased_fan").build());
        out.add(kineticAssembly("create_mechanical_press", 6, 2, 2, 2)
                .input(Component.PRESS_SLIDE, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(Component.CRANK_LINKAGE, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .tool(Tool.FILE, 4)
                .baseBlockOutput("create:mechanical_press").build());
        out.add(kineticAssembly("create_crushing_wheel", 6, 2, 2, 2)
                .input(Component.CRUSHING_DRUM, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(Component.GEAR, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input("minecraft:andesite", 8)
                .tool(Tool.FILE, 4)
                .baseBlockOutput("create:crushing_wheel").build());
        out.add(kineticAssembly("create_mechanical_saw", 6, 2, 2, 2)
                .input(Component.CUTTER, MaterialType.METAL, 4).tier(MachineTier.ULV)
                .input(Component.ROTOR, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .tool(Tool.FILE, 4)
                .baseBlockOutput("create:mechanical_saw").build());
        out.add(kineticAssembly("create_mechanical_drill", 6, 2, 2, 2)
                .input(Component.CHUCK, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(Component.CUTTER, MaterialType.METAL, 3).tier(MachineTier.ULV)
                .tool(Tool.FILE, 4)
                .baseBlockOutput("create:mechanical_drill").build());
        out.add(kineticAssembly("create_mechanical_pump", 6, 2, 2, 2)
                .input(Component.IMPELLER, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(MaterialPart.FLANGE, MaterialType.METAL, 2).tier(MachineTier.ULV)
                .tool(Tool.FILE, 4)
                .baseBlockOutput("create:mechanical_pump").build());
        out.add(kineticAssembly("create_deployer", 6, 2, 2, 2)
                .input(Component.CRANK_LINKAGE, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input(Component.CHUCK, MaterialType.METAL, 1).tier(MachineTier.ULV)
                .input("minecraft:leather", 2)
                .tool(Tool.FILE, 4)
                .baseBlockOutput("create:deployer").build());
    }

    /** Private constructions share fixed Create outputs but retain distinct alloy ingredients. */
    private static void addAndesiteConstructionRecipes(List<AssemblyRecipeDefinition> out) {
        for (IndustrialMaterial alloy : net.mads.industron.material.defenitions.CompoundMaterials.andesiteAlloys()) {
            String suffix = alloy.id().equals("andesite_alloy") ? "" : "/" + alloy.id();
            out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/create_shaft" + suffix)
                    .level(1).baseItemInput(MaterialPart.ROD, MaterialType.METAL).tier(MachineTier.ULV)
                    .input(MaterialPart.INGOT, alloy, 2)
                    .input(Component.RING, MaterialType.METAL, 2).tier(MachineTier.ULV)
                    .tool(Tool.FILE, 2).baseBlockOutput("create:shaft").build());
            out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/andesite_casing" + suffix)
                    .level(1).baseBlockInput(MaterialPart.LOG, MaterialType.WOOD).tier(MachineTier.ULV)
                    .tool(Tool.AXE, 2)
                    .input(Component.WOOD_PLATE, MaterialType.WOOD, 2)
                    .input(MaterialPart.INGOT, alloy, 4)
                    .input(Component.SCREW, MaterialType.METAL, 4).tier(MachineTier.ULV)
                    .tool(Tool.MALLET, 4).baseBlockOutput("create:andesite_casing").build());
        }
    }

    private static AssemblyRecipeDefinition.Builder kineticAssembly(String id, int plates, int rods, int rings, int gears) {
        var recipe = AssemblyRecipeDefinition.recipe("recipes/assembly/" + id).level(1)
                .baseBlockInput("create:andesite_casing")
                .input(Component.PLATE, MaterialType.METAL, plates).tier(MachineTier.ULV)
                .input(Component.ROD, MaterialType.METAL, rods).tier(MachineTier.ULV)
                .input(Component.RING, MaterialType.METAL, rings).tier(MachineTier.ULV);
        if (gears > 0) recipe.input(Component.SMALL_GEAR, MaterialType.METAL, gears).tier(MachineTier.ULV);
        return recipe.input(Component.BEARING, MaterialType.METAL, 2).tier(MachineTier.ULV)
                .input("create:shaft")
                .tool(Tool.WRENCH, 4);
    }

    private static AssemblyRecipeDefinition.Builder steamAssembly(String id, MachineTier metalTier, int plates, int rods, int rings, int gears) {
        var recipe = AssemblyRecipeDefinition.recipe("recipes/assembly/" + id).level(1)
                .baseBlockInput(MaterialPart.FRAME, MaterialType.METAL).tier(metalTier).capture("steam_frame")
                .input(Component.PLATE, MaterialType.METAL, plates).tier(metalTier).sameMaterialAs("steam_frame")
                .input(Component.ROD, MaterialType.METAL, rods).tier(metalTier).sameMaterialAs("steam_frame")
                .input(Component.RING, MaterialType.METAL, rings).tier(metalTier).sameMaterialAs("steam_frame");
        if (gears > 0) recipe.input(Component.SMALL_GEAR, MaterialType.METAL, gears).tier(metalTier).sameMaterialAs("steam_frame");
        recipe.input("industron:fiber_gasket", metalTier == MachineTier.LV ? 4 : 2);
        recipe.input(Component.SAFETY_VALVE, MaterialType.METAL, 1).tier(metalTier).sameMaterialAs("steam_frame");
        if (metalTier == MachineTier.LV) {
            recipe.input(Component.REINFORCED_PLATE, MaterialType.METAL, 4).tier(metalTier).sameMaterialAs("steam_frame")
                    .input(MaterialPart.FLANGE, MaterialType.METAL, 4).tier(metalTier).sameMaterialAs("steam_frame")
                    .tool(Tool.HAMMER, 8)
                .tool(Tool.WRENCH, 4);
        }
        return recipe;
    }

    /** One fixed minecraft:campfire output; wood species only changes which valid input route is used. */
    private static void addCampfireRecipes(List<AssemblyRecipeDefinition> out) {
        for (WoodMaterial wood : WoodMaterials.ALL) {
            if (!woodHas(wood, MaterialPart.LOG)
                    || !woodHas(wood, MaterialPart.STICK)
                    || !woodHas(wood, MaterialPart.BARK)) {
                continue;
            }
            out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/campfire/" + wood.id())
                    .level(1)
                    .baseBlockInput(woodId(wood, MaterialPart.LOG))
                    .input(woodId(wood, MaterialPart.STICK), 3)
                    .input(woodId(wood, MaterialPart.BARK))
                    .baseItemOutput("minecraft:campfire")
                    .build());
        }
    }

    private static boolean woodHas(WoodMaterial wood, MaterialPart part) {
        if (wood.hasExistingPart(part)) return true;
        if (part.isItem()) return wood.generatedForms().contains(part);
        if (part.isBlock()) {
            return StructureMaterialGenerator.blockDefinitions(wood).stream()
                    .anyMatch(definition -> definition.part().filter(candidate -> candidate == part).isPresent());
        }
        return false;
    }

    private static String woodId(WoodMaterial wood, MaterialPart part) {
        if (wood.hasExistingPart(part)) return wood.existingPart(part).toString();
        return Industron.MOD_ID + ":" + part.registryName(wood);
    }

    public static AssemblyRecipeDefinition find(String id) {
        return ALL.stream()
                .filter(recipe -> recipe.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    /** Private/fixed-output recipes consolidated here to keep Assembly's folder structure deterministic. */
    private static final class BasicAssemblyWorkbenchRecipes {
            public static final List<AssemblyRecipeDefinition> ALL = buildAll();

            private BasicAssemblyWorkbenchRecipes() {
            }

            private static List<AssemblyRecipeDefinition> buildAll() {
                List<AssemblyRecipeDefinition> result = new ArrayList<>();
                for (WoodMaterial wood : WoodMaterials.ALL) {
                    if (!wood.hasExistingPart(MaterialPart.LOG)) continue;
                    if (!wood.generatedForms().contains(MaterialPart.STICK)) continue;

                    result.add(AssemblyRecipeDefinition
                            .recipe("workbench/basic/" + wood.id())
                            .level(1)
                            .baseBlockInput(wood.existingPart(MaterialPart.LOG).toString())
                            .input(Component.STICK, wood, 64)
                            .baseBlockOutput("industron:basic_assembly_workbench")
                            .build());
                }
                return List.copyOf(result);
            }
    }

    /** Private/fixed-output recipes consolidated here to keep Assembly's folder structure deterministic. */
    private static final class VanillaEarlyGameAssemblyRecipes {
            private static final List<BedColor> BED_COLORS = List.of(
                    new BedColor("white", "minecraft:white_wool", "minecraft:white_bed"),
                    new BedColor("orange", "minecraft:orange_wool", "minecraft:orange_bed"),
                    new BedColor("magenta", "minecraft:magenta_wool", "minecraft:magenta_bed"),
                    new BedColor("light_blue", "minecraft:light_blue_wool", "minecraft:light_blue_bed"),
                    new BedColor("yellow", "minecraft:yellow_wool", "minecraft:yellow_bed"),
                    new BedColor("lime", "minecraft:lime_wool", "minecraft:lime_bed"),
                    new BedColor("pink", "minecraft:pink_wool", "minecraft:pink_bed"),
                    new BedColor("gray", "minecraft:gray_wool", "minecraft:gray_bed"),
                    new BedColor("light_gray", "minecraft:light_gray_wool", "minecraft:light_gray_bed"),
                    new BedColor("cyan", "minecraft:cyan_wool", "minecraft:cyan_bed"),
                    new BedColor("purple", "minecraft:purple_wool", "minecraft:purple_bed"),
                    new BedColor("blue", "minecraft:blue_wool", "minecraft:blue_bed"),
                    new BedColor("brown", "minecraft:brown_wool", "minecraft:brown_bed"),
                    new BedColor("green", "minecraft:green_wool", "minecraft:green_bed"),
                    new BedColor("red", "minecraft:red_wool", "minecraft:red_bed"),
                    new BedColor("black", "minecraft:black_wool", "minecraft:black_bed")
            );

            public static final List<AssemblyRecipeDefinition> ALL = buildAll();

            private VanillaEarlyGameAssemblyRecipes() {
            }

            private static List<AssemblyRecipeDefinition> buildAll() {
                List<AssemblyRecipeDefinition> result = new ArrayList<>();

                addGenericWoodUtilities(result);
                addStoneWoodUtilities(result);
                addScaffolding(result);
                addBookAndLead(result);
                addMetalUtilities(result);

                return List.copyOf(result);
            }

            private static void addGenericWoodUtilities(List<AssemblyRecipeDefinition> out) {
                // Only WoodMaterial definitions expose MaterialPart.STICK, so ANY is already a wood-only set here.
                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/torch")
                        .level(1)
                        .baseItemInput(MaterialPart.STICK, MaterialType.ANY)
                        .input("minecraft:charcoal")
                        .baseItemOutput("minecraft:torch")
                        .build());

                // Bow and fishing equipment are assembled through EquipmentAssemblyRecipes.

                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/arrow")
                        .level(1)
                        .baseItemInput(MaterialPart.STICK, MaterialType.ANY)
                        .input("minecraft:flint")
                        .input("minecraft:feather")
                        .tool(Tool.KNIFE, 1)
                        .baseItemOutput("minecraft:arrow")
                        .build());

                // Four Component.STICK totals. The physical base is the first stick; its string is the first step.
                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/item_frame")
                        .level(1)
                        .baseItemInput(MaterialPart.STICK, MaterialType.ANY)
                        .input(Component.STICK)
                        .input(PlantPart.STRING)
                        .input(Component.STICK, 2)
                        .baseItemOutput("minecraft:item_frame")
                        .build());

                for (BedColor bed : BED_COLORS) {
                    out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/bed/" + bed.name())
                            .level(1)
                            .baseBlockInput(MaterialPart.PLANKS, MaterialType.WOOD)
                            .input(bed.woolId(), 3)
                            .input(MaterialPart.PLANKS, MaterialType.WOOD, 2)
                            .tool(Tool.MALLET, 2)
                            .baseItemOutput(bed.bedId())
                            .build());
                }

                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/loom")
                        .level(1)
                        .baseBlockInput(MaterialPart.PLANKS, MaterialType.WOOD)
                        .input(PlantPart.STRING, 2)
                        .input(MaterialPart.PLANKS, MaterialType.WOOD)
                        .tool(Tool.MALLET, 1)
                        .baseBlockOutput("minecraft:loom")
                        .build());

                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/cartography_table")
                        .level(1)
                        .baseBlockInput(MaterialPart.PLANKS, MaterialType.WOOD)
                        .input("minecraft:paper", 2)
                        .input(MaterialPart.PLANKS, MaterialType.WOOD, 3)
                        .tool(Tool.MALLET, 1)
                        .baseBlockOutput("minecraft:cartography_table")
                        .build());

                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/tripwire_hook")
                        .level(1)
                        .baseBlockInput(MaterialPart.PLANKS, MaterialType.WOOD)
                        .input(MaterialPart.STICK)
                        .input(MaterialPart.SHORT_ROD, MaterialType.METAL)
                        .tool(Tool.MALLET, 1)
                        .baseBlockOutput("minecraft:tripwire_hook")
                        .build());

                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/composter")
                        .level(1)
                        .baseBlockInput(MaterialPart.SLAB, MaterialType.WOOD)
                        .input(MaterialPart.SLAB, MaterialType.WOOD, 4)
                        .input(PlantPart.STRING, 8)
                        .baseBlockOutput("minecraft:composter")
                        .build());

                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/lectern")
                        .level(1)
                        .baseBlockInput(MaterialPart.BOOKSHELF, MaterialType.WOOD)
                        .input(MaterialPart.SLAB, MaterialType.WOOD, 4)
                        .tool(Tool.MALLET, 2)
                        .baseBlockOutput("minecraft:lectern")
                        .build());
            }

            private static void addStoneWoodUtilities(List<AssemblyRecipeDefinition> out) {
                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/flint_and_pebble")
                        .level(1)
                        .baseItemInput(MaterialPart.PEBBLE, AssemblyMaterialSelector.fixed(StoneMaterials.STONE))
                        .input("minecraft:flint")
                        .baseItemOutput("industron:flint_and_pebble")
                        .build());

                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/lever")
                        .level(1)
                        .baseBlockInput(MaterialPart.PEBBLE, MaterialType.STONE)
                        .input("minecraft:redstone")
                        .input(MaterialPart.STICK)
                        .tool(Tool.MALLET, 1)
                        .baseBlockOutput("minecraft:lever")
                        .build());

                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/armor_stand")
                        .level(1)
                        .baseBlockInput(MaterialPart.SLAB, MaterialType.STONE)
                        .input(MaterialPart.STICK, 6)
                        .tool(Tool.MALLET, 2)
                        .baseItemOutput("minecraft:armor_stand")
                        .build());
            }

            private static void addScaffolding(List<AssemblyRecipeDefinition> out) {
                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/scaffolding")
                        .level(1)
                        .baseBlockInput("minecraft:bamboo")
                        // Seven complete Component.BAMBOO branches plus the placed base bamboo = 8 bamboo.
                        // Each component contributes 2 string; the final 2 string bind the placed base bamboo.
                        .input(Component.BAMBOO, 7)
                        .input(PlantPart.STRING, 2)
                        .baseBlockOutput("minecraft:scaffolding")
                        .build());
            }

            private static void addBookAndLead(List<AssemblyRecipeDefinition> out) {
                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/book")
                        .level(1)
                        .baseItemInput("minecraft:leather")
                        .input("minecraft:paper", 3)
                        .input(PlantPart.STRING)
                        .tool(Tool.KNIFE, 1)
                        .baseItemOutput("minecraft:book")
                        .build());

                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/lead")
                        .level(1)
                        .baseItemInput("minecraft:slime_ball")
                        .input(PlantPart.STRING, 4)
                        .baseItemOutput("minecraft:lead")
                        .build());
            }

            private static void addMetalUtilities(List<AssemblyRecipeDefinition> out) {
                out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/flint_and_steel")
                        .level(1)
                        .baseItemInput("minecraft:flint")
                        .input(MaterialPart.INGOT, MaterialType.METAL)
                        .stat(Stats.MELTING_POINT).atLeast(200)
                        .baseItemOutput("minecraft:flint_and_steel")
                        .build());

                // These retain per-material definitions because their separate metal parts must be the same material.
                for (var metal : IndustrialMaterials.ALL) {
                    if (!metal.properties().metal()) continue;

                    if (MaterialRecipeHelper.hasItems(metal, MaterialPart.SMALL_RING, MaterialPart.SHORT_ROD)) {
                        out.add(AssemblyRecipeDefinition.recipe("recipes/assembly/vanilla/chain/" + metal.id())
                                .level(1)
                                .baseItemInput(MaterialPart.SMALL_RING, metal)
                                .input(MaterialPart.SHORT_ROD, metal)
                                .input(MaterialPart.SMALL_RING, metal)
                                .tool(Tool.HAMMER, 1)
                                .baseItemOutput("minecraft:chain")
                                .build());
                    }


                }
            }

            private record BedColor(String name, String woolId, String bedId) {
            }
    }

    /** Private/fixed-output recipes consolidated here to keep Assembly's folder structure deterministic. */
    private static final class ComponentAssemblyRecipes {
            public static final List<AssemblyRecipeDefinition> ALL = List.of(
                    AssemblyRecipeDefinition
                            .recipe("components/sifter_mesh")
                            .level(1)
                            .baseItemInput(PlantPart.STRING)
                            .input(PlantPart.STRING, 3)
                            .baseItemOutput("industron:sifter_mesh")
                            .build()
            );

            private ComponentAssemblyRecipes() {
            }
    }

    /** Private/fixed-output recipes consolidated here to keep Assembly's folder structure deterministic. */
    private static final class CreateEarlyKineticAssemblyRecipes {
            public static final List<AssemblyRecipeDefinition> ALL = List.of(
                    // One small wooden gear mounted on a pegged wooden shaft.
                    AssemblyRecipeDefinition.recipe("create_early_kinetics/cogwheel")
                            .level(1)
                            .baseItemInput(MaterialPart.SMALL_GEAR, MaterialType.WOOD)
                            .input(Component.WOOD_SHAFT, MaterialType.WOOD)
                            .baseBlockOutput("create:cogwheel")
                            .build(),

                    // The full-size wooden gear uses the same pegged shaft component.
                    AssemblyRecipeDefinition.recipe("create_early_kinetics/large_cogwheel")
                            .level(1)
                            .baseItemInput(MaterialPart.GEAR, MaterialType.WOOD)
                            .input(Component.WOOD_SHAFT, MaterialType.WOOD)
                            .baseBlockOutput("create:large_cogwheel")
                            .build(),

                    // A real stone-family slab is the grinding body. The wooden cogwheel drives it and
                    // the two wooden rings retain/space the rotating assembly.
                    AssemblyRecipeDefinition.recipe("create_early_kinetics/millstone")
                            .level(1)
                            .baseBlockInput(MaterialPart.SLAB, MaterialType.STONE)
                            .input("create:cogwheel")
                            .input(MaterialPart.RING, MaterialType.WOOD, 2)
                            .tool(Tool.HAMMER, 4)
                            .tool(Tool.CHISEL, 4)
                            .baseBlockOutput("create:millstone")
                            .build(),

                    // Four plain wooden sticks form the crank arm/handle. The drivetrain side is still
                    // the pegged wooden-shaft component, so every shaft used here accounts for four pegs.
                    AssemblyRecipeDefinition.recipe("create_early_kinetics/hand_crank")
                            .level(1)
                            .baseItemInput(MaterialPart.STICK, MaterialType.WOOD)
                            .input(MaterialPart.STICK, MaterialType.WOOD, 3)
                            .input(Component.WOOD_SHAFT, MaterialType.WOOD)
                            .tool(Tool.KNIFE, 2)
                            .baseBlockOutput("create:hand_crank")
                            .build(),

                    // The casing is the physical gearbox body. Four mounted small wooden gears and
                    // two pegged shafts make up the internal transmission.
                    AssemblyRecipeDefinition.recipe("create_early_kinetics/gearbox")
                            .level(1)
                            .baseBlockInput("create:andesite_casing")
                            .input(Component.SMALL_WOOD_GEAR, MaterialType.WOOD, 4)
                            .input(Component.WOOD_SHAFT, MaterialType.WOOD, 2)
                            .baseBlockOutput("create:gearbox")
                            .build(),

                    // Build the wheel outward from one placed plank: install the axle/gear first,
                    // then mount eight pegged wooden plate sections as paddles.
                    AssemblyRecipeDefinition.recipe("create_early_kinetics/water_wheel")
                            .level(1)
                            .baseBlockInput(MaterialPart.PLANKS, MaterialType.WOOD)
                            .input(Component.WOOD_SHAFT, MaterialType.WOOD)
                            .input(Component.WOOD_GEAR, MaterialType.WOOD)
                            .input(Component.WOOD_PLATE, MaterialType.WOOD, 8)
                            .input(Component.BEARING, MaterialType.METAL, 2).tier(MachineTier.ULV)
                            .input(Component.SCREW, MaterialType.METAL, 8).tier(MachineTier.ULV)
                            .baseBlockOutput("create:water_wheel")
                            .build(),

                    // The large wheel is an extension of an already assembled water wheel.
                    AssemblyRecipeDefinition.recipe("create_early_kinetics/large_water_wheel")
                            .level(1)
                            .baseBlockInput("create:water_wheel")
                            .input(Component.WOOD_PLATE, MaterialType.WOOD, 8)
                            .input(Component.WOOD_PEG, MaterialType.WOOD, 8)
                            .input(Component.BEARING, MaterialType.METAL, 2).tier(MachineTier.ULV)
                            .input(Component.SCREW, MaterialType.METAL, 8).tier(MachineTier.ULV)
                            .baseBlockOutput("create:large_water_wheel")
                            .build(),

                    // The casing is the stationary body. The wooden ring remains a raw shaped part,
                    // while the shaft/gears and the final four retaining pegs are mounted components.
                    AssemblyRecipeDefinition.recipe("create_early_kinetics/windmill_bearing")
                            .level(1)
                            .baseBlockInput("create:andesite_casing")
                            .input(Component.WOOD_SHAFT, MaterialType.WOOD)
                            .input(MaterialPart.RING, MaterialType.WOOD)
                            .input(Component.SMALL_WOOD_GEAR, MaterialType.WOOD, 2)
                            .input(Component.WOOD_PEG, MaterialType.WOOD, 4)
                            .input(Component.BEARING, MaterialType.METAL, 2).tier(MachineTier.ULV)
                            .baseBlockOutput("create:windmill_bearing")
                            .build(),

                    // A plank is the physical workpiece for the primitive wooden sail frame.
                    AssemblyRecipeDefinition.recipe("create_early_kinetics/sail_frame")
                            .level(1)
                            .baseBlockInput(MaterialPart.PLANKS, MaterialType.WOOD)
                            .input(MaterialPart.STICK, MaterialType.WOOD, 4)
                            .input(Component.WOOD_PEG, MaterialType.WOOD, 4)
                            .input(PlantPart.STRING, 2)
                            .baseBlockOutput("create:sail_frame")
                            .build(),

                    // Cover the completed frame with a substantial amount of plant-derived string.
                    AssemblyRecipeDefinition.recipe("create_early_kinetics/sail")
                            .level(1)
                            .baseBlockInput("create:sail_frame")
                            .input(PlantPart.STRING, 16)
                            .baseBlockOutput("create:white_sail")
                            .build()
            );

            private CreateEarlyKineticAssemblyRecipes() {
            }
    }

    /** Private/fixed-output recipes consolidated here to keep Assembly's folder structure deterministic. */
    private static final class HeatingCoilAssemblyRecipes {
            public static final AssemblyRecipeDefinition ISKARIUM = AssemblyRecipeDefinition.recipe("coils/iskarium")
                    .level(WorkbenchLevels.forTier(Metal.ISKARIUM.tier()))
                    .baseBlockInput(Material.FRAME, MaterialType.METAL)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(100)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(130)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(130)
                    .stat(Stats.YIELD_STRENGTH).atLeast(130)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(130)
                    .stat(Stats.FATIGUE_RESISTANCE).atLeast(130)

                    .input(Component.VERY_LONG_ROD, MaterialType.METAL, 4)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(100)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(130)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(130)
                    .stat(Stats.YIELD_STRENGTH).atLeast(130)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(130)
                    .stat(Stats.THERMAL_SHOCK_RESISTANCE).atLeast(130)

                    .input(Component.REINFORCED_PLATE, MaterialType.METAL, 4)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(100)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(130)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(130)
                    .stat(Stats.YIELD_STRENGTH).atLeast(130)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(130)
                    .stat(Stats.FATIGUE_RESISTANCE).atLeast(130)

                    .input(Component.HEAT_EXCHANGER_PLATE, MaterialType.METAL, 2)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(100)
                    .stat(Stats.THERMAL_CONDUCTIVITY).atLeast(130)
                    .stat(Stats.THERMAL_SHOCK_RESISTANCE).atLeast(130)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(130)
                    .input(Material.COIL, Metal.ISKARIUM, 4)
                    .input(Material.FINE_WIRE, Metal.ISKARIUM, 16)
                    .input(Material.WIRE_8X, Metal.ISKARIUM, 2)
                    .baseBlockOutput(Industron.MOD_ID + ":" + CoilDefinitions.ISKARIUM.blockId())
                    .build();

            public static final AssemblyRecipeDefinition JUBREX = AssemblyRecipeDefinition.recipe("coils/jubrex")
                    .level(WorkbenchLevels.forTier(Metal.JUBREX.tier()))
                    .baseBlockInput(Material.FRAME, MaterialType.METAL)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(600)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(195)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(195)
                    .stat(Stats.YIELD_STRENGTH).atLeast(195)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(195)
                    .stat(Stats.FATIGUE_RESISTANCE).atLeast(195)

                    .input(Component.VERY_LONG_ROD, MaterialType.METAL, 4)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(600)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(195)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(195)
                    .stat(Stats.YIELD_STRENGTH).atLeast(195)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(195)
                    .stat(Stats.THERMAL_SHOCK_RESISTANCE).atLeast(195)

                    .input(Component.REINFORCED_PLATE, MaterialType.METAL, 4)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(600)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(195)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(195)
                    .stat(Stats.YIELD_STRENGTH).atLeast(195)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(195)
                    .stat(Stats.FATIGUE_RESISTANCE).atLeast(195)

                    .input(Component.HEAT_EXCHANGER_PLATE, MaterialType.METAL, 2)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(600)
                    .stat(Stats.THERMAL_CONDUCTIVITY).atLeast(195)
                    .stat(Stats.THERMAL_SHOCK_RESISTANCE).atLeast(195)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(195)

                    .input(Material.COIL, Metal.JUBREX, 4)
                    .input(Material.FINE_WIRE, Metal.JUBREX, 16)
                    .input(Material.WIRE_8X, Metal.JUBREX, 2)
                    .baseBlockOutput(Industron.MOD_ID + ":" + CoilDefinitions.JUBREX.blockId())
                    .build();

            public static final AssemblyRecipeDefinition HORDELYRA = AssemblyRecipeDefinition.recipe("coils/hordelyra")
                    .level(WorkbenchLevels.forTier(Metal.HORDELYRA.tier()))
                    .baseBlockInput(Material.FRAME, MaterialType.METAL)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(1_000)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(260)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(260)
                    .stat(Stats.YIELD_STRENGTH).atLeast(260)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(260)
                    .stat(Stats.FATIGUE_RESISTANCE).atLeast(260)

                    .input(Component.VERY_LONG_ROD, MaterialType.METAL, 4)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(1_000)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(260)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(260)
                    .stat(Stats.YIELD_STRENGTH).atLeast(260)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(260)
                    .stat(Stats.THERMAL_SHOCK_RESISTANCE).atLeast(260)

                    .input(Component.REINFORCED_PLATE, MaterialType.METAL, 4)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(1_000)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(260)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(260)
                    .stat(Stats.YIELD_STRENGTH).atLeast(260)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(260)
                    .stat(Stats.FATIGUE_RESISTANCE).atLeast(260)

                    .input(Component.HEAT_EXCHANGER_PLATE, MaterialType.METAL, 2)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(1_000)
                    .stat(Stats.THERMAL_CONDUCTIVITY).atLeast(260)
                    .stat(Stats.THERMAL_SHOCK_RESISTANCE).atLeast(260)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(260)

                    .input(Material.COIL, Metal.HORDELYRA, 4)
                    .input(Material.FINE_WIRE, Metal.HORDELYRA, 16)
                    .input(Material.WIRE_8X, Metal.HORDELYRA, 2)
                    .baseBlockOutput(Industron.MOD_ID + ":" + CoilDefinitions.HORDELYRA.blockId())
                    .build();

            public static final AssemblyRecipeDefinition MAVLOX = AssemblyRecipeDefinition.recipe("coils/mavlox")
                    .level(WorkbenchLevels.forTier(Metal.MAVLOX.tier()))
                    .baseBlockInput(Material.FRAME, MaterialType.METAL)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(1_500)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(325)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(325)
                    .stat(Stats.YIELD_STRENGTH).atLeast(325)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(325)
                    .stat(Stats.FATIGUE_RESISTANCE).atLeast(325)

                    .input(Component.VERY_LONG_ROD, MaterialType.METAL, 4)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(1_500)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(325)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(325)
                    .stat(Stats.YIELD_STRENGTH).atLeast(325)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(325)
                    .stat(Stats.THERMAL_SHOCK_RESISTANCE).atLeast(325)

                    .input(Component.REINFORCED_PLATE, MaterialType.METAL, 4)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(1_500)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(325)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(325)
                    .stat(Stats.YIELD_STRENGTH).atLeast(325)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(325)
                    .stat(Stats.FATIGUE_RESISTANCE).atLeast(325)

                    .input(Component.HEAT_EXCHANGER_PLATE, MaterialType.METAL, 2)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(1_500)
                    .stat(Stats.THERMAL_CONDUCTIVITY).atLeast(325)
                    .stat(Stats.THERMAL_SHOCK_RESISTANCE).atLeast(325)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(325)

                    .input(Material.COIL, Metal.MAVLOX, 4)
                    .input(Material.FINE_WIRE, Metal.MAVLOX, 16)
                    .input(Material.WIRE_8X, Metal.MAVLOX, 2)
                    .baseBlockOutput(Industron.MOD_ID + ":" + CoilDefinitions.MAVLOX.blockId())
                    .build();

            public static final AssemblyRecipeDefinition NERYKON = AssemblyRecipeDefinition.recipe("coils/nerykon")
                    .level(WorkbenchLevels.forTier(Metal.NERYKON.tier()))
                    .baseBlockInput(Material.FRAME, MaterialType.METAL)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(2_000)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(390)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(390)
                    .stat(Stats.YIELD_STRENGTH).atLeast(390)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(390)
                    .stat(Stats.FATIGUE_RESISTANCE).atLeast(390)

                    .input(Component.VERY_LONG_ROD, MaterialType.METAL, 4)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(2_000)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(390)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(390)
                    .stat(Stats.YIELD_STRENGTH).atLeast(390)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(390)
                    .stat(Stats.THERMAL_SHOCK_RESISTANCE).atLeast(390)

                    .input(Component.REINFORCED_PLATE, MaterialType.METAL, 4)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(2_000)
                    .stat(Stats.STRUCTURAL_STRENGTH).atLeast(390)
                    .stat(Stats.TENSILE_STRENGTH).atLeast(390)
                    .stat(Stats.YIELD_STRENGTH).atLeast(390)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(390)
                    .stat(Stats.FATIGUE_RESISTANCE).atLeast(390)

                    .input(Component.HEAT_EXCHANGER_PLATE, MaterialType.METAL, 2)
                    .stat(Stats.METAL).is(true)
                    .stat(Stats.MAX_OPERATING_TEMPERATURE).atLeast(2_000)
                    .stat(Stats.THERMAL_CONDUCTIVITY).atLeast(390)
                    .stat(Stats.THERMAL_SHOCK_RESISTANCE).atLeast(390)
                    .stat(Stats.OXIDATION_RESISTANCE).atLeast(390)

                    .input(Material.COIL, Metal.NERYKON, 4)
                    .input(Material.FINE_WIRE, Metal.NERYKON, 16)
                    .input(Material.WIRE_8X, Metal.NERYKON, 2)
                    .baseBlockOutput(Industron.MOD_ID + ":" + CoilDefinitions.NERYKON.blockId())
                    .build();

            public static final List<AssemblyRecipeDefinition> ALL = List.of(
                    ISKARIUM,
                    JUBREX,
                    HORDELYRA,
                    MAVLOX,
                    NERYKON
            );

            private HeatingCoilAssemblyRecipes() {
            }
    }

    /** Private/fixed-output recipes consolidated here to keep Assembly's folder structure deterministic. */
    private static final class BlastFurnaceAssemblyRecipes {
            public static final List<AssemblyRecipeDefinition> ALL = List.of(
                    AssemblyRecipeDefinition.recipe("recipes/assembly/blast_furnace")
                            .level(WorkbenchLevels.forTier(MachineTier.ULV))
                            .baseBlockInput("minecraft:bricks")
                            .input(MaterialPart.PLATE, MaterialType.METAL, 4)
                            .tier(MachineTier.ULV)
                            .tool(Tool.HAMMER, 1)
                            .baseBlockOutput("industron:blast_furnace")
                            .build()
            );

            private BlastFurnaceAssemblyRecipes() { }
    }

    /** Private/fixed-output recipes consolidated here to keep Assembly's folder structure deterministic. */
    private static final class AnvilAssemblyRecipes {
            private static final String ANVIL_MATERIAL = "anvil_material";

            public static final List<AssemblyRecipeDefinition> ALL = List.of(
                    AssemblyRecipeDefinition.recipe("recipes/assembly/anvil")
                            .baseBlockInput(MaterialPart.BLOCK, MaterialType.METAL)
                            .tier(MachineTier.LV)
                            .capture(ANVIL_MATERIAL)
                            .input(MaterialPart.INGOT, MaterialType.METAL, 4)
                            .tier(MachineTier.LV)
                            .sameMaterialAs(ANVIL_MATERIAL)
                            .input(MaterialPart.PLATE, MaterialType.METAL, 2)
                            .tier(MachineTier.LV)
                            .sameMaterialAs(ANVIL_MATERIAL)
                            .tool(Tool.HAMMER, 8)
                            .tier(MachineTier.ULV)
                            .tool(Tool.FILE, 4)
                            .tier(MachineTier.ULV)
                            .baseBlockOutput("minecraft:anvil")
                            .build()
            );

            private AnvilAssemblyRecipes() {
            }
    }
}
