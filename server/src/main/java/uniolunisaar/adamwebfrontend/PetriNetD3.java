package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import uniol.apt.adt.pn.*;
import uniolunisaar.adam.ds.util.AdamExtensions;
import uniolunisaar.adam.tools.Tools;

import java.util.*;
import java.util.stream.Collectors;

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
     *
     * @param net                 - A PetriNet
     * @param shouldSendPositions - We will send x/y coordinates for these nodes to the client.
     * @return A JSON object containing the relevant information from the PetriNet
     * <p>
     * See https://github.com/d3/d3-force
     */
    public static JsonElement of(PetriNet net, Set<Node> shouldSendPositions) {
        List<PetriNetLink> links = new ArrayList<>();
        List<PetriNetNode> nodes = new ArrayList<>();

        for (Place place : net.getPlaces()) {
            PetriNetNode placeNode = PetriNetNode.of(place);
            nodes.add(placeNode);
        }

        for (Transition transition : net.getTransitions()) {
            PetriNetNode transitionNode = PetriNetNode.of(transition);
            nodes.add(transitionNode);
        }

        for (Flow flow : net.getEdges()) {
            PetriNetLink petriNetLink = PetriNetLink.of(flow);
            links.add(petriNetLink);
        }

        Map<String, NodePosition> nodePositions = shouldSendPositions.stream()
                .filter(PetriNetD3::hasPosition)
                .collect(Collectors.toMap(
                        Node::getId, PetriNetD3::positionOf
                ));

        PetriNetD3 petriNetD3 = new PetriNetD3(links, nodes, nodePositions);
        return new Gson().toJsonTree(petriNetD3);
    }

    private static boolean hasPosition(Node node) {
        return AdamExtensions.hasXCoord(node) && AdamExtensions.hasYCoord(node);
    }

    private static NodePosition positionOf(Node node) {
        double x = AdamExtensions.getXCoord(node);
        double y = AdamExtensions.getYCoord(node);
        return new NodePosition(x, y);
    }

    /**
     * @param net A P
     * @return
     */
    public static JsonElement of(PetriNet net) {
        return of(net, new HashSet<>());
    }

    /**
     * @param net A PetriNet whose nodes may have X/Y coordinates saved as extensions
     * @return A Map from node ID -> NodePosition (which can be converted into a JSON Object)
     */
    private static Map<String, NodePosition> nodePositionsOf(PetriNet net) {
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
        return nodePositions;
    }

    static class PetriNetLink extends GraphLink {
        private final String tokenFlow; // Null if there is no token flow given
        private final Float tokenFlowHue; // In the interval (0, 1].  Null if no color should be used.

        private PetriNetLink(String sourceId, String targetId, String tokenFlow, Float tokenFlowHue) {
            super(sourceId, targetId);
            this.tokenFlow = tokenFlow;
            this.tokenFlowHue = tokenFlowHue;
        }

        static PetriNetLink of(Flow flow) {
            String sourceId = flow.getSource().getId();
            String targetId = flow.getTarget().getId();
            String tokenFlow = null;
            Float tokenFlowHue = null;

            if (AdamExtensions.hasTokenFlow(flow)) {
                tokenFlow = AdamExtensions.getTokenFlow(flow);
                // Give a unique color to each of the token flows associated with a transition.
                if (!tokenFlow.contains(",")) { // Flows with multiple tokens are black.
                    int max = AdamExtensions.getTokenFlow(flow.getTransition()).size();
                    int id = Tools.calcStringIDSmallPrecedenceReverse(tokenFlow);
                    tokenFlowHue = ((id + 1) * 1.f) / (max * 1.f);
                }
            }

            return new PetriNetLink(sourceId, targetId, tokenFlow, tokenFlowHue);
        }
    }

    static class PetriNetNode extends GraphNode {
        // TODO Ask Manuel if the attribute "isBad" has been deprecated. I think we use isSpecial instead now.
        private final boolean isBad;
        private final long initialToken;
        private final boolean isSpecial;

        private PetriNetNode(String id, String label, GraphNodeType type, boolean isBad, long initialToken, boolean isSpecial) {
            super(id, label, type);
            this.isBad = isBad;
            this.initialToken = initialToken;
            this.isSpecial = isSpecial;
        }

        static PetriNetNode of(Transition t) {
            String id = t.getId();
            String label = t.getLabel();
            // Transitions are never bad or special and have no tokens
            return new PetriNetNode(id, label, GraphNodeType.TRANSITION, false, -1, false);
        }

        static PetriNetNode of(Place place) {
            String id = place.getId();
            String label = id;
            boolean isEnvironment = AdamExtensions.isEnvironment(place);
            boolean isBad = AdamExtensions.isBad(place);
            long initialToken = place.getInitialToken().getValue();
            boolean isSpecial = AdamExtensions.isSpecial(place);
            GraphNodeType nodeType = isEnvironment ? GraphNodeType.ENVPLACE : GraphNodeType.SYSPLACE;
            return new PetriNetNode(id, label, nodeType, isBad, initialToken, isSpecial);
        }
    }
}
