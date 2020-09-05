package uniolunisaar.adamwebfrontend.wirerepresentations;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import uniol.apt.adt.pn.*;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.AdamModelChecker;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;
import uniolunisaar.adam.exceptions.pnwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adamwebfrontend.PetriNetTools;
import uniolunisaar.adamwebfrontend.SerializationException;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import uniolunisaar.adam.ds.objectives.Condition;
import static uniolunisaar.adam.ds.objectives.Condition.Objective.A_BUCHI;
import static uniolunisaar.adam.ds.objectives.Condition.Objective.A_REACHABILITY;
import static uniolunisaar.adam.ds.objectives.Condition.Objective.A_SAFETY;
import static uniolunisaar.adam.ds.objectives.Condition.Objective.E_BUCHI;
import static uniolunisaar.adam.ds.objectives.Condition.Objective.E_REACHABILITY;
import static uniolunisaar.adam.ds.objectives.Condition.Objective.E_SAFETY;

/**
 * Represents the data needed to represent a PetriNet or PNWT or PetriGame in the client.
 * This class is meant to be serialized using GSON.
 */
public class PetriNetClient {
    private final List<PetriNetLink> links;
    private final List<PetriNetNode> nodes;
    private final Map<String, NodePosition> nodePositions;
    private final Map<String, Long> initialMarking;
    private final Map<String, Boolean> fireableTransitions;

    // This is only present for some PNWT/PetriGames
    private String winningCondition;
    // This is only used by PNWT in the model checking case
    private String ltlFormula;

    private PetriNetClient(List<PetriNetLink> links, List<PetriNetNode> nodes, Map<String,
            NodePosition> nodePositions, Map<String, Long> initialMarking,
                           Map<String, Boolean> fireableTransitions) {
        this.links = links;
        this.nodes = nodes;
        this.nodePositions = nodePositions;
        this.initialMarking = initialMarking;
        this.fireableTransitions = fireableTransitions;
    }

    public void setLtlFormula(String ltlFormula) {
        this.ltlFormula = ltlFormula;
    }

