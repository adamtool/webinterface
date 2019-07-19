package uniolunisaar.adamwebfrontend;

import spark.Request;
import spark.Response;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

/**
 * Represents a route handler that operates upon a PetriNetWithTransits
 */
@FunctionalInterface
public interface RouteWithPetriNetWithTransits {
    Object handle(Request var1, Response var2, PetriNetWithTransits petriNet) throws Exception;
}
