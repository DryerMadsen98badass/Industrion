package net.mads.industron.material.chemistry.geology;

import java.util.List;

public interface GeologyWorldgenAdapter {
    record EmissionResult(int emitted,List<String> warnings){public EmissionResult{warnings=List.copyOf(warnings);}}
    EmissionResult emit(List<DepositDefinition> deposits);
}
