package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonElement;
import uniol.apt.adt.pn.Node;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.AdamSynthesizer;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.exceptions.pg.CalculationInterruptedException;
import uniolunisaar.adam.exceptions.pg.NoStrategyExistentException;
import uniolunisaar.adam.exceptions.pg.SolvingException;
import uniolunisaar.adam.exceptions.pnwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;
import uniolunisaar.adam.util.AdamExtensions;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a Petri Game, plus all of the artifacts related to it that we might want to produce.
 * E.g. Strategy BDD, Graph Strategy BDD
 * If the PetriGame gets modified, then these calculated artifacts should be deleted.
 * This lets us guarantee that the strategy BDD, etc. will always correspond to the current state
 * of the Petri Game that is encapsulated in an instance of this class.
 * TODO This ended up not being a very suitable abstraction.  I'm gradually getting rid of this
 *   class. -Ann
 */
public class PetriGameAndMore {
    private final PetriGame petriGame;
    private Optional<Boolean> existsWinningStrategy = Optional.empty();
    private Optional<PetriGame> strategyBDD = Optional.empty();
    private Optional<BDDGraph> graphStrategyBDD = Optional.empty();

    private PetriGameAndMore(PetriGame petriGame) {
        this.petriGame = petriGame;
    }

    public static PetriGameAndMore of(PetriGame petriGame) {
        return new PetriGameAndMore(petriGame);
    }

    /**
     * This is a workaround for a "feature" in ADAM.  Right now, the X/Y coordinates stored in a
     * Petri Game can be accidentally copied over into its Strategy BDD.  This ends up with the
     * undesirable result that multiple Strategy BDD places end up placed directly on top of each
     * other.
     *
     * E.g. In the original Petri Game, you have a Place named "Env".
     * In the Strategy BDD, you may have multiple Places named "Env_0", "Env_3", ...
     * These would all be placed in the exact same place, perfectly overlapping.  You might not even
     * know they were there.
     *
     * To stop that problem from happening, this method should be used to remove the
     * X/Y coordinate annotations from a Strategy BDD before sending it to the client.
     * @param strategyBDD
     */
    private static void removeXAndYCoordinates(PetriGame strategyBDD) {
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
     * Print out the APT of our Petri Game with the nodes' x/y coordinates annotated.
     *
     * @param nodePositions a Map assigning a pair of x,y coordinates to every node ID.
     * @return the APT representation of the annotated Petri Net.
     */
    public String savePetriGameWithXYCoordinates(Map<String, NodePosition> nodePositions) throws RenderException {
        for (Node node : petriGame.getNodes()) {
            String nodeId = node.getId();
            if (nodePositions.containsKey(nodeId)) {
                NodePosition position = nodePositions.get(nodeId);
                petriGame.setXCoord(node, position.x);
                petriGame.setYCoord(node, position.y);
            } else {
                throw new IllegalArgumentException(
                        "APT generation failed: the x/y coordinates are missing for the node " + node);
            }
        }
        return Adam.getAPT(petriGame);
    }

    public boolean calculateExistsWinningStrategy() throws SolvingException, ParseException, CouldNotFindSuitableConditionException, CalculationInterruptedException {
        if (existsWinningStrategy.isPresent()) {
            return existsWinningStrategy.get();
        } else {
            boolean existsWinningStrategy = AdamSynthesizer.existsWinningStrategyBDD(this.petriGame);
            this.existsWinningStrategy = Optional.of(existsWinningStrategy);
            return existsWinningStrategy;
        }
    }

    public JsonElement calculateStrategyBDD() throws ParseException, SolvingException, NoStrategyExistentException, CouldNotFindSuitableConditionException, CalculationInterruptedException {
        PetriGame strategyBDD;
        if (this.strategyBDD.isPresent()) {
            strategyBDD = this.strategyBDD.get();
        } else {
            strategyBDD = AdamSynthesizer.getStrategyBDD(this.petriGame);
            removeXAndYCoordinates(strategyBDD);
            this.strategyBDD = Optional.of(strategyBDD);
        }
        return PetriNetD3.of(strategyBDD);
    }

    public JsonElement calculateGraphStrategyBDD() throws ParseException, SolvingException, NoStrategyExistentException, CouldNotFindSuitableConditionException, CalculationInterruptedException {
        // TODO It's still possible to crash the server by calling this method many times in succession.
        // TODO Introduce some kind of thread safety and make sure that the calculation only happens once.
        // TODO It might make sense to use Future to represent the ongoing computation.
        // TODO This also applies to the other calculation methods like calculateStrategyBDD and calculateExistsWinningStrategy.
        if (!this.graphStrategyBDD.isPresent()) {
            BDDGraph graphStrategyBDD = AdamSynthesizer.getGraphStrategyBDD(this.petriGame);
            this.graphStrategyBDD = Optional.of(graphStrategyBDD);
        }
        return BDDGraphD3.of(this.graphStrategyBDD.get());
    }

    public JsonElement getPetriGameClient() {
        return PetriNetD3.of(this.petriGame);
    }

    public PetriGame getPetriGame() {
        return petriGame;
    }
}
