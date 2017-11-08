package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.ds.exceptions.*;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;

/**
 * Represents a Petri Game, plus all of the artifacts related to it that we might want to produce.
 * E.g. Strategy BDD, Graph Strategy BDD, Graph Game BDD.
 * If the PetriGame gets modified, then these calculated artifacts should be deleted.
 * This lets us guarantee that the strategy BDD, etc. will always correspond to the current state
 * of the Petri Game that is encapsulated in an instance of this class.
 */
public class PetriGameAndMore {
    private final PetriGame petriGame;
    private PetriNet strategyBDD = null;
    private BDDGraph graphStrategyBDD = null;
    private BDDGraphExplorer graphGameBDD = null;

    public PetriGameAndMore(PetriGame petriGame) {
        this.petriGame = petriGame;
    }

    public void calculateStrategyBDD() throws ParseException, ParameterMissingException, CouldNotFindSuitableWinningConditionException, NoSuitableDistributionFoundException, NotSupportedGameException, NoStrategyExistentException {
        this.strategyBDD = Adam.getStrategyBDD(this.petriGame.getNet());
    }

    public void calculateGraphStrategyBDD() throws ParseException, ParameterMissingException, CouldNotFindSuitableWinningConditionException, NoSuitableDistributionFoundException, NotSupportedGameException, NoStrategyExistentException {
        this.graphStrategyBDD = Adam.getGraphStrategyBDD(this.petriGame.getNet());
    }

    public void calculateGraphGameBDD() throws ParseException, ParameterMissingException, CouldNotFindSuitableWinningConditionException, NoSuitableDistributionFoundException, NotSupportedGameException, NoStrategyExistentException {
        BDDGraph graphGameBDD = Adam.getGraphGameBDD(this.petriGame.getNet());
        this.graphGameBDD = BDDGraphExplorer.of(graphGameBDD);
    }

    public JsonElement getPetriGameClient() {
        PetriNetD3 petriGameClient = PetriNetD3.of(petriGame.getNet());
        return jsonify(petriGameClient);
    }

    public JsonElement getStrategyBDDClient() {
        PetriNetD3 petriNetD3 = PetriNetD3.of(this.strategyBDD);
        return jsonify(petriNetD3);
    }

    public JsonElement getGraphStrategyBDDClient() {
        BDDGraphD3 bddGraphD3 = BDDGraphD3.of(this.graphStrategyBDD);
        return jsonify(bddGraphD3);
    }

    public JsonElement getGraphGameBDDClient() {
        BDDGraphD3 visibleGraph = this.graphGameBDD.getVisibleGraph();
        return jsonify(visibleGraph);
    }

    private static JsonElement jsonify(Object o) {
        return new Gson().toJsonTree(o);
    }
}
