package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonElement;
import uniolunisaar.adam.ds.graph.Flow;
import uniolunisaar.adam.ds.graph.synthesis.twoplayergame.symbolic.bddapproach.BDDGraph;
import uniolunisaar.adam.ds.graph.synthesis.twoplayergame.symbolic.bddapproach.BDDState;
import uniolunisaar.adamwebfrontend.wirerepresentations.BDDGraphClient;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wraps a BDDGraph and keeps track of the states that are meant to be visible in our client.
 * Exposes a read-only view of the graph.
 */
public class BDDGraphExplorerCompleteGraph implements BDDGraphExplorer {
    private final BDDGraph bddGraph;
    // States whose postset (children) should be visible
    private final Set<BDDState> postsetExpandedStates;
    // States whose preset (parents) should be visible
    private final Set<BDDState> presetExpandedStates;

    private BDDGraphExplorerCompleteGraph(BDDGraph bddGraph) {
        this.bddGraph = bddGraph;
        this.postsetExpandedStates = new HashSet<>();
        this.presetExpandedStates = new HashSet<>();
    }

    @Override
    public JsonElement getVisibleGraph() {
        Set<BDDState> visibleStates = this.visibleStates();
        Set<BDDState> invisibleStates = bddGraph.getStates().stream()
                .filter(s -> !visibleStates.contains(s))
                .collect(Collectors.toSet());
        Set<BDDState> statesWithInvisibleParents = visibleStates.stream()
                .filter(s -> invisibleStates.stream().anyMatch(invisibleState -> {
                    Set<BDDState> postset = bddGraph.getPostset(invisibleState.getId());
                    boolean isInvisibleStateAParent = postset.contains(s);
                    return isInvisibleStateAParent;
                })).collect(Collectors.toSet());
        Set<BDDState> statesWithInvisibleChildren = visibleStates.stream()
                .filter(s -> bddGraph.getPostset(s.getId()).stream().anyMatch(child -> {
                    boolean isChildInvisible = !visibleStates.contains(child);
                    return isChildInvisible;
                })).collect(Collectors.toSet());
        return BDDGraphClient.ofSubsetOfBddGraph(
                visibleStates,
                this.visibleFlows(),
                this.postsetExpandedStates,
                this.presetExpandedStates,
                statesWithInvisibleParents,
                statesWithInvisibleChildren);
    }

    private Set<BDDState> visibleStates() {
        BDDState initial = bddGraph.getInitial();
        Set<BDDState> visibleStates = new HashSet<>();
        Stack<BDDState> statesToExplore = new Stack<>();
        statesToExplore.add(initial);

        // Partially traverse the graph, expanding nodes (States) in two directions:
        // For nodes that are "postset expanded", we expand all of their descendants.
        // For nodes that are "preset expanded", we expand all of the nodes from which they descend.
        // All the nodes that we visit during this traversal will be visible in the UI.
        while (!statesToExplore.isEmpty()) {
            BDDState state = statesToExplore.pop();
            visibleStates.add(state);
            if (postsetExpandedStates.contains(state)) {
                Set<BDDState> postset = bddGraph.getPostset(state.getId());
                Set<BDDState> newUnexploredStates = postset.stream()
                        .filter(s -> !visibleStates.contains(s)) // Don't expand the same state twice
                        .collect(Collectors.toSet());
                statesToExplore.addAll(newUnexploredStates);
            }
            if (presetExpandedStates.contains(state)) {
                // There's no nice method "bddGraph.getPreset()", so we have to construct the preset ourselves.
                // This is O(n^2).
                Stream<BDDState> presetStream = bddGraph.getStates().stream()
                        .filter(s -> bddGraph.getPostset(s.getId()).contains(state));
                Set<BDDState> newUnexploredStates = presetStream
                        .filter(s -> !visibleStates.contains(s)) // Don't expand the same state twice
                        .collect(Collectors.toSet());
                statesToExplore.addAll(newUnexploredStates);
            }
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
        return new BDDGraphExplorerCompleteGraph(graphGameBDD);
    }

    @Override
    public void toggleStatePostset(int stateId) {
        BDDState state = this.bddGraph.getState(stateId);
        if (state == null) {
            throw new NoSuchElementException("No state was found with the given ID (" + stateId
                    + ") in this BDDGraph.");
        } else {
            boolean isStateExpanded = postsetExpandedStates.contains(state);
            if (isStateExpanded) {
                postsetExpandedStates.remove(state);
            } else {
                postsetExpandedStates.add(state);
            }
        }
    }

    @Override
    public void toggleStatePreset(int stateId) {
        BDDState state = this.bddGraph.getState(stateId);
        if (state == null) {
            throw new NoSuchElementException("No state was found with the given ID (" + stateId
                    + ") in this BDDGraph.");
        } else {
            boolean isStatePreSetVisible = presetExpandedStates.contains(state);
            if (isStatePreSetVisible) {
                presetExpandedStates.remove(state);
            } else {
                presetExpandedStates.add(state);
            }
        }
    }
}
