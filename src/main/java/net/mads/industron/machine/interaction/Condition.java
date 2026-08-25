package net.mads.industron.machine.interaction;

/** Short public facade for building machine and recipe conditions. */
public final class Condition {
    private Condition() {
    }

    public static MachineCondition weather(Weather weather) {
        return MachineCondition.weather(weather);
    }

    public static MachineCondition.RangeBuilder time() {
        return MachineCondition.time();
    }

    public static MachineCondition.RangeBuilder height() {
        return MachineCondition.height();
    }

    public static MachineCondition.RangeBuilder redstone() {
        return MachineCondition.redstone();
    }

    public static MachineCondition.RangeBuilder light() {
        return MachineCondition.light();
    }

    public static MachineCondition biome(String biomeId) {
        return MachineCondition.biome(biomeId);
    }

    public static MachineCondition biomeTag(String tagId) {
        return MachineCondition.biomeTag(tagId);
    }

    public static MachineCondition dimension(String dimensionId) {
        return MachineCondition.dimension(dimensionId);
    }

    public static MachineCondition canSeeSky() {
        return MachineCondition.canSeeSky();
    }

    public static MachineCondition all(MachineCondition... conditions) {
        return MachineCondition.all(conditions);
    }

    public static MachineCondition any(MachineCondition... conditions) {
        return MachineCondition.any(conditions);
    }

    public static MachineCondition not(MachineCondition condition) {
        return MachineCondition.not(condition);
    }
}
