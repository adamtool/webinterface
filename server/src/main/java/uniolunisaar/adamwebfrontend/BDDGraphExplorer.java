package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonElement;
import uniolunisaar.adam.ds.graph.Flow;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps a BDDGraph and keeps track of the states that are meant to be visible in our client.
 * Exposes a read-only view of the graph.
 */
public class BDDGraphExplorer {
    private final BDDGraph bddGraph;
    private Set<BDDState> expandedStates;

    private BDDGraphExplorer(BDDGraph bddGraph) {
        this.bddGraph = bddGraph;
        this.expandedStates = Collections.emptySet();
    }

    public JsonElement getVisibleGraph() {
        return BDDGraphD3.of(this.visibleStates(), this.visibleFlows());
    }

    private Set<BDDState> visibleStates() {
        BDDState initial = bddGraph.getInitial();
        HashSet<BDDState> visibleStates = new HashSet<>(Arrays.asList(initial));
        for (BDDState expandedState : expandedStates) {
            Set<BDDState> postset = bddGraph.getPostset(expandedState.getId());
            visibleStates.addAll(postset);
        }
        return visibleStates;
    }

    private Set<Flow> visibleFlows() {
        Set<BDDState> visibleStates = this.visibleStates();
        return this.bddGraph.getFlows().stream()
                .filter(flow -> {
                    boolean isSourceVisible = visibleStates.stream()
                            .anyMatch(state -> state.getId() == flow.getSourceid());
                    boolean isTargetVisible = visibleStates.stream()
                            .anyMatch(state -> state.getId() == flow.getTargetid());
                    return isSourceVisible && isTargetVisible;
                }).collect(Collectors.toSet());
    }


    public static BDDGraphExplorer of(BDDGraph graphGameBDD) {
        return new BDDGraphExplorer(graphGameBDD);
    }

    public void toggleState(int stateId) {
        BDDState state = this.bddGraph.getState(stateId);
        if (state == null) {
            throw new NoSuchElementException("No state was found with the given ID (" + stateId
                    + ") in this BDDGraph.");
        } else {
            boolean isStateExpanded = expandedStates.contains(state);
            if (isStateExpanded) {
                expandedStates.remove(state);
            } else {
                expandedStates.add(state);
            }
        }
    }
}
