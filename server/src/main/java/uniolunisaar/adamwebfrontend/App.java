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
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrinet.objectives.Condition;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.pg.*;
import uniolunisaar.adam.exceptions.pnwt.CouldNotFindSuitableConditionException;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.PGTools;
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

        post("/fireTransition", this::handleFireTransition);

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

    // TODO Refactor, see #293
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

    private Object handleParseApt(Request req, Response res) {
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

        JsonElement netJson;
        switch (netType) {
            case "petriNetWithTransits":
                netJson = PetriNetD3.serializePNWT(net, net.getNodes(), true);
                break;
            case "petriGame":
                // TODO Refactor this route into two or three separate routes for PN, PNWT, and PG
                netJson = PetriNetD3.serializePetriGame((PetriGame) net, net.getNodes(), true);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized net type: " + netType);
        }
        JsonElement serializedNet = PetriGameD3.of(netJson, netUuid);

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

        // TODO Refactor, see #293
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

    // Update the X/Y coordinates of multiple nodes in a net in the editor.
    // Send them back to the client to confirm it worked
    private Object handleUpdateXYCoordinates(Request req, Response res, PetriNetWithTransits petriNet) throws CouldNotFindSuitableConditionException {
        JsonElement body = parser.parse(req.body());
        JsonObject nodesXYCoordinatesJson =
                body.getAsJsonObject().get("nodeXYCoordinateAnnotations").getAsJsonObject();
        Type type = new TypeToken<Map<String, NodePosition>>() {
        }.getType();
        Map<String, NodePosition> nodePositions = gson.fromJson(nodesXYCoordinatesJson, type);
        PetriGameTools.saveXYCoordinates(petriNet, nodePositions);

        // Send all X/Y coordinates back to the client
        JsonElement serializedNet = PetriNetD3.serializeEditorNet(
                petriNet, new HashSet<>(petriNet.getNodes()));

        return successResponse(serializedNet);
    }

    private Object handleInsertPlace(Request req, Response res, PetriNetWithTransits net) {
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
                // TODO #293 refactor
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

        // Send the x/y coordinate of the newly created node back to the client
        JsonElement serializedNet = PetriNetD3.serializeEditorNet(
                net, new HashSet<>(Collections.singletonList(node)));

        return successResponse(serializedNet);
    }

    private Object handleDeleteNode(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();

        net.removeNode(nodeId);

        JsonElement serializedNet = PetriNetD3.serializeEditorNet(net);
        return successResponse(serializedNet);
    }

    private Object handleRenameNode(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeIdOld = body.get("nodeIdOld").getAsString();
        String nodeIdNew = body.get("nodeIdNew").getAsString();

        Node oldNode = net.getNode(nodeIdOld);
        net.rename(oldNode, nodeIdNew);

        JsonElement serializedNet = PetriNetD3.serializeEditorNet(net);
        return successResponse(serializedNet);
    }

    private Object handleToggleEnvironmentPlace(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();

        // TODO #293 refactor
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

        JsonElement serializedNet = PetriNetD3.serializeEditorNet(petriGame);
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

        JsonElement serializedNet = PetriNetD3.serializeEditorNet(net);
        return successResponse(serializedNet);
    }

    private Object handleSetInitialToken(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String nodeId = body.get("nodeId").getAsString();
        int tokens = body.get("tokens").getAsInt();

        Place place = net.getPlace(nodeId);
        place.setInitialToken(tokens);

        JsonElement serializedNet = PetriNetD3.serializeEditorNet(net);

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

        JsonElement serializedNet = PetriNetD3.serializeEditorNet(net);
        return successResponse(serializedNet);
    }

    private Object handleSetWinningCondition(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String winningCondition = body.get("winningCondition").getAsString();

        Condition.Objective objective = Condition.Objective.valueOf(winningCondition);
        PNWTTools.setConditionAnnotation(net, objective);

        JsonElement serializedNet = PetriNetD3.serializeEditorNet(net);
        return successResponse(serializedNet);
    }

    private Object handleCreateFlow(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String source = body.get("source").getAsString();
        String destination = body.get("destination").getAsString();

        net.createFlow(source, destination);
        JsonElement serializedNet = PetriNetD3.serializeEditorNet(net);

        return successResponse(serializedNet);
    }

    private Object handleDeleteFlow(Request req, Response res, PetriNetWithTransits net) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String source = body.get("sourceId").getAsString();
        String target = body.get("targetId").getAsString();

        net.removeFlow(source, target);

        JsonElement serializedNet = PetriNetD3.serializeEditorNet(net);
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

        JsonElement serializedNet = PetriNetD3.serializeEditorNet(net);

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

    private Object handleFireTransitionNew(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();

        JsonObject preMarkingJson = body.get("markings").getAsJsonObject();
        Type markingMapTypeToken = new TypeToken<Map<String, Long>>() {
        }.getType();
        Map<String, Long> markingsMap = gson.fromJson(preMarkingJson, markingMapTypeToken);

        String transitionId = body.get("transitionId").getAsString();

        String petriNetId = body.get("petriNetId").getAsString();

        NetType netType = NetType.valueOf(body.get("netType").getAsString());

        // TODO Refactor (See #293)
        // TODO Allow firing transitions in petri nets outside the editor
        // (Model checking net / Winning Strategy)
        PetriNet net = getPetriNet(petriNetId);
        PetriNet netCopy = null;
        switch (netType) {
            case PETRI_NET:
                netCopy = new PetriNet(net);
                break;
            case PETRI_NET_WITH_TRANSITS:
                netCopy = new PetriNetWithTransits((PetriNetWithTransits) net);
            case PETRI_GAME:
                netCopy = new PetriGame((PetriGame) net);
                break;
        }

        // Fire the transition in a copy of the net, leaving the original net alone
        Transition transition = netCopy.getTransition(transitionId);
        Marking preMarking = mapToMarking(markingsMap, net);
        Marking postMarking = transition.fire(preMarking);

        Map<String, Long> postMarkingMap = markingToMap(postMarking);
        JsonElement postMarkingJson = gson.toJsonTree(postMarkingMap, markingMapTypeToken);
        return successResponse(postMarkingJson);
    }

    private static Marking mapToMarking(Map<String, Long> map, PetriNet net) {
        Marking marking = new Marking(net);
        map.forEach((placeId, tokenCount) -> {
            Place place = net.getPlace(placeId);
            Token token = Token.valueOf(tokenCount);
            marking.setTokenCount(place, token);
        });
        return marking;
    }

    private static Map<String, Long> markingToMap(Marking marking) {
        Map<String, Long> map = new HashMap<>();
        for (Place place : marking.getNet().getPlaces()) {
            String placeId = place.getId();
            Long tokenCount = marking.getToken(placeId).getValue();
            map.put(placeId, tokenCount);
        }
        return map;
    }

    private Object handleFireTransition(Request req, Response res) {
        JsonObject body = parser.parse(req.body()).getAsJsonObject();
        String apt;
        JsonElement aptJson = body.get("apt");
        JsonElement netIdJson = body.get("petriNetId");
        JsonElement transitionIdJson = body.get("transitionId");
        // netType should be a String representation of an element of the Enum NetType
        JsonElement netTypeJson = body.get("netType");
        if (aptJson != null && aptJson.isJsonPrimitive() && aptJson.getAsJsonPrimitive().isString()) {
            apt = aptJson.getAsString();
        } else if (netIdJson != null) {
            String netId = netIdJson.getAsString();

            PetriNetWithTransits pg = getPetriNet(netId);
            try {
                apt = pg.toAPT(true, true);
            } catch (RenderException e) {
                e.printStackTrace();
                return errorResponse("An exception was thrown when converting the given " +
                        "net to APT.  Check the server log for details: " + exceptionToString(e));
            }
        } else {
            return errorResponse("Invalid request. No 'apt' or 'petriNetId' was provided.");
        }

        if (transitionIdJson == null) {
            return errorResponse("Invalid request.  No 'transitionId' was provided.");
        }
        NetType netType;
        if (netTypeJson == null || !netTypeJson.isJsonPrimitive() || !netTypeJson.getAsJsonPrimitive().isString()) {
            return errorResponse("Invalid request.  A 'netType' must be provided, and it should" +
                    "correspond to one of the elements of the enum 'NetType'.");
        } else {
            try {
                netType = NetType.valueOf(netTypeJson.getAsString());
            } catch (IllegalArgumentException e) {
                return errorResponse("The value provided for 'netType' does not match any of the " +
                        "elements of the enum 'NetType'.");
            }
        }

        String transitionId = transitionIdJson.getAsString();
        PetriNet pn;
        try {
            switch (netType) {
                case PETRI_NET:
                    pn = Tools.getPetriNetFromString(apt);
                    break;
                case PETRI_NET_WITH_TRANSITS:
                    pn = PNWTTools.getPetriNetWithTransits(apt, true);
                    break;
                case PETRI_GAME:
                    pn = PGTools.getPetriGameFromAPTString(apt, true, true);
                    break;
                default:
                    return errorResponse("Missing switch branch. Please send a bug report");
            }
        } catch (ParseException | IOException | CouldNotCalculateException | NotSupportedGameException e) {
            return errorResponse(exceptionToString(e));
        }

        fireTransition(pn, transitionId);
        String newApt;
        try {
            switch (netType) {
                case PETRI_NET:
                    newApt = Tools.getPN(pn);
                    break;
                case PETRI_NET_WITH_TRANSITS:
                    newApt = PNWTTools.getAPT((PetriNetWithTransits) pn, true, true);
                    break;
                case PETRI_GAME:
                    newApt = PGTools.getAPT((PetriGame) pn, true, true);
                    break;
                default:
                    return errorResponse("Missing switch branch.  Please file a bug report");
            }
        } catch (RenderException e) {
            return errorResponse(exceptionToString(e));
        }
        JsonObject result = new JsonObject();
        result.addProperty("apt", newApt);
        JsonElement graphJson;
        switch (netType) {
            case PETRI_NET:
                graphJson = PetriNetD3.serializePetriNet(pn, new HashSet<>());
                break;
            case PETRI_NET_WITH_TRANSITS:
                graphJson = PetriNetD3.serializePNWT((PetriNetWithTransits) pn, new HashSet<>(), false);
                break;
            case PETRI_GAME:
                graphJson = PetriNetD3.serializePetriGame((PetriGame) pn, new HashSet<>(), false);
                break;
            default:
                return errorResponse("Missing switch branch.  Please file a bug report");
        }
        result.add("graph", graphJson);
        return successResponse(result);
    }

    private void fireTransition(PetriNet net, String transitionId) {
        Transition transition = net.getTransition(transitionId);
        Marking initialMarking = net.getInitialMarking();
        Marking newInitialMarking = transition.fire(initialMarking);
        net.setInitialMarking(newInitialMarking);
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
        return successResponse(PetriNetD3.serializeEditorNet(net));
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
        return successResponse(PetriNetD3.serializeEditorNet(net));
    }

}
