package uniolunisaar.adamwebfrontend;

import uniolunisaar.adam.tools.processHandling.ProcessPool;

import java.time.Instant;
import java.util.HashSet;
import java.util.concurrent.*;

import static uniolunisaar.adamwebfrontend.JobStatus.*;

/**
 * This class represents a possibly long-running calculation that should run in its own thread.
 * It can be canceled.
 * You can check its status.  If it's finished, you can access the result.
 *
 * @param <T> The result type of the calculation
 */
public class Job<T> {
    private final Callable<T> callable;
    private final String netId;

    private CompletableFuture<T> future = null;
    private volatile boolean isStarted = false;
    private volatile Instant timeStarted = Instant.EPOCH;
    private volatile Instant timeFinished = Instant.EPOCH;
    private final HashSet<JobObserver> observers = new HashSet<>();

    private boolean isQueued = false;

    public Future<T> getFuture() {
        return future;
    }

    public void addObserver(JobObserver observer) {
        observers.add(observer);
    }

    public void fireJobStatusChanged() {
        for (JobObserver observer : observers) {
            observer.onJobChange(this);
        }
    }

    /**
     * @param v     a lambda that will return a value you want to have be computed
     * @param netId the ID of the Petri Net that is underlying this job.
     *              It will get used to kill the processes in case the job gets canceled.
     */
    public Job(Callable<T> v, String netId) {
        this.callable = v;
        this.netId = netId;
    }

    public void queue(ExecutorService executorService) {
        if (this.future != null) {
            return;
        }
        isQueued = true;
        fireJobStatusChanged();
        this.future = asFuture(() -> {
            isStarted = true;
            timeStarted = Instant.now();
            fireJobStatusChanged();
            try {
                T result = callable.call();
                timeFinished = Instant.now();
                return result;
            } catch (Throwable e) {
                timeFinished = Instant.now();
                throw e;
            }
        }, executorService);
        this.future.whenComplete((result, exception) -> this.fireJobStatusChanged());
    }

    public void cancel() {
        if (this.getStatus() != RUNNING && this.getStatus() != QUEUED) {
            throw new UnsupportedOperationException(
                    "This Job is not running/queued, so you can't cancel it.");
        }
        this.future.cancel(true);
        ProcessPool.getInstance().destroyProcessesOfNet(netId);
        fireJobStatusChanged();
    }

    public boolean isFinished() {
        return future.isDone();
    }

    public T getResult() throws ExecutionException, InterruptedException {
        return future.get();
    }

    public JobStatus getStatus() {
        if (future == null) {
            if (!isQueued) {
                return NOT_STARTED;
            } else {
                return QUEUED;
            }
        }
        if (future.isCancelled()) {
            if (isStarted && timeFinished == Instant.EPOCH) {
                // We have interrupted the thread and are waiting for it to check the interrupt
                // flag and stop running.
                return CANCELING;
            }
            return CANCELED;
        }
        if (future.isDone()) {
            try {
                future.get();
            } catch (InterruptedException e) {
                return CANCELED;
            } catch (ExecutionException e) {
                return FAILED;
            }
            return COMPLETED;
        }
        if (isStarted) {
            return RUNNING;
        }
        return QUEUED;
    }

    public String getFailedReason() {
        if (getStatus() == FAILED) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                return e.getMessage();
            }
        }
        return "";
    }

    public Instant getTimeStarted() {
        return timeStarted;
    }

    public Instant getTimeFinished() {
        return timeFinished;
    }

    public static <T> CompletableFuture<T> asFuture(Callable<? extends T> callable, Executor executor) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                future.complete(callable.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
