package net.mads.industron.material.atomic;

import net.mads.industron.material.MaterialProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Single source of truth for neutral electron configurations and ion states.
 *
 * <p>The model is deliberately deterministic and element-name agnostic. It is a
 * gameplay chemistry model, not a table of real-world elements. Neutral atoms and
 * ions use the same electron-configuration engine; ion charge changes electron count,
 * never proton/neutron identity.</p>
 */
public final class AtomicModel {
    /**
     * Automatic chemistry considers charges in this bounded range. This prevents
     * pathological compound searches while still covering ordinary and high oxidation states.
     */
    public static final int MAX_AUTOMATIC_ION_MAGNITUDE = 8;

    private static final ConcurrentMap<Integer, AtomicState> NEUTRAL_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<IonKey, IonState> ION_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Integer, List<IonState>> ALLOWED_ION_CACHE = new ConcurrentHashMap<>();

    private AtomicModel() {
    }

    public static AtomicState neutral(int atomicNumber) {
        requireAtomicNumber(atomicNumber);
        return NEUTRAL_CACHE.computeIfAbsent(atomicNumber, AtomicModel::calculateNeutral);
    }

    private static AtomicState calculateNeutral(int atomicNumber) {
        ElectronConfiguration configuration = electronConfiguration(atomicNumber);
        ElectronicMetrics metrics = metrics(atomicNumber, configuration);
        int neutrons = stableNeutrons(atomicNumber, configuration.outerShell(), metrics.atomicStability());

        return new AtomicState(
                atomicNumber,
                neutrons,
                atomicNumber,
                configuration.shells(),
                configuration.outerShell(),
                configuration.outerShellCapacity(),
                configuration.outerShellElectrons(),
                configuration.stableValenceTarget(),
                configuration.electronsToStableShell(),
                configuration.electronsFromStableShell(),
                metrics.preferredIonCharge(),
                metrics.unpairedElectrons(),
                metrics.ionizationEnergy(),
                metrics.electronAffinity(),
                metrics.electronDonationTendency(),
                metrics.electronAcceptanceTendency(),
                metrics.bondStrength(),
                metrics.atomicStability(),
                metrics.effectiveNuclearCharge(),
                metrics.atomicRadius(),
                configuration.outerS(),
                configuration.outerP(),
                configuration.activeD(),
                configuration.activeF(),
                configuration.frontierL(),
                configuration.frontierOccupancy(),
                configuration.frontierCapacity(),
                metrics.directionalBonding(),
                metrics.family()
        );
    }

    public static IonState ion(int atomicNumber, int charge) {
        requireIonCharge(atomicNumber, charge);
        IonKey key = new IonKey(atomicNumber, charge);
        return ION_CACHE.computeIfAbsent(key, ignored -> calculateIon(neutral(atomicNumber), charge));
    }

    public static List<IonState> allowedIonStates(int atomicNumber) {
        requireAtomicNumber(atomicNumber);
        return ALLOWED_ION_CACHE.computeIfAbsent(atomicNumber, AtomicModel::calculateAllowedIonStates);
    }

    private static List<IonState> calculateAllowedIonStates(int atomicNumber) {
        AtomicState neutral = neutral(atomicNumber);
        List<IonState> states = new ArrayList<>();

        for (int charge = -MAX_AUTOMATIC_ION_MAGNITUDE; charge <= MAX_AUTOMATIC_ION_MAGNITUDE; charge++) {
            if (charge == 0 || !hasEnoughElectronsForCharge(neutral.protons(), charge)) {
                continue;
            }
            IonState state = ion(atomicNumber, charge);
            if (state.chemicallyAllowed()) {
                states.add(state);
            }
        }

        int preferredCharge = neutral.preferredIonCharge();
        states.sort(Comparator
                .comparing((IonState state) -> state.charge() != preferredCharge)
                .thenComparing(Comparator.comparingInt(IonState::viabilityScore).reversed())
                .thenComparingInt(state -> Math.abs(state.charge()))
                .thenComparingInt(IonState::charge));
        return List.copyOf(states);
    }

