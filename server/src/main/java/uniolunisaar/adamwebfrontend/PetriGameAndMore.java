package uniolunisaar.adamwebfrontend;

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

    public JsonElement calculateGraphStrategyBDD() throws ParseException, ParameterMissingException, CouldNotFindSuitableWinningConditionException, NoSuitableDistributionFoundException, NotSupportedGameException, NoStrategyExistentException {
        // TODO It's still possible to crash the server by calling this method many times in succession.
        // TODO Introduce some kind of thread safety and make sure that the calculation only happens once.
        // TODO It might make sense to use Future to represent the ongoing computation.
        // TODO This also applies to the other calculation methods like calculateStrategyBDD and calculateExistsWinningStrategy.
        if (!this.graphStrategyBDD.isPresent()) {
            BDDGraph graphStrategyBDD = Adam.getGraphStrategyBDD(this.petriGame.getNet());
            this.graphStrategyBDD = Optional.of(graphStrategyBDD);
        }
        return BDDGraphD3.of(this.graphStrategyBDD.get());
    }

    public JsonElement calculateGraphGameBDD() throws ParseException, ParameterMissingException, CouldNotFindSuitableWinningConditionException, NoSuitableDistributionFoundException, NotSupportedGameException, NoStrategyExistentException {
        if (!this.graphGameBDDExplorer.isPresent()) {
            BDDGraph graphGameBDD = Adam.getGraphGameBDD(this.petriGame.getNet());
            BDDGraphExplorer graphExplorer = BDDGraphExplorer.of(graphGameBDD);
            this.graphGameBDDExplorer = Optional.of(graphExplorer);
        }
        return this.graphGameBDDExplorer.get().getVisibleGraph();
    }

    public JsonElement getPetriGameClient() {
        return PetriNetD3.of(this.petriGame.getNet());
    }

    public JsonElement toggleGraphGameBDDNode(int nodeId) throws GraphGameBDDNotYetCalculated {
        if (this.graphGameBDDExplorer.isPresent()) {
            BDDGraphExplorer bddGraphExplorer = this.graphGameBDDExplorer.get();
            bddGraphExplorer.toggleState(nodeId);
            return bddGraphExplorer.getVisibleGraph();
        } else {
            throw new GraphGameBDDNotYetCalculated("The graph game BDD for this Petri Game has not " +
                    "yet been calculated.");
        }
    }

    private class GraphGameBDDNotYetCalculated extends Exception {
        public GraphGameBDDNotYetCalculated(String s) {
            super(s);
        }
    }
}
