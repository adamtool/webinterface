package uniolunisaar.adamwebfrontend;
/**
 * Created by Ann on 11.09.2017.
 */

import static spark.Spark.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.AdamModelChecker;
import uniolunisaar.adam.AdamSynthesizer;
import uniolunisaar.adam.ds.logics.ltl.flowltl.IRunFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.modelchecking.CounterExample;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrigame.PetriGameExtensionHandler;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.PNWTTools;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class App {
    // Whenever we load a PetriGame from APT, we put it into this hashmap.  The client refers to it via a uuid.
    private final Map<String, PetriGameAndMore> petriGamesReadFromApt = new ConcurrentHashMap<>();

    // When we calculate a graph game BDD from a petri game, we convert the petri game to APT, then
    // put the (APT, BDDGraphExplorer) pair in here
    // TODO Consider whether to use this APT as a key, or if there is a better key to use
    // TODO Remove the BDDGraphExplorer from PetriGameAndMore.  Just store the BDDGraphs here.
    private final Map<String, BDDGraphExplorer> bddGraphsOfApts = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final JsonParser parser = new JsonParser();

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

        post("/convertAptToGraph", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String apt = body.getAsJsonObject().get("params").getAsJsonObject().get("apt").getAsString();
            PetriGame petriGame = Adam.getPetriGame(apt);

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
        });

        post("/existsWinningStrategy", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();

            PetriGameAndMore petriGame = getPetriGame(petriGameId);

            System.out.println("Is there a winning strategy for PetriGame id#" + petriGameId + "?");
            boolean existsWinningStrategy = petriGame.calculateExistsWinningStrategy();

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.addProperty("result", existsWinningStrategy);
            responseJson.add("petriGame", PetriNetD3.of(petriGame.getPetriGame()));
            return responseJson.toString();
        });

        post("/getStrategyBDD", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();

            PetriGameAndMore petriGame = getPetriGame(petriGameId);
            System.out.println("Calculating strategy BDD for PetriGame id#" + petriGameId);
            JsonElement strategyBDDJson = petriGame.calculateStrategyBDD();

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("strategyBDD", strategyBDDJson);
            responseJson.add("petriGame", PetriNetD3.of(petriGame.getPetriGame()));
            return responseJson.toString();
        });

        // TODO change this so it doesn't calculate the same bdd a bunch of times just because you click the button more than once.
        // Right now you can crash the server just by sending a bunch of requests to this endpoint
        post("/getGraphStrategyBDD", (req, res) -> {
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
        });

        post("/getGraphGameBDD", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();

            // TODO Consider not putting PetriGame and everything together inside of
            //   PetriGameAndMore
            PetriGameAndMore petriGame = getPetriGame(petriGameId);

            // Just in case the petri game gets modified after the computation starts, we will
            // save its apt right here already
            String canonicalApt = Adam.getAPT(petriGame.getPetriGame());
            BDDGraphExplorer bddGraphExplorer;
            if (this.bddGraphsOfApts.containsKey(canonicalApt)) {
                // We don't have to compute it, we already have the GraphBDD!  :)
                bddGraphExplorer = this.bddGraphsOfApts.get(canonicalApt);
            } else {
                // Calculate the Graph Game BDD
                // TODO Track the state of this computation somewhere
                // TODO What happens if you modify the petri game while this calculation is ongoing?
                // TODO Do I need to do something to stop that from happening?  -Ann
                System.out.println("Calculating graph game BDD for PetriGame id#" + petriGameId);
                BDDGraph graphGameBDD = AdamSynthesizer.getGraphGameBDD(petriGame.getPetriGame());
                bddGraphExplorer = BDDGraphExplorer.of(graphGameBDD);
            }
            JsonElement graphGame = bddGraphExplorer.getVisibleGraph();

            this.bddGraphsOfApts.put(canonicalApt, bddGraphExplorer);

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("graphGameBDD", graphGame);
            responseJson.add("petriGame", PetriNetD3.of(petriGame.getPetriGame()));
            responseJson.addProperty("canonicalApt", canonicalApt);
            return responseJson.toString();
        });

        post("/getListOfAvailableBDDGraphs", (req, res) -> {
            // Return a list containing the canonical APT representation of each PetriGame for
            // which a BDDGraph has been calculated.
            // These APT strings can be used as keys to access the BDDGraphs using getBDDGraph.
            JsonArray result = new JsonArray();
            for (String aptOfPetriGame : this.bddGraphsOfApts.keySet()) {
                result.add(aptOfPetriGame);
            }

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("apts", result);
            return responseJson.toString();
        });

        // Given the canonical APT representation of a Petri Game, return the current view
        // of the BDDGraph that has been calculated for it, if one is present.
        post("/getBDDGraph", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();

            if (!this.bddGraphsOfApts.containsKey(canonicalApt)) {
                return errorResponse("No BDDGraph has been calculated yet for the Petri " +
                        "Game with the given APT representation: \n" + canonicalApt);
            }
            BDDGraphExplorer bddGraphExplorer = this.bddGraphsOfApts.get(canonicalApt);

            JsonElement bddGraph = bddGraphExplorer.getVisibleGraph();
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("bddGraph", bddGraph);
            return responseJson.toString();
        });

        post("/toggleGraphGameBDDNodePostset", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();
            int stateId = body.getAsJsonObject().get("stateId").getAsInt();

            if (!this.bddGraphsOfApts.containsKey(canonicalApt)) {
                return errorResponse("There is no Graph Game BDD yet for that APT input");
            }
            BDDGraphExplorer bddGraphExplorer = this.bddGraphsOfApts.get(canonicalApt);
            bddGraphExplorer.toggleStatePostset(stateId);

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("graphGameBDD", bddGraphExplorer.getVisibleGraph());
            return responseJson.toString();
        });

        post("/toggleGraphGameBDDNodePreset", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();
            int stateId = body.getAsJsonObject().get("stateId").getAsInt();

            if (!this.bddGraphsOfApts.containsKey(canonicalApt)) {
                return errorResponse("There is no Graph Game BDD yet for that APT input");
            }
            BDDGraphExplorer bddGraphExplorer = this.bddGraphsOfApts.get(canonicalApt);
            bddGraphExplorer.toggleStatePreset(stateId);

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("graphGameBDD", bddGraphExplorer.getVisibleGraph());
            return responseJson.toString();
        });

        post("/savePetriGameAsAPT", (req, res) -> {
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
        });

        post("/insertPlace", (req, res) -> {
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
        });

        post("/deleteNode", (req, res) -> {
            JsonObject body = parser.parse(req.body()).getAsJsonObject();
            String gameId = body.get("petriGameId").getAsString();
            String nodeId = body.get("nodeId").getAsString();

            PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
            PetriGame petriGame = petriGameAndMore.getPetriGame();

            petriGame.removeNode(nodeId);

            JsonElement petriGameClient = PetriNetD3.of(petriGame);
            return successResponse(petriGameClient);
        });

        post("/renameNode", (req, res) -> {
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
        });

        post("/toggleEnvironmentPlace", (req, res) -> {
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
        });

        post("/toggleIsInitialTokenFlow", (req, res) -> {
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
        });

        post("/setInitialToken", (req, res) -> {
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
        });

        post("/setWinningCondition", (req, res) -> {
            JsonObject body = parser.parse(req.body()).getAsJsonObject();
            String gameId = body.get("petriGameId").getAsString();
            String winningCondition = body.get("winningCondition").getAsString();

            PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
            PetriGame petriGame = petriGameAndMore.getPetriGame();
            Condition.Objective objective = Condition.Objective.valueOf(winningCondition);
            PNWTTools.setConditionAnnotation(petriGame, objective);

            JsonElement petriGameClient = PetriNetD3.of(petriGame);
            return successResponse(petriGameClient);
        });
        post("/createFlow", (req, res) -> {
            JsonObject body = parser.parse(req.body()).getAsJsonObject();
            String gameId = body.get("petriGameId").getAsString();
            String source = body.get("source").getAsString();
            String destination = body.get("destination").getAsString();
            PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
            PetriGame petriGame = petriGameAndMore.getPetriGame();

            petriGame.createFlow(source, destination);
            JsonElement petriGameClient = PetriNetD3.of(petriGame);

            return successResponse(petriGameClient);
        });

        post("/deleteFlow", (req, res) -> {
            JsonObject body = parser.parse(req.body()).getAsJsonObject();
            String gameId = body.get("petriGameId").getAsString();
            String source = body.get("sourceId").getAsString();
            String target = body.get("targetId").getAsString();
            PetriGameAndMore petriGameAndMore = getPetriGame(gameId);
            PetriGame petriGame = petriGameAndMore.getPetriGame();

            petriGame.removeFlow(source, target);

            JsonElement petriGameClient = PetriNetD3.of(petriGame);
            return successResponse(petriGameClient);
        });

        post("/createTokenFlow", (req, res) -> {
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
        });

        post("/checkLtlFormula", (req, res) -> {
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
        });

        post("/getModelCheckingNet", (req, res) -> {
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

        });

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
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "error");
        responseJson.addProperty("message", reason);
        return responseJson.toString();
    }
}