    public static Optional<IonState> preferredIonState(int atomicNumber) {
        AtomicState neutral = neutral(atomicNumber);
        int preferredCharge = neutral.preferredIonCharge();
        if (preferredCharge == 0 || !hasEnoughElectronsForCharge(atomicNumber, preferredCharge)) {
            return Optional.empty();
        }

        IonState preferred = ion(atomicNumber, preferredCharge);
        return preferred.chemicallyAllowed() ? Optional.of(preferred) : Optional.empty();
    }

    private static IonState calculateIon(AtomicState neutral, int charge) {
        long electronCount = (long) neutral.protons() - charge;
        ElectronConfiguration configuration = electronConfiguration(electronCount);
        ElectronicMetrics metrics = metrics(neutral.protons(), configuration);

        int formationCost = formationCost(neutral, charge, metrics.atomicStability());
        int viabilityScore = ionViability(neutral, charge, metrics.atomicStability(), formationCost, metrics.family());
        boolean allowed = chemicallyAllowed(neutral, charge, formationCost, viabilityScore);

        return new IonState(
                neutral.protons(),
                neutral.neutrons(),
                charge,
                electronCount,
                configuration.shells(),
                configuration.outerShell(),
                configuration.outerShellCapacity(),
                configuration.outerShellElectrons(),
                configuration.outerS(),
                configuration.outerP(),
                configuration.activeD(),
                configuration.activeF(),
                metrics.unpairedElectrons(),
                metrics.effectiveNuclearCharge(),
                metrics.atomicRadius(),
                metrics.ionizationEnergy(),
                metrics.electronAffinity(),
                metrics.electronDonationTendency(),
                metrics.electronAcceptanceTendency(),
                metrics.bondStrength(),
                metrics.atomicStability(),
                formationCost,
                viabilityScore,
                allowed,
                metrics.family()
        );
    }

    private static boolean chemicallyAllowed(
            AtomicState neutral,
            int charge,
            int formationCost,
            int viabilityScore
    ) {
        int preferred = neutral.preferredIonCharge();
        if (charge == preferred && charge != 0) {
            return formationCost <= 650;
        }
        if (preferred != 0 && Integer.signum(preferred) == Integer.signum(charge)) {
            int threshold = Math.abs(charge) <= Math.abs(preferred) ? 38 : 50;
            return formationCost <= 520 && viabilityScore >= threshold;
        }
        return formationCost <= 420 && viabilityScore >= 55;
    }

    private static int formationCost(AtomicState neutral, int charge, int ionStability) {
        int magnitude = Math.abs(charge);
        int directionCost;
        if (charge > 0) {
            directionCost = neutral.ionizationEnergy();
        } else {
            directionCost = Math.max(5, 100 - neutral.electronAffinity());
        }

        long repeatedChargePenalty = 16L * magnitude * Math.max(0, magnitude - 1);
        int preferredMagnitude = Math.abs(neutral.preferredIonCharge());
        int beyondPreferred = Math.max(0, magnitude - preferredMagnitude);
        long coreElectronPenalty = 180L * beyondPreferred * beyondPreferred;
        long raw = (long) directionCost * magnitude + repeatedChargePenalty + coreElectronPenalty;

        // Completely stripping a multi-proton atom is a plasma-like extreme, not an
        // ordinary compound ion. Hydrogen-like +1 remains possible.
        if ((long) neutral.protons() - charge == 0L && neutral.protons() > 1) {
            raw += 500L;
        }

        int stabilityGain = ionStability - neutral.atomicStability();
        raw -= Math.max(0, stabilityGain);
        return clamp(raw, magnitude * 5, 1000);
    }

