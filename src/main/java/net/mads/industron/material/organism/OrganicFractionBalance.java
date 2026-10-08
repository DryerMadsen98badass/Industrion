package net.mads.industron.material.organism;

import net.mads.industron.material.MaterialComponent;
import java.util.*;
import java.util.function.Predicate;

/** Exact direct-level accounting only. Does not assert a machine can perform the separation. */
public final class OrganicFractionBalance {
    private OrganicFractionBalance() {}
    public record Fraction(int units, List<MaterialComponent> composition) {
        public Fraction { composition=List.copyOf(composition); }
    }
    public record Partition(int inputUnits, Fraction selected, Fraction residue) {}

    /** Nested compounds remain intact; their internal ratios must never multiply the parent ratio. */
    public static Partition partition(List<MaterialComponent> input, Predicate<MaterialComponent> selector) {
        if (input.isEmpty()) throw new IllegalArgumentException("Missing input composition");
        Objects.requireNonNull(selector);
        int divisor=0;
        Set<String> seen=new HashSet<>();
        for (MaterialComponent c : input) {
            if (!seen.add(c.substance().id())) throw new IllegalArgumentException("Duplicate input component");
            divisor=gcd(divisor,c.amount());
        }
        List<MaterialComponent> yes=new ArrayList<>(), no=new ArrayList<>();
        int sum=0, yesUnits=0, noUnits=0;
        for (MaterialComponent c : input) {
            int amount=c.amount()/divisor;
            sum=Math.addExact(sum,amount);
            var reduced=new MaterialComponent(c.substance(),amount);
            if (selector.test(c)) { yes.add(reduced); yesUnits=Math.addExact(yesUnits,amount); }
            else { no.add(reduced); noUnits=Math.addExact(noUnits,amount); }
        }
        if (yesUnits==0 || noUnits==0) throw new IllegalArgumentException("Not a two-fraction separation");
        return new Partition(sum,new Fraction(yesUnits,normalize(yes)),new Fraction(noUnits,normalize(no)));
    }
    private static List<MaterialComponent> normalize(List<MaterialComponent> input) {
        int d=0;
        for (MaterialComponent c : input) d=gcd(d,c.amount());
        final int divisor=d;
        return input.stream().map(c->new MaterialComponent(c.substance(),c.amount()/divisor)).toList();
    }
    private static int gcd(int a,int b) { while (b!=0) { int r=a%b; a=b; b=r; } return a; }
}
