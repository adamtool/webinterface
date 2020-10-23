package uniolunisaar.adamwebfrontend.jobsystem;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adamwebfrontend.BDDGraphExplorer;
import uniolunisaar.adamwebfrontend.BDDGraphExplorerBuilder;
import uniolunisaar.adamwebfrontend.WebSocketHandler;
import uniolunisaar.adamwebfrontend.wirerepresentations.CxType;

import java.io.PrintStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static uniolunisaar.adamwebfrontend.jobsystem.JobStatus.COMPLETED;
import static uniolunisaar.adamwebfrontend.jobsystem.JobStatus.FAILED;
import static uniolunisaar.adamwebfrontend.jobsystem.JobType.MODEL_CHECKING_RESULT;

/**
 * Stores data related to a single user session
 */
public class UserContext {
    // Each user has a queue that will run one job at a time in a single thread
    public final ThreadPoolExecutor executorService;
    private final UUID clientUuid;  // Stored to allow sending messages over websocket to clients
    private final ThreadGroup threadGroup;

    // Each user has their own logging streams
    private final PrintStream printStreamVerbose;
    private final PrintStream printStreamNormal;
    private final PrintStream printStreamWarning;
    private final PrintStream printStreamError;

    private final LinkedBlockingQueue<Runnable> jobQueue;
    private final Map<JobKey, Job> jobsByKey = new ConcurrentHashMap<>();

