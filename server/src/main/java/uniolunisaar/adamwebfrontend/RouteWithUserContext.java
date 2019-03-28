package uniolunisaar.adamwebfrontend;

import spark.Request;
import spark.Response;

@FunctionalInterface
public interface RouteWithUserContext {
    Object handle(Request var1, Response var2, UserContext uc) throws Exception;
}
