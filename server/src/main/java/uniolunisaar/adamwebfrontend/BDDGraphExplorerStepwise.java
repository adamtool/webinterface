package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonElement;
import uniol.apt.util.Pair;
import uniolunisaar.adam.AdamSynthesizer;
import uniolunisaar.adam.ds.graph.Flow;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.exceptions.pg.SolvingException;
import uniolunisaar.adam.exceptions.pnwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDState;
import uniolunisaar.adam.symbolic.bddapproach.solver.BDDSolver;
import uniolunisaar.adam.symbolic.bddapproach.solver.BDDSolverOptions;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BDDGraphExplorerStepwise implements BDDGraphExplorer {
    private final BDDSolver<? extends Condition> solver;
    private final BDDGraph bddGraph;
    // States whose postset (children) should be visible
    private final Set<BDDState> postsetExpandedStates;
    // States whose preset (parents) should be visible
    private final Set<BDDState> presetExpandedStates;
    // States that we called getSuccessors() on already
    private final Set<BDDState> expandedStates;

    public BDDGraphExplorerStepwise(PetriGame game) throws SolvingException, CouldNotFindSuitableConditionException {
        solver = AdamSynthesizer.getBDDSolver(
                game,
                PetriNetD3.getObjectiveOfPetriNet(game),
                new BDDSolverOptions());
        bddGraph = new BDDGraph("My Graph");
        solver.initialize();
        BDDState initialState = AdamSynthesizer.getInitialGraphGameState(bddGraph, solver);
        postsetExpandedStates = new HashSet<>();
        presetExpandedStates = new HashSet<>();
        expandedStates = new HashSet<>();
        Pair<List<Flow>, List<BDDState>> successors = AdamSynthesizer.getSuccessors(initialState, bddGraph, solver);
    }

    private Set<BDDState> visibleStates() {
        BDDState initial = bddGraph.getInitial();
        Set<BDDState> visibleStates = new HashSet<>();
        Stack<BDDState> statesToExplore = new Stack<>();
        statesToExplore.add(initial);

        // Partially traverse the bddGraph, expanding nodes (States) in two directions:
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
                // TODO Prevent visiting the same node twice?
                // (If we expand node A, and node B is in preset(A) as well as postset(A), then
                // B will get added twice to the stack of statesToExplore.)
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
                .filter(s -> {
                    return !expandedStates.contains(s) ||
                            bddGraph.getPostset(s.getId()).stream().anyMatch(child -> {
                                boolean isChildInvisible = !visibleStates.contains(child);
                                return isChildInvisible;
                            });
                }).collect(Collectors.toSet());
        return BDDGraphD3.ofSubsetOfBddGraph(
                visibleStates,
                this.visibleFlows(),
                this.postsetExpandedStates,
                this.presetExpandedStates,
                statesWithInvisibleParents,
                statesWithInvisibleChildren);
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
                if (!expandedStates.contains(state)) {
                    expandedStates.add(state);
                    Pair<List<Flow>, List<BDDState>> successors = getSuccessors(state);
                }
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

    private Pair<List<Flow>, List<BDDState>> getSuccessors(BDDState state) {
        return AdamSynthesizer.getSuccessors(state, bddGraph, solver);
    }
}
