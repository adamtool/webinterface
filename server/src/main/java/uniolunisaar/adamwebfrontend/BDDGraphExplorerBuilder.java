package uniolunisaar.adamwebfrontend;

import uniolunisaar.adam.ds.graph.synthesis.twoplayergame.symbolic.bddapproach.BDDGraph;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.exceptions.synthesis.pgwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.exceptions.synthesis.pgwt.SolvingException;

public class BDDGraphExplorerBuilder {
    private boolean isBuilt = false;

    private boolean isIncremental;

    private BDDGraphExplorer explorer;

    private PetriGameWithTransits petriGame;
    private boolean isWithRecurrentlyInterferingEnv;

    // Incremental approach.  The flag 'withRecurrentlyInterferingEnv' should be set in a
    // second step after this constructor.
    public BDDGraphExplorerBuilder(PetriGameWithTransits petriGame) {
        this.petriGame = petriGame;
    }

    public void initializeStepwise(boolean withRecurrentlyInterferingEnv) throws SolvingException, CouldNotFindSuitableConditionException {
        if (isBuilt) {
            throw new IllegalArgumentException("The BDD Graph explorer has already been initialized.");
        }
        explorer = new BDDGraphExplorerStepwise(petriGame, withRecurrentlyInterferingEnv);
        isIncremental = true;
        isWithRecurrentlyInterferingEnv = withRecurrentlyInterferingEnv;
        isBuilt = true;
    }

    // "Complete Game" approach
    public BDDGraphExplorerBuilder(BDDGraph graphGameBDD) {
        explorer = BDDGraphExplorerCompleteGraph.of(graphGameBDD);
        isIncremental = false;
        isBuilt = true;
    }

    public boolean isBuilt() {
        return isBuilt;
    }

    public BDDGraphExplorer getExplorer() {
        if (!isBuilt) {
            throw new IllegalArgumentException("The BDD Graph explorer has not yet been initialized.");
        }
        return explorer;
    }

    public boolean isIncremental() {
        return isIncremental;
    }

    public boolean getWithRecurrentlyInterferingEnv() {
        return isWithRecurrentlyInterferingEnv;
    }
}
