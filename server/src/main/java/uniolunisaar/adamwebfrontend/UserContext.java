package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static uniolunisaar.adamwebfrontend.JobStatus.COMPLETED;
import static uniolunisaar.adamwebfrontend.JobStatus.FAILED;
import static uniolunisaar.adamwebfrontend.JobType.*;

/**
 * Stores data related to a single user session
 */
public class UserContext {
    /*
    When we calculate anything based on a petri game, we convert the petri game to APT, then
    put the (APT, Job) pair in one of these maps.
    I call this APT the "canonical APT" representation of the Petri Game.
    This allows us to recognize when a user is trying to repeat an operation unnecessarily.
    */
    // Store the results of "getGraphGameBdd"
    public final Map<String, Job<BDDGraphExplorer>> graphGameBddsOfApts =
            new ConcurrentHashMap<>();
    // Store the results of "existsWinningStrategy"
    public final Map<String, Job<Boolean>> existsWinningStrategyOfApts =
            new ConcurrentHashMap<>();
    // Store the results of "getStrategyBdd"
    public final Map<String, Job<PetriGame>> strategyBddsOfApts =
            new ConcurrentHashMap<>();
    // Store the results of "getGraphStrategyBdd"
    public final Map<String, Job<BDDGraph>> graphStrategyBddsOfApts =
            new ConcurrentHashMap<>();
    // "Check LTL formula"
    public final Map<String, Job<ModelCheckingResult>> modelCheckingResults =
            new ConcurrentHashMap<>();

    public JsonArray getJobList() {
        JsonArray result = new JsonArray();
        for (String aptOfPetriGame : this.graphGameBddsOfApts.keySet()) {
            Job<BDDGraphExplorer> job = this.graphGameBddsOfApts.get(aptOfPetriGame);
            JsonObject entry = jobListEntry(job, aptOfPetriGame, GRAPH_GAME_BDD);
            result.add(entry);
        }
        for (String canonicalApt : this.existsWinningStrategyOfApts.keySet()) {
            Job<Boolean> job = this.existsWinningStrategyOfApts.get(canonicalApt);
            JsonObject entry = jobListEntry(
                    job, canonicalApt, EXISTS_WINNING_STRATEGY);
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
        for (String canonicalApt : this.strategyBddsOfApts.keySet()) {
            Job<PetriGame> job = this.strategyBddsOfApts.get(canonicalApt);
            JsonObject entry = jobListEntry(
                    job, canonicalApt, WINNING_STRATEGY);
            result.add(entry);
        }
        for (String canonicalApt : this.graphStrategyBddsOfApts.keySet()) {
            Job<BDDGraph> job = this.graphStrategyBddsOfApts.get(canonicalApt);
            JsonObject entry = jobListEntry(
                    job, canonicalApt, GRAPH_STRATEGY_BDD
            );
            result.add(entry);
        }
        for (String canonicalApt : this.modelCheckingResults.keySet()) {
            Job<ModelCheckingResult> job = this.modelCheckingResults.get(canonicalApt);
            JsonObject entry = jobListEntry(
                    job, canonicalApt, MODEL_CHECKING_RESULT);
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

        return result;
    }

    private static JsonObject jobListEntry(Job job,
                                           String canonicalApt,
                                           JobType jobType) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", jobType.toString());
        entry.addProperty("canonicalApt", canonicalApt);
        entry.addProperty("jobStatus", job.getStatus().toString());
        entry.addProperty("timeStarted", job.getTimeStarted().getEpochSecond());
        entry.addProperty("timeFinished", job.getTimeFinished().getEpochSecond());
        if (job.getStatus() == FAILED) {
            entry.addProperty("failureReason", job.getFailedReason());
        }
        return entry;
    }

    public Map<String, ? extends Job> getJobMap(JobType jobType) {
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
            default:
                throw new IllegalArgumentException("Missing switch case in getJobMap for " +
                        "the following JobType: " + jobType.toString());
        }
    }
}