    private static int ionViability(
            AtomicState neutral,
            int charge,
            int ionStability,
            int formationCost,
            MaterialProperties.ElectronicFamily ionFamily
    ) {
        int magnitude = Math.abs(charge);
        int direction = charge > 0
                ? neutral.electronDonationTendency()
                : neutral.electronAcceptanceTendency();

        int preferred = neutral.preferredIonCharge();
        int preferredBonus;
        if (preferred == charge) {
            preferredBonus = 70;
        } else if (preferred != 0 && Integer.signum(preferred) == Integer.signum(charge)) {
            preferredBonus = Math.max(0, 22 - Math.abs(Math.abs(preferred) - magnitude) * 8);
        } else {
            preferredBonus = -18;
        }

        int closedShellBonus = ionFamily == MaterialProperties.ElectronicFamily.NOBLE_GAS_LIKE ? 18 : 0;
        int stabilityGain = ionStability - neutral.atomicStability();
        int score = 16
                + direction / 3
                + ionStability / 3
                + preferredBonus
                + closedShellBonus
                + stabilityGain / 2
                - Math.max(0, magnitude - 1) * 10
                - formationCost / 12;
        return clamp(score, 0, 100);
    }

    private static ElectronicMetrics metrics(int atomicNumber, ElectronConfiguration configuration) {
        MaterialProperties.ElectronicFamily family = electronicFamily(configuration);
        int preferredIonCharge = preferredIonCharge(
                family,
                configuration.outerShellElectrons(),
                configuration.electronsToStableShell(),
                configuration.outerS(),
                configuration.activeD()
        );
        int unpaired = valenceUnpairedElectrons(configuration);
        int effectiveNuclearCharge = effectiveNuclearCharge(
                configuration.outerShell(),
                configuration.outerS(),
                configuration.outerP(),
                configuration.activeD(),
                configuration.activeF(),
                atomicNumber
        );
        int atomicRadius = atomicRadius(
                configuration.outerShell(),
                effectiveNuclearCharge,
                configuration.outerShellElectrons(),
                configuration.activeD(),
                configuration.activeF()
        );
        int ionizationEnergy = ionizationEnergy(family, configuration.outerShell(), effectiveNuclearCharge, atomicRadius);
        int electronAffinity = electronAffinity(family, configuration.outerShell(), configuration.outerP(), effectiveNuclearCharge);
        int donationTendency = donationTendency(family, configuration.outerShell(), ionizationEnergy, atomicRadius);
        int acceptanceTendency = acceptanceTendency(family, electronAffinity, configuration.outerP());
        int directionalBonding = directionalBonding(family, configuration.outerP(), unpaired);
        int bondStrength = bondStrength(
                family,
                configuration.outerShell(),
                directionalBonding,
                unpaired,
                donationTendency,
                acceptanceTendency
        );
        int atomicStability = atomicStability(
                family,
                configuration.outerS(),
                configuration.outerP(),
                configuration.activeD(),
                configuration.activeF(),
                unpaired,
                donationTendency,
                acceptanceTendency
        );

        return new ElectronicMetrics(
                family,
                preferredIonCharge,
                unpaired,
                ionizationEnergy,
                electronAffinity,
                donationTendency,
                acceptanceTendency,
                bondStrength,
                atomicStability,
                effectiveNuclearCharge,
                atomicRadius,
                directionalBonding
        );
    }

