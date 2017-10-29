package uniolunisaar.adamwebfrontend;

import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDState;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents the data needed to display a BDD graph in our client.
 * This class is meant to be serialized using GSON.
 */
public class BDDGraphD3 {
    private final List<State> nodes;
    private final List<Flow> links;

    private BDDGraphD3(List<State> states, List<Flow> flows) {
        this.nodes = states;
        this.links = flows;
    }

    public static BDDGraphD3 of(BDDGraph bddGraph) {
        List<State> states = bddGraph.getStates().stream()
                .map((BDDState state) -> State.of(state))
                .collect(Collectors.toList());
        List<Flow> flows = bddGraph.getFlows().stream()
                .map((uniolunisaar.adam.ds.graph.Flow flow) -> Flow.of(flow))
                .collect(Collectors.toList());
        return new BDDGraphD3(states, flows);
    }


    private static class Flow extends GraphLink {
        public static Flow of(uniolunisaar.adam.ds.graph.Flow flow) {
            return new Flow(Integer.toString(flow.getSourceid()), Integer.toString(flow.getTargetid()));
        }
        private Flow(String source, String target) {
            super(source, target);
        }
    }

    private static class State {
        private final String id;
        private final boolean isMcut;
        private final boolean isSpecial;
        private final String content;

        public static State of(BDDState bddState) {
            return new State(bddState.getId(), bddState.isMcut(), bddState.isSpecial(), bddState.getContent());
        }

        private State(int id, boolean isMcut, boolean isSpecial, String content) {
            this.id = Integer.toString(id);
            this.isMcut = isMcut;
            this.isSpecial = isSpecial;
            this.content = content;
        }
    }
}
