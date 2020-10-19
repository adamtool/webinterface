package uniolunisaar.adamwebfrontend.wirerepresentations;

import java.util.Map;

public class SimulationHistoryState {
    private final Map<String, Long> marking;
    private final String transitionFired;
    private final Map<String, Boolean> fireableTransitions;
    public SimulationHistoryState(Map<String, Long> marking, String transitionFired,
                                  Map<String, Boolean> fireableTransitions) {
        this.marking = marking;
        this.transitionFired = transitionFired;
        this.fireableTransitions = fireableTransitions;
    }
}
