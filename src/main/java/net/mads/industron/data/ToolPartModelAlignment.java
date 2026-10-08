package net.mads.industron.data;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialVariantResolver;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;

/**
 * GUI-only centering for standalone tool-part items whose source sprites are deliberately
 * positioned on the shared 16x16 composition canvas. The source PNGs stay untouched so
 * ComposedToolRenderer can still stack the parts in their original relative positions.
 */
public final class ToolPartModelAlignment {
    private ToolPartModelAlignment() {
    }

    public static void apply(ItemModelBuilder model, MaterialPart part) {
        if (model == null || part == null) return;

        MaterialPart cold = MaterialVariantResolver.coldTexturePart(part);
        float x;
        float y;
        switch (cold) {
            case TOOL_HANDLE -> {
                // handle.png opaque bounds are centered about two pixels left/down.
                x = 2.0F;
                y = 2.0F;
            }
            case TOOL_HEAD_DRILL -> {
                // drill-head sprite occupies the upper-left of the composition canvas.
                x = 4.5F;
                y = -3.5F;
            }
            default -> {
                return;
            }
        }

        model.transforms()
                .transform(ItemDisplayContext.GUI)
                .translation(x, y, 0.0F)
                .end()
                .end();
    }
}
