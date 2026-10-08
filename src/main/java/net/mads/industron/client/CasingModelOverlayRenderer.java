package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Renders the concrete casing model selected by a formed multiblock. */
final class CasingModelOverlayRenderer {
    private static final float FRONT_BORDER = 2.0F / 16.0F;
    private static final float EPSILON = 0.0001F;

    private CasingModelOverlayRenderer() {
    }

    static void render(
            BlockEntity target,
            ResourceLocation modelId,
            @Nullable Direction borderFace,
            float offset,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockState sourceState = sourceBlockState(modelId);
        if (sourceState == null) {
            return;
        }
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(sourceState);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose pose = poseStack.last();
        long seed = target.getBlockPos().asLong();
        RandomSource random = RandomSource.create(seed);

        for (Direction side : Direction.values()) {
            random.setSeed(seed);
            renderQuads(
                    target,
                    sourceState,
                    model.getQuads(sourceState, side, random, ModelData.EMPTY, null),
                    borderFace,
                    pose,
                    consumer,
                    packedLight,
                    offset
            );
        }

        random.setSeed(seed);
        renderQuads(
                target,
                sourceState,
                model.getQuads(sourceState, null, random, ModelData.EMPTY, null),
                borderFace,
                pose,
                consumer,
                packedLight,
                offset
        );
    }

    private static void renderQuads(
            BlockEntity target,
            @Nullable BlockState sourceState,
            List<BakedQuad> quads,
            @Nullable Direction borderFace,
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int packedLight,
            float offset
    ) {
        for (BakedQuad quad : quads) {
            int color = quadColor(target, sourceState, quad);
            float shade = target.getLevel() != null
                    ? target.getLevel().getShade(quad.getDirection(), quad.isShade())
                    : 1.0F;

            int faceLight = faceLight(target, quad.getDirection(), packedLight);
            if (borderFace != null && quad.getDirection() == borderFace) {
                renderFrontBorder(pose, consumer, quad, color, shade, faceLight, offset);
            } else {
                renderFullQuad(pose, consumer, quad, color, shade, faceLight, offset);
            }
        }
    }


    /**
     * A formed casing model is rendered as a block-entity overlay on top of a full opaque
     * ability/controller block. The BER's packed light is sampled at that block position,
     * which can be 0 because the position itself is occupied by the opaque cube. Sample the
     * block immediately outside each rendered face instead, matching normal block lighting.
     */
    private static int faceLight(BlockEntity target, Direction direction, int fallbackLight) {
        if (target.getLevel() == null) {
            return fallbackLight;
        }
        return LevelRenderer.getLightColor(
                target.getLevel(),
                target.getBlockPos().relative(direction)
        );
    }

    @Nullable
    private static BlockState sourceBlockState(ResourceLocation modelId) {
        String path = modelId.getPath();
        if (!path.startsWith("block/") || path.length() <= "block/".length()) {
            return null;
        }

        ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(
                modelId.getNamespace(),
                path.substring("block/".length())
        );
        return BuiltInRegistries.BLOCK.getOptional(blockId)
                .map(Block::defaultBlockState)
                .orElse(null);
    }

    private static int quadColor(BlockEntity target, @Nullable BlockState sourceState, BakedQuad quad) {
        if (!quad.isTinted() || sourceState == null) {
            return 0xFFFFFFFF;
        }

        int color = Minecraft.getInstance().getBlockColors().getColor(
                sourceState,
                target.getLevel(),
                target.getBlockPos(),
                quad.getTintIndex()
        );
        if (color == -1) {
            return 0xFFFFFFFF;
        }
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    private static void renderFullQuad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            BakedQuad quad,
            int color,
            float shade,
            int packedLight,
            float offset
    ) {
        QuadVertex[] vertices = readVertices(quad);
        if (vertices == null) {
            return;
        }

        for (QuadVertex vertex : vertices) {
            writeVertex(pose, consumer, vertex, quad.getDirection(), color, shade, packedLight, offset);
        }
    }

