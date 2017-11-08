package uniolunisaar.adamwebfrontend;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;

/**
 * Represents a Petri Game, plus all of the artifacts related to it that we might want to produce.
 * E.g. Strategy BDD, Graph Strategy BDD, Graph Game BDD.
 */
public class PetriGameAndMore {
    private final PetriGame petriGame;
    private PetriNet strategyBDD = null;
    private BDDGraph graphStrategyBDD = null;
    private BDDGraph graphGameBDD = null;

    public PetriGameAndMore(PetriGame petriGame) {
        this.petriGame = petriGame;
    }

    public void calculateStrategyBDD
}
