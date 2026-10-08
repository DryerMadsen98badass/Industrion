package net.mads.industron.client.model;

import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import net.mads.industron.IndustronPartialModels;
import net.mads.industron.energy.EnergyWireBlockEntity;
import net.mads.industron.energy.WireThickness;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * GTCEu-style wire rendering: the blockstate is constant while the six visual
 * connections are read from the block entity and composed from cached partial models.
 */
public final class EnergyWireDynamicModel extends BakedModelWrapperWithData {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final ModelProperty<Integer> CONNECTIONS_PROPERTY = new ModelProperty<>();

    private final WireThickness thickness;
    private final boolean insulated;

    public EnergyWireDynamicModel(BakedModel template, WireThickness thickness, boolean insulated) {
        super(template);
        this.thickness = thickness;
        this.insulated = insulated;
    }

    @Override
    protected ModelData.Builder gatherModelData(
            ModelData.Builder builder,
            BlockAndTintGetter world,
            BlockPos pos,
            BlockState state,
            ModelData blockEntityData
    ) {
        int connections = 0;
        if (world.getBlockEntity(pos) instanceof EnergyWireBlockEntity wire) {
            connections = wire.visualConnectionsMask();
        }
        return builder.with(CONNECTIONS_PROPERTY, connections);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(
            @NotNull BlockState state,
            @NotNull RandomSource random,
            @NotNull ModelData modelData
    ) {
        List<ChunkRenderTypeSet> renderTypes = new ArrayList<>();
        renderTypes.add(super.getRenderTypes(state, random, modelData));

        int connections = connections(modelData);
        for (Direction direction : DIRECTIONS) {
            if ((connections & bit(direction)) == 0) {
                continue;
            }
            renderTypes.add(IndustronPartialModels.energyWireArm(thickness, insulated, direction).get()
                    .getRenderTypes(state, random, modelData));
        }

        return ChunkRenderTypeSet.union(renderTypes);
    }

    @Override
    public List<BakedQuad> getQuads(
            BlockState state,
            Direction side,
            RandomSource random,
            ModelData modelData,
            RenderType renderType
    ) {
        List<BakedQuad> base = super.getQuads(state, side, random, modelData, renderType);
        int connections = connections(modelData);
        if (connections == 0) return base;
        List<BakedQuad> quads = new ArrayList<>(base);

        for (Direction direction : DIRECTIONS) {
            if ((connections & bit(direction)) == 0) {
                continue;
            }
            quads.addAll(IndustronPartialModels.energyWireArm(thickness, insulated, direction).get()
                    .getQuads(state, side, random, modelData, renderType));
        }
        return quads;
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        return TriState.TRUE;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    private static int connections(ModelData modelData) {
        Integer connections = modelData.get(CONNECTIONS_PROPERTY);
        return connections == null ? 0 : connections & 0x3F;
    }

    private static int bit(Direction direction) {
        return 1 << direction.ordinal();
    }
}
