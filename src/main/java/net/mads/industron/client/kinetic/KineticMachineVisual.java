package net.mads.industron.client.kinetic;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.mads.industron.IndustronPartialModels;
import net.mads.industron.integration.create.kinetic.CEKineticRecipeHost;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import java.util.function.Consumer;

/** Instanced shaft/mechanism rotation and translational carriage/mesh/feed motion. */
public final class KineticMachineVisual<T extends KineticBlockEntity & CEKineticRecipeHost>
        extends SingleAxisRotatingVisual<T> implements SimpleDynamicVisual {
    private final OrientedInstance moving;
    public KineticMachineVisual(VisualizationContext context, T be, float partialTick) {
        // Partials are pre-oriented per facing; from==rotationAxis avoids rotating them twice.
        super(context, be, partialTick,
                Direction.get(Direction.AxisDirection.POSITIVE, KineticBlockEntityVisual.rotationAxis(be.getBlockState())),
                Models.partial(IndustronPartialModels.kineticPart(KineticMachineMotion.id(be), "rotor", KineticMachineMotion.facing(be))));
        moving = instancerProvider().instancer(InstanceTypes.ORIENTED,
                Models.partial(IndustronPartialModels.kineticPart(KineticMachineMotion.id(be), "moving", KineticMachineMotion.facing(be))))
                .createInstance();
        moving.rotation(new Quaternionf());
        transform(partialTick);
    }
    @Override public void beginFrame(DynamicVisual.Context context) { transform(context.partialTick()); }
    private void transform(float pt) {
        float[] delta = KineticMachineMotion.offset(blockEntity, pt);
        moving.position(getVisualPosition()).translatePosition(delta[0], delta[1], delta[2]).setChanged();
    }
    @Override public void updateLight(float partialTick) { super.updateLight(partialTick); relight(moving); }
    @Override protected void _delete() { super._delete(); moving.delete(); }
    @Override public void collectCrumblingInstances(Consumer<Instance> consumer) {
        super.collectCrumblingInstances(consumer); consumer.accept(moving);
    }
}
