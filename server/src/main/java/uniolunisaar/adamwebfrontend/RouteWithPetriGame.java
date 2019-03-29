package uniolunisaar.adamwebfrontend;

import spark.Request;
import spark.Response;
import uniolunisaar.adam.ds.petrigame.PetriGame;

/**
 * Represents a route handler that operates upon a Petri Game
 */
@FunctionalInterface
public interface RouteWithPetriGame {
    Object handle(Request var1, Response var2, PetriGame pg) throws Exception;
}
