package net.mads.industron.material.chemistry;

import net.mads.industron.material.ElementDefinition;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.atomic.AtomicModel;
import net.mads.industron.material.atomic.IonState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Phase 11 solver for neutral two-component ionic formula candidates. */
public final class IonicCompoundSolver {
    public record Candidate(
            String cationId,
            int cationCharge,
            int cationCount,
            String anionId,
            int anionCharge,
            int anionCount,
            int score,
            ChemicalFormula formula
    ) {
    }

    private IonicCompoundSolver() {
    }

    public static Optional<Candidate> best(MaterialSnapshot first, MaterialSnapshot second) {
        return candidates(first, second).stream().max(Comparator.comparingInt(Candidate::score));
    }

    public static List<Candidate> candidates(MaterialSnapshot first, MaterialSnapshot second) {
        List<IonState> firstIons = ionStates(first);
        List<IonState> secondIons = ionStates(second);
        List<Candidate> result = new ArrayList<>();
        for (IonState a : firstIons) {
            for (IonState b : secondIons) {
                if (Integer.signum(a.charge()) == Integer.signum(b.charge())) continue;
                Candidate candidate = candidate(first.id(), a, second.id(), b);
                if (candidate != null) result.add(candidate);
            }
        }
        result.sort(Comparator.comparingInt(Candidate::score).reversed());
        return List.copyOf(result);
    }

    private static Candidate candidate(String firstId, IonState first, String secondId, IonState second) {
        int firstAbs = Math.abs(first.charge());
        int secondAbs = Math.abs(second.charge());
        int lcm = lcm(firstAbs, secondAbs);
        int firstCount = lcm / firstAbs;
        int secondCount = lcm / secondAbs;
        int charge = first.charge() * firstCount + second.charge() * secondCount;
        if (charge != 0) return null;

        String cationId = first.charge() > 0 ? firstId : secondId;
        String anionId = first.charge() < 0 ? firstId : secondId;
        int cationCharge = first.charge() > 0 ? first.charge() : second.charge();
        int anionCharge = first.charge() < 0 ? first.charge() : second.charge();
        int cationCount = first.charge() > 0 ? firstCount : secondCount;
        int anionCount = first.charge() < 0 ? firstCount : secondCount;
        int score = first.viabilityScore() + second.viabilityScore()
                - first.formationCost() / 10 - second.formationCost() / 10
                - Math.max(0, cationCount + anionCount - 2) * 4;

        return new Candidate(
                cationId, cationCharge, cationCount,
                anionId, anionCharge, anionCount,
                score,
                new ChemicalFormula(java.util.Map.of(cationId, cationCount, anionId, anionCount), 0)
        );
    }

    private static List<IonState> ionStates(MaterialSnapshot snapshot) {
        Object backing = snapshot.backingMaterial();
        if (backing instanceof ElementDefinition element) return element.allowedIonStates();
        if (backing instanceof IndustrialMaterial material && material.atomicNumber() > 0) {
            return AtomicModel.allowedIonStates(material.atomicNumber());
        }
        return List.of();
    }

    private static int lcm(int a, int b) {
        return Math.multiplyExact(a / gcd(a, b), b);
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int next = a % b;
            a = b;
            b = next;
        }
        return Math.abs(a);
    }
}
