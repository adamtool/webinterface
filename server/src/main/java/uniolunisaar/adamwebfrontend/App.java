package uniolunisaar.adamwebfrontend;
/**
 * Created by Ann on 11.09.2017.
 */

import static spark.Spark.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.util.AdamExtensions;
import uniolunisaar.adam.tools.Logger;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class App {
    public static void main(String[] args) {
        // Whenever we load a PetriGame from APT, we put it into this hashmap.  The client refers to it via a uuid.
        final Map<String, PetriGameAndMore> petriGamesReadFromApt = new ConcurrentHashMap<>();
        final Gson gson = new Gson();
        final JsonParser parser = new JsonParser();

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

            JsonElement petriNetD3Json = PetriNetD3.of(petriGame.getNet(), petriGame.getNet().getNodes());
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

            PetriGameAndMore petriGame = petriGamesReadFromApt.get(petriGameId);

            System.out.println("Is there a winning strategy for PetriGame id#" + petriGameId + "?");
            boolean existsWinningStrategy = petriGame.calculateExistsWinningStrategy();

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.addProperty("result", existsWinningStrategy);
            return responseJson.toString();
        });

        post("/getStrategyBDD", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();

            PetriGameAndMore petriGame = petriGamesReadFromApt.get(petriGameId);
            System.out.println("Calculating strategy BDD for PetriGame id#" + petriGameId);
            JsonElement strategyBDDJson = petriGame.calculateStrategyBDD();

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("strategyBDD", strategyBDDJson);
            return responseJson.toString();
        });
        exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "error");
            String exceptionName = exception.getClass().getSimpleName();
            String exceptionAsString = exceptionName + ": " + exception.getMessage();
            responseJson.addProperty("message", exceptionAsString);

            String responseBody = responseJson.toString();
            response.body(responseBody);
        });

        // TODO change this so it doesn't calculate the same bdd a bunch of times just because you click the button more than once.
        // Right now you can crash the server just by sending a bunch of requests to this endpoint
        post("/getGraphStrategyBDD", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();

            PetriGameAndMore petriGame = petriGamesReadFromApt.get(petriGameId);
            System.out.println("Getting graph strategy BDD for PetriGame id#" + petriGameId);
            JsonElement bddGraph = petriGame.calculateGraphStrategyBDD();

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("graphStrategyBDD", bddGraph);
            return responseJson.toString();
        });

        post("/getGraphGameBDD", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();

            PetriGameAndMore petriGame = petriGamesReadFromApt.get(petriGameId);
            System.out.println("Getting graph game BDD for PetriGame id#" + petriGameId);
            JsonElement graphGame = petriGame.calculateGraphGameBDD();

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("graphGameBDD", graphGame);
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
            PetriGameAndMore petriGameAndMore = petriGamesReadFromApt.get(petriGameId);
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

            PetriGameAndMore petriGameAndMore = petriGamesReadFromApt.get(petriGameId);
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

            PetriGameAndMore petriGameAndMore = petriGamesReadFromApt.get(petriGameId);
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

            PetriGameAndMore petriGameAndMore = petriGamesReadFromApt.get(gameId);
            PetriGame petriGame = petriGameAndMore.getPetriGame();
            PetriNet net = petriGame.getNet();

            Place place = net.createPlace();
            AdamExtensions.setXCoord(place, x);
            AdamExtensions.setYCoord(place, y);

            JsonElement petriGameClient = PetriNetD3.of(petriGame.getNet(), new HashSet<>(Collections.singletonList(place)));

            return successResponse(petriGameClient);
        });
//        post("/renameNode", (req, res) -> {
//
//            if (net.containsNode(nodeId)) {
//                return errorResponse("A node with the id '" + nodeId + "' already exists in the petri game.");
//            }
//        })
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
        responseJson.addProperty("reason", reason);
        return responseJson.toString();
    }
}
