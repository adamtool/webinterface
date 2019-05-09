package uniolunisaar.adamwebfrontend;
/**
 * Created by Ann on 11.09.2017.
 */

import static spark.Spark.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import spark.Request;
import spark.Response;
import spark.Route;
import uniol.apt.adt.pn.*;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.AdamModelChecker;
import uniolunisaar.adam.AdamSynthesizer;
import uniolunisaar.adam.ds.logics.ltl.flowltl.IRunFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.exceptions.pg.*;
import uniolunisaar.adam.exceptions.pnwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.PNWTTools;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;

public class App {
    private final Gson gson = new Gson();
    private final JsonParser parser = new JsonParser();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    // Map from clientside-generated browserUuid to browser-specific list of running calculations
    private final Map<UUID, UserContext> userContextMap = new ConcurrentHashMap<>();
    // Whenever we load a PetriGame from APT, we put it into this hashmap with a
    // server-generated UUID as a key.
    private final Map<String, PetriGame> petriGamesReadFromApt = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new App().startServer();
    }

    // TODO serialize the counterexample as well if it is present
    private static JsonElement serializeModelCheckingResult(ModelCheckingResult result) {
        return new JsonPrimitive(result.getSatisfied().toString());
    }

    public void startServer() {
        // Tell ADAM to send all of its messages to our websocket clients instead of stdout
        Logger.getInstance().setVerboseMessageStream(LogWebSocket.getPrintStreamVerbose());
        Logger.getInstance().setShortMessageStream(LogWebSocket.getPrintStreamNormal());
        Logger.getInstance().setWarningStream(LogWebSocket.getPrintStreamWarning());
        Logger.getInstance().setErrorStream(LogWebSocket.getPrintStreamError());
//            Logger.OUTPUT output = Logger.OUTPUT.STREAMS;
//            Logger.getInstance().setOutput(output);

        webSocket("/log", LogWebSocket.class);

        staticFiles.location("/static");
        enableCORS();

        get("/hello", (req, res) -> "Hello World");

        post("/parseApt", this::handleParseApt);

        post("/calculateExistsWinningStrategy", handleQueueCalculation(
                this::calculateExistsWinningStrategy,
                CalculationType.EXISTS_WINNING_STRATEGY,
                JsonPrimitive::new
        ));

        post("/calculateStrategyBDD", handleQueueCalculation(
                this::calculateStrategyBDD,
                CalculationType.WINNING_STRATEGY,
                PetriNetD3::of
        ));

        post("/calculateGraphStrategyBDD", handleQueueCalculation(
                this::calculateGraphStrategyBDD,
                CalculationType.GRAPH_STRATEGY_BDD,
                BDDGraphD3::ofWholeBddGraph
        ));

        post("/calculateGraphGameBDD", handleQueueCalculation(
                this::calculateGraphGameBDD,
                CalculationType.GRAPH_GAME_BDD,
                BDDGraphExplorer::getVisibleGraph));

        // TODO rename to "checkLtlFormula" or something?
        post("/calculateModelCheckingResult", handleQueueCalculation(
                this::calculateModelCheckingResult,
                CalculationType.MODEL_CHECKING_RESULT,
                App::serializeModelCheckingResult));

        post("/getWinningStrategy", handleGetCalculationResult(
                CalculationType.WINNING_STRATEGY,
                PetriNetD3::of
        ));

        post("/getGraphStrategyBDD", handleGetCalculationResult(
                CalculationType.GRAPH_STRATEGY_BDD,
                BDDGraphD3::ofWholeBddGraph
        ));

        post("/getGraphGameBDD", handleGetCalculationResult(
                CalculationType.GRAPH_GAME_BDD,
                BDDGraphExplorer::getVisibleGraph));

        postWithUserContext("/getListOfCalculations", this::handleGetListOfCalculations);

        postWithUserContext("/cancelCalculation", this::handleCancelCalculation);

        postWithUserContext("/deleteCalculation", this::handleDeleteCalculation);

        postWithUserContext("/toggleGraphGameBDDNodePostset", this::handleToggleGraphGameBDDNodePostset);

        postWithUserContext("/toggleGraphGameBDDNodePreset", this::handleToggleGraphGameBDDNodePreset);

        postWithPetriGame("/savePetriGameAsAPT", this::handleSavePetriGameAsAPT);

        postWithPetriGame("/insertPlace", this::handleInsertPlace);

        postWithPetriGame("/deleteNode", this::handleDeleteNode);

        postWithPetriGame("/renameNode", this::handleRenameNode);

        postWithPetriGame("/toggleEnvironmentPlace", this::handleToggleEnvironmentPlace);

        postWithPetriGame("/toggleIsInitialTokenFlow", this::handleToggleIsInitialTokenFlow);

        postWithPetriGame("/setInitialToken", this::handleSetInitialToken);

        postWithPetriGame("/setWinningCondition", this::handleSetWinningCondition);
        postWithPetriGame("/createFlow", this::handleCreateFlow);

        postWithPetriGame("/deleteFlow", this::handleDeleteFlow);

        postWithPetriGame("/createTokenFlow", this::handleCreateTokenFlow);

        postWithPetriGame("/parseLtlFormula", this::handleParseLtlFormula);

        postWithPetriGame("/getModelCheckingNet", this::handleGetModelCheckingNet);
        postWithPetriGame("/checkLtlFormula", this::handleCheckLtlFormula);

        postWithPetriGame("/fireTransition", this::handleFireTransition);

        exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();
            String exceptionAsString = exceptionToString(exception);
            String responseBody = errorResponse(exceptionAsString);
            response.body(responseBody);
        });
    }


    private static String exceptionToString(Exception exception) {
        String exceptionName = exception.getClass().getSimpleName();
        return exceptionName + ": " + exception.getMessage();
    }

    /**
     * Register a POST route where the browserUuid is automatically extracted from each request body
     * and the corresponding UserContext is retrieved.
     * This way, the handler function does not have to handle this frequently repeated operation.
     */
    private void postWithUserContext(String path, RouteWithUserContext handler) {
        post(path, (req, res) -> {
            JsonElement body = parser.parse(req.body());
            String browserUuidString = body.getAsJsonObject().get("browserUuid").getAsString();
            UUID browserUuid = UUID.fromString(browserUuidString);
            if (!userContextMap.containsKey(browserUuid)) {
                userContextMap.put(browserUuid, new UserContext());
            }
            UserContext uc = userContextMap.get(browserUuid);
            Object answer = handler.handle(req, res, uc);
            return answer;
        });
    }

    /**
     * Register a POST route that operates upon a PetriGame.
     * This wrapper handles the parsing of the petriGameId parameter from the request.
     */
    private void postWithPetriGame(String path, RouteWithPetriGame handler) {
        post(path, (req, res) -> {
            JsonElement body = parser.parse(req.body());
            String gameId = body.getAsJsonObject().get("petriGameId").getAsString();
            PetriGame pg = getPetriGame(gameId);
            Object answer = handler.handle(req, res, pg);
            return answer;
        });
    }

    private PetriGame getPetriGame(String uuid) {
        if (!petriGamesReadFromApt.containsKey(uuid)) {
            throw new IllegalArgumentException("We have no PetriGame with the given UUID.  " +
                    "You might see this error if the server has been restarted after you opened the " +
                    "web UI.");
        }
        return petriGamesReadFromApt.get(uuid);
    }

    private static void enableCORS() {
        options("/*",
                (request, response) -> {
                    String accessControlRequestHeaders = request
                            .headers("Access-Control-Request-Headers");
                    if (accessControlRequestHeaders != null) {
                        response.header("Access-Control-Allow-Headers",
                                accessControlRequestHeaders);
                    }
                    String accessControlRequestMethod = request
                            .headers("Access-Control-Request-Method");
                    if (accessControlRequestMethod != null) {
                        response.header("Access-Control-Allow-Methods",
                                accessControlRequestMethod);
                    }

                    return "OK";
                });

        before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));
    }

    private static String successResponse(JsonElement result) {
        return successResponseObject(result).toString();
    }

    private static JsonObject successResponseObject(JsonElement result) {
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("result", result);
        return responseJson;
    }

    private static String errorResponse(String reason) {
        return errorResponseObject(reason).toString();
    }

    private static JsonObject errorResponseObject(String reason) {
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "error");
        responseJson.addProperty("message", reason);
        return responseJson;
    }

    /**
     * @return a list containing an entry for each pending/completed calculation
     * (e.g. Graph Game BDD, Get Winning Condition) on the server.
     */
    private Object handleGetListOfCalculations(Request req, Response res, UserContext uc) {
        JsonArray result = uc.getCalculationList();
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("listings", result);
        return responseJson.toString();
    }

    private Object handleParseApt(Request req, Response res) throws CouldNotFindSuitableConditionException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String apt = body.getAsJsonObject().get("params").getAsJsonObject().get("apt").getAsString();
        PetriGame petriGame;
        try {
            petriGame = AdamSynthesizer.getPetriGame(apt);
            // TODO Use this to parse model checking APT
            //            AdamModelChecker.getPetriNetWithTransits()
        } catch (ParseException | NotSupportedGameException | IOException | CouldNotFindSuitableConditionException | CouldNotCalculateException e) {
            JsonObject errorResponse =
                    errorResponseObject(e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e instanceof ParseException) {
                Pair<Integer, Integer> errorLocation =
                        Tools.getErrorLocation((ParseException) e);
                errorResponse.addProperty("lineNumber", errorLocation.getFirst());
                errorResponse.addProperty("columnNumber", errorLocation.getSecond());
            }
            return errorResponse;
        }

        String petriGameUUID = UUID.randomUUID().toString();
        petriGamesReadFromApt.put(petriGameUUID, petriGame);
        System.out.println("Generated petri game with ID " + petriGameUUID);

        JsonElement petriNetD3Json = PetriNetD3.ofPetriGameWithXYCoordinates(petriGame, petriGame.getNodes());
        JsonElement petriGameClient = PetriGameD3.of(petriNetD3Json, petriGameUUID);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("graph", petriGameClient);
        return responseJson.toString();
    }

    private Calculation<Boolean> calculateExistsWinningStrategy(PetriGame petriGame,
                                                                JsonObject params) {
        return new Calculation<>(() -> {
            boolean existsWinningStrategy = AdamSynthesizer.existsWinningStrategyBDD(petriGame);
            return existsWinningStrategy;
        }, petriGame.getName());
    }

    private Calculation<PetriGame> calculateStrategyBDD(PetriGame petriGame,
                                                        JsonObject params) {
        return new Calculation<>(() -> {
            PetriGame strategyBDD = AdamSynthesizer.getStrategyBDD(petriGame);
            PetriGameTools.removeXAndYCoordinates(strategyBDD);
            return strategyBDD;
        }, petriGame.getName());
    }

    private Calculation<BDDGraph> calculateGraphStrategyBDD(PetriGame petriGame,
                                                            JsonObject params) {
        return new Calculation<>(() -> {
            BDDGraph graphStrategyBDD = AdamSynthesizer.getGraphStrategyBDD(petriGame);
            return graphStrategyBDD;
        }, petriGame.getName());
    }

    private Calculation<BDDGraphExplorer> calculateGraphGameBDD(PetriGame petriGame,
                                                                JsonObject params) {
        // If there is an invalid condition annotation, we should throw an error right away instead
        // of waiting until the calculation gets started (which might take a while if there is a
        // queue).
        try {
            PetriNetD3.getObjectiveOfPetriNet(petriGame);
        } catch (CouldNotFindSuitableConditionException e) {
            throw new IllegalArgumentException(exceptionToString(e));
        }

        boolean shouldSolveStepwise = params.get("incremental").getAsBoolean();
        return new Calculation<>(() -> {
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

    /**
     * Create a Route that allows queueing a job to calculate some attribute of a Petri Game.
     * E.g. What is the winning strategy?  What does the "graph game BDD" look like?
     * What does the model checking net look like?
     * <p>
     * When this route is called, we expect the client to give us two things: A Petri Game's UUID
     * and a browser/user-session UUID.
     * We retrieve the corresponding Petri Game and queue up the requested job.
     *
     * @param calculationFactory A function that creates the Calculation that should be run
     * @param calculationType    The result type of the calculation, used to save it in a
     *                           corresponding Map
     * @param serializerFunction A function that will serialize the result of the calculation so it
     *                           can be sent to the client.
     * @param <T>                The result type of the calculation
     * @return A Sparkjava Route
     */
    private <T> Route handleQueueCalculation(CalculationFactory<T> calculationFactory,
                                             CalculationType calculationType,
                                             SerializerFunction<T> serializerFunction) {
        return (req, res) -> {
            // Read request parameters.  Get the Petri Game that should be operated upon and the
            // UserContext of the client making the request.
            JsonElement body = parser.parse(req.body());
            String gameId = body.getAsJsonObject().get("petriGameId").getAsString();
            // This throws an exception if the Petri Game's not there
            PetriGame petriGame = getPetriGame(gameId);

            // If there isn't a UserContext object for the given browserUuid, create one.
            String browserUuidString = body.getAsJsonObject().get("browserUuid").getAsString();
            UUID browserUuid = UUID.fromString(browserUuidString);
            if (!userContextMap.containsKey(browserUuid)) {
                userContextMap.put(browserUuid, new UserContext());
            }
            UserContext userContext = userContextMap.get(browserUuid);

            // Check if this calculation has already been requested
            String canonicalApt = Adam.getAPT(petriGame);
            Map<String, Calculation<T>> calculationMap =
                    (Map<String, Calculation<T>>) userContext.getCalculationMap(calculationType);
            if (calculationMap.containsKey(canonicalApt)) {
                return errorResponse("There is already a Calculation of " +
                        "\"" + calculationType.toString() + "\" " +
                        "for the given Petri Game. Its status: " +
                        calculationMap.get(canonicalApt).getStatus());
            }

            // Create the calculation and queue it up
            Calculation<T> calculation = calculationFactory.createCalculation(
                    petriGame,
                    body.getAsJsonObject());
            calculationMap.put(canonicalApt, calculation);
            calculation.queue(executorService);

            // If the calculation runs and completes immediately, send the result to the client.
            // Otherwise, let them know that the calculation has been queued.
            return tryToGetResultWithinFiveSeconds(
                    calculation,
                    serializerFunction,
                    canonicalApt,
                    petriGame
            );
        };
    }

    /**
     * When a user queues a calculation, the calculation might finish quickly, or it might take a
     * while.
     * If it's fast, then we want the UI to immediately open the result.
     * Otherwise, a message should just be shown to let the user know that the job has been added
     * to the job queue.
     * This method is meant to handle that type of response.
     *
     * @param calculation      the calculation that has been queued just now
     * @param resultSerializer A function to serialize the result of the calculation into JSON
     *                         for the client to consume
     * @param canonicalApt     The 'canonical apt' of the Petri Game that is being analyzed
     * @param petriGame        The Petri Game that is being analyzed
     * @param <T>              The result type of the calculation
     * @return A JSON response with the result, if it is ready within five seconds.
     * Otherwise, just a message that the calculation is still being calculated.
     * @throws Exception if the serializer function throws an exception.
     */
    private static <T> Object tryToGetResultWithinFiveSeconds(Calculation<T> calculation,
                                                              SerializerFunction<T> resultSerializer,
                                                              String canonicalApt,
                                                              PetriGame petriGame) throws Exception {
        try {
            T result = calculation.getResult(5, TimeUnit.SECONDS);
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.addProperty("message", "The calculation is finished.");
            responseJson.addProperty("canonicalApt", canonicalApt);
            responseJson.addProperty("calculationComplete", true);
            responseJson.add("result", resultSerializer.apply(result));
            responseJson.add("petriGame", PetriNetD3.of(petriGame));
            return responseJson.toString();
        } catch (TimeoutException e) {
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.addProperty("message", "The calculation is taking more " +
                    "than five seconds.  It will run in the background.");
            responseJson.addProperty("canonicalApt", canonicalApt);
            responseJson.addProperty("calculationComplete", false);
            return responseJson.toString();
        } catch (CancellationException e) {
            return errorResponse("The calculation was canceled.");
        } catch (InterruptedException e) {
            return errorResponse("The calculation was interrupted.");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            return errorResponse("The calculation failed with an exception: " +
                    cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
    }

    /**
     * Given the canonical APT representation of a Petri Game and a type of calculation job,
     * return the result of the job, if one has been queued and has completed successfully.
     */
    private <T> Route handleGetCalculationResult(CalculationType calculationType,
                                                 SerializerFunction<T> serializerFunction) {
        return (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();

            // If there isn't a UserContext object for the given browserUuid, create one.
            String browserUuidString = body.getAsJsonObject().get("browserUuid").getAsString();
            UUID browserUuid = UUID.fromString(browserUuidString);
            if (!userContextMap.containsKey(browserUuid)) {
                userContextMap.put(browserUuid, new UserContext());
            }
            UserContext userContext = userContextMap.get(browserUuid);

            Map<String, Calculation<T>> calculationMap =
                    (Map<String, Calculation<T>>) userContext.getCalculationMap(calculationType);
            if (!calculationMap.containsKey(canonicalApt)) {
                return errorResponse("The requested calculation was not found.");
            }

            Calculation<T> calculation = calculationMap.get(canonicalApt);
            if (!calculation.isFinished()) {
                return errorResponse("The requested calculation is not yet finished.  " +
                        "Its status: " + calculation.getStatus());
            }

            T result;
            try {
                result = calculation.getResult();
            } catch (InterruptedException e) {
                return errorResponse("The requested calculation got canceled.");
            } catch (ExecutionException e) {
                return errorResponse(
                        "The requested calculation failed with the following exception: " +
                                e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }

            JsonElement resultJson = serializerFunction.apply(result);
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("result", resultJson);
            return responseJson.toString();
        };
    }

    private Object handleCancelCalculation(Request req, Response res, UserContext uc) {
        JsonElement body = parser.parse(req.body());
        String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();
        String typeString = body.getAsJsonObject().get("type").getAsString();
        CalculationType type = CalculationType.valueOf(typeString);
        Map<String, ? extends Calculation> calculationMap = uc.getCalculationMap(type);
        if (!calculationMap.containsKey(canonicalApt)) {
            return errorResponse("The requested calculation was not found.");
        }
        Calculation calculation = calculationMap.get(canonicalApt);
        calculation.cancel();
        return successResponse(new JsonPrimitive(true));
    }

    private Object handleDeleteCalculation(Request req, Response res, UserContext uc) {
        JsonElement body = parser.parse(req.body());
        String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();
        String typeString = body.getAsJsonObject().get("type").getAsString();
        CalculationType type = CalculationType.valueOf(typeString);
        Map<String, ? extends Calculation> calculationMap = uc.getCalculationMap(type);
        if (!calculationMap.containsKey(canonicalApt)) {
            return errorResponse("The requested calculation was not found.");
        }
        Calculation calculation = calculationMap.get(canonicalApt);
        try {
            calculation.cancel();
        } catch (UnsupportedOperationException e) {
            // We don't care if the calculation is not eligible to be canceled.
            // We just want to cancel it if that's possible.
        }
        calculationMap.remove(canonicalApt);
        return successResponse(new JsonPrimitive(true));
    }

    private Object handleToggleGraphGameBDDNodePostset(Request req, Response res, UserContext uc)
            throws ExecutionException, InterruptedException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();
        int stateId = body.getAsJsonObject().get("stateId").getAsInt();

        if (!uc.graphGameBddsOfApts.containsKey(canonicalApt)) {
            return errorResponse("There is no Graph Game BDD yet for that APT input");
        }

        Calculation<BDDGraphExplorer> calculation = uc.graphGameBddsOfApts.get(canonicalApt);
        if (!calculation.isFinished()) {
            return errorResponse("The calculation for that Graph Game BDD is not yet finished" +
                    ".  Its status: " + calculation.getStatus());
        }
        BDDGraphExplorer bddGraphExplorer = calculation.getResult();
        bddGraphExplorer.toggleStatePostset(stateId);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("graphGameBDD", bddGraphExplorer.getVisibleGraph());
        return responseJson.toString();
    }

    private Object handleToggleGraphGameBDDNodePreset(Request req, Response res, UserContext uc)
            throws ExecutionException, InterruptedException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();
        int stateId = body.getAsJsonObject().get("stateId").getAsInt();

        if (!uc.graphGameBddsOfApts.containsKey(canonicalApt)) {
            return errorResponse("There is no Graph Game BDD yet for that APT input");
        }
        Calculation<BDDGraphExplorer> calculation = uc.graphGameBddsOfApts.get(canonicalApt);
        if (!calculation.isFinished()) {
            return errorResponse("The calculation for that Graph Game BDD is not yet finished" +
                    ".  Its status: " + calculation.getStatus());
        }
        BDDGraphExplorer bddGraphExplorer = calculation.getResult();
        bddGraphExplorer.toggleStatePreset(stateId);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("graphGameBDD", bddGraphExplorer.getVisibleGraph());
        return responseJson.toString();
    }

    private Object handleSavePetriGameAsAPT(Request req, Response res, PetriGame petriGame)
            throws RenderException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        JsonObject nodesXYCoordinatesJson = body.getAsJsonObject().get("nodeXYCoordinateAnnotations").getAsJsonObject();
        Type type = new TypeToken<Map<String, NodePosition>>() {
        }.getType();
        Map<String, NodePosition> nodePositions = gson.fromJson(nodesXYCoordinatesJson, type);

        String apt = PetriGameTools.savePetriGameWithXYCoordinates(petriGame, nodePositions);
        JsonElement aptJson = new JsonPrimitive(apt);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("apt", aptJson);
        return responseJson.toString();
    }

    private Object handleInsertPlace(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        double x = body.get("x").getAsDouble();
        double y = body.get("y").getAsDouble();
        String nodeType = body.get("nodeType").getAsString();
        GraphNodeType graphNodeType = GraphNodeType.valueOf(nodeType);

        Node node = null;
        switch (graphNodeType) {
            case SYSPLACE:
                node = petriGame.createPlace();
                break;
            case ENVPLACE:
                Place place = petriGame.createPlace();
                petriGame.setEnvironment(place);
                node = place;
                break;
            case TRANSITION:
                node = petriGame.createTransition();
                break;
            case GRAPH_STRATEGY_BDD_STATE:
                return errorResponse("You can't insert a GRAPH_STRATEGY_BDD_STATE into a Petri Game.");
        }
        petriGame.setXCoord(node, x);
        petriGame.setYCoord(node, y);

        JsonElement petriGameClient = PetriNetD3.ofPetriGameWithXYCoordinates(
                petriGame, new HashSet<>(Collections.singletonList(node)));

        return successResponse(petriGameClient);
    }

    private Object handleDeleteNode(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();

        petriGame.removeNode(nodeId);

        JsonElement petriGameClient = PetriNetD3.of(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleRenameNode(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeIdOld = body.get("nodeIdOld").getAsString();
        String nodeIdNew = body.get("nodeIdNew").getAsString();

        Node oldNode = petriGame.getNode(nodeIdOld);
        petriGame.rename(oldNode, nodeIdNew);

        JsonElement petriGameClient = PetriNetD3.of(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleToggleEnvironmentPlace(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();

        Place place = petriGame.getPlace(nodeId);
        boolean environment = petriGame.isEnvironment(place);
        if (environment) {
            petriGame.setSystem(place);
        } else {
            petriGame.setEnvironment(place);
        }

        JsonElement petriGameClient = PetriNetD3.of(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleToggleIsInitialTokenFlow(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();

        Place place = petriGame.getPlace(nodeId);
        boolean isInitialTokenFlow = petriGame.isInitialTransit(place);
        if (isInitialTokenFlow) {
            petriGame.removeInitialTokenflow(place);
        } else {
            petriGame.setInitialTokenflow(place);
        }

        JsonElement petriGameClient = PetriNetD3.of(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleSetInitialToken(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();
        int tokens = body.get("tokens").getAsInt();

        Place place = petriGame.getPlace(nodeId);
        place.setInitialToken(tokens);

        JsonElement petriGameClient = PetriNetD3.of(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleSetWinningCondition(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String winningCondition = body.get("winningCondition").getAsString();

        Condition.Objective objective = Condition.Objective.valueOf(winningCondition);
        PNWTTools.setConditionAnnotation(petriGame, objective);

        JsonElement petriGameClient = PetriNetD3.of(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleCreateFlow(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String source = body.get("source").getAsString();
        String destination = body.get("destination").getAsString();

        petriGame.createFlow(source, destination);
        JsonElement petriGameClient = PetriNetD3.of(petriGame);

        return successResponse(petriGameClient);
    }

    private Object handleDeleteFlow(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String source = body.get("sourceId").getAsString();
        String target = body.get("targetId").getAsString();

        petriGame.removeFlow(source, target);

        JsonElement petriGameClient = PetriNetD3.of(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleCreateTokenFlow(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        Optional<String> sourceId = body.has("source") ?
                Optional.of(body.get("source").getAsString()) :
                Optional.empty();
        String transitionId = body.get("transition").getAsString();
        JsonArray postsetJson = body.get("postset").getAsJsonArray();
        List<String> postsetIds = new ArrayList<>();
        postsetJson.forEach(jsonElement -> {
            postsetIds.add(jsonElement.getAsString());
        });

        // Create flows if they don't already exist
        Transition transition = petriGame.getTransition(transitionId);
        sourceId.ifPresent(id -> {
            Place source = petriGame.getPlace(id);
            if (!transition.getPreset().contains(source)) {
                petriGame.createFlow(source, transition);
            }
        });
        for (String postsetId : postsetIds) {
            Place postPlace = petriGame.getPlace(postsetId);
            if (!transition.getPostset().contains(postPlace)) {
                petriGame.createFlow(transition, postPlace);
            }
        }

        // Create a token flow.  It is an initial token flow if if has no source Place.
        String[] postsetArray = postsetIds.toArray(new String[postsetIds.size()]);
        if (sourceId.isPresent()) {
            petriGame.createTransit(sourceId.get(), transitionId, postsetArray);
        } else {
            petriGame.createInitialTransit(transitionId, postsetArray);
        }

        JsonElement petriGameClient = PetriNetD3.of(petriGame);

        return successResponse(petriGameClient);
    }

    private Object handleParseLtlFormula(Request req, Response res, PetriGame petriGame) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String formula = body.get("formula").getAsString();

        try {
            IRunFormula iRunFormula = AdamModelChecker.parseFlowLTLFormula(petriGame, formula);
            return successResponse(new JsonPrimitive(true));
        } catch (ParseException e) {
            Throwable cause = e.getCause();
            System.out.println(cause.getMessage());
            return errorResponse(e.getMessage() + "\n" + cause.getMessage());
        }
    }

    private Object handleGetModelCheckingNet(Request req, Response res, PetriGame petriGame) throws NotSupportedGameException, InterruptedException, ParseException, IOException, ExternalToolException, NotConvertableException, ProcessNotStartedException, CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String formula = body.get("formula").getAsString();

        RunFormula runFormula = AdamModelChecker.parseFlowLTLFormula(petriGame, formula);

        PetriNet modelCheckingNet = AdamModelChecker.getModelCheckingNet(petriGame, runFormula, false);

        return successResponse(PetriNetD3.of(new PetriGame(modelCheckingNet)));
    }

    private Calculation<ModelCheckingResult> calculateModelCheckingResult(
            PetriGame petriGame,
            JsonObject params) {
        return null; // TODO
    }

    private Object handleCheckLtlFormula(Request req, Response res, PetriGame petriGame) throws ParseException, InterruptedException, NotConvertableException, ExternalToolException, ProcessNotStartedException, IOException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String formula = body.get("formula").getAsString();

        System.out.println("Checking flow LTL formula");
        RunFormula runFormula = AdamModelChecker.parseFlowLTLFormula(petriGame, formula);
        ModelCheckerFlowLTL modelCheckerFlowLTL = new ModelCheckerFlowLTL();
        ModelCheckingResult result = AdamModelChecker.checkFlowLTLFormula(petriGame, modelCheckerFlowLTL, runFormula, "/tmp/", null);

        ModelCheckingResult.Satisfied satisfied = result.getSatisfied();
        switch (satisfied) {
            case TRUE:
                return successResponse(new JsonPrimitive(true));
            case FALSE:
                JsonObject response = successResponseObject(new JsonPrimitive(false));
                // TODO Serialize CounterExample and display it in the client
//                CounterExample cex = result.getCex();
//                response.addProperty("counterExample", );
                return response;
            case UNKNOWN:
                return errorResponse("The Model Checker returned the result 'unknown'");
            default:
                return errorResponse("Missing switch branch in handleCheckLtlFormula: " + satisfied);
        }
    }

    private Object handleFireTransition(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String transitionId = body.get("transitionId").getAsString();

        Transition transition = petriGame.getTransition(transitionId);
        Marking initialMarking = petriGame.getInitialMarking();
        Marking newInitialMarking = transition.fire(initialMarking);
        petriGame.setInitialMarking(newInitialMarking);
        return successResponse(PetriNetD3.of(petriGame));
    }
}