    private static void renderFrontBorder(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            BakedQuad quad,
            int color,
            float shade,
            int packedLight,
            float offset
    ) {
        QuadVertex[] vertices = readVertices(quad);
        if (vertices == null) {
            return;
        }

        RectangularQuad rectangle = RectangularQuad.create(vertices, quad.getDirection());
        if (rectangle == null) {
            return;
        }

        renderIntersection(pose, consumer, rectangle, quad.getDirection(), color, shade, packedLight, offset,
                0.0F, 1.0F, 1.0F - FRONT_BORDER, 1.0F);
        renderIntersection(pose, consumer, rectangle, quad.getDirection(), color, shade, packedLight, offset,
                0.0F, 1.0F, 0.0F, FRONT_BORDER);
        renderIntersection(pose, consumer, rectangle, quad.getDirection(), color, shade, packedLight, offset,
                0.0F, FRONT_BORDER, FRONT_BORDER, 1.0F - FRONT_BORDER);
        renderIntersection(pose, consumer, rectangle, quad.getDirection(), color, shade, packedLight, offset,
                1.0F - FRONT_BORDER, 1.0F, FRONT_BORDER, 1.0F - FRONT_BORDER);
    }

    private static void renderIntersection(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            RectangularQuad rectangle,
            Direction direction,
            int color,
            float shade,
            int packedLight,
            float offset,
            float clipMinS,
            float clipMaxS,
            float clipMinT,
            float clipMaxT
    ) {
        float minS = Math.max(rectangle.minS(), clipMinS);
        float maxS = Math.min(rectangle.maxS(), clipMaxS);
        float minT = Math.max(rectangle.minT(), clipMinT);
        float maxT = Math.min(rectangle.maxT(), clipMaxT);
        if (maxS - minS <= EPSILON || maxT - minT <= EPSILON) {
            return;
        }

        for (int index = 0; index < 4; index++) {
            float s = rectangle.highS(index) ? maxS : minS;
            float t = rectangle.highT(index) ? maxT : minT;
            QuadVertex vertex = rectangle.interpolate(s, t);
            writeVertex(pose, consumer, vertex, direction, color, shade, packedLight, offset);
        }
    }

    @Nullable
    private static QuadVertex[] readVertices(BakedQuad quad) {
        int[] data = quad.getVertices();
        if (data.length % 4 != 0) {
            return null;
        }

        int stride = data.length / 4;
        if (stride < 6) {
            return null;
        }

        QuadVertex[] vertices = new QuadVertex[4];
        for (int index = 0; index < 4; index++) {
            int offset = index * stride;
            vertices[index] = new QuadVertex(
                    Float.intBitsToFloat(data[offset]),
                    Float.intBitsToFloat(data[offset + 1]),
                    Float.intBitsToFloat(data[offset + 2]),
                    Float.intBitsToFloat(data[offset + 4]),
                    Float.intBitsToFloat(data[offset + 5])
            );
        }
        return vertices;
    }

