package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.ds.exceptions.*;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;

import java.util.Optional;

/**
 * Represents a Petri Game, plus all of the artifacts related to it that we might want to produce.
 * E.g. Strategy BDD, Graph Strategy BDD, Graph Game BDD.
 * If the PetriGame gets modified, then these calculated artifacts should be deleted.
 * This lets us guarantee that the strategy BDD, etc. will always correspond to the current state
 * of the Petri Game that is encapsulated in an instance of this class.
 */
public class PetriGameAndMore {
    private final PetriGame petriGame;
    private Optional<Boolean> existsWinningStrategy = Optional.empty();
    private Optional<PetriNet> strategyBDD = Optional.empty();
    private Optional<BDDGraph> graphStrategyBDD = Optional.empty();
    private Optional<BDDGraphExplorer> graphGameBDDExplorer = Optional.empty();

    private PetriGameAndMore(PetriGame petriGame) {
        this.petriGame = petriGame;
    }

    public static PetriGameAndMore of(PetriGame petriGame) {
        return new PetriGameAndMore(petriGame);
    }

    public boolean calculateExistsWinningStrategy() throws NotSupportedGameException, ParameterMissingException, CouldNotFindSuitableWinningConditionException, ParseException, NoSuitableDistributionFoundException {
        if (existsWinningStrategy.isPresent()) {
            return existsWinningStrategy.get();
        } else {
            boolean existsWinningStrategy = Adam.existsWinningStrategyBDD(this.petriGame.getNet());
            this.existsWinningStrategy = Optional.of(existsWinningStrategy);
            return existsWinningStrategy;
        }
    }

    public JsonElement calculateStrategyBDD() throws ParseException, ParameterMissingException, CouldNotFindSuitableWinningConditionException, NoSuitableDistributionFoundException, NotSupportedGameException, NoStrategyExistentException {
        PetriNet strategyBDD;
        if (this.strategyBDD.isPresent()) {
            strategyBDD = this.strategyBDD.get();
        } else {
            strategyBDD = Adam.getStrategyBDD(this.petriGame.getNet());
            this.strategyBDD = Optional.of(strategyBDD);
        }
        return PetriNetD3.of(strategyBDD);
    }

    public void calculateGraphStrategyBDD() throws ParseException, ParameterMissingException, CouldNotFindSuitableWinningConditionException, NoSuitableDistributionFoundException, NotSupportedGameException, NoStrategyExistentException {
        this.graphStrategyBDD = Optional.ofNullable(Adam.getGraphStrategyBDD(this.petriGame.getNet()));
    }

    public void calculateGraphGameBDD() throws ParseException, ParameterMissingException, CouldNotFindSuitableWinningConditionException, NoSuitableDistributionFoundException, NotSupportedGameException, NoStrategyExistentException {
        BDDGraph graphGameBDD = Adam.getGraphGameBDD(this.petriGame.getNet());
        this.graphGameBDDExplorer = Optional.of(BDDGraphExplorer.of(graphGameBDD));
    }

    public Optional<Boolean> getExistsWinningStrategy() {
        return this.existsWinningStrategy;
    }

    public JsonElement getPetriGameClient() {
        return PetriNetD3.of(petriGame.getNet());
    }

//    public Optional<JsonElement> getStrategyBDDClient() {
//        return this.strategyBDD.map(bdd -> PetriNetD3.of(bdd));
//    }
//
//    public Optional<JsonElement> getGraphStrategyBDDClient() {
//        return this.graphStrategyBDD.map(bdd -> {
//            BDDGraphD3 bddGraphD3 = BDDGraphD3.of(bdd);
//            return jsonify(bddGraphD3);
//        });
//    }
//
//    public Optional<JsonElement> getGraphGameBDDClient() {
//        return this.graphGameBDDExplorer.map(bddGraphExplorer -> {
//            BDDGraphD3 visibleGraph = bddGraphExplorer.getVisibleGraph();
//            return jsonify(visibleGraph);
//        });
//    }

    private static JsonElement jsonify(Object o) {
        return new Gson().toJsonTree(o);
    }
}
