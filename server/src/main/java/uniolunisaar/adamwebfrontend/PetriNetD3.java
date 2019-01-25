package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import uniol.apt.adt.pn.*;
import uniolunisaar.adam.AdamModelChecker;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;
import uniolunisaar.adam.exceptions.pg.NotSupportedGameException;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.AdamExtensions;
import uniolunisaar.adam.util.PNWTTools;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents the data needed to display a PetriNet in our graph editor.
 * This class is meant to be serialized using GSON and fed directly into our D3 code.
 */
public class PetriNetD3 {
    private final List<PetriNetLink> links;
    private final List<PetriNetNode> nodes;
    private final Map<String, NodePosition> nodePositions;
    private final String winningCondition;
    private final String ltlFormula;

    private PetriNetD3(List<PetriNetLink> links, List<PetriNetNode> nodes, Map<String, NodePosition> nodePositions, String winningCondition, String ltlFormula) {
        this.links = links;
        this.nodes = nodes;
        this.nodePositions = nodePositions;
        this.winningCondition = winningCondition;
        this.ltlFormula = ltlFormula;
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
    public static JsonElement of(PetriGame net, Set<Node> shouldSendPositions) {
        List<PetriNetLink> links = new ArrayList<>();
        List<PetriNetNode> nodes = new ArrayList<>();

        for (Place place : net.getPlaces()) {
            PetriNetNode placeNode = PetriNetNode.of(net, place);
            nodes.add(placeNode);
        }

        for (Transition transition : net.getTransitions()) {
            PetriNetNode transitionNode = PetriNetNode.of(transition);
            nodes.add(transitionNode);

//            for (TokenFlow tokenFlow : net.getTokenFlows(transition)) {
//                Place presetPlace = tokenFlow.getPresetPlace();
//                Set<Place> postset = tokenFlow.getPostset();
//                new PetriNetLink()
//            }
        }


        Map<Flow, String> flowRelationFromTransitions = PNWTTools.getTransitRelationFromTransitions(net);
        for (Flow flow : net.getEdges()) {

            String arcLabel = flowRelationFromTransitions.getOrDefault(flow, "");
            PetriNetLink petriNetLink = PetriNetLink.of(flow, net, arcLabel);
            links.add(petriNetLink);
        }

        Predicate<Node> hasPosition = (node) -> net.hasXCoord(node) && net.hasYCoord(node);
        Function<Node, NodePosition> positionOfNode = (node) -> {
            double x = net.getXCoord(node);
            double y = net.getYCoord(node);
            return new NodePosition(x, y);
        };

        Map<String, NodePosition> nodePositions = shouldSendPositions.stream()
                .filter(hasPosition)
                .collect(Collectors.toMap(
                        Node::getId, positionOfNode
                ));

        boolean hasCondition = net.hasExtension(AdamExtensions.condition.name());
        boolean hasWinningCondition = net.hasExtension(AdamExtensions.winningCondition.name());

        if (hasCondition || hasWinningCondition) {
            String condition = hasCondition ?
                    (String) net.getExtension(AdamExtensions.condition.name()) :
                    (String) net.getExtension(AdamExtensions.winningCondition.name());
            Condition.Objective objective = Condition.Objective.valueOf(condition);

            boolean canConvertToLtl = objective.equals(Condition.Objective.A_BUCHI) ||
                    objective.equals(Condition.Objective.A_REACHABILITY) ||
                    objective.equals(Condition.Objective.A_SAFETY);
            String ltlFormula = canConvertToLtl ? AdamModelChecker.toFlowLTLFormula(net, objective) : "";
            PetriNetD3 petriNetD3 = new PetriNetD3(links, nodes, nodePositions, condition, ltlFormula);
            return new Gson().toJsonTree(petriNetD3);
        } else {
            PetriNetD3 petriNetD3 = new PetriNetD3(links, nodes, nodePositions, "", "");
            return new Gson().toJsonTree(petriNetD3);
        }
    }

    /**
     * @return a JSON representation of a Petri Game. Does not include any X/Y coordinate annotations.
     */
    public static JsonElement of(PetriGame game) {
        return of(game, new HashSet<>());
    }

    public static JsonElement of(PetriNet net) throws NotSupportedGameException {
        // TODO consider if it is reasonable to do this.
        PetriGame game = new PetriGame(net);
        return of(game);
    }


    static class PetriNetLink extends GraphLink {
        private final String tokenFlow; // Null if there is no token flow given
        private final Float tokenFlowHue; // In the interval (0, 1].  Null if no color should be used.

        private PetriNetLink(String sourceId, String targetId, String tokenFlow, Float tokenFlowHue) {
            super(sourceId, targetId);
            this.tokenFlow = tokenFlow;
            this.tokenFlowHue = tokenFlowHue;
        }

        static PetriNetLink of(Flow flow, PetriGame net, String arcLabel) {
            String sourceId = flow.getSource().getId();
            String targetId = flow.getTarget().getId();
            Float tokenFlowHue = null;

            if (!arcLabel.equals("")) {
                // Give a unique color to each of the token flows associated with a transition.
                if (!arcLabel.contains(",")) { // Flows with multiple tokens are black.
                    Transit init = net.getInitialTransit(flow.getTransition());
                    int max =
                            net.getTransits(flow.getTransition()).size() + ((init == null) ? 0 :
                                    init.getPostset().size() - 1);
                    int id = Tools.calcStringIDSmallPrecedenceReverse(arcLabel);
                    tokenFlowHue = ((id + 1) * 1.f) / (max * 1.f);
                }
            }

            return new PetriNetLink(sourceId, targetId, arcLabel, tokenFlowHue);
        }
    }

    static class PetriNetNode extends GraphNode {
        // TODO Ask Manuel if the attribute "isBad" has been deprecated. I think we use isSpecial instead now.
        private final boolean isBad;
        private final long initialToken;
        private final boolean isSpecial;
        private final boolean isInitialTokenFlow;
        private final int partition;

        private PetriNetNode(String id, String label, GraphNodeType type, boolean isBad,
                             long initialToken, boolean isSpecial, boolean isInitialTokenFlow,
                             int partition) {
            super(id, label, type);
            this.isBad = isBad;
            this.initialToken = initialToken;
            this.isSpecial = isSpecial;
            this.isInitialTokenFlow = isInitialTokenFlow;
            this.partition = partition;
        }

        static PetriNetNode of(Transition t) {
            String id = t.getId();
            String label = t.getLabel();
            // Transitions are never bad or special and have no tokens
            return new PetriNetNode(id, label, GraphNodeType.TRANSITION, false, -1, false, false, -1);
        }

        static PetriNetNode of(PetriGame game, Place place) {
            String id = place.getId();
            String label = id;
            boolean isEnvironment = game.isEnvironment(place);
            boolean isBad = game.isBad(place);
            long initialToken = place.getInitialToken().getValue();
            boolean isSpecial = game.isSpecial(place);
            boolean isInitialTokenFlow = game.isInitialTransit(place);
            GraphNodeType nodeType = isEnvironment ? GraphNodeType.ENVPLACE : GraphNodeType.SYSPLACE;

            int partition = game.hasPartition(place) ? game.getPartition(place) : -1;
            return new PetriNetNode(id, label, nodeType, isBad, initialToken, isSpecial, isInitialTokenFlow, partition);
        }

    }
}
