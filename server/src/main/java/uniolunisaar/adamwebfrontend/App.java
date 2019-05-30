package uniolunisaar.adamwebfrontend;
/**
 * Created by Ann on 11.09.2017.
 */

import static spark.Spark.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import spark.Request;
import spark.Response;
import spark.Route;
import uniol.apt.adt.pn.*;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.AdamModelChecker;
import uniolunisaar.adam.AdamSynthesizer;
import uniolunisaar.adam.ds.logics.ltl.flowltl.IRunFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.exceptions.pg.*;
import uniolunisaar.adam.exceptions.pnwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.PNWTTools;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;

public class App {
    private final Gson gson = new Gson();
    private final JsonParser parser = new JsonParser();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    // Map from clientside-generated browserUuid to browser-specific list of running Jobs
    private final Map<UUID, UserContext> userContextMap = new ConcurrentHashMap<>();
    // Whenever we load a PetriGame from APT, we put it into this hashmap with a
    // server-generated UUID as a key.
    private final Map<String, PetriGame> petriGamesReadFromApt = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new App().startServer();
    }

    // TODO serialize the counterexample as well if it is present
    private static JsonElement serializeModelCheckingResult(ModelCheckingResult result) {
        return new JsonPrimitive(result.getSatisfied().toString());
    }

    public void startServer() {
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

        post("/parseApt", this::handleParseApt);

        post("/calculateExistsWinningStrategy", handleQueueJob(
                this::calculateExistsWinningStrategy,
                JobType.EXISTS_WINNING_STRATEGY,
                JsonPrimitive::new
        ));

        post("/calculateStrategyBDD", handleQueueJob(
                this::calculateStrategyBDD,
                JobType.WINNING_STRATEGY,
                PetriNetD3::ofNetWithoutObjective
        ));

        post("/calculateGraphStrategyBDD", handleQueueJob(
                this::calculateGraphStrategyBDD,
                JobType.GRAPH_STRATEGY_BDD,
                BDDGraphD3::ofWholeBddGraph
        ));

        post("/calculateGraphGameBDD", handleQueueJob(
                this::calculateGraphGameBDD,
                JobType.GRAPH_GAME_BDD,
                BDDGraphExplorer::getVisibleGraph));

        // TODO rename to "checkLtlFormula" or something?
        post("/calculateModelCheckingResult", handleQueueJob(
                this::calculateModelCheckingResult,
                JobType.MODEL_CHECKING_RESULT,
                App::serializeModelCheckingResult));

        post("/getWinningStrategy", handleGetJobResult(
                JobType.WINNING_STRATEGY,
                PetriNetD3::ofNetWithoutObjective
        ));

        post("/getGraphStrategyBDD", handleGetJobResult(
                JobType.GRAPH_STRATEGY_BDD,
                BDDGraphD3::ofWholeBddGraph
        ));

        post("/getGraphGameBDD", handleGetJobResult(
                JobType.GRAPH_GAME_BDD,
                BDDGraphExplorer::getVisibleGraph));

        postWithUserContext("/getListOfJobs", this::handleGetListOfJobs);

        postWithUserContext("/cancelJob", this::handleCancelJob);

        postWithUserContext("/deleteJob", this::handleDeleteJob);

        postWithUserContext("/toggleGraphGameBDDNodePostset", this::handleToggleGraphGameBDDNodePostset);

        postWithUserContext("/toggleGraphGameBDDNodePreset", this::handleToggleGraphGameBDDNodePreset);

        postWithPetriGame("/getAptOfPetriGame", this::handleGetAptOfPetriGame);

        postWithPetriGame("/updateXYCoordinates", this::handleUpdateXYCoordinates);

        postWithPetriGame("/insertPlace", this::handleInsertPlace);

        postWithPetriGame("/deleteNode", this::handleDeleteNode);

        postWithPetriGame("/renameNode", this::handleRenameNode);

        postWithPetriGame("/toggleEnvironmentPlace", this::handleToggleEnvironmentPlace);

        postWithPetriGame("/toggleIsInitialTokenFlow", this::handleToggleIsInitialTokenFlow);

        postWithPetriGame("/setIsSpecial", this::handleSetIsSpecial);

        postWithPetriGame("/setInitialToken", this::handleSetInitialToken);

        postWithPetriGame("/setWinningCondition", this::handleSetWinningCondition);
        postWithPetriGame("/createFlow", this::handleCreateFlow);

        postWithPetriGame("/deleteFlow", this::handleDeleteFlow);

        postWithPetriGame("/createTokenFlow", this::handleCreateTokenFlow);

        postWithPetriGame("/parseLtlFormula", this::handleParseLtlFormula);

        postWithPetriGame("/getModelCheckingNet", this::handleGetModelCheckingNet);
        postWithPetriGame("/checkLtlFormula", this::handleCheckLtlFormula);

        postWithPetriGame("/fireTransition", this::handleFireTransition);

        exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();
            String exceptionAsString = exceptionToString(exception);
            String responseBody = errorResponse(exceptionAsString);
            response.body(responseBody);
        });
    }


    private static String exceptionToString(Exception exception) {
        String exceptionName = exception.getClass().getSimpleName();
        return exceptionName + ": " + exception.getMessage();
    }

    /**
     * Register a POST route where the browserUuid is automatically extracted from each request body
     * and the corresponding UserContext is retrieved.
     * This way, the handler function does not have to handle this frequently repeated operation.
     */
    private void postWithUserContext(String path, RouteWithUserContext handler) {
        post(path, (req, res) -> {
            JsonElement body = parser.parse(req.body());
            String browserUuidString = body.getAsJsonObject().get("browserUuid").getAsString();
            UUID browserUuid = UUID.fromString(browserUuidString);
            if (!userContextMap.containsKey(browserUuid)) {
                userContextMap.put(browserUuid, new UserContext());
            }
            UserContext uc = userContextMap.get(browserUuid);
            Object answer = handler.handle(req, res, uc);
            return answer;
        });
    }

    /**
     * Register a POST route that operates upon a PetriGame.
     * This wrapper handles the parsing of the petriGameId parameter from the request.
     */
    private void postWithPetriGame(String path, RouteWithPetriGame handler) {
        post(path, (req, res) -> {
            JsonElement body = parser.parse(req.body());
            String gameId = body.getAsJsonObject().get("petriGameId").getAsString();
            PetriGame pg = getPetriGame(gameId);
            Object answer = handler.handle(req, res, pg);
            return answer;
        });
    }

    private PetriGame getPetriGame(String uuid) {
        if (!petriGamesReadFromApt.containsKey(uuid)) {
            throw new IllegalArgumentException("We have no PetriGame with the given UUID.  " +
                    "You might see this error if the server has been restarted after you opened the " +
                    "web UI.");
        }
        return petriGamesReadFromApt.get(uuid);
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
        return successResponseObject(result).toString();
    }

    private static JsonObject successResponseObject(JsonElement result) {
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("result", result);
        return responseJson;
    }

    private static String errorResponse(String reason) {
        return errorResponseObject(reason).toString();
    }

    private static JsonObject errorResponseObject(String reason) {
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "error");
        responseJson.addProperty("message", reason);
        return responseJson;
    }

    /**
     * @return a list containing an entry for each pending/completed job
     * (e.g. Graph Game BDD, Get Winning Condition) on the server.
     */
    private Object handleGetListOfJobs(Request req, Response res, UserContext uc) {
        JsonArray result = uc.getJobList();
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("listings", result);
        return responseJson.toString();
    }

    private Object handleParseApt(Request req, Response res) throws CouldNotFindSuitableConditionException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String apt = body.getAsJsonObject().get("params").getAsJsonObject().get("apt").getAsString();
        PetriGame petriGame;
        try {
            petriGame = AdamSynthesizer.getPetriGame(apt);
            // TODO Use this to parse model checking APT
            //            AdamModelChecker.getPetriNetWithTransits()
        } catch (ParseException | NotSupportedGameException | IOException | CouldNotFindSuitableConditionException | CouldNotCalculateException e) {
            JsonObject errorResponse =
                    errorResponseObject(e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e instanceof ParseException) {
                Pair<Integer, Integer> errorLocation =
                        Tools.getErrorLocation((ParseException) e);
                errorResponse.addProperty("lineNumber", errorLocation.getFirst());
                errorResponse.addProperty("columnNumber", errorLocation.getSecond());
            }
            return errorResponse;
        }

        String petriGameUUID = UUID.randomUUID().toString();
        petriGamesReadFromApt.put(petriGameUUID, petriGame);
        System.out.println("Generated petri game with ID " + petriGameUUID);

        JsonElement petriNetD3Json =
                PetriNetD3.ofPetriGameWithXYCoordinates(petriGame, petriGame.getNodes(), true);
        JsonElement petriGameClient = PetriGameD3.of(petriNetD3Json, petriGameUUID);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("graph", petriGameClient);
        return responseJson.toString();
    }

    private Job<Boolean> calculateExistsWinningStrategy(PetriGame petriGame,
                                                        JsonObject params) {
        return new Job<>(() -> {
            boolean existsWinningStrategy = AdamSynthesizer.existsWinningStrategyBDD(petriGame);
            return existsWinningStrategy;
        }, petriGame.getName());
    }

    private Job<PetriGame> calculateStrategyBDD(PetriGame petriGame,
                                                JsonObject params) {
        return new Job<>(() -> {
            PetriGame strategyBDD = AdamSynthesizer.getStrategyBDD(petriGame);
            PetriGameTools.removeXAndYCoordinates(strategyBDD);
            return strategyBDD;
        }, petriGame.getName());
    }

    private Job<BDDGraph> calculateGraphStrategyBDD(PetriGame petriGame,
                                                    JsonObject params) {
        return new Job<>(() -> {
            BDDGraph graphStrategyBDD = AdamSynthesizer.getGraphStrategyBDD(petriGame);
            return graphStrategyBDD;
        }, petriGame.getName());
    }

    private Job<BDDGraphExplorer> calculateGraphGameBDD(PetriGame petriGame,
                                                        JsonObject params) {
        // If there is an invalid condition annotation, we should throw an error right away instead
        // of waiting until the job gets started (which might take a while if there is a
        // queue).
        try {
            Adam.getCondition(petriGame);
        } catch (CouldNotFindSuitableConditionException e) {
            throw new IllegalArgumentException(exceptionToString(e));
        }

        boolean shouldSolveStepwise = params.get("incremental").getAsBoolean();
        return new Job<>(() -> {
            if (shouldSolveStepwise) {
                BDDGraphExplorerStepwise bddGraphExplorerStepwise =
                        new BDDGraphExplorerStepwise(petriGame);
                return bddGraphExplorerStepwise;
            } else {
                BDDGraph graphGameBDD = AdamSynthesizer.getGraphGameBDD(petriGame);
                return BDDGraphExplorerCompleteGraph.of(graphGameBDD);
            }
        }, petriGame.getName());
    }

    /**
     * Create a Route that allows queueing a job to calculate some attribute of a Petri Game.
     * E.g. What is the winning strategy?  What does the "graph game BDD" look like?
     * What does the model checking net look like?
     * <p>
     * When this route is called, we expect the client to give us two things: A Petri Game's UUID
     * and a browser/user-session UUID.
     * We retrieve the corresponding Petri Game and queue up the requested job.
     *
     * @param jobFactory         A function that creates the Job that should be run
     * @param jobType            The result type of the job, used to save it in a
     *                           corresponding Map
     * @param serializerFunction A function that will serialize the result of the job so it
     *                           can be sent to the client.
     * @param <T>                The result type of the job
     * @return A Sparkjava Route
     */
    private <T> Route handleQueueJob(JobFactory<T> jobFactory,
                                     JobType jobType,
                                     SerializerFunction<T> serializerFunction) {
        return (req, res) -> {
            // Read request parameters.  Get the Petri Game that should be operated upon and the
            // UserContext of the client making the request.
            JsonObject requestBody = parser.parse(req.body()).getAsJsonObject();
            String gameId = requestBody.get("petriGameId").getAsString();
            JsonObject jobParams = requestBody.get("params").getAsJsonObject();
            // This throws an exception if the Petri Game's not there
            PetriGame petriGame = getPetriGame(gameId);

            // If there isn't a UserContext object for the given browserUuid, create one.
            String browserUuidString = requestBody.get("browserUuid").getAsString();
            UUID browserUuid = UUID.fromString(browserUuidString);
            if (!userContextMap.containsKey(browserUuid)) {
                userContextMap.put(browserUuid, new UserContext());
            }
            UserContext userContext = userContextMap.get(browserUuid);

            // Check if this job has already been requested
            String canonicalApt = Adam.getAPT(petriGame);
            JobKey jobKey = new JobKey(canonicalApt, jobParams);
            Map<JobKey, Job<T>> jobMap =
                    (Map<JobKey, Job<T>>) userContext.getJobMap(jobType);
            if (jobMap.containsKey(jobKey)) {
                return errorResponse("There is already a Job of " +
                        "\"" + jobType.toString() + "\" " +
                        "for the given Petri Game. Its status: " +
                        jobMap.get(jobKey).getStatus());
            }

            // Create the job and queue it up
            Job<T> job = jobFactory.createJob(
                    petriGame,
                    jobParams);
            jobMap.put(jobKey, job);
            job.queue(executorService);

            // If the job runs and completes immediately, send the result to the client.
            // Otherwise, let them know that the job has been queued.
            return tryToGetResultWithinFiveSeconds(
                    job,
                    serializerFunction,
                    jobKey,
                    petriGame
            );
        };
    }

    /**
     * When a user queues a job, the job might finish quickly, or it might take a
     * while.
     * If it's fast, then we want the UI to immediately open the result.
     * Otherwise, a message should just be shown to let the user know that the job has been added
     * to the job queue.
     * This method is meant to handle that type of response.
     *
     * @param job              the job that has been queued just now
     * @param resultSerializer A function to serialize the result of the job into JSON
     *                         for the client to consume
     * @param jobKey     A key containing all parameters needed to uniquely identify the job
     * @param petriGame        The Petri Game that is being analyzed
     * @param <T>              The result type of the job
     * @return A JSON response with the result, if it is ready within five seconds.
     * Otherwise, just a message that the job is still being calculated.
     * @throws Exception if the serializer function throws an exception.
     */
    private static <T> Object tryToGetResultWithinFiveSeconds(Job<T> job,
                                                              SerializerFunction<T> resultSerializer,
                                                              JobKey jobKey,
                                                              PetriGame petriGame) throws Exception {
        try {
            T result = job.getResult(5, TimeUnit.SECONDS);
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.addProperty("message", "The job is finished.");
            responseJson.add("jobKey", new Gson().toJsonTree(jobKey));
            responseJson.addProperty("jobComplete", true);
            responseJson.add("result", resultSerializer.apply(result));
            responseJson.add("petriGame", PetriNetD3.ofPetriGame(petriGame));
            return responseJson.toString();
        } catch (TimeoutException e) {
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.addProperty("message", "The job is taking more " +
                    "than five seconds.  It will run in the background.");
            responseJson.add("jobKey", new Gson().toJsonTree(jobKey));
            responseJson.addProperty("jobComplete", false);
            return responseJson.toString();
        } catch (CancellationException e) {
            return errorResponse("The job was canceled.");
        } catch (InterruptedException e) {
            return errorResponse("The job was interrupted.");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            return errorResponse("The job failed with an exception: " +
                    cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
    }

    /**
     * Given the canonical APT representation of a Petri Game and a type of job job,
     * return the result of the job, if one has been queued and has completed successfully.
     */
    private <T> Route handleGetJobResult(JobType jobType,
                                         SerializerFunction<T> serializerFunction) {
        return (req, res) -> {
            JsonElement body = parser.parse(req.body());
            System.out.println("body: " + body.toString());

            // Deserialize the JobKey
            Type type = new TypeToken<JobKey>() {
            }.getType();
            JsonElement jobKeyJson = body.getAsJsonObject().get("jobKey");
            JobKey jobKey = gson.fromJson(jobKeyJson, type);

            // If there isn't a UserContext object for the given browserUuid, create one.
            String browserUuidString = body.getAsJsonObject().get("browserUuid").getAsString();
            UUID browserUuid = UUID.fromString(browserUuidString);
            if (!userContextMap.containsKey(browserUuid)) {
                userContextMap.put(browserUuid, new UserContext());
            }
            UserContext userContext = userContextMap.get(browserUuid);

            Map<JobKey, Job<T>> jobMap =
                    (Map<JobKey, Job<T>>) userContext.getJobMap(jobType);
            if (!jobMap.containsKey(jobKey)) {
                return errorResponse("The requested job was not found.");
            }

            Job<T> job = jobMap.get(jobKey);
            if (!job.isFinished()) {
                return errorResponse("The requested job is not yet finished.  " +
                        "Its status: " + job.getStatus());
            }

            T result;
            try {
                result = job.getResult();
            } catch (InterruptedException e) {
                return errorResponse("The requested job got canceled.");
            } catch (ExecutionException e) {
                return errorResponse(
                        "The requested job failed with the following exception: " +
                                e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }

            JsonElement resultJson = serializerFunction.apply(result);
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("status", "success");
            responseJson.add("result", resultJson);
            return responseJson.toString();
        };
    }

    private Object handleCancelJob(Request req, Response res, UserContext uc) {
        JsonElement body = parser.parse(req.body());
        String typeString = body.getAsJsonObject().get("type").getAsString();
        JobType jobType = JobType.valueOf(typeString);

        Type t = new TypeToken<JobKey>() {
        }.getType();
        JsonElement jobKeyJson = body.getAsJsonObject().get("jobKey");
        JobKey jobKey = gson.fromJson(jobKeyJson, t);

        Map<JobKey, ? extends Job> jobMap = uc.getJobMap(jobType);
        if (!jobMap.containsKey(jobKey)) {
            return errorResponse("The requested job was not found.");
        }
        Job job = jobMap.get(jobKey);
        job.cancel();
        return successResponse(new JsonPrimitive(true));
    }

    private Object handleDeleteJob(Request req, Response res, UserContext uc) {
        JsonElement body = parser.parse(req.body());
        String typeString = body.getAsJsonObject().get("type").getAsString();
        JobType jobType = JobType.valueOf(typeString);

        Type t = new TypeToken<JobKey>() {
        }.getType();
        JsonElement jobKeyJson = body.getAsJsonObject().get("jobKey");
        JobKey jobKey = gson.fromJson(jobKeyJson, t);

        Map<JobKey, ? extends Job> jobMap = uc.getJobMap(jobType);
        if (!jobMap.containsKey(jobKey)) {
            return errorResponse("The requested job was not found.");
        }
        Job job = jobMap.get(jobKey);
        try {
            job.cancel();
        } catch (UnsupportedOperationException e) {
            // We don't care if the job is not eligible to be canceled.
            // We just want to cancel it if that's possible.
        }
        jobMap.remove(jobKey);
        return successResponse(new JsonPrimitive(true));
    }

    private Object handleToggleGraphGameBDDNodePostset(Request req, Response res, UserContext uc)
            throws ExecutionException, InterruptedException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();
        int stateId = body.getAsJsonObject().get("stateId").getAsInt();

        if (!uc.graphGameBddsOfApts.containsKey(canonicalApt)) {
            return errorResponse("There is no Graph Game BDD yet for that APT input");
        }

        Job<BDDGraphExplorer> job = uc.graphGameBddsOfApts.get(canonicalApt);
        if (!job.isFinished()) {
            return errorResponse("The job for that Graph Game BDD is not yet finished" +
                    ".  Its status: " + job.getStatus());
        }
        BDDGraphExplorer bddGraphExplorer = job.getResult();
        bddGraphExplorer.toggleStatePostset(stateId);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("graphGameBDD", bddGraphExplorer.getVisibleGraph());
        return responseJson.toString();
    }

    private Object handleToggleGraphGameBDDNodePreset(Request req, Response res, UserContext uc)
            throws ExecutionException, InterruptedException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());
        String canonicalApt = body.getAsJsonObject().get("canonicalApt").getAsString();
        int stateId = body.getAsJsonObject().get("stateId").getAsInt();

        if (!uc.graphGameBddsOfApts.containsKey(canonicalApt)) {
            return errorResponse("There is no Graph Game BDD yet for that APT input");
        }
        Job<BDDGraphExplorer> job = uc.graphGameBddsOfApts.get(canonicalApt);
        if (!job.isFinished()) {
            return errorResponse("The job for that Graph Game BDD is not yet finished" +
                    ".  Its status: " + job.getStatus());
        }
        BDDGraphExplorer bddGraphExplorer = job.getResult();
        bddGraphExplorer.toggleStatePreset(stateId);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("graphGameBDD", bddGraphExplorer.getVisibleGraph());
        return responseJson.toString();
    }

    // Get the APT representation of a given Petri Game
    private Object handleGetAptOfPetriGame(Request req, Response res, PetriGame petriGame)
            throws RenderException {
        JsonElement body = parser.parse(req.body());
        String apt = Adam.getAPT(petriGame);
        JsonElement aptJson = new JsonPrimitive(apt);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("apt", aptJson);
        return responseJson.toString();
    }

    // Update the X/Y coordinates of multiple nodes.  Send them back to the client to confirm it
    // worked
    private Object handleUpdateXYCoordinates(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonElement body = parser.parse(req.body());
        JsonObject nodesXYCoordinatesJson =
                body.getAsJsonObject().get("nodeXYCoordinateAnnotations").getAsJsonObject();
        Type type = new TypeToken<Map<String, NodePosition>>() {
        }.getType();
        Map<String, NodePosition> nodePositions = gson.fromJson(nodesXYCoordinatesJson, type);
        PetriGameTools.saveXYCoordinates(petriGame, nodePositions);

        JsonElement petriGameClient = PetriNetD3.ofPetriGameWithXYCoordinates(
                petriGame, new HashSet<>(petriGame.getNodes()), true);

        return successResponse(petriGameClient);
    }

    private Object handleInsertPlace(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        double x = body.get("x").getAsDouble();
        double y = body.get("y").getAsDouble();
        String nodeType = body.get("nodeType").getAsString();
        GraphNodeType graphNodeType = GraphNodeType.valueOf(nodeType);

        Node node = null;
        switch (graphNodeType) {
            case SYSPLACE:
                node = petriGame.createPlace();
                break;
            case ENVPLACE:
                Place place = petriGame.createPlace();
                petriGame.setEnvironment(place);
                node = place;
                break;
            case TRANSITION:
                node = petriGame.createTransition();
                break;
            case GRAPH_STRATEGY_BDD_STATE:
                return errorResponse("You can't insert a GRAPH_STRATEGY_BDD_STATE into a Petri Game.");
        }
        petriGame.setXCoord(node, x);
        petriGame.setYCoord(node, y);

        JsonElement petriGameClient = PetriNetD3.ofPetriGameWithXYCoordinates(
                petriGame, new HashSet<>(Collections.singletonList(node)), true);

        return successResponse(petriGameClient);
    }

    private Object handleDeleteNode(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();

        petriGame.removeNode(nodeId);

        JsonElement petriGameClient = PetriNetD3.ofPetriGame(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleRenameNode(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeIdOld = body.get("nodeIdOld").getAsString();
        String nodeIdNew = body.get("nodeIdNew").getAsString();

        Node oldNode = petriGame.getNode(nodeIdOld);
        petriGame.rename(oldNode, nodeIdNew);

        JsonElement petriGameClient = PetriNetD3.ofPetriGame(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleToggleEnvironmentPlace(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();

        Place place = petriGame.getPlace(nodeId);
        boolean environment = petriGame.isEnvironment(place);
        if (environment) {
            petriGame.setSystem(place);
        } else {
            petriGame.setEnvironment(place);
        }

        JsonElement petriGameClient = PetriNetD3.ofPetriGame(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleToggleIsInitialTokenFlow(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();

        Place place = petriGame.getPlace(nodeId);
        boolean isInitialTokenFlow = petriGame.isInitialTransit(place);
        if (isInitialTokenFlow) {
            petriGame.removeInitialTokenflow(place);
        } else {
            petriGame.setInitialTokenflow(place);
        }

        JsonElement petriGameClient = PetriNetD3.ofPetriGame(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleSetInitialToken(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();
        int tokens = body.get("tokens").getAsInt();

        Place place = petriGame.getPlace(nodeId);
        place.setInitialToken(tokens);

        JsonElement petriGameClient = PetriNetD3.ofPetriGame(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleSetIsSpecial(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();
        boolean special = body.get("newSpecialValue").getAsBoolean();

        Place place = petriGame.getPlace(nodeId);
        Condition.Objective condition = Adam.getCondition(petriGame);
        if (special) {
            petriGame.setSpecial(place, condition);
        } else {
            petriGame.removeSpecial(place, condition);
        }

        JsonElement petriGameClient = PetriNetD3.ofPetriGame(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleSetWinningCondition(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String winningCondition = body.get("winningCondition").getAsString();

        Condition.Objective objective = Condition.Objective.valueOf(winningCondition);
        PNWTTools.setConditionAnnotation(petriGame, objective);

        JsonElement petriGameClient = PetriNetD3.ofPetriGame(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleCreateFlow(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String source = body.get("source").getAsString();
        String destination = body.get("destination").getAsString();

        petriGame.createFlow(source, destination);
        JsonElement petriGameClient = PetriNetD3.ofPetriGame(petriGame);

        return successResponse(petriGameClient);
    }

    private Object handleDeleteFlow(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String source = body.get("sourceId").getAsString();
        String target = body.get("targetId").getAsString();

        petriGame.removeFlow(source, target);

        JsonElement petriGameClient = PetriNetD3.ofPetriGame(petriGame);
        return successResponse(petriGameClient);
    }

    private Object handleCreateTokenFlow(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        Optional<String> sourceId = body.has("source") ?
                Optional.of(body.get("source").getAsString()) :
                Optional.empty();
        String transitionId = body.get("transition").getAsString();
        JsonArray postsetJson = body.get("postset").getAsJsonArray();
        List<String> postsetIds = new ArrayList<>();
        postsetJson.forEach(jsonElement -> {
            postsetIds.add(jsonElement.getAsString());
        });

        // Create flows if they don't already exist
        Transition transition = petriGame.getTransition(transitionId);
        sourceId.ifPresent(id -> {
            Place source = petriGame.getPlace(id);
            if (!transition.getPreset().contains(source)) {
                petriGame.createFlow(source, transition);
            }
        });
        for (String postsetId : postsetIds) {
            Place postPlace = petriGame.getPlace(postsetId);
            if (!transition.getPostset().contains(postPlace)) {
                petriGame.createFlow(transition, postPlace);
            }
        }

        // Create a token flow.  It is an initial token flow if if has no source Place.
        String[] postsetArray = postsetIds.toArray(new String[postsetIds.size()]);
        if (sourceId.isPresent()) {
            petriGame.createTransit(sourceId.get(), transitionId, postsetArray);
        } else {
            petriGame.createInitialTransit(transitionId, postsetArray);
        }

        JsonElement petriGameClient = PetriNetD3.ofPetriGame(petriGame);

        return successResponse(petriGameClient);
    }

    private Object handleParseLtlFormula(Request req, Response res, PetriGame petriGame) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String formula = body.get("formula").getAsString();

        try {
            IRunFormula iRunFormula = AdamModelChecker.parseFlowLTLFormula(petriGame, formula);
            return successResponse(new JsonPrimitive(true));
        } catch (ParseException e) {
            Throwable cause = e.getCause();
            System.out.println(cause.getMessage());
            return errorResponse(e.getMessage() + "\n" + cause.getMessage());
        }
    }

    private Object handleGetModelCheckingNet(Request req, Response res, PetriGame petriGame) throws NotSupportedGameException, InterruptedException, ParseException, IOException, ExternalToolException, NotConvertableException, ProcessNotStartedException, CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String formula = body.get("formula").getAsString();

        RunFormula runFormula = AdamModelChecker.parseFlowLTLFormula(petriGame, formula);

        PetriNet modelCheckingNet = AdamModelChecker.getModelCheckingNet(petriGame, runFormula, false);

        return successResponse(PetriNetD3.ofPetriGame(new PetriGame(modelCheckingNet)));
    }

    private Job<ModelCheckingResult> calculateModelCheckingResult(
            PetriGame petriGame,
            JsonObject params) {
        String formula = params.get("formula").getAsString();
        return new Job<>(() -> {
            RunFormula runFormula = AdamModelChecker.parseFlowLTLFormula(petriGame, formula);
            ModelCheckerFlowLTL modelCheckerFlowLTL = new ModelCheckerFlowLTL();
            ModelCheckingResult result = AdamModelChecker.checkFlowLTLFormula(petriGame, modelCheckerFlowLTL, runFormula, "/tmp/", null);
            return result;
        }, petriGame.getName());
    }

    // TODO delete (replace with job system)
    private Object handleCheckLtlFormula(Request req, Response res, PetriGame petriGame) throws ParseException, InterruptedException, NotConvertableException, ExternalToolException, ProcessNotStartedException, IOException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String formula = body.get("formula").getAsString();

        System.out.println("Checking flow LTL formula");
        RunFormula runFormula = AdamModelChecker.parseFlowLTLFormula(petriGame, formula);
        ModelCheckerFlowLTL modelCheckerFlowLTL = new ModelCheckerFlowLTL();
        ModelCheckingResult result = AdamModelChecker.checkFlowLTLFormula(petriGame, modelCheckerFlowLTL, runFormula, "/tmp/", null);

        ModelCheckingResult.Satisfied satisfied = result.getSatisfied();
        switch (satisfied) {
            case TRUE:
                return successResponse(new JsonPrimitive(true));
            case FALSE:
                JsonObject response = successResponseObject(new JsonPrimitive(false));
                // TODO Serialize CounterExample and display it in the client
//                CounterExample cex = result.getCex();
//                response.addProperty("counterExample", );
                return response;
            case UNKNOWN:
                return errorResponse("The Model Checker returned the result 'unknown'");
            default:
                return errorResponse("Missing switch branch in handleCheckLtlFormula: " + satisfied);
        }
    }

    private Object handleFireTransition(Request req, Response res, PetriGame petriGame) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String transitionId = body.get("transitionId").getAsString();

        Transition transition = petriGame.getTransition(transitionId);
        Marking initialMarking = petriGame.getInitialMarking();
        Marking newInitialMarking = transition.fire(initialMarking);
        petriGame.setInitialMarking(newInitialMarking);
        return successResponse(PetriNetD3.ofPetriGame(petriGame));
    }
}