    /**
     * Madelung n+l filling shared by neutral atoms and ions. Electron count is long so
     * an anion of Z=Integer.MAX_VALUE does not overflow simply because it gained electrons.
     */
    private static ElectronConfiguration electronConfiguration(long electronCount) {
        if (electronCount < 0) {
            throw new IllegalArgumentException("Electron count cannot be negative: " + electronCount);
        }
        if (electronCount == 0) {
            return new ElectronConfiguration(
                    0,
                    List.of(0),
                    1,
                    2,
                    0,
                    2,
                    2,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    2
            );
        }

        long remaining = electronCount;
        int[] shellCounts = new int[16];
        int[] sOccupancy = new int[16];
        int[] pOccupancy = new int[16];
        int[] dOccupancy = new int[16];
        int[] fOccupancy = new int[16];

        int lastN = 1;
        int lastL = 0;
        int lastOccupancy = 0;
        int lastCapacity = 2;
        int diagonal = 1;

        while (remaining > 0) {
            for (int n = 1; n <= diagonal && remaining > 0; n++) {
                int l = diagonal - n;
                if (l < 0 || l >= n) {
                    continue;
                }

                int capacity = subshellCapacity(l);
                int occupancy = (int) Math.min(remaining, capacity);
                int requiredLength = n + 1;
                if (requiredLength > shellCounts.length) {
                    int newLength = Math.max(requiredLength, shellCounts.length * 2);
                    shellCounts = Arrays.copyOf(shellCounts, newLength);
                    sOccupancy = Arrays.copyOf(sOccupancy, newLength);
                    pOccupancy = Arrays.copyOf(pOccupancy, newLength);
                    dOccupancy = Arrays.copyOf(dOccupancy, newLength);
                    fOccupancy = Arrays.copyOf(fOccupancy, newLength);
                }

                shellCounts[n] = safeAdd(shellCounts[n], occupancy);
                switch (l) {
                    case 0 -> sOccupancy[n] = occupancy;
                    case 1 -> pOccupancy[n] = occupancy;
                    case 2 -> dOccupancy[n] = occupancy;
                    case 3 -> fOccupancy[n] = occupancy;
                    default -> {
                        // Higher subshells still participate in filling/frontier physics.
                    }
                }

                remaining -= occupancy;
                lastN = n;
                lastL = l;
                lastOccupancy = occupancy;
                lastCapacity = capacity;
            }

            if (diagonal == Integer.MAX_VALUE) {
                throw new IllegalStateException("Could not extend electron configuration for electronCount=" + electronCount);
            }
            diagonal++;
        }

        // Generalized half-filled/full-subshell promotion. This intentionally remains
        // element-name agnostic and is applied by the shared neutral/ion engine.
        if (lastL >= 2 && (lastOccupancy == lastCapacity / 2 - 1 || lastOccupancy == lastCapacity - 1)) {
            int donorShell = lastN + lastL - 1;
            if (donorShell > 0 && donorShell < sOccupancy.length && sOccupancy[donorShell] >= 2) {
                sOccupancy[donorShell]--;
                shellCounts[donorShell]--;
                shellCounts[lastN] = safeAdd(shellCounts[lastN], 1);
                switch (lastL) {
                    case 2 -> dOccupancy[lastN]++;
                    case 3 -> fOccupancy[lastN]++;
                    default -> {
                        // Higher-subshell promotion is represented by frontier occupancy.
                    }
                }
                lastOccupancy++;
            }
        }

        int outerShell = 1;
        for (int n = shellCounts.length - 1; n >= 1; n--) {
            if (shellCounts[n] > 0) {
                outerShell = n;
                break;
            }
        }

        List<Integer> shells = new ArrayList<>(outerShell);
        for (int n = 1; n <= outerShell; n++) {
            shells.add(shellCounts[n]);
        }

        int outerS = sOccupancy[outerShell];
        int outerP = pOccupancy[outerShell];
        int activeD = outerShell > 1 ? dOccupancy[outerShell - 1] : 0;
        int activeF = outerShell > 2 ? fOccupancy[outerShell - 2] : 0;
        int outerElectrons = outerS + outerP;
        int outerShellCapacity = outerShell == 1 ? 2 : 8;
        int stableTarget = outerShellCapacity;
        int electronsToStable = Math.max(0, stableTarget - outerElectrons);
        int electronsFromStable = outerElectrons == stableTarget ? 0 : outerElectrons;

        return new ElectronConfiguration(
                electronCount,
                List.copyOf(shells),
                outerShell,
                outerShellCapacity,
                outerElectrons,
                stableTarget,
                electronsToStable,
                electronsFromStable,
                outerS,
                outerP,
                activeD,
                activeF,
                lastL,
                lastOccupancy,
                lastCapacity
        );
    }

    private static int safeAdd(int left, int right) {
        long value = (long) left + right;
        if (value > Integer.MAX_VALUE) {
            throw new IllegalStateException("Electron shell occupancy overflow: " + value);
        }
        return (int) value;
    }

