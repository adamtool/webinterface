package uniolunisaar.adamwebfrontend;

import com.google.gson.*;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.util.Pair;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.AdamModelChecker;
import uniolunisaar.adam.AdamSynthesizer;
import uniolunisaar.adam.ds.graph.symbolic.bddapproach.BDDGraph;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.modelchecking.CounterExample;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.pnwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;

import java.util.Random;
import java.util.UUID;

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
            JsonObject resultJson = new JsonObject();
            Pair<ModelCheckingResult, AdamCircuitFlowLTLMCStatistics> mcResult =
                    (Pair<ModelCheckingResult, AdamCircuitFlowLTLMCStatistics>) result;
            ModelCheckingResult.Satisfied satisfied = mcResult.getFirst().getSatisfied();
            resultJson.addProperty("satisfied", satisfied.toString());

            if (satisfied == ModelCheckingResult.Satisfied.FALSE) {
                CounterExample counterExample = mcResult.getFirst().getCex();
                resultJson.addProperty("counterExample", counterExample.toString());
            }

            // Add statistics.
            JsonElement statisticsJson = serializeStatistics(mcResult.getSecond());
            resultJson.add("statistics", statisticsJson);

            return resultJson;
        }

        Job<Pair<ModelCheckingResult, AdamCircuitFlowLTLMCStatistics>> makeJob(
                PetriNetWithTransits net,
                JsonObject params) {
            String formula = params.get("formula").getAsString();
            return new Job<>(() -> {
                RunFormula runFormula = AdamModelChecker.parseFlowLTLFormula(net, formula);

                AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings();
                AdamCircuitFlowLTLMCStatistics statistics = new AdamCircuitFlowLTLMCStatistics();
                String tempFilePrefix = getTempFilePrefix();
                AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(
                        tempFilePrefix, false, false,false);
                settings.setStatistics(statistics);
                settings.setOutputData(data);

                ModelCheckerFlowLTL modelCheckerFlowLTL = new ModelCheckerFlowLTL(settings);
                ModelCheckingResult result = AdamModelChecker.checkFlowLTLFormula(net, modelCheckerFlowLTL, runFormula);
                return new Pair<>(result, statistics);
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

                AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings();
                // TODO display statistics in UI
                AdamCircuitFlowLTLMCStatistics statistics = new AdamCircuitFlowLTLMCStatistics();
                String tempFilePrefix = getTempFilePrefix();
                AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(
                        tempFilePrefix, false, false, false);
                settings.setStatistics(statistics);
                settings.setOutputData(data);

                PetriNet modelCheckingNet = AdamModelChecker.getModelCheckingNet(net, runFormula, settings);
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
            PetriGame petriGame = (PetriGame) petriGame1;

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

    private static String getTempFilePrefix() {
        String tempFileDirectory = System.getProperty(
                "ADAMWEB_TEMP_DIRECTORY", "./tmp/");
        String tempFileName = "tmp" + UUID.randomUUID().toString();
        String prefix = tempFileDirectory + tempFileName;
        return prefix;
    }

    abstract JsonElement serialize(Object result) throws SerializationException;

    abstract Job<?> makeJob(PetriNetWithTransits net, JsonObject params);

    // Serialize a statistics object, including only its primitive and String fields
    public static JsonElement serializeStatistics(AdamCircuitFlowLTLMCStatistics stats) {
        Gson gson = new GsonBuilder()
                .addSerializationExclusionStrategy(new OnlyStringsAndPrimitives())
                .create();

        return gson.toJsonTree(stats);
    }
}

/**
 * This 'serialization strategy' tells GSON to serialize an object while excluding all fields that
 * aren't either strings or primitives.
 */
class OnlyStringsAndPrimitives implements ExclusionStrategy {
    @Override
    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
        return !shouldIncludeField(fieldAttributes.getDeclaredClass());
    }

    public static boolean shouldIncludeField(Class<?> aClass) {
        return aClass.isPrimitive()
                || aClass.equals(String.class);
    }

    @Override
    public boolean shouldSkipClass(Class<?> aClass) {
        return false;
    }
}
