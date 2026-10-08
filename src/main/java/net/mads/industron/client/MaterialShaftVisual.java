package net.mads.industron.client;

import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.createmod.catnip.theme.Color;
import net.mads.industron.kinetics.shaft.MaterialShaftBlockEntity;
import net.mads.industron.material.IndustrialMaterialShaftBlock;

/** Flywheel visual for a material shaft using its dedicated rotating partial model. */
public final class MaterialShaftVisual extends SingleAxisRotatingVisual<MaterialShaftBlockEntity> {
    private final Color baseColor;

    public MaterialShaftVisual(
            VisualizationContext context,
            MaterialShaftBlockEntity blockEntity,
            float partialTick,
            Model model
    ) {
        super(context, blockEntity, partialTick, model);
        this.baseColor = resolveBaseColor(blockEntity);
        applyMaterialColor();
    }

    @Override
    public void update(float partialTick) {
        super.update(partialTick);
        applyMaterialColor();
    }

    @Override
    public void tick(Context context) {
        Color color = blockEntity.isOverStressed()
                ? baseColor.copy().mixWith(Color.RED, 0.65f)
                : baseColor;
        rotatingModel.setColor(color).setChanged();
    }

    private void applyMaterialColor() {
        rotatingModel.setColor(baseColor).setChanged();
    }

    private static Color resolveBaseColor(MaterialShaftBlockEntity blockEntity) {
        if (blockEntity.getBlockState().getBlock() instanceof IndustrialMaterialShaftBlock shaft) {
            return new Color(shaft.material().color(), false);
        }
        // Wood shaft textures are already generated in their final wood color.
        return Color.WHITE;
    }
}