    private static int subshellCapacity(int l) {
        long capacity = 4L * l + 2L;
        return capacity >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity;
    }

    private static MaterialProperties.ElectronicFamily electronicFamily(ElectronConfiguration c) {
        if (c.electronCount() == 0) {
            return MaterialProperties.ElectronicFamily.NOBLE_GAS_LIKE;
        }
        if ((c.outerShell() == 1 && c.outerS() == 2)
                || (c.outerShell() > 1 && c.outerS() == 2 && c.outerP() == 6)) {
            return MaterialProperties.ElectronicFamily.NOBLE_GAS_LIKE;
        }
        if (c.outerP() > 0) {
            return switch (c.outerP()) {
                case 1 -> MaterialProperties.ElectronicFamily.NETWORK_CRYSTAL_P1;
                case 2 -> MaterialProperties.ElectronicFamily.NETWORK_CRYSTAL_P2;
                case 3 -> MaterialProperties.ElectronicFamily.PNICTOGEN_LIKE;
                case 4 -> MaterialProperties.ElectronicFamily.CHALCOGEN_LIKE;
                case 5 -> MaterialProperties.ElectronicFamily.HALOGEN_LIKE;
                default -> MaterialProperties.ElectronicFamily.NOBLE_GAS_LIKE;
            };
        }
        if (c.activeF() > 0 && c.activeF() < 14) {
            return MaterialProperties.ElectronicFamily.F_BLOCK;
        }
        if (c.activeD() > 0) {
            if (c.activeD() == 10 && c.outerS() == 1) {
                return MaterialProperties.ElectronicFamily.COINAGE_LIKE;
            }
            if (c.activeD() == 10 && c.outerS() == 2) {
                return MaterialProperties.ElectronicFamily.CLOSED_D_SHELL;
            }
            if (c.activeD() <= 3) {
                return MaterialProperties.ElectronicFamily.TRANSITION_EARLY;
            }
            if (c.activeD() <= 7) {
                return MaterialProperties.ElectronicFamily.TRANSITION_MIDDLE;
            }
            return MaterialProperties.ElectronicFamily.TRANSITION_LATE;
        }
        if (c.frontierL() >= 4 && c.frontierOccupancy() < c.frontierCapacity()) {
            return MaterialProperties.ElectronicFamily.EXTENDED_BLOCK;
        }
        if (c.outerS() == 1) {
            return MaterialProperties.ElectronicFamily.ALKALI_LIKE;
        }
        return MaterialProperties.ElectronicFamily.ALKALINE_EARTH_LIKE;
    }

    private static int preferredIonCharge(
            MaterialProperties.ElectronicFamily family,
            int outerElectrons,
            int electronsToStable,
            int outerS,
            int activeD
    ) {
        return switch (family) {
            case NOBLE_GAS_LIKE -> 0;
            case ALKALI_LIKE -> 1;
            case ALKALINE_EARTH_LIKE -> 2;
            case HALOGEN_LIKE -> -1;
            case CHALCOGEN_LIKE -> -2;
            case PNICTOGEN_LIKE -> -3;
            case NETWORK_CRYSTAL_P1 -> 3;
            case NETWORK_CRYSTAL_P2 -> outerElectrons <= 4 ? 4 : -4;
            case COINAGE_LIKE -> 1;
            case CLOSED_D_SHELL -> 2;
            case TRANSITION_EARLY -> Math.max(2, Math.min(4, outerS + Math.min(2, activeD)));
            case TRANSITION_MIDDLE, TRANSITION_LATE, F_BLOCK, EXTENDED_BLOCK -> Math.max(1, outerS);
        };
    }

    private static int valenceUnpairedElectrons(ElectronConfiguration c) {
        int unpaired = unpairedElectrons(c.outerS(), 2)
                + unpairedElectrons(c.outerP(), 6)
                + unpairedElectrons(c.activeD(), 10)
                + unpairedElectrons(c.activeF(), 14);
        if (c.frontierL() >= 4) {
            unpaired += unpairedElectrons(c.frontierOccupancy(), c.frontierCapacity());
        }
        return Math.min(100, unpaired);
    }

