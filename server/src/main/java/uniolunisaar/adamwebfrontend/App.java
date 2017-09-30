package uniolunisaar.adamwebfrontend;
/**
 * Created by Ann on 11.09.2017.
 */

import static spark.Spark.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class App {
    public static void main(String[] args) {
        enableCORS();

        get("/hello", (req, res) -> "Hello World");

        get("/convertAptToGraph", (req, res) -> {
            // String apt = req.queryParams("apt");
            // (Here, the APT should be parsed to create a graph.)
            return mockResponse();
        });
        post("/convertAptToGraph", (req, res) -> mockResponse());

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

    private static String mockResponse() {
        JsonObject graph = new JsonObject();
        JsonArray links = new JsonArray();
        links.add(makeLink("1", "2"));
        links.add(makeLink("2", "3"));

        JsonArray nodes = new JsonArray();
        nodes.add(makeNode("1", "First place", "place", 3, 242, 311, true));
        nodes.add(makeNode("2", "A transition", "transition", -1, 201, 300, true));
        nodes.add(makeNode("3", "Second place", "place", 1, 154, 309, true));
        graph.add("links", links);
        graph.add("nodes", nodes);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("graph", graph);
        return responseJson.toString();
    }

    private static JsonObject makeNode(String id, String label, String type, int tokens, double x, double y, boolean isPositionFixed) {
        JsonObject node = new JsonObject();
        node.addProperty("id", id);
        node.addProperty("label", label);
        node.addProperty("type", type);
        node.addProperty("tokens", tokens);
        node.addProperty("x", x);
        node.addProperty("y", y);
        node.addProperty("isPositionFixed", isPositionFixed);
        return node;
    }

    private static JsonObject makeLink(String source, String target) {
        JsonObject link = new JsonObject();
        link.addProperty("source", source);
        link.addProperty("target", target);
        return link;
    }



}
