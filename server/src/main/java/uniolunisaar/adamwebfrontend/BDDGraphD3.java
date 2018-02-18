package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents the data needed to display a BDD graph in our client.
 * This class is meant to be serialized using GSON.
 */
public class BDDGraphD3 {
    private final Set<State> nodes;
    private final Set<Flow> links;
    private final Map<String, NodePosition> nodePositions;

    private BDDGraphD3(Set<State> states, Set<Flow> flows) {
        this.nodes = states;
        this.links = flows;
        this.nodePositions = new HashMap<>();
    }

    /**
     * @return JSON representing all of the contents of bddGraph, with no states being hidden
     */
    public static JsonElement of(BDDGraph bddGraph) {
        return of(bddGraph.getStates(), bddGraph.getFlows(), bddGraph.getStates(), Collections.emptySet());
    }

    /**
     * @param states a set of visible states in a BDDGraph
     * @param flows a set of flows in a BDDGraph
     * @param postSetExpandedStates Set of states whose postsets have been expanded in the UI
     * @param presetExpandedStates Set of states whose presets have been expanded in the UI
     * @return JSON representing a subset of a BDDGraph.
     * States that have been clicked on by the user to display their preset/postset are annotated
     * so that the user knows if they clicked on them yet or not.
     */
    public static JsonElement of(Set<BDDState> states,
                                 Set<uniolunisaar.adam.ds.graph.Flow> flows,
                                 Set<BDDState> postSetExpandedStates,
                                 Set<BDDState> presetExpandedStates) {
        Set<State> nodes = states.stream()
                .map((BDDState state) -> State.of(state, postSetExpandedStates.contains(state), presetExpandedStates.contains(state)))
                .collect(Collectors.toSet());
        Set<Flow> links = flows.stream()
                .map((uniolunisaar.adam.ds.graph.Flow flow) -> Flow.of(flow))
                .collect(Collectors.toSet());
        BDDGraphD3 bddGraphD3 = new BDDGraphD3(nodes, links);
        return new Gson().toJsonTree(bddGraphD3);
    }


    private static class Flow extends GraphLink {
        private final String transitionId; // Nullable

        public static Flow of(uniolunisaar.adam.ds.graph.Flow flow) {
            Transition transition = flow.getTransition();
            String transitionId = transition == null ? null : transition.getId();
            return new Flow(Integer.toString(flow.getSourceid()), Integer.toString(flow.getTargetid()), transitionId);
        }

        private Flow(String source, String target, String transitionId) {
            super(source, target);
            this.transitionId = transitionId;
        }
    }

    private static class State extends GraphNode {
        private final boolean isMcut;
        private final boolean isGood;
        private final boolean isBad;
        private final String content;

        // Has the State been clicked on in order to show its children in the UI?
        private final boolean isPostsetExpanded;
        // Has the State been clicked on to show its parents?
        private final boolean isPresetExpanded;

        public static State of(BDDState bddState, boolean isPostsetExpanded, boolean isPresetExpanded) {
            return new State(
                    bddState.getId(),
                    bddState.isMcut(),
                    bddState.isGood(),
                    bddState.isBad(),
                    bddState.getContent(),
                    isPostsetExpanded,
                    isPresetExpanded);
            // TODO Isbad dicke schwarze Rahmen
            // TODO isGood doppelter Rahmen (nicht so dick)
            // TODO See Graphenspiele Beispiele in den Besipielen
        }

        private State(int id, boolean isMcut, boolean isGood, boolean isBad, String content, boolean isPostsetExpanded, boolean isPresetExpanded) {
            super(Integer.toString(id), Integer.toString(id), GraphNodeType.GRAPH_STRATEGY_BDD_STATE);
            this.isMcut = isMcut;
            this.isGood = isGood;
            this.isBad = isBad;
            this.content = content;
            this.isPostsetExpanded = isPostsetExpanded;
            this.isPresetExpanded = isPresetExpanded;
        }
    }
}
