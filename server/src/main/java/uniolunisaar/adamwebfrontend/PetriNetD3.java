package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import uniol.apt.adt.pn.*;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.AdamModelChecker;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinet.objectives.Condition;
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

    // This is only present for some PNWT/PetriGames
    private final String winningCondition;
    // This is only used by PNWT in the model checking case
    private final String ltlFormula;

    private PetriNetD3(List<PetriNetLink> links, List<PetriNetNode> nodes, Map<String, NodePosition> nodePositions, String winningCondition, String ltlFormula) {
        this.links = links;
        this.nodes = nodes;
        this.nodePositions = nodePositions;
        this.winningCondition = winningCondition;
        this.ltlFormula = ltlFormula;
    }

    @FunctionalInterface
    interface FlowSerializer<T extends PetriNet> {
        PetriNetLink serializeFlow(Flow flow, T net, String arcLabel);
    }

    @FunctionalInterface
    interface PlaceSerializer<T extends PetriNet> {
        PetriNetNode serializePlace(T net, Place place);
    }

    @FunctionalInterface
    interface TransitionSerializer<T extends PetriNet> {
        PetriNetNode serializeTransition(T net, Transition transition);
    }


    public static JsonElement serializePetriNet(PetriNet net,
                                                Set<Node> shouldSendPositions) {
        return ofNetWithXYCoordinates(net, shouldSendPositions, false,
                PetriNetNode::fromPetriNetPlace,
                PetriNetNode::fromTransition,
                PetriNetLink::fromPetriNetFlow);
    }

    public static JsonElement serializePNWT(PetriNetWithTransits net,
                                            Set<Node> shouldSendPositions,
                                            boolean shouldIncludeObjective) {
        return ofNetWithXYCoordinates(net, shouldSendPositions, shouldIncludeObjective,
                PetriNetNode::fromPNWTPlace,
                PetriNetNode::fromTransition,
                PetriNetLink::fromPNWTFlow);
    }

    public static JsonElement serializePetriGame(PetriGame net,
                                                 Set<Node> shouldSendPositions,
                                                 boolean shouldIncludeObjective) {

        return ofNetWithXYCoordinates(net, shouldSendPositions, shouldIncludeObjective,
                PetriNetNode::fromPetriGamePlace,
                PetriNetNode::fromTransition,
                PetriNetLink::fromPetriGameFlow);
    }


    /**
     * Extract all the information needed to display a PetriNet in our graph editor.
     *
     * @param net                    - A PetriNet, PetriNetWithTransits, or PetriGame
     * @param shouldSendPositions    - We will send x/y coordinates for these nodes to the client.
     * @param shouldIncludeObjective - For Petri Games, we want to include the objective/winning
     *                               condition, but for other Petri Nets (e.g. those which represent winning strategies), the
     *                               annotation may not be present, and we shouldn't try to send it to the client.
     * @return A JSON object containing the relevant information from the PetriNet
     * <p>
     * See https://github.com/d3/d3-force
     */
    public static <T extends PetriNet> JsonElement ofNetWithXYCoordinates(
            T net,
            Set<Node> shouldSendPositions,
            boolean shouldIncludeObjective,
            PlaceSerializer<T> placeSerializer,
            TransitionSerializer<T> transitionSerializer,
            FlowSerializer<T> flowSerializer) throws SerializationException {
        List<PetriNetLink> links = new ArrayList<>();
        List<PetriNetNode> nodes = new ArrayList<>();

        for (Place place : net.getPlaces()) {
            PetriNetNode placeNode = placeSerializer.serializePlace(net, place);
            nodes.add(placeNode);
        }

        for (Transition transition : net.getTransitions()) {
            PetriNetNode transitionNode = transitionSerializer.serializeTransition(net, transition);
            nodes.add(transitionNode);
        }

        Predicate<Node> hasPosition =
                (node) -> PetriNetExtensionHandler.hasXCoord(node) &&
                        PetriNetExtensionHandler.hasYCoord(node);
        Function<Node, NodePosition> positionOfNode = (node) -> {
            double x = PetriNetExtensionHandler.getXCoord(node);
            double y = PetriNetExtensionHandler.getYCoord(node);
            return new NodePosition(x, y);
        };

        Map<String, NodePosition> nodePositions = shouldSendPositions.stream()
                .filter(hasPosition)
                .collect(Collectors.toMap(
                        Node::getId, positionOfNode
                ));

        String objectiveString;
        String ltlFormulaString;
        Map<Flow, String> flowRelationFromTransitions;
        if (net instanceof PetriNetWithTransits) {
            PetriNetWithTransits pnwt = (PetriNetWithTransits) net;
            flowRelationFromTransitions = PNWTTools.getTransitRelationFromTransitions(pnwt);

            if (shouldIncludeObjective) {
                Condition.Objective objective;
                try {
                    objective = Adam.getCondition(pnwt);
                } catch (CouldNotFindSuitableConditionException e) {
                    throw new SerializationException(e);
                }
                boolean canConvertToLtl = objective.equals(Condition.Objective.A_BUCHI) ||
                        objective.equals(Condition.Objective.A_REACHABILITY) ||
                        objective.equals(Condition.Objective.A_SAFETY);
                ltlFormulaString = canConvertToLtl ?
                        AdamModelChecker.toFlowLTLFormula(pnwt, objective) :
                        "";
                objectiveString = objective.toString();
            } else {
                objectiveString = "";
                ltlFormulaString = "";
            }
        } else {
            objectiveString = "";
            ltlFormulaString = "";
            flowRelationFromTransitions = new HashMap<>();
        }
        for (Flow flow : net.getEdges()) {
            String arcLabel = flowRelationFromTransitions.getOrDefault(flow, "");
            PetriNetLink petriNetLink = flowSerializer.serializeFlow(flow, net, arcLabel);
            links.add(petriNetLink);
        }
        PetriNetD3 petriNetD3 = new PetriNetD3(links, nodes, nodePositions, objectiveString,
                ltlFormulaString);
        return new Gson().toJsonTree(petriNetD3);
    }

    /**
     * @return a JSON representation of a Petri Net/PNWT/Petri Game. Does not include any X/Y
     * coordinate annotations.
     */
    public static JsonElement ofPetriNetWithTransits(PetriNet net) throws SerializationException {
        try {
            return ofPetriNetWithXYCoordinates(net, new HashSet<>(), true);
        } catch (CouldNotFindSuitableConditionException e) {
            throw new SerializationException(e);
        }
    }

    /**
     * @return a JSON representation of a Petri Net/PNWT/Petri Game. Does not include any X/Y
     * coordinate annotations or winning condition.
     */
    public static JsonElement ofNetWithoutObjective(PetriNet net) throws SerializationException {
        try {
            return ofPetriNetWithXYCoordinates(net, new HashSet<>(), false);
        } catch (CouldNotFindSuitableConditionException e) {
            throw new SerializationException(e);
        }
    }


    static class PetriNetLink extends GraphLink {
        private final String type = "petriNetLink";
        private final String transit; // Null if there is no transit given
        private final boolean isInhibitorArc; // Is it an inhibitor arc?
        // This is only present for PNWT and Petri Games
        private Float transitHue; // In the interval (0, 1].  Null if no color should be used.

        private PetriNetLink(String sourceId, String targetId, String transit, Float transitHue, boolean isInhibitorArc) {
            super(sourceId, targetId);
            this.transit = transit;
            this.transitHue = transitHue;
            this.isInhibitorArc = isInhibitorArc;
        }

        static PetriNetLink fromPetriGameFlow(Flow flow, PetriGame net, String arcLabel) {
            return fromPNWTFlow(flow, net, arcLabel);
        }

        static PetriNetLink fromPNWTFlow(Flow flow, PetriNetWithTransits net, String arcLabel) {
            PetriNetLink link = fromPetriNetFlow(flow, net, arcLabel);
            if (!arcLabel.equals("")) {
                // Give a unique color to each of the transits associated with a transition.
                if (!arcLabel.contains(",")) { // Flows with multiple tokens are black.
                    Transit init = net.getInitialTransit(flow.getTransition());
                    int max = net.getTransits(flow.getTransition()).size() + ((init == null) ? 0 :
                            init.getPostset().size() - 1);
                    int id = Tools.calcStringIDSmallPrecedenceReverse(arcLabel);
                    float transitHue = ((id + 1) * 1.f) / (max * 1.f);
                    link.setTransitHue(transitHue);
                }
            }
            return link;
        }

        static PetriNetLink fromPetriNetFlow(Flow flow, PetriNet net, String arcLabel) {
            String sourceId = flow.getSource().getId();
            String targetId = flow.getTarget().getId();
            boolean isInhibitorArc = PetriNetExtensionHandler.isInhibitor(flow);
            Float transitHue = null;

            return new PetriNetLink(sourceId, targetId, arcLabel, transitHue, isInhibitorArc);
        }

        void setTransitHue(float transitHue) {
            this.transitHue = transitHue;
        }
    }

    static class PetriNetNode extends GraphNode {
        // TODO Ask Manuel if the attribute "isBad" has been deprecated. I think we use isSpecial instead now.
        private final boolean isBad;
        private final long initialToken;
        private final String fairness;
        private final boolean isReadyToFire;
        // These properties only belong to PNWT
        private boolean isSpecial;
        private boolean isInitialTransit;
        private int partition;
        // These properties only belong to Petri Games
        private GraphNodeType nodeType;

        private PetriNetNode(String id, String label, GraphNodeType type, boolean isBad,
                             long initialToken, boolean isSpecial, boolean isInitialTransit,
                             int partition, String fairness, boolean isReadyToFire) {
            super(id, label, type);
            this.isBad = isBad;
            this.initialToken = initialToken;
            this.isSpecial = isSpecial;
            this.isInitialTransit = isInitialTransit;
            this.partition = partition;
            this.fairness = fairness;
            this.isReadyToFire = isReadyToFire;
        }

        static PetriNetNode fromTransition(PetriNet net, Transition t) {
            String id = t.getId();
            String label = t.getLabel();
            String fairness;
            if (PetriNetExtensionHandler.isStrongFair(t)) {
                fairness = "strong";
            } else if (PetriNetExtensionHandler.isWeakFair(t)) {
                fairness = "weak";
            } else {
                fairness = "none";
            }
            boolean isReadyToFire = t.isFireable(net.getInitialMarking());
            // Transitions are never bad or special and have no tokens
            return new PetriNetNode(id, label, GraphNodeType.TRANSITION, false, -1, false, false,
                    -1, fairness, isReadyToFire);
        }

        static PetriNetNode fromPetriGamePlace(PetriGame game, Place place) {
            PetriNetNode node = fromPNWTPlace(game, place);
            boolean isEnvironment = game.isEnvironment(place);
            GraphNodeType nodeType = isEnvironment ? GraphNodeType.ENVPLACE : GraphNodeType.SYSPLACE;
            node.setNodeType(nodeType);
            return node;
        }

        static PetriNetNode fromPNWTPlace(PetriNetWithTransits net, Place place) {
            PetriNetNode node = fromPetriNetPlace(net, place);
            int partition = net.hasPartition(place) ? net.getPartition(place) : -1;
            node.setPartition(partition);
            boolean isInitialTransit = net.isInitialTransit(place);
            node.setIsInitialTransit(isInitialTransit);
            boolean isSpecial;
            try {
                Condition.Objective objective = Adam.getCondition(net);
                isSpecial = isSpecial(net, place, objective);
            } catch (CouldNotFindSuitableConditionException e) {
                isSpecial = false;
            }
            node.setIsSpecial(isSpecial);
            return node;
        }

        static PetriNetNode fromPetriNetPlace(PetriNet net, Place place) {
            String id = place.getId();
            String label = id;

            boolean isBad = PetriNetExtensionHandler.isBad(place);
            long initialToken = place.getInitialToken().getValue();

            boolean isSpecial = false;
            boolean isInitialTransit = false;
            int partition = -1;

            GraphNodeType nodeType = GraphNodeType.SYSPLACE;

            String fairness = "none"; // Places have no concept of fairness
            boolean isReadyToFire = false; // Places can't be fired, only Transitions can.

            return new PetriNetNode(id, label, nodeType, isBad, initialToken, isSpecial,
                    isInitialTransit, partition, fairness, isReadyToFire);
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

        public void setIsInitialTransit(boolean isInitialTransit) {
            this.isInitialTransit = isInitialTransit;
        }

        public void setPartition(int partition) {
            this.partition = partition;
        }

        public void setIsSpecial(boolean isSpecial) {
            this.isSpecial = isSpecial;
        }

        public void setNodeType(GraphNodeType nodeType) {
            this.nodeType = nodeType;
        }
    }
}
