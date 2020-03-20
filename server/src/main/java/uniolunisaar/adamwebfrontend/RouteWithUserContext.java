package uniolunisaar.adamwebfrontend;

import spark.Request;
import spark.Response;
import uniolunisaar.adamwebfrontend.jobsystem.UserContext;

@FunctionalInterface
public interface RouteWithUserContext {
    Object handle(Request var1, Response var2, UserContext uc) throws Exception;
}
