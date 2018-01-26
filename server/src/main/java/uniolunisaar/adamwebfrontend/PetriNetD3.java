package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import uniol.apt.adt.Node;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.util.AdamExtensions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the data needed to display a PetriNet in our graph editor.
 * This class is meant to be serialized using GSON and fed directly into our D3 code.
 */
public class PetriNetD3 {
    private final List<PetriNetLink> links;
    private final List<PetriNetNode> nodes;
    private final Map<String, NodePosition> nodePositions;

    private PetriNetD3(List<PetriNetLink> links, List<PetriNetNode> nodes, Map<String, NodePosition> nodePositions) {
        this.links = links;
        this.nodes = nodes;
        this.nodePositions = nodePositions;
    }

    /**
     * Extract all the information needed to display a PetriNet in our graph editor.
     * @param net - A PetriNet
     * @return A serializable object containing the relevant information from the PetriNet
     * <p>
     * See https://github.com/d3/d3-force
     */
    public static JsonElement of(PetriNet net) {
        List<PetriNetLink> links = new ArrayList<>();
        List<PetriNetNode> nodes = new ArrayList<>();

        for (Place place : net.getPlaces()) {
            boolean isBad = AdamExtensions.isBad(place);
            long initialToken = place.getInitialToken().getValue();
            if (AdamExtensions.isEnvironment(place)) {
                nodes.add(PetriNetNode.envPlace(place.getId(), place.getId(), isBad, initialToken));
            } else {
                nodes.add(PetriNetNode.sysPlace(place.getId(), place.getId(), isBad, initialToken));
            }

            // TODO: Can this be done neater using a loop like "for (Flow edge : net.getEdges()) {...}"?
            for (Transition preTransition : place.getPreset()) {
                PetriNetLink link = PetriNetLink.of(preTransition, place);
                links.add(link);
            }
            for (Transition postTransition : place.getPostset()) {
                PetriNetLink link = PetriNetLink.of(place, postTransition);
                links.add(link);
            }
        }

        for (Transition transition : net.getTransitions()) {
            PetriNetNode transitionNode = PetriNetNode.transition(transition.getId(), transition.getLabel());
            nodes.add(transitionNode);
        }

        // Add X/Y coordinates for nodes that have them
        Map<String, NodePosition> nodePositions = new HashMap<>();
        for (Node node : net.getNodes()) {
            // TODO note that this does not cover the strange case of a node having only one of X or Y specified.
            // I won't bother checking for that here, since it would clutter up the code.
            if (AdamExtensions.hasXCoord(node) && AdamExtensions.hasYCoord(node)) {
                double x = AdamExtensions.getXCoord(node);
                double y = AdamExtensions.getYCoord(node);
                nodePositions.put(node.getId(), new NodePosition(x, y));
            }
        }

        PetriNetD3 petriNetD3 = new PetriNetD3(links, nodes, nodePositions);
        return new Gson().toJsonTree(petriNetD3);
    }

    static class PetriNetLink extends GraphLink {
        private PetriNetLink(String source, String target) {
            super(source, target);
        }
        static PetriNetLink of(Place place, Transition transition) {
            return new PetriNetLink(place.getId(), transition.getId());
        }
        static PetriNetLink of(Transition transition, Place place) {
            return new PetriNetLink(transition.getId(), place.getId());
        }
    }

    static class PetriNetNode extends GraphNode {
        private final boolean isBad;
        private final long initialToken;

        PetriNetNode(String id, String label, GraphNodeType type, boolean isBad, long initialToken) {
            super(id, label, type);
            this.isBad = isBad;
            this.initialToken = initialToken;
        }

        static PetriNetNode transition(String id, String label) {
            // Transitions are never bad and have no tokens
            return new PetriNetNode(id, label, GraphNodeType.TRANSITION, false, -1);
        }

        static PetriNetNode envPlace(String id, String label, boolean isBad, long initialToken) {
            return new PetriNetNode(id, label, GraphNodeType.ENVPLACE, isBad, initialToken);
        }

        static PetriNetNode sysPlace(String id, String label, boolean isBad, long initialToken) {
            return new PetriNetNode(id, label, GraphNodeType.SYSPLACE, isBad, initialToken);
        }
    }
}