    private static int stableNeutrons(int atomicNumber, int outerShell, int electronicStability) {
        double ratio = 1.0D
                + Math.min(0.68D, Math.log1p(atomicNumber) * 0.095D)
                + Math.max(0, outerShell - 7) * 0.002D;
        ratio -= electronicStability / 1000.0D;
        return safePositiveInt(atomicNumber * Math.max(1.0D, ratio));
    }

    private static int unpairedElectrons(int occupancy, int capacity) {
        if (occupancy <= 0 || capacity <= 0) {
            return 0;
        }
        int half = capacity / 2;
        return occupancy <= half ? occupancy : capacity - occupancy;
    }

    private static int effectiveNuclearCharge(
            int outerShell,
            int outerS,
            int outerP,
            int activeD,
            int activeF,
            int atomicNumber
    ) {
        int outerValence = outerS + outerP;
        int transitionScreening = activeD / 2 + activeF / 3;
        int periodPull = Math.min(30, outerValence * 7 + Math.max(0, outerP - 2) * 3);
        int nuclearScale = Math.min(22, (int) Math.round(Math.log1p(atomicNumber) * 4.0D));
        return clampScore(28 + periodPull + nuclearScale - outerShell * 3 - transitionScreening);
    }

    private static int atomicRadius(
            int outerShell,
            int effectiveNuclearCharge,
            int outerElectrons,
            int activeD,
            int activeF
    ) {
        return clampScore(
                26
                        + outerShell * 10
                        - effectiveNuclearCharge / 3
                        - outerElectrons * 2
                        + activeD / 2
                        + activeF / 3
        );
    }

    private static int ionizationEnergy(
            MaterialProperties.ElectronicFamily family,
            int outerShell,
            int effectiveNuclearCharge,
            int atomicRadius
    ) {
        int familyBase = switch (family) {
            case NOBLE_GAS_LIKE -> 96;
            case HALOGEN_LIKE -> 82;
            case CHALCOGEN_LIKE -> 72;
            case PNICTOGEN_LIKE -> 66;
            case NETWORK_CRYSTAL_P2 -> 62;
            case NETWORK_CRYSTAL_P1 -> 54;
            case COINAGE_LIKE -> 60;
            case CLOSED_D_SHELL -> 55;
            case TRANSITION_LATE -> 54;
            case TRANSITION_MIDDLE -> 50;
            case TRANSITION_EARLY -> 44;
            case F_BLOCK -> 38;
            case ALKALINE_EARTH_LIKE -> 36;
            case ALKALI_LIKE -> 22;
            case EXTENDED_BLOCK -> 45;
        };
        return clampScore(Math.round(
                familyBase
                        + effectiveNuclearCharge * 0.18F
                        - atomicRadius * 0.12F
                        - Math.max(0, outerShell - 2) * 1.5F
        ));
    }

    private static int electronAffinity(
            MaterialProperties.ElectronicFamily family,
            int outerShell,
            int outerP,
            int effectiveNuclearCharge
    ) {
        int familyBase = switch (family) {
            case HALOGEN_LIKE -> 96;
            case CHALCOGEN_LIKE -> 82;
            case PNICTOGEN_LIKE -> 65;
            case NETWORK_CRYSTAL_P2 -> 52;
            case NETWORK_CRYSTAL_P1 -> 38;
            case TRANSITION_LATE -> 42;
            case COINAGE_LIKE -> 35;
            case TRANSITION_MIDDLE -> 34;
            case TRANSITION_EARLY -> 28;
            case CLOSED_D_SHELL -> 18;
            case F_BLOCK, EXTENDED_BLOCK -> 24;
            case ALKALI_LIKE -> 18;
            case ALKALINE_EARTH_LIKE -> 10;
            case NOBLE_GAS_LIKE -> 3;
        };
        return clampScore(Math.round(
                familyBase
                        + outerP * 1.5F
                        + effectiveNuclearCharge * 0.05F
                        - Math.max(0, outerShell - 4) * 1.5F
        ));
    }

