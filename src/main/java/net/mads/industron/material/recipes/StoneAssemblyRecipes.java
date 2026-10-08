package net.mads.industron.material.recipes;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.recipe.recipes.assembly.WorkbenchLevels;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRecipeDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Manual stone-working routes generated from forms exposed by each StoneMaterial.
 * Pattern-carved full-block forms are deliberately excluded here and live in Chiseling.
 */
public final class StoneAssemblyRecipes {
    public static final List<AssemblyRecipeDefinition> ALL = buildAll();

    private StoneAssemblyRecipes() {
    }

    private static List<AssemblyRecipeDefinition> buildAll() {
        List<AssemblyRecipeDefinition> result = new ArrayList<>();
        for (StoneMaterial stone : StoneMaterials.ALL) {
            addPebbleCobbledRoutes(result, stone);
            addStoneShapeFamily(result, stone);
            addStonePolishing(result, stone);
            addPolishedStoneShapeFamily(result, stone);
            addStoneBrickShapeFamily(result, stone);
            addStoneTileShapeFamily(result, stone);
            addExplicitCutStoneShapeFamily(result, stone);
            addCutPolishing(result, stone);
            addPolishedCutStoneShapeFamily(result, stone);
            addCutStoneBrickShapeFamily(result, stone);
            addSmallStoneBrickShapeFamily(result, stone);
            addPolishedStoneBrickShapeFamily(result, stone);
            addSmoothStoneShapeFamily(result, stone);
            addButtonAndPressurePlate(result, stone);
        }
        return List.copyOf(result);
    }

    private static void addPebbleCobbledRoutes(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        Optional<ResourceLocation> pebbleBlock = StoneRecipeResolver.loosePebbleBlock(stone);
        Optional<ResourceLocation> pebbleItem = StoneRecipeResolver.item(stone, MaterialPart.PEBBLE);
        Optional<ResourceLocation> slab = StoneRecipeResolver.block(stone, MaterialPart.COBBLED_SLAB);
        Optional<ResourceLocation> stairs = StoneRecipeResolver.block(stone, MaterialPart.COBBLED_STAIRS);
        Optional<ResourceLocation> wall = StoneRecipeResolver.block(stone, MaterialPart.COBBLED_WALL);
        Optional<ResourceLocation> block = StoneRecipeResolver.block(stone, MaterialPart.COBBLED_STONE);
        if (pebbleBlock.isEmpty() || pebbleItem.isEmpty()) return;

        if (slab.isPresent()) {
            out.add(recipe(stone, "cobbled_slab")
                    .baseBlockInput(pebbleBlock.get().toString())
                    .input(pebbleItem.get().toString(), 3)
                    .baseBlockOutput(slab.get().toString())
                    .build());
        }
        if (slab.isPresent() && stairs.isPresent()) {
            out.add(recipe(stone, "cobbled_stairs")
                    .baseBlockInput(slab.get().toString())
                    .input(pebbleItem.get().toString(), 2)
                    .baseBlockOutput(stairs.get().toString())
                    .build());
        }
        if (slab.isPresent() && wall.isPresent()) {
            out.add(recipe(stone, "cobbled_wall")
                    .baseBlockInput(slab.get().toString())
                    .tool(Tool.HAMMER, 1)
                    .input(pebbleItem.get().toString(), 2)
                    .baseBlockOutput(wall.get().toString())
                    .build());
        }
        if (stairs.isPresent() && block.isPresent()) {
            out.add(recipe(stone, "cobbled_block")
                    .baseBlockInput(stairs.get().toString())
                    .input(pebbleItem.get().toString(), 2)
                    .baseBlockOutput(block.get().toString())
                    .build());
        }
    }

    private static void addStoneShapeFamily(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        addShape(out, stone, MaterialPart.STONE, MaterialPart.SLAB, "stone_slab", 2, b -> b.tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.STONE, MaterialPart.STAIRS, "stone_stairs", 1,
                b -> b.tool(Tool.HAMMER, 1).tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.STONE, MaterialPart.WALL, "stone_wall", 1,
                b -> b.tool(Tool.FILE, 1).tool(Tool.SAW, 1));
    }

    /** One File operation per cube face: STONE -> POLISHED_STONE. */
    private static void addStonePolishing(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        addShape(out, stone, MaterialPart.STONE, MaterialPart.POLISHED_STONE,
                "polished_stone", 1, b -> b.tool(Tool.FILE, 6));
    }

