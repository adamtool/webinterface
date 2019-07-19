package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.AdamModelChecker;
import uniolunisaar.adam.AdamSynthesizer;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.pnwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;

import static uniolunisaar.adamwebfrontend.App.exceptionToString;
import static uniolunisaar.adamwebfrontend.App.promoteToPetriGame;

/**
 * Different kinds of jobs that can be requested by the user to run on the server
 */
public enum JobType {
    EXISTS_WINNING_STRATEGY {
        JsonElement serialize(Object result) {
            return new JsonPrimitive((boolean) result);
        }

        Job<Boolean> makeJob(PetriNetWithTransits net,
                             JsonObject params) {
            PetriGame petriGame = promoteToPetriGame(net);
            return new Job<>(() -> {
                boolean existsWinningStrategy = AdamSynthesizer.existsWinningStrategyBDD(petriGame);
                return existsWinningStrategy;
            }, petriGame.getName());
        }
    }, WINNING_STRATEGY {
        JsonElement serialize(Object result) {
            return PetriNetD3.ofNetWithoutObjective((PetriGame) result);
        }

        Job<PetriGame> makeJob(PetriNetWithTransits net,
                               JsonObject params) {
            PetriGame petriGame = promoteToPetriGame(net);
            return new Job<>(() -> {
                PetriGame strategyBDD = AdamSynthesizer.getStrategyBDD(petriGame);
                PetriGameTools.removeXAndYCoordinates(strategyBDD);
                return strategyBDD;
            }, petriGame.getName());
        }

    }, GRAPH_STRATEGY_BDD {
        JsonElement serialize(Object result) {
            return BDDGraphD3.ofWholeBddGraph((BDDGraph) result);
        }

        Job<BDDGraph> makeJob(PetriNetWithTransits net,
                              JsonObject params) {
            PetriGame petriGame = promoteToPetriGame(net);
            return new Job<>(() -> {
                BDDGraph graphStrategyBDD = AdamSynthesizer.getGraphStrategyBDD(petriGame);
                return graphStrategyBDD;
            }, petriGame.getName());
        }
    }, MODEL_CHECKING_RESULT {
        JsonElement serialize(Object result) {
            // TODO serialize the counterexample as well if it is present
            ModelCheckingResult mcResult = (ModelCheckingResult) result;
            return new JsonPrimitive(mcResult.getSatisfied().toString());
        }

        Job<ModelCheckingResult> makeJob(
                PetriNetWithTransits net,
                JsonObject params) {
            String formula = params.get("formula").getAsString();
            return new Job<>(() -> {
                RunFormula runFormula = AdamModelChecker.parseFlowLTLFormula(net, formula);
                ModelCheckerFlowLTL modelCheckerFlowLTL = new ModelCheckerFlowLTL();
                ModelCheckingResult result = AdamModelChecker.checkFlowLTLFormula(net, modelCheckerFlowLTL, runFormula, "/tmp/", null);
                return result;
            }, net.getName());
        }
    }, MODEL_CHECKING_NET {
        JsonElement serialize(Object result) {
            return PetriNetD3.ofPetriNetWithTransits(new PetriNetWithTransits((PetriNet) result));
        }

        Job<PetriNet> makeJob(PetriNetWithTransits net, JsonObject params) {
            String formula = params.get("formula").getAsString();
            return new Job<>(() -> {
                RunFormula runFormula = AdamModelChecker.parseFlowLTLFormula(net, formula);
                PetriNet modelCheckingNet = AdamModelChecker.getModelCheckingNet(net, runFormula, false);
                return modelCheckingNet;
            }, net.getName());
        }
    }, GRAPH_GAME_BDD {
        JsonElement serialize(Object result) {
            return ((BDDGraphExplorer) result).getVisibleGraph();
        }

        Job<BDDGraphExplorer> makeJob(PetriNetWithTransits petriGame1,
                                      JsonObject params) {
            if (!(petriGame1 instanceof PetriGame)) {
                throw new IllegalArgumentException("The given net is not a PetriGame, but merely a PetriNetWithTransits, so you can't insert an environment place.");
            }
            PetriGame petriGame = (PetriGame)petriGame1;

            // If there is an invalid condition annotation, we should throw an error right away instead
            // of waiting until the job gets started (which might take a while if there is a
            // queue).
            try {
                Adam.getCondition(petriGame);
            } catch (CouldNotFindSuitableConditionException e) {
                throw new IllegalArgumentException(exceptionToString(e));
            }

            boolean shouldSolveStepwise = params.get("incremental").getAsBoolean();
            return new Job<>(() -> {
                if (shouldSolveStepwise) {
                    BDDGraphExplorerStepwise bddGraphExplorerStepwise =
                            new BDDGraphExplorerStepwise(petriGame);
                    return bddGraphExplorerStepwise;
                } else {
                    BDDGraph graphGameBDD = AdamSynthesizer.getGraphGameBDD(petriGame);
                    return BDDGraphExplorerCompleteGraph.of(graphGameBDD);
                }
            }, petriGame.getName());
        }
    };

    abstract JsonElement serialize(Object result) throws SerializationException;

    abstract Job<?> makeJob(PetriNetWithTransits net, JsonObject params);

}