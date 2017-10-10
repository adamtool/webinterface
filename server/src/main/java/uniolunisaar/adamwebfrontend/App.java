package uniolunisaar.adamwebfrontend;
/**
 * Created by Ann on 11.09.2017.
 */

import static spark.Spark.*;

import com.google.gson.*;
import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.util.AdamExtensions;

import java.util.ArrayList;
import java.util.List;

public class App {
    public static void main(String[] args) {
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        staticFiles.location("/static");
        enableCORS();

        get("/hello", (req, res) -> "Hello World");

        post("/convertAptToGraph", (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());
            String apt = body.getAsJsonObject().get("params").getAsJsonObject().get("apt").getAsString();
            PetriGame petriGame = Adam.getPetriGame(apt);
            JsonElement petriNetGraph = petriNetToJson(petriGame.getNet());

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("graph", petriNetGraph);
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

    private static JsonElement petriNetToJson(PetriNet net) {
        List<GraphLink> links = new ArrayList<>();
        List<GraphNode> nodes = new ArrayList<>();

        for (Place place : net.getPlaces()) {
            if (AdamExtensions.isEnviroment(place)) {
                nodes.add(GraphNode.envPlace(place.getId(), place.getId()));
            } else {
                nodes.add(GraphNode.sysPlace(place.getId(), place.getId()));
            }

            // TODO: Can this be done neater using a loop like "for (Flow edge : net.getEdges()) {...}"?
            for (Transition preTransition : place.getPreset()) {
                GraphLink link = new GraphLink(preTransition.getId(), place.getId());
                links.add(link);
            }
            for (Transition postTransition : place.getPostset()) {
                GraphLink link = new GraphLink(place.getId(), postTransition.getId());
                links.add(link);
            }
        }

        for (Transition transition : net.getTransitions()) {
            /*
            TODO Find out if it is relevant whether a transition is "system" or "environment"
            Manuel sent me a code skeleton that uses this loop, which does not make it possible
            to tell this difference.  So, for now, in this version, I am encoding all transitions
            as environment transitions, even though this is technically incorrect.
            (The client does not yet distinguish between the two, either.)
            */
            GraphNode transitionNode = GraphNode.envTransition(transition.getId(), transition.getLabel());
            nodes.add(transitionNode);
        }

//        for (Flow edge : net.getEdges()) {
//            Place p = edge.getPlace();
//            Transition t = edge.getTransition();
//            // In which direction does the edge run?  Is that piece of information not available here?
//        }

        return makePetriNet(nodes, links);
    }

    private static JsonObject makePetriNet(List<GraphNode> nodes, List<GraphLink> links) {
        JsonObject graph = new JsonObject();
        JsonArray linksJson = new JsonArray();
        JsonArray nodesJson = new JsonArray();
        for (GraphLink link : links) {
            linksJson.add(makeLink(link.getSource(), link.getTarget()));
        }
        for (GraphNode node : nodes) {
            nodesJson.add(makeNode(node.getId(), node.getLabel(), node.getType().toString(), -1, -1, -1, false));
        }
        graph.add("links", linksJson);
        graph.add("nodes", nodesJson);

        return graph;
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
