package net.mads.industron.item;

import java.util.ArrayList;
import java.util.List;
import net.mads.industron.machine.MachineTier;

/** Completed electromechanical components: one ordinary, untinted item per tier. */
public final class MachineComponentItems {
    public enum Component {
        MOTOR("motor", "Motor"),
        PISTON("piston", "Piston"),
        PUMP("pump", "Pump"),
        CONVEYOR_BELT("conveyor_belt", "Conveyor Belt"),
        ROBOT_ARM("robot_arm", "Robot Arm"),
        SENSOR("sensor", "Sensor"),
        EMITTER("emitter", "Emitter"),
        FIELD_GENERATOR("field_generator", "Field Generator");

        private final String id;
        private final String displayName;

        Component(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public String registryName(MachineTier tier) { return tier.id() + "_" + id; }
        public String texture(MachineTier tier) {
            return "item/machine_components/" + id + "/" + tier.id();
        }
    }

    public static final List<SimpleItemDefinition> ALL = createDefinitions();

    private static List<SimpleItemDefinition> createDefinitions() {
        List<SimpleItemDefinition> definitions = new ArrayList<>();
        for (MachineTier tier : MachineTier.ELECTRIC_TIERS) {
            for (Component component : Component.values()) {
                definitions.add(new SimpleItemDefinition(
                        component.registryName(tier),
                        tier.displayName() + " " + component.displayName(),
                        component.texture(tier),
                        null, 0, 0
                ));
            }
        }
        return List.copyOf(definitions);
    }

    private MachineComponentItems() { }
}
