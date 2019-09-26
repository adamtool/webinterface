package uniolunisaar.adamwebfrontend;
/**
 * Created by Ann on 11.09.2017.
 */

import static spark.Spark.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import spark.Request;
import spark.Response;
import uniol.apt.adt.pn.*;
import uniol.apt.adt.pn.Flow;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.Adam;
import uniolunisaar.adam.AdamModelChecker;
import uniolunisaar.adam.AdamSynthesizer;
import uniolunisaar.adam.ds.logics.ltl.flowltl.IRunFormula;
import uniolunisaar.adam.ds.objectives.Condition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.pg.*;
import uniolunisaar.adam.exceptions.pnwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.PNWTTools;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;

public class App {
    private final Gson gson = new Gson();
    private final JsonParser parser = new JsonParser();

    // Map from clientside-generated browserUuid to browser-specific list of running Jobs
    private final Map<UUID, UserContext> userContextMap = new ConcurrentHashMap<>();

    // Whenever we load a PetriGame or PetriNetWithTransits (in the model checking case) from APT,
    // we put it into this hashmap with a server-generated UUID as a key.
    private final Map<String, PetriNetWithTransits> petriNets = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new App().startServer();
    }

    public void startServer() {
        webSocket("/log", LogWebSocket.class);

        staticFiles.location("/static");
        enableCORS();
        LogWebSocket.startPingThread();
        LogWebSocket.startMessageQueueThread();

        get("/hello", (req, res) -> "Hello World");

        post("/parseApt", this::handleParseApt);

        post("/queueJob", this::handleQueueJob);

        postWithUserContext("/getListOfJobs", this::handleGetListOfJobs);

        postWithUserContext("/cancelJob", this::handleCancelJob);

        postWithUserContext("/deleteJob", this::handleDeleteJob);

        postWithUserContext("/toggleGraphGameBDDNodePostset", this::handleToggleGraphGameBDDNodePostset);

        postWithUserContext("/toggleGraphGameBDDNodePreset", this::handleToggleGraphGameBDDNodePreset);

        postWithPetriNetWithTransits("/getAptOfPetriGame", this::handleGetAptOfPetriGame);

        postWithPetriNetWithTransits("/updateXYCoordinates", this::handleUpdateXYCoordinates);

        postWithPetriNetWithTransits("/insertPlace", this::handleInsertPlace);

        postWithPetriNetWithTransits("/deleteNode", this::handleDeleteNode);

        postWithPetriNetWithTransits("/renameNode", this::handleRenameNode);

        postWithPetriNetWithTransits("/toggleEnvironmentPlace", this::handleToggleEnvironmentPlace);

        postWithPetriNetWithTransits("/toggleIsInitialTransit", this::handleToggleIsInitialTransit);

        postWithPetriNetWithTransits("/setIsSpecial", this::handleSetIsSpecial);

        postWithPetriNetWithTransits("/setInitialToken", this::handleSetInitialToken);

        postWithPetriNetWithTransits("/setWinningCondition", this::handleSetWinningCondition);
        postWithPetriNetWithTransits("/createFlow", this::handleCreateFlow);

        postWithPetriNetWithTransits("/deleteFlow", this::handleDeleteFlow);

        postWithPetriNetWithTransits("/createTransit", this::handleCreateTransit);

        postWithPetriNetWithTransits("/parseLtlFormula", this::handleParseLtlFormula);

        postWithPetriNetWithTransits("/fireTransition", this::handleFireTransition);

        postWithPetriNetWithTransits("/setFairness", this::handleSetFairness);

        postWithPetriNetWithTransits("/setInhibitorArc", this::handleSetInhibitorArc);

        exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();
            String exceptionAsString = exceptionToString(exception);
            String responseBody = errorResponse(exceptionAsString);
            response.body(responseBody);
        });
    }

    public static String exceptionToString(Exception exception) {
        String exceptionName = exception.getClass().getSimpleName();
        return exceptionName + ": " + exception.getMessage();
    }

    public static PetriGame promoteToPetriGame(PetriNetWithTransits net) {
        if (!(net instanceof PetriGame)) {
            throw new IllegalArgumentException("The given net is not a PetriGame, but merely a PetriNetWithTransits.");
        }
        return (PetriGame) net;
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
                userContextMap.put(browserUuid, new UserContext(browserUuid));
            }
            UserContext uc = userContextMap.get(browserUuid);
            Object answer = handler.handle(req, res, uc);
            return answer;
        });
    }

    /**
     * Register a POST route that operates upon a PetriNetWithTransits.
     * This wrapper handles the parsing of the petriNetId parameter from the request.
     */
    private void postWithPetriNetWithTransits(String path, RouteWithPetriNetWithTransits handler) {
        post(path, (req, res) -> {
            JsonElement body = parser.parse(req.body());
            String netId = body.getAsJsonObject().get("petriNetId").getAsString();
            PetriNetWithTransits pg = getPetriNet(netId);
            Object answer = handler.handle(req, res, pg);
            return answer;
        });
    }

    private PetriNetWithTransits getPetriNet(String uuid) {
        if (!petriNets.containsKey(uuid)) {
            throw new IllegalArgumentException("We have no PG/PetriNetWithTransits with the given UUID.  " +
                    "You might see this error if the server has been restarted after you opened the " +
                    "web UI.");
        }
        return petriNets.get(uuid);
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
        JsonObject params = body.getAsJsonObject().get("params").getAsJsonObject();
        String apt = params.get("apt").getAsString();
        String netType = params.get("netType").getAsString();
        PetriNetWithTransits net;
        try {
            switch (netType) {
                case "petriNetWithTransits":
                    net = AdamModelChecker.getPetriNetWithTransits(apt);
                    break;
                case "petriGame":
                    net = AdamSynthesizer.getPetriGame(apt);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized net type: " + netType);
            }
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

        String netUuid = UUID.randomUUID().toString();
        petriNets.put(netUuid, net);
        System.out.println("Generated petri net with ID " + netUuid);

        JsonElement petriNetD3Json =
                PetriNetD3.ofPetriNetWithXYCoordinates(net, net.getNodes(), true);
        JsonElement serializedNet = PetriGameD3.of(petriNetD3Json, netUuid);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("graph", serializedNet);
        return responseJson.toString();
    }


    private Object handleQueueJob(Request req, Response res) throws RenderException {
        // Read request parameters.  Get the Petri Net that should be operated upon and the
        // UserContext of the client making the request.
        JsonObject requestBody = parser.parse(req.body()).getAsJsonObject();
        String netId = requestBody.get("petriNetId").getAsString();
        JsonObject jobParams = requestBody.get("params").getAsJsonObject();
        // This throws an exception if the Petri Net's not there
        PetriNetWithTransits net = getPetriNet(netId);

        String jobTypeString = requestBody.get("jobType").getAsString();
        JobType jobType = JobType.valueOf(jobTypeString);

        // If there isn't a UserContext object for the given browserUuid, create one.
        String browserUuidString = requestBody.get("browserUuid").getAsString();
        UUID browserUuid = UUID.fromString(browserUuidString);
        if (!userContextMap.containsKey(browserUuid)) {
            userContextMap.put(browserUuid, new UserContext(browserUuid));
        }
        UserContext userContext = userContextMap.get(browserUuid);

        PetriNetWithTransits netCopy = (net instanceof PetriGame) ? new PetriGame((PetriGame) net) : new PetriNetWithTransits(net);
        JobKey jobKey = new JobKey(
                PNWTTools.getAPT(netCopy, true, true),
                jobParams,
                jobType
        );
        // TODO refactor so the client just sends us APT?  (IE dont give the client
        //  a reference to a specific PetriGame object on the server)
        //  This would allow the client to simply send us a 'jobKey' by itself to queue a job.

        // Check if this job has already been requested
        if (userContext.hasJobWithKey(jobKey)) {
            Job existingJob = userContext.getJobFromKey(jobKey);
            switch (existingJob.getStatus()) {
                case NOT_STARTED:
                case QUEUED:
                case RUNNING:
                case COMPLETED: {
                    JsonObject response = errorResponseObject("An identical job has already been queued.  Its status: " +
                            userContext.getJobFromKey(jobKey).getStatus());
                    response.addProperty("errorType", "JOB_ALREADY_QUEUED");
                    response.add("jobKey", gson.toJsonTree(jobKey));
                    return response;
                }
                case FAILED:
                case CANCELING:
                case CANCELED:
                    // Delete the existing job so we can queue it again
                    userContext.deleteJobWithKey(jobKey);
            }
        }

        // Create the job and queue it up in the user's job queue
        Job job = jobType.makeJob(net, jobParams);
        userContext.queueJob(jobKey, job);

        /*
         When the job's status changes, a websocket message should be sent to the client,
         which contains the updated job list entry for this job.
         TODO Distinguish between different kinds of events (e.g. old status -> new status)
         in order to provide nice notifications about specific jobs
         */
        job.addObserver((Job ignored) -> {
            JsonObject message = new JsonObject();
            message.addProperty("type", "jobStatusChanged");
            message.add("jobListing", userContext.jobListEntry(job, jobKey));
            LogWebSocket.queueWebsocketMessage(browserUuid, message);
        });

        JsonObject jobListing = userContext.jobListEntry(job, jobKey);
        JsonObject responseJson = successResponseObject(jobListing);
        responseJson.addProperty("message", "The job has been queued.");
        return responseJson.toString();
    }

    private Object handleCancelJob(Request req, Response res, UserContext uc) {
        JsonElement body = parser.parse(req.body());

        Type t = new TypeToken<JobKey>() {
        }.getType();
        JsonElement jobKeyJson = body.getAsJsonObject().get("jobKey");
        JobKey jobKey = gson.fromJson(jobKeyJson, t);

        if (!uc.hasJobWithKey(jobKey)) {
            return errorResponse("The requested job was not found.");
        }
        Job job = uc.getJobFromKey(jobKey);
        job.cancel();
        return successResponse(new JsonPrimitive(true));
    }

    private Object handleDeleteJob(Request req, Response res, UserContext uc) {
        JsonElement body = parser.parse(req.body());

        Type t = new TypeToken<JobKey>() {
        }.getType();
        JsonElement jobKeyJson = body.getAsJsonObject().get("jobKey");
        JobKey jobKey = gson.fromJson(jobKeyJson, t);

        if (!uc.hasJobWithKey(jobKey)) {
            return errorResponse("The requested job was not found.");
        }
        Job job = uc.getJobFromKey(jobKey);
        if (job.getStatus() == JobStatus.CANCELING) {
            return errorResponse("That job is still in the process of being canceled.  " +
                    "You can't delete it until it has finished canceling.");
        }
        try {
            job.cancel();
        } catch (UnsupportedOperationException e) {
            // We don't care if the job is not eligible to be canceled.
            // We just want to cancel it if that's possible.
        }
        uc.deleteJobWithKey(jobKey);
        return successResponse(new JsonPrimitive(true));
    }

    private Object handleToggleGraphGameBDDNodePostset(Request req, Response res, UserContext uc)
            throws ExecutionException, InterruptedException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());

        Type type = new TypeToken<JobKey>() {
        }.getType();
        JsonElement jobKeyJson = body.getAsJsonObject().get("jobKey");
        JobKey jobKey = gson.fromJson(jobKeyJson, type);

        int stateId = body.getAsJsonObject().get("stateId").getAsInt();

        if (!uc.hasJobWithKey(jobKey)) {
            return errorResponse("The requested Graph Game BDD does not exist.");
        }

        Job<BDDGraphExplorer> job = uc.getGraphGameBDDJob(jobKey);
        if (!job.isFinished()) {
            return errorResponse("The job for that Graph Game BDD is not yet finished" +
                    ".  Its status: " + job.getStatus());
        }
        BDDGraphExplorer bddGraphExplorer = job.getResult();
        bddGraphExplorer.toggleStatePostset(stateId);
        job.fireJobStatusChanged();

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("graphGameBDD", bddGraphExplorer.getVisibleGraph());
        return responseJson.toString();
    }

    private Object handleToggleGraphGameBDDNodePreset(Request req, Response res, UserContext uc)
            throws ExecutionException, InterruptedException {
        JsonElement body = parser.parse(req.body());
        System.out.println("body: " + body.toString());

        Type type = new TypeToken<JobKey>() {
        }.getType();
        JsonElement jobKeyJson = body.getAsJsonObject().get("jobKey");
        JobKey jobKey = gson.fromJson(jobKeyJson, type);

        int stateId = body.getAsJsonObject().get("stateId").getAsInt();

        if (!uc.hasJobWithKey(jobKey)) {
            return errorResponse("The requested Graph Game BDD does not exist.");
        }

        Job<BDDGraphExplorer> job = uc.getGraphGameBDDJob(jobKey);
        if (!job.isFinished()) {
            return errorResponse("The job for that Graph Game BDD is not yet finished" +
                    ".  Its status: " + job.getStatus());
        }
        BDDGraphExplorer bddGraphExplorer = job.getResult();
        bddGraphExplorer.toggleStatePreset(stateId);
        job.fireJobStatusChanged();

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("graphGameBDD", bddGraphExplorer.getVisibleGraph());
        return responseJson.toString();
    }

    // Get the APT representation of a given Petri Game/PetriNetWithTransits
    private Object handleGetAptOfPetriGame(Request req, Response res, PetriNetWithTransits net)
            throws RenderException {
        JsonElement body = parser.parse(req.body());

        String apt = PNWTTools.getAPT(net, true, true);
        JsonElement aptJson = new JsonPrimitive(apt);

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", "success");
        responseJson.add("apt", aptJson);
        return responseJson.toString();
    }

    // Update the X/Y coordinates of multiple nodes.  Send them back to the client to confirm it
    // worked
    private Object handleUpdateXYCoordinates(Request req, Response res, PetriNetWithTransits petriNet) throws CouldNotFindSuitableConditionException {
        JsonElement body = parser.parse(req.body());
        JsonObject nodesXYCoordinatesJson =
                body.getAsJsonObject().get("nodeXYCoordinateAnnotations").getAsJsonObject();
        Type type = new TypeToken<Map<String, NodePosition>>() {
        }.getType();
        Map<String, NodePosition> nodePositions = gson.fromJson(nodesXYCoordinatesJson, type);
        PetriGameTools.saveXYCoordinates(petriNet, nodePositions);

        JsonElement serializedNet = PetriNetD3.ofPetriNetWithXYCoordinates(
                petriNet, new HashSet<>(petriNet.getNodes()), true);

        return successResponse(serializedNet);
    }

    private Object handleInsertPlace(Request req, Response res, PetriNetWithTransits net) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        double x = body.get("x").getAsDouble();
        double y = body.get("y").getAsDouble();
        String nodeType = body.get("nodeType").getAsString();
        GraphNodeType graphNodeType = GraphNodeType.valueOf(nodeType);

        Node node = null;
        switch (graphNodeType) {
            case SYSPLACE:
                node = net.createPlace();
                break;
            case ENVPLACE:
                if (!(net instanceof PetriGame)) {
                    throw new IllegalArgumentException("The given net is not a PetriGame, but merely a PetriNetWithTransits, so you can't insert an environment place.");
                } else {
                    PetriGame game = (PetriGame) net;
                    Place place = game.createPlace();
                    game.setEnvironment(place);
                    node = place;
                    break;
                }
            case TRANSITION:
                node = net.createTransition();
                break;
            case GRAPH_STRATEGY_BDD_STATE:
                return errorResponse("You can't insert a GRAPH_STRATEGY_BDD_STATE into a Petri Game.");
        }
        net.setXCoord(node, x);
        net.setYCoord(node, y);

        JsonElement serializedNet = PetriNetD3.ofPetriNetWithXYCoordinates(
                net, new HashSet<>(Collections.singletonList(node)), true);

        return successResponse(serializedNet);
    }

    private Object handleDeleteNode(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();

        net.removeNode(nodeId);

        JsonElement serializedNet = PetriNetD3.ofPetriNetWithTransits(net);
        return successResponse(serializedNet);
    }

    private Object handleRenameNode(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeIdOld = body.get("nodeIdOld").getAsString();
        String nodeIdNew = body.get("nodeIdNew").getAsString();

        Node oldNode = net.getNode(nodeIdOld);
        net.rename(oldNode, nodeIdNew);

        JsonElement serializedNet = PetriNetD3.ofPetriNetWithTransits(net);
        return successResponse(serializedNet);
    }

    private Object handleToggleEnvironmentPlace(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();

        if (!(net instanceof PetriGame)) {
            throw new IllegalArgumentException("The given net is not a PetriGame, but merely a PetriNetWithTransits, so you can't insert an environment place.");
        }
        PetriGame petriGame = (PetriGame) net;

        Place place = petriGame.getPlace(nodeId);
        boolean environment = petriGame.isEnvironment(place);
        if (environment) {
            petriGame.setSystem(place);
        } else {
            petriGame.setEnvironment(place);
        }

        JsonElement serializedNet = PetriNetD3.ofPetriNetWithTransits(petriGame);
        return successResponse(serializedNet);
    }

    private Object handleToggleIsInitialTransit(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();

        Place place = net.getPlace(nodeId);
        boolean isInitialTransit = net.isInitialTransit(place);
        if (isInitialTransit) {
            net.removeInitialTransit(place);
        } else {
            net.setInitialTransit(place);
        }

        JsonElement serializedNet = PetriNetD3.ofPetriNetWithTransits(net);
        return successResponse(serializedNet);
    }

    private Object handleSetInitialToken(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();
        int tokens = body.get("tokens").getAsInt();

        Place place = net.getPlace(nodeId);
        place.setInitialToken(tokens);

        JsonElement serializedNet = PetriNetD3.ofPetriNetWithTransits(net);
        return successResponse(serializedNet);
    }

    private Object handleSetIsSpecial(Request req, Response res, PetriNetWithTransits net) throws CouldNotFindSuitableConditionException {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();
        boolean special = body.get("newSpecialValue").getAsBoolean();

        Place place = net.getPlace(nodeId);
        Condition.Objective condition = Adam.getCondition(net);
        if (special) {
            net.setSpecial(place, condition);
        } else {
            net.removeSpecial(place, condition);
        }

        JsonElement serializedNet = PetriNetD3.ofPetriNetWithTransits(net);
        return successResponse(serializedNet);
    }

    private Object handleSetWinningCondition(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String winningCondition = body.get("winningCondition").getAsString();

        Condition.Objective objective = Condition.Objective.valueOf(winningCondition);
        PNWTTools.setConditionAnnotation(net, objective);

        JsonElement serializedNet = PetriNetD3.ofPetriNetWithTransits(net);
        return successResponse(serializedNet);
    }

    private Object handleCreateFlow(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String source = body.get("source").getAsString();
        String destination = body.get("destination").getAsString();

        net.createFlow(source, destination);
        JsonElement serializedNet = PetriNetD3.ofPetriNetWithTransits(net);

        return successResponse(serializedNet);
    }

    private Object handleDeleteFlow(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String source = body.get("sourceId").getAsString();
        String target = body.get("targetId").getAsString();

        net.removeFlow(source, target);

        JsonElement serializedNet = PetriNetD3.ofPetriNetWithTransits(net);
        return successResponse(serializedNet);
    }

    private Object handleCreateTransit(Request req, Response res, PetriNetWithTransits net) {
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
        Transition transition = net.getTransition(transitionId);
        sourceId.ifPresent(id -> {
            Place source = net.getPlace(id);
            if (!transition.getPreset().contains(source)) {
                net.createFlow(source, transition);
            }
        });
        for (String postsetId : postsetIds) {
            Place postPlace = net.getPlace(postsetId);
            if (!transition.getPostset().contains(postPlace)) {
                net.createFlow(transition, postPlace);
            }
        }

        // Create a transit.  It is an initial transit if if has no source Place.
        String[] postsetArray = postsetIds.toArray(new String[postsetIds.size()]);
        if (sourceId.isPresent()) {
            net.createTransit(sourceId.get(), transitionId, postsetArray);
        } else {
            net.createInitialTransit(transitionId, postsetArray);
        }

        JsonElement serializedNet = PetriNetD3.ofPetriNetWithTransits(net);

        return successResponse(serializedNet);
    }

    private Object handleParseLtlFormula(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String formula = body.get("formula").getAsString();

        try {
            IRunFormula iRunFormula = AdamModelChecker.parseFlowLTLFormula(net, formula);
            return successResponse(new JsonPrimitive(true));
        } catch (ParseException e) {
            Throwable cause = e.getCause();
            System.out.println(cause.getMessage());
            return errorResponse(e.getMessage() + "\n" + cause.getMessage());
        }
    }


    private Object handleFireTransition(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String transitionId = body.get("transitionId").getAsString();

        Transition transition = net.getTransition(transitionId);
        Marking initialMarking = net.getInitialMarking();
        Marking newInitialMarking = transition.fire(initialMarking);
        net.setInitialMarking(newInitialMarking);
        return successResponse(PetriNetD3.ofPetriNetWithTransits(net));
    }

    private Object handleSetFairness(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String transitionId = body.get("transitionId").getAsString();
        String fairness = body.get("fairness").getAsString();

        Transition transition = net.getTransition(transitionId);
        switch (fairness) {
            case "weak":
                net.removeStrongFair(transition);
                net.setWeakFair(transition);
                break;
            case "strong":
                net.removeWeakFair(transition);
                net.setStrongFair(transition);
                break;
            case "none":
                net.removeWeakFair(transition);
                net.removeStrongFair(transition);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized value for 'fairness': " + fairness);
        }
        return successResponse(PetriNetD3.ofPetriNetWithTransits(net));
    }

    private Object handleSetInhibitorArc(Request req, Response response, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String sourceId = body.get("sourceId").getAsString();
        String targetId = body.get("targetId").getAsString();
        boolean isInhibitorArc = body.get("isInhibitorArc").getAsBoolean();

        Node sourceNode = net.getNode(sourceId);
        Node targetNode = net.getNode(targetId);

        Flow flow = net.getFlow(sourceId, targetId);
        if (isInhibitorArc) {
            if (!(sourceNode instanceof Place) || !(targetNode instanceof Transition)) {
                throw new IllegalArgumentException(
                        "Inhibitor arcs must point from a Place to a Transition, and not vice versa.");
            }
            net.setInhibitor(flow);
        } else {
            net.removeInhibitor(flow);
        }
        return successResponse(PetriNetD3.ofPetriNetWithTransits(net));
    }

}
