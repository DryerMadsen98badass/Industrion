package net.mads.industron.material.chemistry.process;

import java.util.Objects;

public record PropertyConstraint(String property, double minimum, double maximum) {
    public PropertyConstraint {
        property=Objects.requireNonNull(property).trim().toLowerCase(java.util.Locale.ROOT);
        if (property.isEmpty()) throw new IllegalArgumentException("property cannot be blank");
        if (!Double.isFinite(minimum)||!Double.isFinite(maximum)||minimum>maximum) throw new IllegalArgumentException("invalid range");
    }

    public static PropertyConstraint exactly(String property,double value){return new PropertyConstraint(property,value,value);}
    public static PropertyConstraint atLeast(String property,double value){return new PropertyConstraint(property,value,Double.MAX_VALUE);}
    public static PropertyConstraint atMost(String property,double value){return new PropertyConstraint(property,-Double.MAX_VALUE,value);}
    public static PropertyConstraint between(String property,double min,double max){return new PropertyConstraint(property,min,max);}
    public boolean matches(double value){return value>=minimum&&value<=maximum;}
}
