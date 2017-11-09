package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonElement;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDState;

import java.util.Arrays;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Wraps a BDDGraph and keeps track of the states that are meant to be visible in our client.
 * Exposes a read-only view of the graph.
 */
public class BDDGraphExplorer {
    private final BDDGraph bddGraph;
    private Set<BDDState> visibleStates;

    private BDDGraphExplorer(BDDGraph bddGraph) {
        this.bddGraph = bddGraph;
        this.visibleStates = new HashSet<>(Arrays.asList(bddGraph.getInitial()));
    }

    public JsonElement getVisibleGraph() {
        return BDDGraphD3.of(this.visibleStates, this.bddGraph.getFlows());
    }

    public void expandState(int id) {
        BDDState state = this.bddGraph.getState(id);
        if (state == null) {
            throw new NoSuchElementException("No state was found with the given ID in this BDDGraph.");
        } else {
            Set<BDDState> postset = bddGraph.getPostset(id);
            visibleStates.addAll(postset);
        }
    }

    public void collapseState(int id) {
        BDDState state = this.bddGraph.getState(id);
        if (state == null) {
            throw new NoSuchElementException("No state was found with the given ID in this BDDGraph.");
        } else {
            Set<BDDState> postset = bddGraph.getPostset(id);
            visibleStates.removeAll(postset);
        }
    }

    public static BDDGraphExplorer of(BDDGraph graphGameBDD) {
        return new BDDGraphExplorer(graphGameBDD);
    }
}
