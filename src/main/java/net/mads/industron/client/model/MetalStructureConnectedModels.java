package net.mads.industron.client.model;

import com.simibubi.create.content.decoration.MetalScaffoldingCTBehaviour;
import com.simibubi.create.content.decoration.RoofBlockCTBehaviour;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import com.simibubi.create.foundation.block.connected.GlassPaneCTBehaviour;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.mads.industron.material.structure.MetalMaterial;
import net.mads.industron.material.structure.MetalMaterials;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureSetResolver;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers material-specific structure CT with Create's own model swapper.
 * The original and target sprites always point at the generated, tinted
 * Industron textures rather than Create's source textures.
 */
public final class MetalStructureConnectedModels {
    private static boolean registered;

    private MetalStructureConnectedModels() {
    }

    /**
     * Must run during client setup, before model baking. CreateRegistrate's CT
     * consumer registers a CTModel wrapper in CreateClient.MODEL_SWAPPER, which
     * is the same path Create uses for its own windows, scaffolds and roofs.
     */
    public static void init() {
        if (registered) {
            return;
        }

        for (MetalMaterial material : MetalMaterials.ALL) {
            for (StructureBlockDefinition definition
                    : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                ConnectedTextureBehaviour behaviour = behaviour(definition);
                if (behaviour == null) {
                    continue;
                }

                var holder = BlockRegistry.getStructureMaterialBlock(definition.registryName());
                if (holder == null) {
                    throw new IllegalStateException(
                            "Missing generated metal block " + definition.registryName()
                    );
                }
                Block block = holder.get();
                CreateRegistrate.connectedTextures(() -> behaviour).accept(block);
            }
        }

        registered = true;
    }

    private static @Nullable ConnectedTextureBehaviour behaviour(StructureBlockDefinition definition) {
        return switch (definition.modelKind()) {
            case CREATE_SCAFFOLD -> new MetalScaffoldingCTBehaviour(
                    shift(AllCTTypes.HORIZONTAL, definition, "side", "connected"),
                    shift(AllCTTypes.HORIZONTAL, definition, "inside", "inside_connected"),
                    shift(AllCTTypes.OMNIDIRECTIONAL, definition, "casing", "casing_connected")
            );
            case CREATE_WINDOW -> windowBehaviour(definition, false);
            case CREATE_WINDOW_PANE -> windowBehaviour(definition, true);
            default -> definition.texture("connected").isPresent()
                    && definition.texture("top").isPresent()
                    && isRoofShape(definition)
                    ? new RoofBlockCTBehaviour(
                            shift(AllCTTypes.ROOF, definition, "top", "connected")
                    )
                    : null;
        };
    }

    private static boolean isRoofShape(StructureBlockDefinition definition) {
        return switch (definition.shape()) {
            case CUBE, SLAB, STAIRS -> true;
            default -> false;
        };
    }

    private static ConnectedTextureBehaviour windowBehaviour(
            StructureBlockDefinition definition,
            boolean pane
    ) {
        if (definition.texture("connected_1").isPresent()) {
            List<CTSpriteShiftEntry> shifts = new ArrayList<>();
            for (int index = 1; index <= 4; index++) {
                shifts.add(shift(
                        AllCTTypes.RECTANGLE,
                        definition,
                        "side",
                        "connected_" + index
                ));
            }
            return pane
                    ? new RandomWindowPaneBehaviour(shifts)
                    : new RandomWindowBehaviour(shifts);
        }

        CTType type = definition.requiredTexture("side").contains("ornate_iron_window")
                ? AllCTTypes.VERTICAL
                : AllCTTypes.RECTANGLE;
        CTSpriteShiftEntry shift = shift(type, definition, "side", "connected");
        return pane ? new GlassPaneCTBehaviour(shift) : new HorizontalCTBehaviour(shift);
    }

    private static CTSpriteShiftEntry shift(
            CTType type,
            StructureBlockDefinition definition,
            String originalSlot,
            String connectedSlot
    ) {
        return CTSpriteShifter.getCT(
                type,
                StructureSetResolver.generatedTexture(
                        definition.material(),
                        definition.requiredTexture(originalSlot)
                ),
                StructureSetResolver.generatedTexture(
                        definition.material(),
                        definition.requiredTexture(connectedSlot)
                )
        );
    }

    /** Matches Create's WeatheredIronWindowCTBehaviour. */
    private static final class RandomWindowBehaviour extends ConnectedTextureBehaviour.Base {
        private final List<CTSpriteShiftEntry> shifts;

        private RandomWindowBehaviour(List<CTSpriteShiftEntry> shifts) {
            this.shifts = List.copyOf(shifts);
        }

        @Override
        public @Nullable CTSpriteShiftEntry getShift(
                BlockState state,
                RandomSource random,
                Direction direction,
                @NotNull TextureAtlasSprite sprite
        ) {
            if (direction.getAxis() == Axis.Y) {
                return null;
            }
            CTSpriteShiftEntry entry = shifts.get(random.nextInt(shifts.size()));
            return entry.getOriginal() == sprite ? entry : super.getShift(state, random, direction, sprite);
        }

        @Override
        public @Nullable CTSpriteShiftEntry getShift(
                BlockState state,
                Direction direction,
                @Nullable TextureAtlasSprite sprite
        ) {
            return null;
        }

        @Override
        public @Nullable CTType getDataType(
                BlockAndTintGetter world,
                BlockPos pos,
                BlockState state,
                Direction direction
        ) {
            return AllCTTypes.RECTANGLE;
        }
    }

    /** Matches Create's WeatheredIronWindowPaneCTBehaviour. */
    private static final class RandomWindowPaneBehaviour extends GlassPaneCTBehaviour {
        private final List<CTSpriteShiftEntry> shifts;

        private RandomWindowPaneBehaviour(List<CTSpriteShiftEntry> shifts) {
            super(null);
            this.shifts = List.copyOf(shifts);
        }

        @Override
        public @Nullable CTSpriteShiftEntry getShift(
                BlockState state,
                RandomSource random,
                Direction direction,
                @NotNull TextureAtlasSprite sprite
        ) {
            if (direction.getAxis() == Axis.Y) {
                return null;
            }
            CTSpriteShiftEntry entry = shifts.get(random.nextInt(shifts.size()));
            return entry.getOriginal() == sprite ? entry : super.getShift(state, random, direction, sprite);
        }

        @Override
        public @Nullable CTSpriteShiftEntry getShift(
                BlockState state,
                Direction direction,
                @Nullable TextureAtlasSprite sprite
        ) {
            return null;
        }

        @Override
        public @Nullable CTType getDataType(
                BlockAndTintGetter world,
                BlockPos pos,
                BlockState state,
                Direction direction
        ) {
            return AllCTTypes.RECTANGLE;
        }
    }
}
