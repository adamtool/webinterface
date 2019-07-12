package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import uniolunisaar.adam.tools.Logger;

import java.io.PrintStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static uniolunisaar.adamwebfrontend.JobStatus.COMPLETED;
import static uniolunisaar.adamwebfrontend.JobStatus.FAILED;

/**
 * Stores data related to a single user session
 */
public class UserContext {
    // Each user has a queue that will run one job at a time in a single thread
    public final ThreadPoolExecutor executorService;
    private final UUID uuid; // Stored for debugging purposes
    private final ThreadGroup threadGroup;

    // Each user has their own logging streams
    private final PrintStream printStreamVerbose;
    private final PrintStream printStreamNormal;
    private final PrintStream printStreamWarning;
    private final PrintStream printStreamError;

    private final LinkedBlockingQueue<Runnable> jobQueue;
    private final Map<JobKey, Job> jobsByKey = new ConcurrentHashMap<>();

    public UserContext(UUID uuid) {
        this.uuid = uuid;
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
        this.printStreamVerbose = LogWebSocket.makePrintStream(1, this.uuid);
        this.printStreamNormal = LogWebSocket.makePrintStream(2, this.uuid);
        this.printStreamWarning = LogWebSocket.makePrintStream(3, this.uuid);
        this.printStreamError = LogWebSocket.makePrintStream(4, this.uuid);
        this.executorService.submit(() -> {
            Logger.getInstance().setVerboseMessageStream(printStreamVerbose);
            Logger.getInstance().setShortMessageStream(printStreamNormal);
            Logger.getInstance().setWarningStream(printStreamWarning);
            Logger.getInstance().setErrorStream(printStreamError);
        });
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

    private JsonObject jobListEntry(Job job,
                                    JobKey jobKey) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", jobKey.getJobType().toString()); // TODO delete.  this is redundant; the jobKey itself also contains the job type
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
        jobsByKey.put(jobKey, job);
        job.queue(this.executorService);
    }

    public boolean hasJobWithKey(JobKey jobKey) {
        return jobsByKey.containsKey(jobKey);
    }

    public Job getJobFromKey(JobKey jobKey) {
        return jobsByKey.get(jobKey);
    }

    public void removeJobWithKey(JobKey jobKey) {
        jobsByKey.remove(jobKey);
    }

    public Job<BDDGraphExplorer> getGraphGameBDDJob(JobKey jobKey) {
        if (jobKey.getJobType() != JobType.GRAPH_GAME_BDD) {
            throw new IllegalArgumentException("The given jobKey doesn't correspond to a Graph Game BDD.");
        }
        return (Job<BDDGraphExplorer>) jobsByKey.get(jobKey);
    }
}