    private static void writeVertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            QuadVertex vertex,
            Direction direction,
            int color,
            float shade,
            int packedLight,
            float offset
    ) {
        int alpha = color >>> 24 & 0xFF;
        int red = shadedComponent(color >> 16 & 0xFF, shade);
        int green = shadedComponent(color >> 8 & 0xFF, shade);
        int blue = shadedComponent(color & 0xFF, shade);

        float x = vertex.x() + direction.getStepX() * offset;
        float y = vertex.y() + direction.getStepY() * offset;
        float z = vertex.z() + direction.getStepZ() * offset;

        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(vertex.u(), vertex.v())
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private static int shadedComponent(int component, float shade) {
        return Math.max(0, Math.min(255, Math.round(component * shade)));
    }

    private static float faceS(QuadVertex vertex, Direction direction) {
        return switch (direction.getAxis()) {
            case X -> vertex.z();
            case Y, Z -> vertex.x();
        };
    }

    private static float faceT(QuadVertex vertex, Direction direction) {
        return switch (direction.getAxis()) {
            case Y -> vertex.z();
            case X, Z -> vertex.y();
        };
    }

    private record QuadVertex(float x, float y, float z, float u, float v) {
        private static QuadVertex lerp(QuadVertex first, QuadVertex second, float amount) {
            return new QuadVertex(
                    first.x + (second.x - first.x) * amount,
                    first.y + (second.y - first.y) * amount,
                    first.z + (second.z - first.z) * amount,
                    first.u + (second.u - first.u) * amount,
                    first.v + (second.v - first.v) * amount
            );
        }
    }

    private record RectangularQuad(
            boolean[] highS,
            boolean[] highT,
            QuadVertex lowSLowT,
            QuadVertex highSLowT,
            QuadVertex lowSHighT,
            QuadVertex highSHighT,
            float minS,
            float maxS,
            float minT,
            float maxT
    ) {
        @Nullable
        private static RectangularQuad create(QuadVertex[] vertices, Direction direction) {
            float minS = Float.POSITIVE_INFINITY;
            float maxS = Float.NEGATIVE_INFINITY;
            float minT = Float.POSITIVE_INFINITY;
            float maxT = Float.NEGATIVE_INFINITY;
            for (QuadVertex vertex : vertices) {
                float s = faceS(vertex, direction);
                float t = faceT(vertex, direction);
                minS = Math.min(minS, s);
                maxS = Math.max(maxS, s);
                minT = Math.min(minT, t);
                maxT = Math.max(maxT, t);
            }

            if (maxS - minS <= EPSILON || maxT - minT <= EPSILON) {
                return null;
            }

            boolean[] highS = new boolean[4];
            boolean[] highT = new boolean[4];
            QuadVertex lowSLowT = null;
            QuadVertex highSLowT = null;
            QuadVertex lowSHighT = null;
            QuadVertex highSHighT = null;

            for (int index = 0; index < 4; index++) {
                QuadVertex vertex = vertices[index];
                float s = faceS(vertex, direction);
                float t = faceT(vertex, direction);
                boolean atLowS = Math.abs(s - minS) <= EPSILON;
                boolean atHighS = Math.abs(s - maxS) <= EPSILON;
                boolean atLowT = Math.abs(t - minT) <= EPSILON;
                boolean atHighT = Math.abs(t - maxT) <= EPSILON;
                if ((!atLowS && !atHighS) || (!atLowT && !atHighT)) {
                    return null;
                }

                highS[index] = atHighS;
                highT[index] = atHighT;
                if (atLowS && atLowT) {
                    lowSLowT = vertex;
                } else if (atHighS && atLowT) {
                    highSLowT = vertex;
                } else if (atLowS) {
                    lowSHighT = vertex;
                } else {
                    highSHighT = vertex;
                }
            }

            if (lowSLowT == null || highSLowT == null || lowSHighT == null || highSHighT == null) {
                return null;
            }

            return new RectangularQuad(
                    highS,
                    highT,
                    lowSLowT,
                    highSLowT,
                    lowSHighT,
                    highSHighT,
                    minS,
                    maxS,
                    minT,
                    maxT
            );
        }

        private boolean highS(int index) {
            return highS[index];
        }

        private boolean highT(int index) {
            return highT[index];
        }

        private QuadVertex interpolate(float s, float t) {
            float sAmount = (s - minS) / (maxS - minS);
            float tAmount = (t - minT) / (maxT - minT);
            QuadVertex lowTEdge = QuadVertex.lerp(lowSLowT, highSLowT, sAmount);
            QuadVertex highTEdge = QuadVertex.lerp(lowSHighT, highSHighT, sAmount);
            return QuadVertex.lerp(lowTEdge, highTEdge, tAmount);
        }
    }
}
