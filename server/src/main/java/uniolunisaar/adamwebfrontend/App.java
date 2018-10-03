package uniolunisaar.adamwebfrontend;
/**
 * Created by Ann on 11.09.2017.
 */

import static spark.Spark.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrigame.PetriGameExtensionHandler;
import uniolunisaar.adam.ds.winningconditions.WinningCondition.Objective;
import uniolunisaar.adam.tools.Logger;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class App {
    // Whenever we load a PetriGame from APT, we put it into this hashmap.  The client refers to it via a uuid.
    private final Map<String, PetriGameAndMore> petriGamesReadFromApt = new ConcurrentHashMap<>();
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

            PetriGameAndMore petriGame = getPetriGame(petriGameId);
            System.out.println("Getting graph game BDD for PetriGame id#" + petriGameId);
            JsonElement graphGame = petriGame.calculateGraphGameBDD();

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("graphGameBDD", graphGame);
            responseJson.add("petriGame", PetriNetD3.of(petriGame.getPetriGame()));
            return responseJson.toString();
        });

        post("/toggleGraphGameBDDNodePostset", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();
            int stateId = body.getAsJsonObject().get("stateId").getAsInt();

            // TODO Instead of using Map.get() directly, write a helper method that will throw
            // TODO an informative exception in case the petri game ID is not found in the map.
            // TODO e.g. PetriGameNotFound
            PetriGameAndMore petriGameAndMore = getPetriGame(petriGameId);
            JsonElement graphGameBdd = petriGameAndMore.toggleGraphGameBDDNodePostset(stateId);

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("graphGameBDD", graphGameBdd);
            return responseJson.toString();
        });

        post("/toggleGraphGameBDDNodePreset", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();
            int stateId = body.getAsJsonObject().get("stateId").getAsInt();

            PetriGameAndMore petriGameAndMore = getPetriGame(petriGameId);
            JsonElement graphGameBdd = petriGameAndMore.toggleGraphGameBDDNodePreset(stateId);

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("graphGameBDD", graphGameBdd);
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
            boolean isInitialTokenFlow = petriGame.isInitialTokenflow(place);
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
            Objective objective = Objective.valueOf(winningCondition);
            PetriGameExtensionHandler.setWinningConditionAnnotation(petriGame, objective);

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
                petriGame.createTokenFlow(sourceId.get(), transitionId, postsetArray);
            } else {
                petriGame.createInitialTokenFlow(transitionId, postsetArray);
            }

            JsonElement petriGameClient = PetriNetD3.of(petriGame);

            return successResponse(petriGameClient);
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