    private static int donationTendency(
            MaterialProperties.ElectronicFamily family,
            int outerShell,
            int ionizationEnergy,
            int atomicRadius
    ) {
        int base = switch (family) {
            case ALKALI_LIKE -> 94;
            case ALKALINE_EARTH_LIKE -> 78;
            case TRANSITION_EARLY -> 70;
            case F_BLOCK -> 68;
            case TRANSITION_MIDDLE -> 60;
            case EXTENDED_BLOCK -> 58;
            case CLOSED_D_SHELL -> 56;
            case TRANSITION_LATE -> 50;
            case COINAGE_LIKE -> 42;
            case NETWORK_CRYSTAL_P1 -> 40;
            case NETWORK_CRYSTAL_P2 -> 28;
            case PNICTOGEN_LIKE -> 20;
            case CHALCOGEN_LIKE -> 13;
            case HALOGEN_LIKE -> 7;
            case NOBLE_GAS_LIKE -> 2;
        };
        return clampScore(Math.round(
                base
                        + Math.max(0, outerShell - 2) * 1.5F
                        + atomicRadius * 0.05F
                        - ionizationEnergy * 0.05F
        ));
    }

    private static int acceptanceTendency(
            MaterialProperties.ElectronicFamily family,
            int electronAffinity,
            int outerP
    ) {
        int structuralBonus = switch (family) {
            case HALOGEN_LIKE -> 10;
            case CHALCOGEN_LIKE -> 6;
            case PNICTOGEN_LIKE -> 3;
            default -> 0;
        };
        return clampScore(Math.round(electronAffinity * 0.90F + outerP + structuralBonus));
    }

    private static int directionalBonding(
            MaterialProperties.ElectronicFamily family,
            int outerP,
            int unpairedElectrons
    ) {
        int base = switch (family) {
            case NETWORK_CRYSTAL_P2 -> 98;
            case NETWORK_CRYSTAL_P1 -> 88;
            case PNICTOGEN_LIKE -> 70;
            case CHALCOGEN_LIKE -> 52;
            case HALOGEN_LIKE -> 28;
            case TRANSITION_MIDDLE -> 48;
            case TRANSITION_EARLY, TRANSITION_LATE -> 40;
            case COINAGE_LIKE -> 28;
            case CLOSED_D_SHELL -> 22;
            case F_BLOCK, EXTENDED_BLOCK -> 36;
            case ALKALINE_EARTH_LIKE -> 24;
            case ALKALI_LIKE -> 16;
            case NOBLE_GAS_LIKE -> 3;
        };
        return clampScore(base + Math.min(12, outerP * 2) + Math.min(8, unpairedElectrons));
    }

    private static int bondStrength(
            MaterialProperties.ElectronicFamily family,
            int outerShell,
            int directionalBonding,
            int unpairedElectrons,
            int donationTendency,
            int acceptanceTendency
    ) {
        int base = switch (family) {
            case NETWORK_CRYSTAL_P2 -> 94;
            case NETWORK_CRYSTAL_P1 -> 86;
            case TRANSITION_MIDDLE -> 84;
            case TRANSITION_EARLY -> 77;
            case TRANSITION_LATE -> 73;
            case PNICTOGEN_LIKE -> 68;
            case F_BLOCK, EXTENDED_BLOCK -> 66;
            case COINAGE_LIKE -> 62;
            case ALKALINE_EARTH_LIKE -> 58;
            case CLOSED_D_SHELL -> 47;
            case CHALCOGEN_LIKE -> 44;
            case ALKALI_LIKE -> 36;
            case HALOGEN_LIKE -> 26;
            case NOBLE_GAS_LIKE -> 7;
        };
        int complementaryBonding = Math.min(donationTendency, acceptanceTendency) / 8;
        return clampScore(Math.round(
                base
                        + directionalBonding * 0.08F
                        + Math.min(12, unpairedElectrons * 2)
                        + complementaryBonding
                        - Math.max(0, outerShell - 6) * 1.5F
        ));
    }

