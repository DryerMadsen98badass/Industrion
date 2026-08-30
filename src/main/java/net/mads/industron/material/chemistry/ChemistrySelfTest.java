package net.mads.industron.material.chemistry;

import net.mads.industron.material.chemistry.process.ProcessKind;
import net.mads.industron.material.chemistry.process.ProcessMaterial;
import net.mads.industron.material.chemistry.process.ProcessStep;
import java.util.List;

public final class ChemistrySelfTest {
    private ChemistrySelfTest() {}
    public static void run() {
        ProcessStep fractional = new ProcessStep(
                "self_test/fractional_fluid",
                ProcessKind.CENTRIFUGING,
                List.of(ProcessMaterial.fluid("input", ChemistryPhase.LIQUID, 144, 2, "MV", null)),
                List.of(ProcessMaterial.fluid("output", ChemistryPhase.LIQUID, 144, 3, "HV", null)),
                List.of(),
                "Duration rounding test"
        );
        check(fractional.balanced(), "144 mB balance");
        check(fractional.durationTicks() == 87, "144 mB must ceil to 87 ticks, got " + fractional.durationTicks());
        check(fractional.recipeTierIndex() == 2, "HV output must create MV recipe");

        ProcessStep mixed = new ProcessStep(
                "self_test/mixed_output",
                ProcessKind.CENTRIFUGING,
                List.of(ProcessMaterial.item("feed", 2, 1, "LV", null), ProcessMaterial.fluid("wash", ChemistryPhase.LIQUID, 4000, 1, "LV", null)),
                List.of(ProcessMaterial.item("solid", 2, 2, "MV", null), ProcessMaterial.fluid("liquid", ChemistryPhase.LIQUID, 4000, 4, "EV", null)),
                List.of(),
                "Six unit duration and highest-output-tier test"
        );
        check(mixed.outputMilliUnits() == 6000, "mixed output units");
        check(mixed.durationTicks() == 3600, "six units must take 3600 ticks");
        check(mixed.recipeTierIndex() == 3, "EV highest output must produce HV recipe");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Chemistry self-test failed: " + message);
    }
}
