package uniolunisaar.adamwebfrontend;

import uniol.apt.adt.pn.*;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.util.AdamExtensions;
import uniolunisaar.adam.util.PNTools;
import uniolunisaar.adamwebfrontend.wirerepresentations.NodePosition;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import uniolunisaar.adam.util.ExtensionManagement;

/**
 * This class just has some useful static methods for dealing with Petri Nets which are used
 * in different places throughout the code base
 */
public class PetriNetTools {
    /**
     * This is a workaround for a "feature" in ADAM.  Right now, the X/Y coordinates stored in a
     * Petri Game can be accidentally copied over into its Strategy BDD.  This ends up with the
     * undesirable result that multiple Strategy BDD places end up placed directly on top of each
     * other.
     * <p>
     * E.g. In the original Petri Game, you have a Place named "Env".
     * In the Strategy BDD, you may have multiple Places named "Env_0", "Env_3", ...
     * These would all be placed in the exact same place, perfectly overlapping.  You might not even
     * know they were there.
     * <p>
     * To stop that problem from happening, this method should be used to remove the
     * X/Y coordinate annotations from a Strategy BDD before sending it to the client.
     *
     * @param strategyBDD
     */
    public static void removeXAndYCoordinates(PetriNetWithTransits strategyBDD) {
        Set<Node> nodes = strategyBDD.getNodes();
        for (Node node : nodes) {
            if (strategyBDD.hasXCoord(node)) {
                ExtensionManagement.getInstance().removeExtension(node, AdamExtensions.xCoord);
            }
            if (strategyBDD.hasYCoord(node)) {
                ExtensionManagement.getInstance().removeExtension(node, AdamExtensions.yCoord);
            }
        }
    }


    /**
     * Update the x/y coordinate annotations of the given Petri net.
     * There has to be an annotation for every node.
     */
    public static void saveXYCoordinates(PetriNet petriGame,
                                         Map<String, NodePosition> nodePositions) {
        for (Node node : petriGame.getNodes()) {
            String nodeId = node.getId();
            if (nodePositions.containsKey(nodeId)) {
                NodePosition position = nodePositions.get(nodeId);
                PetriNetExtensionHandler.setXCoord(node, position.x);
                PetriNetExtensionHandler.setYCoord(node, position.y);
            } else {
                throw new IllegalArgumentException(
                        "The x/y coordinates are missing for the node " + node);
            }
        }
    }

    /**
     * Transform the Marking of a PetriNet into a Map<String, Long> that associates each Place's ID
     * with the number of tokens at the Place.
     */
    public static Map<String, Long> markingToMap(Marking marking) {
        Map<String, Long> map = new HashMap<>();
        for (Place place : marking.getNet().getPlaces()) {
            String placeId = place.getId();
            Long tokenCount = marking.getToken(placeId).getValue();
            map.put(placeId, tokenCount);
        }
        return map;
    }

    /**
     * @return A Map<String, Boolean> which indicates for each Transition in the net whether it
     * is fireable or not in the given marking.  The Strings are the Transitions' IDs.
     */
    public static Map<String, Boolean> getFireableTransitions(PetriNet petriNet, Marking marking) {
        Map<String, Boolean> fireableTransitions = new HashMap<>();
        for (Transition t : petriNet.getTransitions()) {
            boolean isFireable = PNTools.isFireable(t, marking);
            fireableTransitions.put(t.getId(), isFireable);
        }
        return fireableTransitions;
    }
}
