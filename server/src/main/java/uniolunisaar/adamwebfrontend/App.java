package uniolunisaar.adamwebfrontend;
/**
 * Created by Ann on 11.09.2017.
 */

import static spark.Spark.*;

import com.google.gson.*;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.ds.petrigame.PetriGame;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class App {
    public static void main(String[] args) {
        // Whenever we load a PetriGame from APT, we put it into this hashmap.  The client refers to it via a uuid.
        final Map<String, PetriGame> petriGamesReadFromApt = new ConcurrentHashMap<>();
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
            petriGamesReadFromApt.put(petriGameUUID, petriGame);
            System.out.println("Generated petri game with ID " + petriGameUUID);
            PetriNetD3 petriNetD3 = PetriNetD3.of(petriGame.getNet(), petriGameUUID);

            JsonElement petriNetD3Json = gson.toJsonTree(petriNetD3);
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("graph", petriNetD3Json);
            return responseJson.toString();
        });

        post("/existsWinningStrategy", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String petriGameId = body.getAsJsonObject().get("petriGameId").getAsString();

            PetriGame petriGame = petriGamesReadFromApt.get(petriGameId);
            System.out.println("Is there a winning strategy for PetriGame id#" + petriGameId + "?");
            boolean existsStrategy = Adam.existsWinningStrategyBDD(petriGame.getNet());
            if (existsStrategy) {
                System.out.println("Yes, there is a strategy.");
            } else {
                System.out.println("No, there is not a strategy to solve the game.");
            }

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.addProperty("result", existsStrategy);
            return responseJson.toString();
        });

        exception(ParseException.class, (exception, request, response) -> {
            // Handle the exception here
            response.body("There was a parsing exception");
        });

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
