package uniolunisaar.adamwebfrontend;
/**
 * Created by Ann on 11.09.2017.
 */

import static spark.Spark.*;

import com.google.gson.*;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;
import uniolunisaar.adam.symbolic.bddapproach.solver.BDDSolverOptions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class App {
    public static void main(String[] args) {
        // Whenever we load a PetriGame from APT, we put it into this hashmap.  The client refers to it via a uuid.
        final Map<String, PetriGameAndMore> petriGamesReadFromApt = new ConcurrentHashMap<>();
        final Gson gson = new Gson();
        final JsonParser parser = new JsonParser();

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

            JsonElement petriNetD3Json = petriGameAndMore.getPetriGameClient();
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
            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.getBuffer().toString();
            responseJson.addProperty("message", exceptionAsString);

            String responseBody = responseJson.toString();
            response.body(responseBody);
        });

        // TODO Fix these methods and uncomment htem
        // TODO change this so it doesn't calculate the same bdd a bunch of times just because you click the button more than once.
        // Right now you can crash the server just by sending a bunch of requests to this endpoint
//        post("/getGraphStrategyBDD", (req, res) -> {
//            JsonElement body = parser.parse(req.body());
//            System.out.println("body: " + body.toString());
//            String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();
//
//            PetriGame petriGame = petriGamesReadFromApt.get(petriGameId);
//            System.out.println("Getting graph strategy BDD for PetriGame id#" + petriGameId);
//            BDDGraph bddGraph = Adam.getGraphStrategyBDD(petriGame.getNet(), new BDDSolverOptions());
//
//            BDDGraphD3 bddGraphD3 = BDDGraphD3.of(bddGraph);
//            JsonElement bddGraphJson = gson.toJsonTree(bddGraphD3);
//
//            JsonObject responseJson = new JsonObject();
//            responseJson.addProperty("status", "success");
//            responseJson.add("graphStrategyBDD", bddGraphJson);
//            return responseJson.toString();
//        });
//
//        post("/getGraphGameBDD", (req, res) -> {
//            JsonElement body = parser.parse(req.body());
//            System.out.println("body: " + body.toString());
//            String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();
//
//            PetriGame petriGame = petriGamesReadFromApt.get(petriGameId);
//            System.out.println("Getting graph game BDD for PetriGame id#" + petriGameId);
//            BDDGraph graphGame = Adam.getGraphGameBDD(petriGame.getNet(), new BDDSolverOptions());
//
//            BDDGraphD3 graphGameD3 = BDDGraphD3.of(graphGame);
//            JsonElement graphGameJson = gson.toJsonTree(graphGameD3);
//
//            JsonObject responseJson = new JsonObject();
//            responseJson.addProperty("status", "success");
//            responseJson.add("graphGameBDD", graphGameJson);
//            return responseJson.toString();
//        });
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
}
