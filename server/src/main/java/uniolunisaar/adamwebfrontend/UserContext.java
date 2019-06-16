package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;
import uniolunisaar.adam.tools.Logger;

import java.io.PrintStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static uniolunisaar.adamwebfrontend.JobStatus.COMPLETED;
import static uniolunisaar.adamwebfrontend.JobStatus.FAILED;
import static uniolunisaar.adamwebfrontend.JobType.*;

/**
 * Stores data related to a single user session
 */
public class UserContext {
    // Each user has a queue that will run one job at a time in a single thread
    public final ExecutorService executorService;
    private final UUID uuid; // Stored for debugging purposes
    private final ThreadGroup threadGroup;

    // Each user has their own logging streams
    private static final PrintStream printStreamVerbose = LogWebSocket.makePrintStream(1);
    private static final PrintStream printStreamNormal = LogWebSocket.makePrintStream(2);
    private static final PrintStream printStreamWarning = LogWebSocket.makePrintStream(3);
    private static final PrintStream printStreamError = LogWebSocket.makePrintStream(4);

    // Store the results of "getGraphGameBdd"
    public final Map<JobKey, Job<BDDGraphExplorer>> graphGameBddsOfApts =
            new ConcurrentHashMap<>();
    // Store the results of "existsWinningStrategy"
    public final Map<JobKey, Job<Boolean>> existsWinningStrategyOfApts =
            new ConcurrentHashMap<>();
    // Store the results of "getStrategyBdd"
    public final Map<JobKey, Job<PetriGame>> strategyBddsOfApts =
            new ConcurrentHashMap<>();
    // Store the results of "getGraphStrategyBdd"
    public final Map<JobKey, Job<BDDGraph>> graphStrategyBddsOfApts =
            new ConcurrentHashMap<>();
    // "Check LTL formula"
    public final Map<JobKey, Job<ModelCheckingResult>> modelCheckingResults =
            new ConcurrentHashMap<>();
    public final Map<JobKey, Job<PetriNet>> modelCheckingNets = new ConcurrentHashMap<>();


    public UserContext(UUID uuid) {
        this.uuid = uuid;
        this.threadGroup = new ThreadGroup("UserContext " + uuid.toString());
        this.executorService = Executors.newSingleThreadExecutor((Runnable jobRunnable) -> {
            Thread thread = new Thread(
                    this.threadGroup,
                    jobRunnable,
                    "Thread of UserContext " + uuid.toString());
            return thread;
        });
        // Set ADAM's loggers to use our specific PrintStreams for the threads in this user
        // session's ThreadGroup.
        // This only needs to be done once per ThreadGroup.
        // (Logger.getInstance() returns a ThreadGroup-local instance.)
        new Thread(this.threadGroup, () -> {
            Logger.getInstance().setVerboseMessageStream(printStreamVerbose);
            Logger.getInstance().setShortMessageStream(printStreamNormal);
            Logger.getInstance().setWarningStream(printStreamWarning);
            Logger.getInstance().setErrorStream(printStreamError);
        }).start();
    }

    public JsonArray getJobList() {
        JsonArray result = new JsonArray();
        for (JobKey jobKey : this.graphGameBddsOfApts.keySet()) {
            Job<BDDGraphExplorer> job = this.graphGameBddsOfApts.get(jobKey);
            JsonObject entry = jobListEntry(job, jobKey, GRAPH_GAME_BDD);
            result.add(entry);
        }
        for (JobKey jobKey : this.existsWinningStrategyOfApts.keySet()) {
            Job<Boolean> job = this.existsWinningStrategyOfApts.get(jobKey);
            JsonObject entry = jobListEntry(
                    job, jobKey, EXISTS_WINNING_STRATEGY);
            if (job.getStatus() == COMPLETED) {
                try {
                    entry.addProperty("result", job.getResult());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    throw new RuntimeException("job.getResult() threw an exception, " +
                            "although job.getStatus() == COMPLETED.  This is a bug.  " +
                            "Please let Ann know about it. :)");
                }
            }
            result.add(entry);
        }
        for (JobKey jobKey : this.strategyBddsOfApts.keySet()) {
            Job<PetriGame> job = this.strategyBddsOfApts.get(jobKey);
            JsonObject entry = jobListEntry(
                    job, jobKey, WINNING_STRATEGY);
            result.add(entry);
        }
        for (JobKey jobKey : this.graphStrategyBddsOfApts.keySet()) {
            Job<BDDGraph> job = this.graphStrategyBddsOfApts.get(jobKey);
            JsonObject entry = jobListEntry(
                    job, jobKey, GRAPH_STRATEGY_BDD
            );
            result.add(entry);
        }
        for (JobKey jobKey : this.modelCheckingResults.keySet()) {
            Job<ModelCheckingResult> job = this.modelCheckingResults.get(jobKey);
            JsonObject entry = jobListEntry(
                    job, jobKey, MODEL_CHECKING_RESULT);
            if (job.getStatus() == COMPLETED) {
                try {
                    entry.addProperty(
                            "result",
                            job.getResult().getSatisfied().toString());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    throw new RuntimeException("job.getResult() threw an exception, " +
                            "although job.getStatus() == COMPLETED.  This is a bug.  " +
                            "Please let Ann know about it. :)");
                }
            }
            result.add(entry);
        }
        for (JobKey jobKey : this.modelCheckingNets.keySet()) {
            Job<PetriNet> job = this.modelCheckingNets.get(jobKey);
            JsonObject entry = jobListEntry(
                    job, jobKey, MODEL_CHECKING_NET
            );
            result.add(entry);
        }

        return result;
    }

    private static JsonObject jobListEntry(Job job,
                                           JobKey jobKey,
                                           JobType jobType) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", jobType.toString());
        entry.add("jobKey", new Gson().toJsonTree(jobKey));
        entry.addProperty("jobStatus", job.getStatus().toString());
        entry.addProperty("timeStarted", job.getTimeStarted().getEpochSecond());
        entry.addProperty("timeFinished", job.getTimeFinished().getEpochSecond());
        if (job.getStatus() == FAILED) {
            entry.addProperty("failureReason", job.getFailedReason());
        }
        return entry;
    }

    public Map<JobKey, ? extends Job> getJobMap(JobType jobType) {
        switch (jobType) {
            case EXISTS_WINNING_STRATEGY:
                return existsWinningStrategyOfApts;
            case WINNING_STRATEGY:
                return strategyBddsOfApts;
            case GRAPH_STRATEGY_BDD:
                return graphStrategyBddsOfApts;
            case GRAPH_GAME_BDD:
                return graphGameBddsOfApts;
            case MODEL_CHECKING_RESULT:
                return modelCheckingResults;
            case MODEL_CHECKING_NET:
                return modelCheckingNets;
            default:
                throw new IllegalArgumentException("Missing switch case in getJobMap for " +
                        "the following JobType: " + jobType.toString());
        }
    }
}
