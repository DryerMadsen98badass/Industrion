package net.mads.industron.client;

import net.mads.industron.Industron;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlockVariant;
import net.mads.industron.block.SimpleBlocks;
import net.mads.industron.item.SimpleItemDefinition;
import net.mads.industron.item.SimpleItems;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.MaterialFormGenerator;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.registry.BlockRegistry;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(
        modid = Industron.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientColorHandlers {

    private ClientColorHandlers() {
    }

    /**
     * Registrerer farge for blokker plassert i verden.
     */
    @SubscribeEvent
    public static void registerBlockColors(
            RegisterColorHandlersEvent.Block event
    ) {
        registerMaterialBlockColors(event);

        for (SimpleBlockDefinition definition : SimpleBlocks.ALL) {
            if (!definition.hasColor()) {
                continue;
            }

            int color = definition.blockColor();

            event.register(
                    (state, level, position, tintIndex) ->
                            tintIndex == 0
                                    ? color
                                    : 0xFFFFFFFF,
                    BlockRegistry
                            .getSimpleBlock(definition.id())
                            .get()
            );

            for (SimpleBlockVariant variant
                    : definition.variants()) {

                Block variantBlock = BlockRegistry
                        .getSimpleBlockVariant(
                                definition.id(),
                                variant
                        )
                        .get();

                event.register(
                        (state, level, position, tintIndex) ->
                                tintIndex == 0
                                        ? color
                                        : 0xFFFFFFFF,
                        variantBlock
                );
            }
        }
    }

    /**
     * Registrerer farge for items i inventory, JEI,
     * creative tab og når spilleren holder dem.
     */
    @SubscribeEvent
    public static void registerItemColors(
            RegisterColorHandlersEvent.Item event
    ) {
        registerMaterialItemColors(event);
        registerSimpleItemColors(event);
        registerSimpleBlockItemColors(event);
        registerStructureMaterialItemColors(event);
    }

    private static void registerMaterialBlockColors(
            RegisterColorHandlersEvent.Block event
    ) {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            int baseColor = argb(material.properties().baseColor());
            int secondaryColor = argb(material.properties().highlightColor());

            for (MaterialPart part : material.parts()) {
                if (!part.isBlock() || material.hasExistingPart(part)) {
                    continue;
                }

                var holder = BlockRegistry.getMaterialBlock(material, part);
                if (holder == null) {
                    continue;
                }

                event.register(
                        (state, level, position, tintIndex) -> switch (tintIndex) {
                            case 0 -> baseColor;
                            case 1 -> secondaryColor;
                            default -> 0xFFFFFFFF;
                        },
                        holder.get()
                );
            }

            if (MaterialOreHost.hasNaturalOre(material)) {
                for (MaterialOreHost host : MaterialOreHost.all()) {
                    for (boolean small : new boolean[]{false, true}) {
                        var holder = BlockRegistry.getMaterialOreHostBlock(material, host, small);
                        if (holder == null) continue;
                        event.register(
                                (state, level, position, tintIndex) -> switch (tintIndex) {
                                    case 0 -> baseColor;
                                    case 1 -> secondaryColor;
                                    default -> 0xFFFFFFFF;
                                },
                                holder.get()
                        );
                    }
                }
            }
        }
    }

    private static void registerMaterialItemColors(
            RegisterColorHandlersEvent.Item event
    ) {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            int baseColor = argb(material.properties().baseColor());
            int secondaryColor = argb(material.properties().highlightColor());

            for (MaterialPart part : material.parts()) {
                if (part.isFluid() || material.hasExistingPart(part)) {
                    continue;
                }

                var item = ItemRegistry.getMaterialItem(material, part);
                if (item != null) {
                    event.register(
                            (stack, tintIndex) -> materialItemColor(material, part, tintIndex, baseColor, secondaryColor),
                            item.get()
                    );
                }

                if (MaterialFormGenerator.hasMagneticVariant(material, part)) {
                    var magneticItem = ItemRegistry.getMagneticMaterialItem(material, part);
                    if (magneticItem != null) {
                        event.register(
                                (stack, tintIndex) -> materialItemColor(material, part, tintIndex, baseColor, secondaryColor),
                                magneticItem.get()
                        );
                    }
                }
            }

            if (MaterialOreHost.hasNaturalOre(material)) {
                for (MaterialOreHost host : MaterialOreHost.all()) {
                    for (boolean small : new boolean[]{false, true}) {
                        var item = ItemRegistry.getMaterialOreHostItem(material, host, small);
                        if (item == null) continue;
                        event.register(
                                (stack, tintIndex) -> switch (tintIndex) {
                                    case 0 -> baseColor;
                                    case 1 -> secondaryColor;
                                    default -> 0xFFFFFFFF;
                                },
                                item.get()
                        );
                    }
                }
            }
        }
    }

    private static int materialItemColor(
            IndustrialMaterial material,
            MaterialPart part,
            int tintIndex,
            int baseColor,
            int secondaryColor
    ) {
        if (tintIndex == 0) return baseColor;
        if (tintIndex == 1 && hasSecondaryItemLayer(material, part)) return secondaryColor;
        return 0xFFFFFFFF;
    }

    private static boolean hasSecondaryItemLayer(IndustrialMaterial material, MaterialPart part) {
        if (material.hasCustomPartTexture(part)) return false;
        MaterialPart texturePart = part == MaterialPart.HOT_INGOT ? MaterialPart.INGOT : part;
        return net.mads.industron.material.MaterialVariantResolver.itemTextures(material, texturePart)
                .flatMap(net.mads.industron.material.MaterialVariantResolver.ItemTextureSet::secondary)
                .isPresent();
    }

    private static void registerStructureMaterialItemColors(
            RegisterColorHandlersEvent.Item event
    ) {
        for (StructureMaterial material : StructureMaterials.ALL) {
            int color = argb(material.color());
            for (MaterialPart part : StructureMaterialGenerator.generatedItemForms(material)) {
                var item = ItemRegistry.getStructureMaterialFormItem(material, part);
                if (item == null) {
                    continue;
                }
                event.register(
                        (stack, tintIndex) -> switch (tintIndex) {
                            case 0, 1 -> color;
                            default -> 0xFFFFFFFF;
                        },
                        item.get()
                );
            }
        }
    }

    private static int argb(int rgb) {
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    private static void registerSimpleItemColors(
            RegisterColorHandlersEvent.Item event
    ) {
        for (SimpleItemDefinition definition : SimpleItems.ALL) {
            if (!definition.hasColor()) {
                continue;
            }

            int color = definition.itemColor();

            event.register(
                    (stack, tintIndex) ->
                            tintIndex == 0
                                    ? color
                                    : 0xFFFFFFFF,
                    ItemRegistry
                            .getSimpleItem(definition.id())
                            .get()
            );
        }
    }

    private static void registerSimpleBlockItemColors(
            RegisterColorHandlersEvent.Item event
    ) {
        for (SimpleBlockDefinition definition : SimpleBlocks.ALL) {
            if (!definition.hasColor()) {
                continue;
            }

            int color = definition.blockColor();

            event.register(
                    (stack, tintIndex) ->
                            tintIndex == 0
                                    ? color
                                    : 0xFFFFFFFF,
                    ItemRegistry
                            .getSimpleBlockItem(definition.id())
                            .get()
            );

            for (SimpleBlockVariant variant
                    : definition.variants()) {

                event.register(
                        (stack, tintIndex) ->
                                tintIndex == 0
                                        ? color
                                        : 0xFFFFFFFF,
                        ItemRegistry
                                .getSimpleBlockVariantItem(
                                        definition.id(),
                                        variant
                                )
                                .get()
                );
            }
        }
    }
}
