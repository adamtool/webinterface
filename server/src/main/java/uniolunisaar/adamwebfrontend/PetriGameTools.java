package uniolunisaar.adamwebfrontend;

import uniol.apt.adt.pn.Node;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.util.AdamExtensions;

import java.util.Map;
import java.util.Set;

/**
 * This class just has some useful static methods for dealing with Petri Games
 * and the x/y coordinates that we save inside of them.
 */
public class PetriGameTools {
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
    public static void removeXAndYCoordinates(PetriGame strategyBDD) {
        Set<Node> nodes = strategyBDD.getNodes();
        for (Node node : nodes) {
            if (strategyBDD.hasXCoord(node)) {
                node.removeExtension(AdamExtensions.xCoord.name());
            }
            if (strategyBDD.hasYCoord(node)) {
                node.removeExtension(AdamExtensions.yCoord.name());
            }
        }
    }


    /**
     * Update the x/y coordinate annotations of the given petri game.
     * There has to be an annotation for every node.
     * @param petriGame
     * @param nodePositions
     */
    public static void saveXYCoordinates(PetriGame petriGame,
                                         Map<String, NodePosition> nodePositions) {
        for (Node node : petriGame.getNodes()) {
            String nodeId = node.getId();
            if (nodePositions.containsKey(nodeId)) {
                NodePosition position = nodePositions.get(nodeId);
                petriGame.setXCoord(node, position.x);
                petriGame.setYCoord(node, position.y);
            } else {
                throw new IllegalArgumentException(
                        "The x/y coordinates are missing for the node " + node);
            }
        }
    }
}