    private static void addPolishedStoneShapeFamily(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        addShape(out, stone, MaterialPart.POLISHED_STONE, MaterialPart.POLISHED_SLAB, "polished_slab", 2,
                b -> b.tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.POLISHED_STONE, MaterialPart.POLISHED_STAIRS, "polished_stairs", 1,
                b -> b.tool(Tool.HAMMER, 1).tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.POLISHED_STONE, MaterialPart.POLISHED_WALL, "polished_wall", 1,
                b -> b.tool(Tool.FILE, 1).tool(Tool.SAW, 1));
    }

    private static void addStoneBrickShapeFamily(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        addShape(out, stone, MaterialPart.STONE_BRICKS, MaterialPart.STONE_BRICK_SLAB, "stone_brick_slab", 2,
                b -> b.tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.STONE_BRICKS, MaterialPart.STONE_BRICK_STAIRS, "stone_brick_stairs", 1,
                b -> b.tool(Tool.HAMMER, 1).tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.STONE_BRICKS, MaterialPart.STONE_BRICK_WALL, "stone_brick_wall", 1,
                b -> b.tool(Tool.FILE, 1).tool(Tool.SAW, 1));
    }

    private static void addStoneTileShapeFamily(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        addShape(out, stone, MaterialPart.STONE_TILES, MaterialPart.STONE_TILE_SLAB, "stone_tile_slab", 2,
                b -> b.tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.STONE_TILES, MaterialPart.STONE_TILE_STAIRS, "stone_tile_stairs", 1,
                b -> b.tool(Tool.HAMMER, 1).tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.STONE_TILES, MaterialPart.STONE_TILE_WALL, "stone_tile_wall", 1,
                b -> b.tool(Tool.FILE, 1).tool(Tool.SAW, 1));
    }

    /** Shape recipes for the CUT_STONE family. */
    private static void addExplicitCutStoneShapeFamily(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        addShape(out, stone, MaterialPart.CUT_STONE, MaterialPart.CUT_STONE_SLAB, "cut_stone_slab", 2,
                b -> b.tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.CUT_STONE, MaterialPart.CUT_STONE_STAIRS, "cut_stone_stairs", 1,
                b -> b.tool(Tool.HAMMER, 1).tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.CUT_STONE, MaterialPart.CUT_STONE_WALL, "cut_stone_wall", 1,
                b -> b.tool(Tool.FILE, 1).tool(Tool.SAW, 1));
    }

    /** One File operation per cube face: CUT_STONE -> POLISHED_CUT_STONE. */
    private static void addCutPolishing(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        addShape(out, stone, MaterialPart.CUT_STONE, MaterialPart.POLISHED_CUT_STONE,
                "polished_cut_stone", 1, b -> b.tool(Tool.FILE, 6));
    }

    private static void addPolishedCutStoneShapeFamily(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        addShape(out, stone, MaterialPart.POLISHED_CUT_STONE, MaterialPart.POLISHED_CUT_STONE_SLAB, "polished_cut_stone_slab", 2,
                b -> b.tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.POLISHED_CUT_STONE, MaterialPart.POLISHED_CUT_STONE_STAIRS, "polished_cut_stone_stairs", 1,
                b -> b.tool(Tool.HAMMER, 1).tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.POLISHED_CUT_STONE, MaterialPart.POLISHED_CUT_STONE_WALL, "polished_cut_stone_wall", 1,
                b -> b.tool(Tool.FILE, 1).tool(Tool.SAW, 1));
    }

    private static void addCutStoneBrickShapeFamily(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        addShape(out, stone, MaterialPart.CUT_STONE_BRICKS, MaterialPart.CUT_STONE_BRICK_SLAB, "cut_stone_brick_slab", 2,
                b -> b.tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.CUT_STONE_BRICKS, MaterialPart.CUT_STONE_BRICK_STAIRS, "cut_stone_brick_stairs", 1,
                b -> b.tool(Tool.HAMMER, 1).tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.CUT_STONE_BRICKS, MaterialPart.CUT_STONE_BRICK_WALL, "cut_stone_brick_wall", 1,
                b -> b.tool(Tool.FILE, 1).tool(Tool.SAW, 1));
    }