    private static int atomicStability(
            MaterialProperties.ElectronicFamily family,
            int outerS,
            int outerP,
            int activeD,
            int activeF,
            int unpairedElectrons,
            int donationTendency,
            int acceptanceTendency
    ) {
        int base = switch (family) {
            case NOBLE_GAS_LIKE -> 100;
            case COINAGE_LIKE -> 92;
            case CLOSED_D_SHELL -> 88;
            case NETWORK_CRYSTAL_P2 -> 86;
            case TRANSITION_MIDDLE -> 82;
            case NETWORK_CRYSTAL_P1 -> 78;
            case TRANSITION_LATE -> 76;
            case PNICTOGEN_LIKE -> 72;
            case TRANSITION_EARLY -> 68;
            case F_BLOCK, EXTENDED_BLOCK -> 64;
            case ALKALINE_EARTH_LIKE -> 60;
            case CHALCOGEN_LIKE -> 54;
            case HALOGEN_LIKE -> 42;
            case ALKALI_LIKE -> 34;
        };
        int halfFilledBonus = 0;
        if (activeD == 5 || activeD == 10) {
            halfFilledBonus += 7;
        }
        if (activeF == 7 || activeF == 14) {
            halfFilledBonus += 7;
        }
        int closedValenceBonus = outerS == 2 && outerP == 6 ? 12 : 0;
        return clampScore(
                base
                        + halfFilledBonus
                        + closedValenceBonus
                        + Math.min(8, unpairedElectrons)
                        - Math.max(donationTendency, acceptanceTendency) / 10
        );
    }

    private static void requireAtomicNumber(int atomicNumber) {
        if (atomicNumber <= 0) {
            throw new IllegalArgumentException("Atomic number must be positive: " + atomicNumber);
        }
    }

    private static void requireIonCharge(int atomicNumber, int charge) {
        requireAtomicNumber(atomicNumber);
        if (charge == 0) {
            throw new IllegalArgumentException("Ion charge cannot be zero; use neutral(...) for neutral atoms");
        }
        if (Math.abs((long) charge) > MAX_AUTOMATIC_ION_MAGNITUDE) {
            throw new IllegalArgumentException(
                    "Ion charge magnitude exceeds automatic chemistry limit "
                            + MAX_AUTOMATIC_ION_MAGNITUDE + ": " + charge
            );
        }
        if (!hasEnoughElectronsForCharge(atomicNumber, charge)) {
            throw new IllegalArgumentException(
                    "Cannot remove " + charge + " electrons from atomic number " + atomicNumber
            );
        }
    }

    private static boolean hasEnoughElectronsForCharge(int atomicNumber, int charge) {
        return charge <= 0 || charge <= atomicNumber;
    }

    private static int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static int clamp(long value, int min, int max) {
        if (value <= min) return min;
        if (value >= max) return max;
        return (int) value;
    }

    private static int safeInt(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) Math.round(value);
    }

    private static int safePositiveInt(double value) {
        return Math.max(1, safeInt(value));
    }

    private record IonKey(int atomicNumber, int charge) {
    }

    private record ElectronConfiguration(
            long electronCount,
            List<Integer> shells,
            int outerShell,
            int outerShellCapacity,
            int outerShellElectrons,
            int stableValenceTarget,
            int electronsToStableShell,
            int electronsFromStableShell,
            int outerS,
            int outerP,
            int activeD,
            int activeF,
            int frontierL,
            int frontierOccupancy,
            int frontierCapacity
    ) {
    }

    private record ElectronicMetrics(
            MaterialProperties.ElectronicFamily family,
            int preferredIonCharge,
            int unpairedElectrons,
            int ionizationEnergy,
            int electronAffinity,
            int electronDonationTendency,
            int electronAcceptanceTendency,
            int bondStrength,
            int atomicStability,
            int effectiveNuclearCharge,
            int atomicRadius,
            int directionalBonding
    ) {
    }
}
