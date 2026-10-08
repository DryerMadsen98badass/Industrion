package net.mads.industron.transport;

import net.mads.industron.material.IndustrialMaterial;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record FluidTransportTier(
        String id,
        String name,
        IndustrialMaterial material
) {
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9]+(?:_[a-z0-9]+)*");
    private static final Map<String, FluidTransportTier> ALL = new LinkedHashMap<>();
    private static volatile List<FluidTransportTier> snapshot;

    public FluidTransportTier {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(material, "material");
    }

    public int color() {
        return material.color();
    }

    public double pumpRate() {
        return material.properties().pumpFlowRate();
    }

    public int pumpStressImpact() {
        return material.properties().pumpStressImpact();
    }

    public int maximumPipeRate() {
        return material.properties().pipeThroughput();
    }

    public int maxFluidTemperature() {
        return material.properties().maxFluidTemperature();
    }

    public int maxPressure() {
        return material.properties().maxPressure();
    }

    public int tankCapabilityScore() {
        return material.properties().tankCapabilityScore();
    }

    public int tankCapacity() {
        return material.properties().tankCapacity();
    }

    public int minChemicalRange() {
        return material.properties().minChemicalRange();
    }

    public int maxChemicalRange() {
        return material.properties().maxChemicalRange();
    }

    public double frictionCoefficient() {
        return material.properties().frictionCoefficient();
    }

    public String pipeId() {
        return id + "_fluid_pipe";
    }

    public String glassPipeId() {
        return id + "_glass_fluid_pipe";
    }

    public String pumpId() {
        return id + "_mechanical_pump";
    }

    public String tankId() {
        return id + "_fluid_tank";
    }

    public String pipeDisplayName() {
        return name + " Fluid Pipe";
    }

    public String glassPipeDisplayName() {
        return name + " Glass Fluid Pipe";
    }

    public String pumpDisplayName() {
        return name + " Mechanical Pump";
    }

    public String tankDisplayName() {
        return name + " Fluid Tank";
    }

    public static Builder transportTier(String id, String name) {
        return new Builder(id, name);
    }

    public static List<FluidTransportTier> all() {
        FluidTransportTiers.bootstrap();
        List<FluidTransportTier> result = snapshot;
        if (result == null) {
            result = List.copyOf(ALL.values());
            snapshot = result;
        }
        return result;
    }

    public static FluidTransportTier byId(String id) {
        FluidTransportTiers.bootstrap();
        FluidTransportTier tier = ALL.get(id);
        if (tier == null) {
            throw new IllegalArgumentException("Unknown fluid transport tier: " + id);
        }
        return tier;
    }

    private static FluidTransportTier register(FluidTransportTier tier) {
        FluidTransportTier previous = ALL.putIfAbsent(tier.id(), tier);
        if (previous != null) {
            throw new IllegalStateException("Duplicate fluid transport tier id: " + tier.id());
        }
        snapshot = null;
        return tier;
    }

    public static final class Builder {
        private final String id;
        private final String name;
        private IndustrialMaterial material;

        private Builder(String id, String name) {
            this.id = Objects.requireNonNull(id, "id").trim();
            this.name = Objects.requireNonNull(name, "name").trim();
        }

        public Builder material(IndustrialMaterial material) {
            this.material = Objects.requireNonNull(material, "material");
            return this;
        }

        public FluidTransportTier build() {
            if (!VALID_ID.matcher(id).matches()) {
                throw new IllegalStateException("Invalid fluid transport tier id: " + id);
            }
            if (name.isBlank()) {
                throw new IllegalStateException("name cannot be blank for " + id);
            }
            if (material == null) {
                throw new IllegalStateException("material must be set for fluid transport family " + id);
            }
            return register(new FluidTransportTier(id, name, material));
        }
    }
}
