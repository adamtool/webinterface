package uniolunisaar.adamwebfrontend;

import spark.Request;
import spark.Response;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

/**
 * Represents a route handler that operates upon a net in the editor.
 * It could be a PetriGame or just a plain PetriNetWithTransits.
 * Most of the methods used to edit nets are applicable to both classes.
 * TODO 293 refactor?
 */
@FunctionalInterface
public interface EditorNetRoute {
    Object handle(Request var1, Response var2, PetriNetWithTransits petriNet) throws Exception;
}
