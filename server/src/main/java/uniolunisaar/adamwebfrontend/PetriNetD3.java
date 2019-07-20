package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import uniol.apt.adt.pn.*;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.AdamModelChecker;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;
import uniolunisaar.adam.exceptions.pnwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.tools.Tools;
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
     * @param net                    - A PetriNet
     * @param shouldSendPositions    - We will send x/y coordinates for these nodes to the client.
     * @param shouldIncludeObjective - For Petri Games, we want to include the objective/winning
     *                               condition, but for other Petri Nets (e.g. those which represent winning strategies), the
     *                               annotation may not be present, and we shouldn't try to send it to the client.
     * @return A JSON object containing the relevant information from the PetriNet
     * <p>
     * See https://github.com/d3/d3-force
     */
    public static JsonElement ofPetriNetWithXYCoordinates(PetriNetWithTransits net,
                                                          Set<Node> shouldSendPositions,
                                                          boolean shouldIncludeObjective) throws CouldNotFindSuitableConditionException {
        List<PetriNetLink> links = new ArrayList<>();
        List<PetriNetNode> nodes = new ArrayList<>();

        for (Place place : net.getPlaces()) {
            PetriNetNode placeNode = PetriNetNode.of(net, place);
            nodes.add(placeNode);
        }

        for (Transition transition : net.getTransitions()) {
            PetriNetNode transitionNode = PetriNetNode.of(net, transition);
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

        String objectiveString;
        String ltlFormulaString;
        if (shouldIncludeObjective) {
            Condition.Objective objective = Adam.getCondition(net);
            boolean canConvertToLtl = objective.equals(Condition.Objective.A_BUCHI) ||
                    objective.equals(Condition.Objective.A_REACHABILITY) ||
                    objective.equals(Condition.Objective.A_SAFETY);
            ltlFormulaString = canConvertToLtl ? AdamModelChecker.toFlowLTLFormula(net, objective) :
                    "";
            objectiveString = objective.toString();
        } else {
            objectiveString = "";
            ltlFormulaString = "";
        }
        PetriNetD3 petriNetD3 = new PetriNetD3(links, nodes, nodePositions, objectiveString,
                ltlFormulaString);
        return new Gson().toJsonTree(petriNetD3);
    }

    /**
     * @return a JSON representation of a Petri Game. Does not include any X/Y coordinate annotations.
     */
    public static JsonElement ofPetriNetWithTransits(PetriNetWithTransits net) throws SerializationException {
        try {
            return ofPetriNetWithXYCoordinates(net, new HashSet<>(), true);
        } catch (CouldNotFindSuitableConditionException e) {
            throw new SerializationException(e);
        }
    }

    public static JsonElement ofNetWithoutObjective(PetriGame game) throws SerializationException {
        try {
            return ofPetriNetWithXYCoordinates(game, new HashSet<>(), false);
        } catch (CouldNotFindSuitableConditionException e) {
            throw new SerializationException(e);
        }
    }


    static class PetriNetLink extends GraphLink {
        private final String tokenFlow; // Null if there is no token flow given
        private final Float tokenFlowHue; // In the interval (0, 1].  Null if no color should be used.

        private PetriNetLink(String sourceId, String targetId, String tokenFlow, Float tokenFlowHue) {
            super(sourceId, targetId);
            this.tokenFlow = tokenFlow;
            this.tokenFlowHue = tokenFlowHue;
        }

        static PetriNetLink of(Flow flow, PetriNetWithTransits net, String arcLabel) {
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
        private final String fairness;

        private PetriNetNode(String id, String label, GraphNodeType type, boolean isBad,
                             long initialToken, boolean isSpecial, boolean isInitialTokenFlow,
                             int partition, String fairness) {
            super(id, label, type);
            this.isBad = isBad;
            this.initialToken = initialToken;
            this.isSpecial = isSpecial;
            this.isInitialTokenFlow = isInitialTokenFlow;
            this.partition = partition;
            this.fairness = fairness;
        }

        static PetriNetNode of(PetriNetWithTransits net, Transition t) {
            String id = t.getId();
            String label = t.getLabel();
            String fairness;
            if (net.isStrongFair(t)) {
                fairness = "strong";
            } else if (net.isWeakFair(t)) {
                fairness = "weak";
            } else {
                fairness = "none";
            }
            // Transitions are never bad or special and have no tokens
            return new PetriNetNode(id, label, GraphNodeType.TRANSITION, false, -1, false, false,
                    -1, fairness);
        }

        static PetriNetNode of(PetriNetWithTransits net, Place place) {
            String id = place.getId();
            String label = id;

            boolean isEnvironment;
            if (net instanceof PetriGame) {
                PetriGame game1 = (PetriGame) net;
                isEnvironment = game1.isEnvironment(place);
            } else {
                isEnvironment = false;
            }
            boolean isBad = net.isBad(place);
            long initialToken = place.getInitialToken().getValue();

            boolean isSpecial;
            try {
                Condition.Objective objective = Adam.getCondition(net);
                isSpecial = isSpecial(net, place, objective);
            } catch (CouldNotFindSuitableConditionException e) {
                isSpecial = false;
            }

            boolean isInitialTokenFlow = net.isInitialTransit(place);
            GraphNodeType nodeType = isEnvironment ? GraphNodeType.ENVPLACE : GraphNodeType.SYSPLACE;

            int partition = net.hasPartition(place) ? net.getPartition(place) : -1;

            String fairness = "none"; // Places have no concept of fairness

            return new PetriNetNode(id, label, nodeType, isBad, initialToken, isSpecial,
                    isInitialTokenFlow, partition, fairness);
        }

        static boolean isSpecial(PetriNetWithTransits game, Place place, Condition.Objective objective) {
            switch (objective) {
                case A_REACHABILITY:
                case E_REACHABILITY:
                    return game.isReach(place);
                case A_BUCHI:
                case E_BUCHI:
                    return game.isBuchi(place);
                case A_SAFETY:
                case E_SAFETY:
                    return game.isBad(place);
                default:
                    return false;
            }
        }

    }
}
