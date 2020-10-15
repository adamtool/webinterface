package uniolunisaar.adamwebfrontend.jobsystem;

import com.google.gson.*;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.io.renderer.RenderException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.AdamModelChecker;
import uniolunisaar.adam.AdamSynthesizer;
import uniolunisaar.adam.ds.graph.synthesis.twoplayergame.symbolic.bddapproach.BDDGraph;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.synthesis.pgwt.PetriGameWithTransits;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.PGTools;
import uniolunisaar.adamwebfrontend.*;
import uniolunisaar.adamwebfrontend.wirerepresentations.BDDGraphClient;
import uniolunisaar.adamwebfrontend.wirerepresentations.PetriNetClient;

import java.util.HashSet;
import java.util.UUID;

import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.cex.CounterExample;
import uniolunisaar.adam.ds.modelchecking.results.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.exceptions.synthesis.pgwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.logic.modelchecking.ltl.circuits.ModelCheckerFlowLTL;

import static uniolunisaar.adamwebfrontend.App.exceptionToString;
import static uniolunisaar.adamwebfrontend.App.promoteToPetriGame;

/**
 * Different kinds of jobs that can be requested by the user to run on the
 * server
 */
public enum JobType {
    EXISTS_WINNING_STRATEGY {
        JsonElement serialize(Object result) {
            return new JsonPrimitive((boolean) result);
        }

        public Job<Boolean> makeJob(PetriNetWithTransits net,
                                    JsonObject params) {
            // TODO #293 refactor
            PetriGameWithTransits petriGame = promoteToPetriGame(net);
            return new Job<>(() -> {
                // TODO #172 add options for the solving algorithms
                boolean existsWinningStrategy = AdamSynthesizer.existsWinningStrategyBDD(petriGame);
                return existsWinningStrategy;
            }, PetriNetExtensionHandler.getProcessFamilyID(petriGame));
        }
    }, WINNING_STRATEGY {
        JsonElement serialize(Object result) {
            JsonObject json = new JsonObject();
            PetriGameWithTransits game = (PetriGameWithTransits) result;
            // TODO #292 include the positions of nodes here
            JsonElement netJson = PetriNetClient.serializePetriGame(
                    game,
                    game.getNodes(),
                    false);
            json.add("graph", netJson);

            // TODO #296 Remove. This is no longer needed.  (The simulation used to rely on
            //  having a copy of the APT)
            String apt;
            try {
                apt = PGTools.getAPT(game, true, true);
            } catch (RenderException e) {
                throw new SerializationException(e);
            }
            json.addProperty("apt", apt);
            return json;
        }

        public Job<PetriGameWithTransits> makeJob(PetriNetWithTransits net,
                                                  JsonObject params) {
            // TODO #293 refactor
            PetriGameWithTransits petriGame = promoteToPetriGame(net);
            return new Job<>(() -> {
                // TODO #172 add options for the solving algorithms
                PetriGameWithTransits strategyBDD = AdamSynthesizer.getStrategyBDD(petriGame);
                return strategyBDD;
            }, PetriNetExtensionHandler.getProcessFamilyID(petriGame));
        }

    }, GRAPH_STRATEGY_BDD {
        JsonElement serialize(Object result) {
            return BDDGraphClient.ofWholeBddGraph((BDDGraph) result);
        }

        public Job<BDDGraph> makeJob(PetriNetWithTransits net,
                                     JsonObject params) {
            // TODO #293 refactor
            PetriGameWithTransits petriGame = promoteToPetriGame(net);
            return new Job<>(() -> {
                // TODO #172 add options for the solving algorithms
                BDDGraph graphStrategyBDD = AdamSynthesizer.getGraphStrategyBDD(petriGame);
                return graphStrategyBDD;
            }, PetriNetExtensionHandler.getProcessFamilyID(net));
        }
    }, MODEL_CHECKING_RESULT {
        JsonElement serialize(Object result) {
            JsonObject resultJson = new JsonObject();
            Pair<ModelCheckingResult, AdamCircuitFlowLTLMCStatistics> mcResult
                    = (Pair<ModelCheckingResult, AdamCircuitFlowLTLMCStatistics>) result;
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

        public Job<Pair<ModelCheckingResult, AdamCircuitFlowLTLMCStatistics>> makeJob(
                PetriNetWithTransits net,
                JsonObject params) {
            String formula = params.get("formula").getAsString();
            return new Job<>(() -> {
                RunLTLFormula runFormula = AdamModelChecker.parseFlowLTLFormula(net, formula);

                // TODO #172 add options for the solving algorithms
                String tempFilePrefix = getTempFilePrefix();
                AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(
                        tempFilePrefix, false, false, false);
                AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(data);
                AdamCircuitFlowLTLMCStatistics statistics = new AdamCircuitFlowLTLMCStatistics();
                settings.setStatistics(statistics);
                settings.setOutputData(data);

                ModelCheckerFlowLTL modelCheckerFlowLTL = new ModelCheckerFlowLTL(settings);
                ModelCheckingResult result = AdamModelChecker.checkFlowLTLFormula(net, modelCheckerFlowLTL, runFormula);
                return new Pair<>(result, statistics);
            }, PetriNetExtensionHandler.getProcessFamilyID(net));
        }
    }, MODEL_CHECKING_NET {
        JsonElement serialize(Object result) {
            JsonObject json = new JsonObject();
            PetriNet net = (PetriNet) result;
            JsonElement netJson = PetriNetClient.serializePetriNet(net, net.getNodes());
            json.add("graph", netJson);

            // TODO #296 Remove. This is no longer needed.  (The simulation used to rely on
            //  having a copy of the APT)
            String apt;
            try {
                apt = Tools.getPN(net);
            } catch (RenderException e) {
                throw new SerializationException(e);
            }
            json.addProperty("apt", apt);
            return json;
        }

        public Job<PetriNet> makeJob(PetriNetWithTransits net, JsonObject params) {
            String formula = params.get("formula").getAsString();
            return new Job<>(() -> {
                RunLTLFormula runFormula = AdamModelChecker.parseFlowLTLFormula(net, formula);

                // TODO #172 add options for the solving algorithms        
                String tempFilePrefix = getTempFilePrefix();
                AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(
                        tempFilePrefix, false, false, false);
                AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(data);
                // These statistics could be shown in the UI, but for now, they are not visible.
                AdamCircuitFlowLTLMCStatistics statistics = new AdamCircuitFlowLTLMCStatistics();
                settings.setStatistics(statistics);
                settings.setOutputData(data);

                PetriNet modelCheckingNet = AdamModelChecker.getModelCheckingNet(net, runFormula, settings);
                return modelCheckingNet;
            }, PetriNetExtensionHandler.getProcessFamilyID(net));
        }
    }, MODEL_CHECKING_FORMULA {
        @Override
        JsonElement serialize(Object result) throws SerializationException {
            ILTLFormula result1 = (ILTLFormula) result;
            return new JsonPrimitive(result1.toString());
        }

        @Override
        public Job<ILTLFormula> makeJob(PetriNetWithTransits net, JsonObject params) {
            String formula = params.get("formula").getAsString();
            return new Job<>(() -> {
                RunLTLFormula runFormula = AdamModelChecker.parseFlowLTLFormula(net, formula);
                // TODO #172 add options for the solving algorithms
                String tempFilePrefix = getTempFilePrefix();
                AdamCircuitFlowLTLMCOutputData data = new AdamCircuitFlowLTLMCOutputData(
                        tempFilePrefix, false, false, false);
                AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(data);
                // These statistics could be shown in the UI, but for now, they are not visible.
                AdamCircuitFlowLTLMCStatistics statistics = new AdamCircuitFlowLTLMCStatistics();
                settings.setStatistics(statistics);
                settings.setOutputData(data);

                PetriNet modelCheckingNet = AdamModelChecker.getModelCheckingNet(net, runFormula, settings);
                ILTLFormula modelCheckingFormula = AdamModelChecker.getModelCheckingFormula(
                        net, modelCheckingNet, runFormula, settings);
                return modelCheckingFormula;
            }, net.getName());
        }
    }, GRAPH_GAME_BDD {
        JsonElement serialize(Object result) {
            return ((BDDGraphExplorer) result).getVisibleGraph();
        }

        public Job<BDDGraphExplorer> makeJob(PetriNetWithTransits petriGame1,
                                             JsonObject params) {
            // TODO #293 consider refactoring
            if (!(petriGame1 instanceof PetriGameWithTransits)) {
                throw new IllegalArgumentException("The given net is not a PetriGame, but merely a PetriNetWithTransits.");
            }
            PetriGameWithTransits petriGame = (PetriGameWithTransits) petriGame1;

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
                // TODO #172 add options for the solving algorithms
                if (shouldSolveStepwise) {
                    BDDGraphExplorerStepwise bddGraphExplorerStepwise
                            = new BDDGraphExplorerStepwise(petriGame, false); //TODO add the correct flag here depending on the approach
                    return bddGraphExplorerStepwise;
                } else {
                    BDDGraph graphGameBDD = AdamSynthesizer.getGraphGameBDD(petriGame);
                    return BDDGraphExplorerCompleteGraph.of(graphGameBDD);
                }
            }, PetriNetExtensionHandler.getProcessFamilyID(petriGame));
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

    public abstract Job<?> makeJob(PetriNetWithTransits net, JsonObject params);

    // Serialize a statistics object, including only its primitive and String fields
    public static JsonElement serializeStatistics(AdamCircuitFlowLTLMCStatistics stats) {
        Gson gson = new GsonBuilder()
                .addSerializationExclusionStrategy(new OnlyStringsAndPrimitives())
                .create();

        return gson.toJsonTree(stats);
    }
}

/**
 * This 'serialization strategy' tells GSON to serialize an object while
 * excluding all fields that aren't either strings or primitives.
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