    public UserContext(UUID uuid) {
        this.clientUuid = uuid;
        this.threadGroup = new ThreadGroup("UserContext " + uuid.toString());
        ThreadFactory threadFactory = (Runnable jobRunnable) -> {
            Thread thread = new Thread(
                    this.threadGroup,
                    jobRunnable,
                    "Thread of UserContext " + uuid.toString());
            return thread;
        };
        this.jobQueue = new LinkedBlockingQueue<Runnable>();
        this.executorService = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                this.jobQueue,
                threadFactory);
        // Set ADAM's loggers to use our specific PrintStreams for the threads in this user
        // session's ThreadGroup.
        // This only needs to be done once per ThreadGroup.
        // (Logger.getInstance() returns a ThreadGroup-local instance.)
        this.printStreamVerbose = WebSocketHandler.makePrintStream(1, this.clientUuid);
        this.printStreamNormal = WebSocketHandler.makePrintStream(2, this.clientUuid);
        this.printStreamWarning = WebSocketHandler.makePrintStream(3, this.clientUuid);
        this.printStreamError = WebSocketHandler.makePrintStream(4, this.clientUuid);
        this.executorService.submit(() -> {
            Logger.getInstance().setVerboseMessageStream(printStreamVerbose);
            Logger.getInstance().setShortMessageStream(printStreamNormal);
            Logger.getInstance().setWarningStream(printStreamWarning);
            Logger.getInstance().setErrorStream(printStreamError);
        });
    }

    public UUID getClientUuid() {
        return clientUuid;
    }

    public JsonArray getJobList() {
        JsonArray jobListJson = new JsonArray();
        for (JobKey jobKey : this.jobsByKey.keySet()) {
            Job job = this.jobsByKey.get(jobKey);
            JsonObject entry = jobListEntry(job, jobKey);
            jobListJson.add(entry);
        }

        return jobListJson;
    }

    public JsonObject jobListEntry(Job job,
                                   JobKey jobKey) {
        JsonObject entry = new JsonObject();
        // TODO #296 delete the 'type' property here.  it's redundant, because the jobKey itself
        //  also contains the job type
        entry.addProperty("type", jobKey.getJobType().toString());
        entry.add("jobKey", new Gson().toJsonTree(jobKey));
        entry.addProperty("jobStatus", job.getStatus().toString());
        entry.addProperty("timeStarted", job.getTimeStarted().getEpochSecond());
        entry.addProperty("timeFinished", job.getTimeFinished().getEpochSecond());
        if (job.getStatus() == FAILED) {
            entry.addProperty("failureReason", job.getFailedReason());
        }

        int queuePosition = getQueuePosition(job);
        entry.addProperty("queuePosition", queuePosition);

        if (job.getStatus() == COMPLETED) {
            try {
                // TODO #288 Consider not sending the results of every job right away, but rather
                // only sending them when the user clicks "load".
                Object jobResult = job.getResult();
                JsonElement resultJson = jobKey.getJobType().serialize(jobResult);
                entry.add("result", resultJson);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                throw new RuntimeException("job.getResult() threw an exception, " +
                        "although job.getStatus() == COMPLETED.  This is a bug.  " +
                        "Please let Ann know about it. :)");
            }
        }
        return entry;
    }

    /**
     * @param job A job in this UserContext
     * @return -1 if the job's not running or in the queue, else 1, 2, 3, ... for queued jobs
     * (skipping the number zero).
     */
    private int getQueuePosition(Job job) {
        if (job.getStatus() == JobStatus.RUNNING) {
            return 0;
        }
        if (job.getFuture() == null) {
            return -1;
        }
        Future future = job.getFuture();
        if (!jobQueue.contains(future)) {
            return -1;
        }
        int index = 0;
        for (Runnable runnable : jobQueue) {
            if (runnable == future) {
                return index + 1;
            }
            index++;
        }
        return -1;
    }

    public <T> void queueJob(JobKey jobKey, Job<T> job) {
        assert (!jobsByKey.containsKey(jobKey));
        jobsByKey.put(jobKey, job);
        job.queue(this.executorService);
    }

    public boolean hasJobWithKey(JobKey jobKey) {
        return jobsByKey.containsKey(jobKey);
    }

    public Job getJobFromKey(JobKey jobKey) {
        return jobsByKey.get(jobKey);
    }

    public void deleteJobWithKey(JobKey jobKey) {
        jobsByKey.remove(jobKey);
        JsonObject jobDeletedMessage = new JsonObject();
        jobDeletedMessage.addProperty("type", "jobDeleted");
        jobDeletedMessage.add("jobKey", new Gson().toJsonTree(jobKey));
        WebSocketHandler.queueWebsocketMessage(this.clientUuid, jobDeletedMessage);
    }

    public Job<BDDGraphExplorerBuilder> getGraphGameBDDJob(JobKey jobKey) {
        if (jobKey.getJobType() != JobType.GRAPH_GAME_BDD) {
            throw new IllegalArgumentException("The given jobKey doesn't correspond to a Graph Game BDD.");
        }
        return (Job<BDDGraphExplorerBuilder>) jobsByKey.get(jobKey);
    }

    public PetriNet getPetriNetFromJob(JobKey jobKey) throws ExecutionException,
            InterruptedException {
        JobType jobType = jobKey.getJobType();
        Job<?> job = jobsByKey.get(jobKey);
        if (!job.isFinished()) {
            throw new IllegalArgumentException("The given job is not finished, so the PetriNet it" +
                    " will produce can not be accessed yet.");
        }
        switch (jobType) {
            case MODEL_CHECKING_NET:
            case WINNING_STRATEGY:
                return (PetriNet) job.getResult();
            default:
                throw new IllegalArgumentException("The given jobKey doesn't match a Job that would " +
                        "produce a Petri Net.  Its 'type' is " + jobKey.getJobType().toString());
        }
    }

    public PetriNet getPetriNetFromMcResult(JobKey jobKey, CxType cxType) throws ExecutionException,
            InterruptedException {
        Job<?> job = jobsByKey.get(jobKey);
        if (!job.isFinished()) {
            throw new IllegalArgumentException("The given job is not finished, so the PetriNet it" +
                    " will produce can not be accessed yet.");
        }
        JobType jobType = jobKey.getJobType();
        if (jobType != MODEL_CHECKING_RESULT) {
            throw new IllegalArgumentException("The given job key does not correspond to a model " +
                    "checking job.");
        }
        ModelCheckingJobResult result = (ModelCheckingJobResult) job.getResult();
        switch (cxType) {
            case INPUT_NET:
                return result.getInputNet();
            case MODEL_CHECKING_NET:
                return result.getModelCheckingNet();
            default:
                throw new RuntimeException("This switch case should be unreachable.  Please file " +
                        "a bug report :)");
        }
    }
}