    private static void addSmallStoneBrickShapeFamily(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        addShape(out, stone, MaterialPart.SMALL_STONE_BRICKS, MaterialPart.SMALL_STONE_BRICK_SLAB, "small_stone_brick_slab", 2,
                b -> b.tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.SMALL_STONE_BRICKS, MaterialPart.SMALL_STONE_BRICK_STAIRS, "small_stone_brick_stairs", 1,
                b -> b.tool(Tool.HAMMER, 1).tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.SMALL_STONE_BRICKS, MaterialPart.SMALL_STONE_BRICK_WALL, "small_stone_brick_wall", 1,
                b -> b.tool(Tool.FILE, 1).tool(Tool.SAW, 1));
    }

    private static void addPolishedStoneBrickShapeFamily(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        addShape(out, stone, MaterialPart.POLISHED_STONE_BRICKS, MaterialPart.POLISHED_STONE_BRICK_SLAB, "polished_stone_brick_slab", 2,
                b -> b.tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.POLISHED_STONE_BRICKS, MaterialPart.POLISHED_STONE_BRICK_STAIRS, "polished_stone_brick_stairs", 1,
                b -> b.tool(Tool.HAMMER, 1).tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.POLISHED_STONE_BRICKS, MaterialPart.POLISHED_STONE_BRICK_WALL, "polished_stone_brick_wall", 1,
                b -> b.tool(Tool.FILE, 1).tool(Tool.SAW, 1));
    }

    private static void addSmoothStoneShapeFamily(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        addShape(out, stone, MaterialPart.SMOOTH_STONE, MaterialPart.SMOOTH_STONE_SLAB, "smooth_stone_slab", 2,
                b -> b.tool(Tool.SAW, 1));
        addShape(out, stone, MaterialPart.SMOOTH_STONE, MaterialPart.SMOOTH_STONE_STAIRS, "smooth_stone_stairs", 1,
                b -> b.tool(Tool.HAMMER, 1).tool(Tool.SAW, 1));
    }

    private static void addButtonAndPressurePlate(List<AssemblyRecipeDefinition> out, StoneMaterial stone) {
        Optional<ResourceLocation> slab = StoneRecipeResolver.block(stone, MaterialPart.SLAB);
        if (slab.isEmpty()) slab = StoneRecipeResolver.block(stone, MaterialPart.POLISHED_SLAB);
        if (slab.isEmpty()) return;
        ResourceLocation slabId = slab.get();

        StoneRecipeResolver.block(stone, MaterialPart.BUTTON).ifPresent(button -> out.add(recipe(stone, "button")
                .baseBlockInput(slabId.toString())
                .tool(Tool.FILE, 1)
                .baseBlockOutput(button.toString())
                .build()));

        StoneRecipeResolver.block(stone, MaterialPart.PRESSURE_PLATE).ifPresent(pressurePlate -> out.add(recipe(stone, "pressure_plate")
                .baseBlockInput(slabId.toString())
                .tool(Tool.HAMMER, 1)
                .baseBlockOutput(pressurePlate.toString())
                .build()));
    }

    private static void addShape(
            List<AssemblyRecipeDefinition> out,
            StoneMaterial stone,
            MaterialPart basePart,
            MaterialPart outputPart,
            String recipeName,
            int outputCount,
            Consumer<AssemblyRecipeDefinition.Builder> actions
    ) {
        addRoute(out, stone, StoneRecipeResolver.block(stone, basePart), outputPart, recipeName, outputCount, actions);
    }

    private static void addRoute(
            List<AssemblyRecipeDefinition> out,
            StoneMaterial stone,
            Optional<ResourceLocation> base,
            MaterialPart outputPart,
            String recipeName,
            int outputCount,
            Consumer<AssemblyRecipeDefinition.Builder> actions
    ) {
        Optional<ResourceLocation> result = StoneRecipeResolver.block(stone, outputPart);
        if (base.isEmpty() || result.isEmpty() || base.get().equals(result.get())) return;

        AssemblyRecipeDefinition.Builder builder = recipe(stone, recipeName)
                .baseBlockInput(base.get().toString());
        actions.accept(builder);
        builder.baseBlockOutput(result.get().toString(), outputCount);
        out.add(builder.build());
    }

    private static AssemblyRecipeDefinition.Builder recipe(StoneMaterial stone, String name) {
        return AssemblyRecipeDefinition.recipe("stone/" + stone.id() + "/" + name)
                .level(WorkbenchLevels.forTier(stone.tier()));
    }
}
