package uniolunisaar.adamwebfrontend;
/**
 * Created by Ann on 11.09.2017.
 */

import static spark.Spark.*;
import static uniolunisaar.adamwebfrontend.CalculationStatus.COMPLETED;
import static uniolunisaar.adamwebfrontend.CalculationStatus.FAILED;
import static uniolunisaar.adamwebfrontend.CalculationType.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import spark.Request;
import spark.Response;
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
    // Whenever we load a PetriGame from APT, we put it into this hashmap.  The client refers to it via a uuid.
    private final Map<String, PetriGameAndMore> petriGamesReadFromApt = new ConcurrentHashMap<>();

    // When we calculate a graph game BDD from a petri game, we convert the petri game to APT, then
    // put the (APT, BDDGraphExplorer) pair in here
    private final Map<String, Calculation<BDDGraphExplorer>> bddGraphsOfApts = new ConcurrentHashMap<>();
    // Store the results of "existsWinningStrategy"
    private final Map<String, Calculation<Boolean>> existsWinningStrategyOfApts =
            new ConcurrentHashMap<>();
    // Store the results of "getStrategyBdd"
    private final Map<String, Calculation<PetriGame>> strategyBddsOfApts =
            new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final JsonParser parser = new JsonParser();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        new App().startServer();
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

        post("/existsWinningStrategy", this::handleExistsWinningStrategy);

        post("/getStrategyBDD", this::handleGetStrategyBDD);

        post("/getGraphStrategyBDD", this::handleGetGraphStrategyBDD);

        post("/calculateGraphGameBDD", this::handleCalculateGraphGameBDD);

        post("/getListOfCalculations", this::handleGetListOfCalculations);

        post("/getBDDGraph", this::handleGetBDDGraph);

        post("/loadWinningStrategy", this::handleLoadWinningStrategy);

        post("/cancelCalculation", this::handleCancelCalculation);

        post("/toggleGraphGameBDDNodePostset", this::handleToggleGraphGameBDDNodePostset);

        post("/toggleGraphGameBDDNodePreset", this::handleToggleGraphGameBDDNodePreset);

        post("/savePetriGameAsAPT", this::handleSavePetriGameAsAPT);

        post("/insertPlace", this::handleInsertPlace);

        post("/deleteNode", this::handleDeleteNode);

        post("/renameNode", this::handleRenameNode);

        post("/toggleEnvironmentPlace", this::handleToggleEnvironmentPlace);

        post("/toggleIsInitialTokenFlow", this::handleToggleIsInitialTokenFlow);

        post("/setInitialToken", this::handleSetInitialToken);

        post("/setWinningCondition", this::handleSetWinningCondition);
        post("/createFlow", this::handleCreateFlow);

        post("/deleteFlow", this::handleDeleteFlow);

        post("/createTokenFlow", this::handleCreateTokenFlow);

        post("/checkLtlFormula", this::handleCheckLtlFormula);

        post("/getModelCheckingNet", this::handleGetModelCheckingNet);

        post("/fireTransition", this::handleFireTransition);

        exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();
            String exceptionName = exception.getClass().getSimpleName();
            String exceptionAsString = exceptionName + ": " + exception.getMessage();
            String responseBody = errorResponse(exceptionAsString);
            response.body(responseBody);
        });
    }

    private PetriGameAndMore getPetriGame(String uuid) {
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
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("result", result);
        return responseJson.toString();
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
    private Object handleGetListOfCalculations(Request req, Response res) throws ExecutionException, InterruptedException {
        JsonArray result = new JsonArray();
        for (String aptOfPetriGame : this.bddGraphsOfApts.keySet()) {
            Calculation<BDDGraphExplorer> calculation = this.bddGraphsOfApts.get(aptOfPetriGame);
            JsonObject entry = calculationListEntry(calculation, aptOfPetriGame, GRAPH_GAME_BDD);
            result.add(entry);
        }
        for (String canonicalApt : this.existsWinningStrategyOfApts.keySet()) {
            Calculation<Boolean> calculation = this.existsWinningStrategyOfApts.get(canonicalApt);
            JsonObject entry = calculationListEntry(
                    calculation, canonicalApt, EXISTS_WINNING_STRATEGY);
            if (calculation.getStatus() == COMPLETED) {
                entry.addProperty("result", calculation.getResult());
            }
            result.add(entry);
        }
        for (String canonicalApt : this.strategyBddsOfApts.keySet()) {
            Calculation<PetriGame> calculation = this.strategyBddsOfApts.get(canonicalApt);
            JsonObject entry = calculationListEntry(
                    calculation, canonicalApt, WINNING_STRATEGY);
            result.add(entry);
        }
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("listings", result);
        return responseJson.toString();
    }

    private JsonObject calculationListEntry(Calculation calculation,
                                            String canonicalApt,
                                            CalculationType calculationType) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", calculationType.toString());
        entry.addProperty("canonicalApt", canonicalApt);
        entry.addProperty("calculationStatus", calculation.getStatus().toString());
        entry.addProperty("timeStarted", calculation.getTimeStarted().getEpochSecond());
        entry.addProperty("timeFinished", calculation.getTimeFinished().getEpochSecond());
        if (calculation.getStatus() == FAILED) {
            entry.addProperty("failureReason", calculation.getFailedReason());
        }
        return entry;
    }

    private Object handleParseApt(Request req, Response res) {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String apt = body.getAsJsonObject().get("params").getAsJsonObject().get("apt").getAsString();
        PetriGame petriGame;
        try {
            petriGame = Adam.getPetriGame(apt);
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
        PetriGameAndMore petriGameAndMore = PetriGameAndMore.of(petriGame);
        petriGamesReadFromApt.put(petriGameUUID, petriGameAndMore);
        System.out.println("Generated petri game with ID " + petriGameUUID);

        JsonElement petriNetD3Json = PetriNetD3.of(petriGame, petriGame.getNodes());
        JsonElement petriGameClient = PetriGameD3.of(petriNetD3Json, petriGameUUID);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("graph", petriGameClient);
        return responseJson.toString();
    }

    private Object handleExistsWinningStrategy(Request req, Response res) throws ExecutionException, InterruptedException, RenderException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();

        PetriGameAndMore petriGame = getPetriGame(petriGameId);

        System.out.println("Is there a winning strategy for PetriGame id#" + petriGameId + "?");

        String canonicalApt = Adam.getAPT(petriGame.getPetriGame());
        if (this.existsWinningStrategyOfApts.containsKey(canonicalApt)) {
            return errorResponse("There is already a Calculation queued up to find the " +
                    "Exists Winning Condition of the Petri Game with the given APT.  Its " +
                    "status: " +
                    this.existsWinningStrategyOfApts.get(canonicalApt).getStatus());
        }

        System.out.println("Calculating Exists Winning Strategy for PetriGame id#" + petriGameId);
        Calculation<Boolean> calculation = new Calculation<>(() -> {
            boolean existsWinningStrategy = AdamSynthesizer.existsWinningStrategyBDD(petriGame.getPetriGame());
            return existsWinningStrategy;
        }, petriGame.getPetriGame().getName());
        this.existsWinningStrategyOfApts.put(canonicalApt, calculation);
        calculation.queue(executorService);

        try {
            boolean result = calculation.getResult(5, TimeUnit.SECONDS);
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.addProperty("message", "The calculation of Exists Winning Strategy " +
                    "is finished.");
            responseJson.addProperty("canonicalApt", canonicalApt);
            responseJson.addProperty("calculationComplete", true);
            // Annotations might have been added to the petri game, and the client should
            // know about them.
            responseJson.add("petriGame", PetriNetD3.of(petriGame.getPetriGame()));
            responseJson.addProperty("result", result);
            return responseJson.toString();
        } catch (TimeoutException e) {
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.addProperty("message", "The calculation of Exists Winning Strategy " +
                    "is taking more than five seconds.  It will run in the background.");
            responseJson.addProperty("canonicalApt", canonicalApt);
            responseJson.addProperty("calculationComplete", false);
            return responseJson.toString();
        } catch (CancellationException e) {
            return errorResponse("The calculation was canceled.");
        }
    }

    private Object handleGetStrategyBDD(Request req, Response res) throws ExecutionException, InterruptedException, RenderException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();

        PetriGameAndMore petriGame = getPetriGame(petriGameId);

        String canonicalApt = Adam.getAPT(petriGame.getPetriGame());
        if (this.strategyBddsOfApts.containsKey(canonicalApt)) {
            return errorResponse("There is already a Calculation queued up to find the " +
                    "winning strategy of the Petri Game with the given APT.  Its status: " +
                    this.strategyBddsOfApts.get(canonicalApt).getStatus());
        }
        PetriGame game = petriGame.getPetriGame();
        Calculation<PetriGame> calculation = new Calculation<>(() -> {
            PetriGame strategyBDD = AdamSynthesizer.getStrategyBDD(game);
            PetriGameAndMore.removeXAndYCoordinates(strategyBDD);
            return strategyBDD;
        }, game.getName());
        this.strategyBddsOfApts.put(canonicalApt, calculation);
        calculation.queue(executorService);

        try {
            // TODO Handle other exceptions thrown by getResult (maybe refactor to reuse
            //  error handling from /getGraphBDD))
            PetriGame result = calculation.getResult(5, TimeUnit.SECONDS);
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.addProperty("message", "The calculation of the " +
                    "winning strategy is finished.");
            responseJson.addProperty("canonicalApt", canonicalApt);
            responseJson.addProperty("calculationComplete", true);
            responseJson.add("strategyBDD", PetriNetD3.of(result));
            responseJson.add("petriGame", PetriNetD3.of(petriGame.getPetriGame()));
            return responseJson.toString();
        } catch (TimeoutException e) {
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.addProperty("message", "The calculation of the winning " +
                    "strategy is taking more than five seconds.  It will run in the " +
                    "background.");
            responseJson.addProperty("canonicalApt", canonicalApt);
            responseJson.addProperty("calculationComplete", false);
            return responseJson.toString();
        } catch (CancellationException e) {
            return errorResponse("The calculation was canceled.");
        }
    }

    // TODO Prevent DDOS by throttling this endpoint somehow
    private Object handleGetGraphStrategyBDD(Request req, Response res) throws NoStrategyExistentException, CouldNotFindSuitableConditionException, CalculationInterruptedException, ParseException, SolvingException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();

        PetriGameAndMore petriGame = getPetriGame(petriGameId);
        System.out.println("Getting graph strategy BDD for PetriGame id#" + petriGameId);
        JsonElement bddGraph = petriGame.calculateGraphStrategyBDD();

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("graphStrategyBDD", bddGraph);
        responseJson.add("petriGame", PetriNetD3.of(petriGame.getPetriGame()));
        return responseJson.toString();
    }

    private Object handleCalculateGraphGameBDD(Request req, Response res) throws RenderException, ExecutionException, InterruptedException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();
        boolean shouldSolveStepwise =
                body.getAsJsonObject().get("incremental").getAsBoolean();

        // TODO Consider not putting PetriGame and everything together inside of
        //   PetriGameAndMore
        PetriGameAndMore petriGame = getPetriGame(petriGameId);

        // Just in case the petri game gets modified after the computation starts, we will
        // save its apt right here already
        String canonicalApt = Adam.getAPT(petriGame.getPetriGame());
        if (this.bddGraphsOfApts.containsKey(canonicalApt)) {
            return errorResponse("There is already a Calculation queued up to find the " +
                    "Graph Game BDD of the Petri Game with the given APT.  Its status: " +
                    this.bddGraphsOfApts.get(canonicalApt).getStatus());
        }

        // Calculate the Graph Game BDD
        // TODO What happens if you modify the petri game while this calculation is ongoing?
        // TODO Do I need to do something to stop that from happening?  -Ann
        System.out.println("Calculating graph game BDD for PetriGame id#" + petriGameId);
        PetriGame game = petriGame.getPetriGame();
        Optional<Condition.Objective> objective = PetriNetD3.getObjectiveOfPetriNet(game);
        if (!objective.isPresent()) {
            return errorResponse("No winning condition is present for the given Petri Game.");
        }
        Calculation<BDDGraphExplorer> calculation = new Calculation<>(() -> {
            if (shouldSolveStepwise) {
                BDDGraphExplorerStepwise bddGraphExplorerStepwise = new BDDGraphExplorerStepwise(game);
                return bddGraphExplorerStepwise;
            } else {
                BDDGraph graphGameBDD = AdamSynthesizer.getGraphGameBDD(game);
                return BDDGraphExplorerCompleteGraph.of(graphGameBDD);
            }
        }, game.getName());
        this.bddGraphsOfApts.put(canonicalApt, calculation);
        calculation.queue(executorService);

        try {
            // TODO Handle other exceptions thrown by getResult (maybe refactor to reuse
            //  error handling from /getGraphBDD))
            BDDGraphExplorer result = calculation.getResult(5, TimeUnit.SECONDS);
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.addProperty("message", "The calculation of the graph game " +
                    "BDD is finished.");
            responseJson.addProperty("canonicalApt", canonicalApt);
            responseJson.addProperty("calculationComplete", true);
            responseJson.add("bddGraph", result.getVisibleGraph());
            return responseJson.toString();
        } catch (TimeoutException e) {
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.addProperty("message", "The calculation of the graph game " +
                    "BDD is taking more than five seconds.  It will run in the background.");
            responseJson.addProperty("canonicalApt", canonicalApt);
            responseJson.addProperty("calculationComplete", false);
            return responseJson.toString();
        } catch (CancellationException e) {
            return errorResponse("The calculation was canceled.");
        }
    }

    // Given the canonical APT representation of a Petri Game, return the current view
    // of the BDDGraph that has been calculated for it, if one is present.
    private Object handleGetBDDGraph(Request req, Response res) {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();

        if (!this.bddGraphsOfApts.containsKey(canonicalApt)) {
            return errorResponse("No BDDGraph has been calculated yet for the Petri " +
                    "Game with the given APT representation: \n" + canonicalApt);
        }
        Calculation<BDDGraphExplorer> calculation = this.bddGraphsOfApts.get(canonicalApt);
        if (!calculation.isFinished()) {
            return errorResponse("The calculation for that Graph Game BDD is not yet finished" +
                    ".  Its status: " + calculation.getStatus());
        }
        BDDGraphExplorer bddGraphExplorer;
        try {
            bddGraphExplorer = calculation.getResult();
        } catch (InterruptedException e) {
            return errorResponse("The calculation for that Graph Game BDD got canceled.");
        } catch (ExecutionException e) {
            return errorResponse("The calculation for that Graph Game BDD failed with the " +
                    "following exception: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
        }

        JsonElement bddGraph = bddGraphExplorer.getVisibleGraph();
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("bddGraph", bddGraph);
        return responseJson.toString();
    }

    // Load the winning strategy (strategy BDD) of a Petri Game that has been calculated
    private Object handleLoadWinningStrategy(Request req, Response res) {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();

        if (!this.strategyBddsOfApts.containsKey(canonicalApt)) {
            return errorResponse("No winning strategy has been calculated yet for the Petri " +
                    "Game with the given APT representation: \n" + canonicalApt);
        }
        Calculation<PetriGame> calculation = this.strategyBddsOfApts.get(canonicalApt);
        if (!calculation.isFinished()) {
            return errorResponse("The calculation of that winning strategy is not yet " +
                    "finished.  Its status: " + calculation.getStatus());
        }
        PetriGame strategyBdd;
        try {
            strategyBdd = calculation.getResult();
        } catch (InterruptedException e) {
            return errorResponse("The calculation for that winning strategy got canceled.");
        } catch (ExecutionException e) {
            return errorResponse("The calculation for that winning strategy failed with the " +
                    "following exception: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
        }

        JsonElement strategyBddJson = PetriNetD3.of(strategyBdd);
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("strategyBDD", strategyBddJson);
        return responseJson.toString();
    }

    private Object handleCancelCalculation(Request req, Response res) {
        JsonElement body = parser.parse(req.body());
        String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();
        String typeString = body.getAsJsonObject().get("type").getAsString();
        CalculationType type = CalculationType.valueOf(typeString);
        Map<String, ? extends Calculation> calculationMap = null;
        // TODO Consider putting the "calculationMaps" into a Map<CalculationType, Map<...>>
        switch (type) {
            case EXISTS_WINNING_STRATEGY:
                calculationMap = this.existsWinningStrategyOfApts;
                break;
            case WINNING_STRATEGY:
                calculationMap = this.strategyBddsOfApts;
                break;
            case GRAPH_GAME_BDD:
                calculationMap = this.bddGraphsOfApts;
                break;
        }
        if (!calculationMap.containsKey(canonicalApt)) {
            return errorResponse("No calculation of " + type + " for the given APT was found.");
        }
        Calculation calculation = calculationMap.get(canonicalApt);
        calculation.cancel();
        return successResponse(new JsonPrimitive(true));
    }

    private Object handleToggleGraphGameBDDNodePostset(Request req, Response res) throws ExecutionException, InterruptedException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();
        int stateId = body.getAsJsonObject().get("stateId").getAsInt();

        if (!this.bddGraphsOfApts.containsKey(canonicalApt)) {
            return errorResponse("There is no Graph Game BDD yet for that APT input");
        }

        Calculation<BDDGraphExplorer> calculation = bddGraphsOfApts.get(canonicalApt);
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

    private Object handleToggleGraphGameBDDNodePreset(Request req, Response res) throws ExecutionException, InterruptedException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();
        int stateId = body.getAsJsonObject().get("stateId").getAsInt();

        if (!this.bddGraphsOfApts.containsKey(canonicalApt)) {
            return errorResponse("There is no Graph Game BDD yet for that APT input");
        }
        Calculation<BDDGraphExplorer> calculation = bddGraphsOfApts.get(canonicalApt);
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

    private Object handleSavePetriGameAsAPT(Request req, Response res) throws RenderException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();
        JsonObject nodesXYCoordinatesJson = body.getAsJsonObject().get("nodeXYCoordinateAnnotations").getAsJsonObject();
        Type type = new TypeToken<Map<String, NodePosition>>() {
        }.getType();
        Map<String, NodePosition> nodePositions = gson.fromJson(nodesXYCoordinatesJson, type);

        PetriGameAndMore petriGameAndMore = getPetriGame(petriGameId);
        String apt = petriGameAndMore.savePetriGameWithXYCoordinates(nodePositions);
        JsonElement aptJson = new JsonPrimitive(apt);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("apt", aptJson);
        return responseJson.toString();
    }

    private Object handleInsertPlace(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String gameId = body.get("petriGameId").getAsString();
        double x = body.get("x").getAsDouble();
        double y = body.get("y").getAsDouble();
        String nodeType = body.get("nodeType").getAsString();
        GraphNodeType graphNodeType = GraphNodeType.valueOf(nodeType);

        PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
        PetriGame petriGame = petriGameAndMore.getPetriGame();

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

        JsonElement petriGameClient = PetriNetD3.of(petriGame, new HashSet<>(Collections.singletonList(node)));

        return successResponse(petriGameClient);
    }

    private Object handleDeleteNode(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String gameId = body.get("petriGameId").getAsString();
        String nodeId = body.get("nodeId").getAsString();

        PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
        PetriGame petriGame = petriGameAndMore.getPetriGame();

        petriGame.removeNode(nodeId);

        JsonElement petriGameClient = PetriNetD3.of(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleRenameNode(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String gameId = body.get("petriGameId").getAsString();
        String nodeIdOld = body.get("nodeIdOld").getAsString();
        String nodeIdNew = body.get("nodeIdNew").getAsString();

        PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
        PetriGame petriGame = petriGameAndMore.getPetriGame();
        Node oldNode = petriGame.getNode(nodeIdOld);
        petriGame.rename(oldNode, nodeIdNew);

        JsonElement petriGameClient = PetriNetD3.of(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleToggleEnvironmentPlace(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String gameId = body.get("petriGameId").getAsString();
        String nodeId = body.get("nodeId").getAsString();

        PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
        PetriGame petriGame = petriGameAndMore.getPetriGame();
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

    private Object handleToggleIsInitialTokenFlow(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String gameId = body.get("petriGameId").getAsString();
        String nodeId = body.get("nodeId").getAsString();

        PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
        PetriGame petriGame = petriGameAndMore.getPetriGame();
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

    private Object handleSetInitialToken(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String gameId = body.get("petriGameId").getAsString();
        String nodeId = body.get("nodeId").getAsString();
        int tokens = body.get("tokens").getAsInt();

        PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
        PetriGame petriGame = petriGameAndMore.getPetriGame();
        Place place = petriGame.getPlace(nodeId);
        place.setInitialToken(tokens);

        JsonElement petriGameClient = PetriNetD3.of(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleSetWinningCondition(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String gameId = body.get("petriGameId").getAsString();
        String winningCondition = body.get("winningCondition").getAsString();

        PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
        PetriGame petriGame = petriGameAndMore.getPetriGame();
        Condition.Objective objective = Condition.Objective.valueOf(winningCondition);
        PNWTTools.setConditionAnnotation(petriGame, objective);

        JsonElement petriGameClient = PetriNetD3.of(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleCreateFlow(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String gameId = body.get("petriGameId").getAsString();
        String source = body.get("source").getAsString();
        String destination = body.get("destination").getAsString();
        PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
        PetriGame petriGame = petriGameAndMore.getPetriGame();

        petriGame.createFlow(source, destination);
        JsonElement petriGameClient = PetriNetD3.of(petriGame);

        return successResponse(petriGameClient);
    }

    private Object handleDeleteFlow(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String gameId = body.get("petriGameId").getAsString();
        String source = body.get("sourceId").getAsString();
        String target = body.get("targetId").getAsString();
        PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
        PetriGame petriGame = petriGameAndMore.getPetriGame();

        petriGame.removeFlow(source, target);

        JsonElement petriGameClient = PetriNetD3.of(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleCreateTokenFlow(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String gameId = body.get("petriGameId").getAsString();
        Optional<String> sourceId = body.has("source") ?
                Optional.of(body.get("source").getAsString()) :
                Optional.empty();
        String transitionId = body.get("transition").getAsString();
        JsonArray postsetJson = body.get("postset").getAsJsonArray();
        List<String> postsetIds = new ArrayList<>();
        postsetJson.forEach(jsonElement -> {
            postsetIds.add(jsonElement.getAsString());
        });

        PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
        PetriGame petriGame = petriGameAndMore.getPetriGame();

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

    private Object handleCheckLtlFormula(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String gameId = body.get("petriGameId").getAsString();
        String formula = body.get("formula").getAsString();

        PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
        PetriGame petriGame = petriGameAndMore.getPetriGame();

        try {
            IRunFormula iRunFormula = AdamModelChecker.parseFlowLTLFormula(petriGame, formula);
            return successResponse(new JsonPrimitive(true));
        } catch (ParseException e) {
            Throwable cause = e.getCause();
            System.out.println(cause.getMessage());
            return errorResponse(e.getMessage() + "\n" + cause.getMessage());
        }
    }

    private Object handleGetModelCheckingNet(Request req, Response res) throws NotSupportedGameException, InterruptedException, ParseException, IOException, ExternalToolException, NotConvertableException, ProcessNotStartedException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String gameId = body.get("petriGameId").getAsString();
        String formula = body.get("formula").getAsString();

        PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
        PetriGame petriGame = petriGameAndMore.getPetriGame();

        IRunFormula iRunFormula = AdamModelChecker.parseFlowLTLFormula(petriGame, formula);
        // TODO ask Manuel if this cast is OK / normal / expected
        RunFormula runFormula = (RunFormula) iRunFormula;

        PetriNet modelCheckingNet = AdamModelChecker.getModelCheckingNet(petriGame, runFormula, false);
        System.out.println("Checking flow LTL formula");
        // TODO check the flow ltl formula
        ModelCheckerFlowLTL modelCheckerFlowLTL = new ModelCheckerFlowLTL();
        ModelCheckingResult result = AdamModelChecker.checkFlowLTLFormula(petriGame, modelCheckerFlowLTL, runFormula, "/tmp/", null);
        System.out.println("result:");
        System.out.println(result);

        return successResponse(PetriNetD3.of(modelCheckingNet));
    }

    private Object handleFireTransition(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String gameId = body.get("petriGameId").getAsString();
        String transitionId = body.get("transitionId").getAsString();

        PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
        PetriGame petriGame = petriGameAndMore.getPetriGame();
        Transition transition = petriGame.getTransition(transitionId);
        Marking initialMarking = petriGame.getInitialMarking();
        Marking newInitialMarking = transition.fire(initialMarking);
        petriGame.setInitialMarking(newInitialMarking);
        return successResponse(petriGameAndMore.getPetriGameClient());
    }
}