    public void setWinningCondition(String winningCondition) {
        this.winningCondition = winningCondition;
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

    /**
     * TODO #293 See comment for the other method called serializeEditorNet below
     * @return a JSON representation of a Petri Net/PNWT/Petri Game in the editor.
     * Does not include any X/Y coordinate annotations.
     */
    public static JsonElement serializeEditorNet(PetriNetWithTransits net) {
        return serializeEditorNet(net, new HashSet<>());
    }

    /**
     * TODO #293 This is not an ideal practice, to use a single method to serialize both PNWT
     * and PetriGames, based on instanceof, but this is still present here in order to cope with
     * some implementation decisions that were made as support for the model checking approach
     * was added.
     *
     * @param includePositions the set of nodes whose x/y coordinates should be sent to the client
     * @return a JSON-encoded representation of a Petri Net/PNWT/Petri Game in the editor.
     * Includes the X/Y coordinates of the nodes specified in includePositions.
     */
    public static JsonElement serializeEditorNet(PetriNetWithTransits net,
                                                 Set<Node> includePositions) {
        if (net instanceof PetriGameWithTransits) {
            return serializePetriGame((PetriGameWithTransits) net, includePositions, true);
        } else {
            return serializePNWT(net, includePositions, true);
        }
    }

    /**
     * Serialize a plain Petri Net, as in a model checking net.
     * @param net - The Petri Net
     * @param shouldSendPositions - The set of Nodes whose X/Y coordinates should be set in the client.
     * @return A JSON-encoded instance of this class
     */
    public static JsonElement serializePetriNet(PetriNet net,
                                                Set<Node> shouldSendPositions) {
        PetriNetClient netD3 = ofNetWithXYCoordinates(net, shouldSendPositions,
                PetriNetNode::fromPetriNetPlace,
                PetriNetNode::fromTransition,
                PetriNetLink::fromPetriNetFlow,
                (ignored) -> new HashMap<>());
        return new Gson().toJsonTree(netD3);
    }

    /**
     * Serialize a PetriNetWithTransits as in the model checking case.
     * @param net - The PetriNetWithTransits
     * @param shouldSendPositions - The set of Nodes whose X/Y coordinates should be set in the client.
     * @param shouldIncludeObjective - For the PNWT you edit in the editor, we want to include the
     *                               objective/winning condition, but for other Petri Nets (e.g.
     *                               those which represent winning strategies), the annotation may
     *                               not be present, and we shouldn't try to send it to the client.
     * @return A JSON-encoded instance of this class
     */
    public static JsonElement serializePNWT(PetriNetWithTransits net,
                                            Set<Node> shouldSendPositions,
                                            boolean shouldIncludeObjective) {
        PetriNetClient netD3 = ofNetWithXYCoordinates(net, shouldSendPositions,
                PetriNetNode::fromPNWTPlace,
                PetriNetNode::fromTransition,
                PetriNetLink::fromPNWTFlow,
                PNWTTools::getTransitRelationFromTransitions);

        // Add winning condition and, if applicable, LTL formula
        if (shouldIncludeObjective) {
            Condition.Objective objective;
            try {
                objective = Adam.getCondition(net);
            } catch (CouldNotFindSuitableConditionException e) {
                throw new SerializationException(e);
            }
            boolean canConvertToLtl = objective.equals(Condition.Objective.A_BUCHI) ||
                    objective.equals(Condition.Objective.A_REACHABILITY) ||
                    objective.equals(Condition.Objective.A_SAFETY);
            String ltlFormulaString = canConvertToLtl ?
                    AdamModelChecker.toFlowLTLFormula(net, objective) :
                    "";
            netD3.setLtlFormula(ltlFormulaString);
            String objectiveString = objective.toString();
            netD3.setWinningCondition(objectiveString);
        }
        return new Gson().toJsonTree(netD3);
    }

    /**
     * Serialize a Petri Game, as in the Synthesis case.
     * @param net - The PetriGame
     * @param shouldSendPositions - The set of Nodes whose X/Y coordinates should be set in the client.
     * @param shouldIncludeObjective - For Petri Games, we want to include the objective/winning
     *                               condition, but for other Petri Nets (e.g. those which represent winning strategies), the
     *                               annotation may not be present, and we shouldn't try to send it to the client.
     * @return A JSON-encoded instance of this class
     */
    public static JsonElement serializePetriGame(PetriGameWithTransits net,
                                                 Set<Node> shouldSendPositions,
                                                 boolean shouldIncludeObjective) {

        PetriNetClient netD3 = ofNetWithXYCoordinates(net, shouldSendPositions,
                PetriNetNode::fromPetriGamePlace,
                PetriNetNode::fromTransition,
                PetriNetLink::fromPetriGameFlow,
                PNWTTools::getTransitRelationFromTransitions);
        // Add winning condition
        if (shouldIncludeObjective) {
            Condition.Objective objective;
            try {
                objective = Adam.getCondition(net);
            } catch (CouldNotFindSuitableConditionException e) {
                throw new SerializationException(e);
            }
            String objectiveString = objective.toString();
            netD3.setWinningCondition(objectiveString);
        }
        return new Gson().toJsonTree(netD3);
    }


    /**
     * Extract all the information needed to display a PetriNet in our graph editor.
     *
     * @param net                    - A PetriNet, PetriNetWithTransits, or PetriGame
     * @param shouldSendPositions    - We will send x/y coordinates for these nodes to the client.
     * @return A JSON object containing the relevant information from the PetriNet
     * <p>
     * See https://github.com/d3/d3-force
     */
    public static <T extends PetriNet> PetriNetClient ofNetWithXYCoordinates(
            T net,
            Set<Node> shouldSendPositions,
            PlaceSerializer<T> placeSerializer,
            TransitionSerializer<T> transitionSerializer,
            FlowSerializer<T> flowSerializer,
            Function<T, Map<Flow, String>> getFlowRelationFromTransitions) throws SerializationException {
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

        Map<String, Long> initialMarkingMap = PetriNetTools.markingToMap(net.getInitialMarking());
        Map<String, Boolean> fireableTransitions = PetriNetTools.getFireableTransitions(net);

        Map<Flow, String> flowRelationFromTransitions = getFlowRelationFromTransitions.apply(net);
        for (Flow flow : net.getEdges()) {
            String arcLabel = flowRelationFromTransitions.getOrDefault(flow, "");
            PetriNetLink petriNetLink = flowSerializer.serializeFlow(flow, net, arcLabel);
            links.add(petriNetLink);
        }
        PetriNetClient petriNetClient = new PetriNetClient(links, nodes, nodePositions, initialMarkingMap, fireableTransitions);
        return petriNetClient;
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

        static PetriNetLink fromPetriGameFlow(Flow flow, PetriGameWithTransits net, String arcLabel) {
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
        private final String fairness;
        // These properties only belong to PNWT
        private boolean isSpecial;
        private boolean isInitialTransit;
        private int partition;

        private PetriNetNode(String id, String label, GraphNodeType type,
                             boolean isSpecial, boolean isInitialTransit,
                             int partition, String fairness) {
            super(id, label, type);
            this.isSpecial = isSpecial;
            this.isInitialTransit = isInitialTransit;
            this.partition = partition;
            this.fairness = fairness;
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
            // Transitions are never bad or special and have no tokens
            return new PetriNetNode(id, label, GraphNodeType.TRANSITION, false, false,
                    -1, fairness);
        }

        static PetriNetNode fromPetriGamePlace(PetriGameWithTransits game, Place place) {
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

            boolean isSpecial = false;
            boolean isInitialTransit = false;
            int partition = -1;

            GraphNodeType nodeType = GraphNodeType.SYSPLACE;

            String fairness = "none"; // Places have no concept of fairness

            return new PetriNetNode(id, label, nodeType, isSpecial,
                    isInitialTransit, partition, fairness);
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
            this.type = nodeType;
        }
    }
}
