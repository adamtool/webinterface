package uniolunisaar.adamwebfrontend;

import uniol.apt.util.Pair;
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

    private Pair<Future<?>, CompletableFuture<T>> futurePair = null;
    private volatile boolean isStarted = false;
    private volatile Instant timeStarted = Instant.EPOCH;
    private volatile Instant timeFinished = Instant.EPOCH;
    private final HashSet<JobObserver> observers = new HashSet<>();

    private boolean isQueued = false;

    public Future<T> getFuture() {
        return futurePair.getSecond();
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
        if (this.futurePair != null) {
            return;
        }
        isQueued = true;
        fireJobStatusChanged();
        this.futurePair = asFuture(() -> {
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
        this.futurePair.getSecond().whenComplete((result, exception) -> {
            this.fireJobStatusChanged();
            if (exception != null) {
                System.err.println("A job failed with an exception.  Stack track below.");
                exception.printStackTrace();
            }
        });
    }

    public void cancel() {
        if (this.getStatus() != RUNNING && this.getStatus() != QUEUED) {
            throw new UnsupportedOperationException(
                    "This Job is not running/queued, so you can't cancel it.");
        }
        this.futurePair.getFirst().cancel(true);
        this.futurePair.getSecond().cancel(true);
        ProcessPool.getInstance().destroyProcessesOfNet(netId);
        fireJobStatusChanged();
    }

    public boolean isFinished() {
        return futurePair.getSecond().isDone();
    }

    public T getResult() throws ExecutionException, InterruptedException {
        return futurePair.getSecond().get();
    }

    public JobStatus getStatus() {
        if (futurePair == null) {
            if (!isQueued) {
                return NOT_STARTED;
            } else {
                return QUEUED;
            }
        }
        if (futurePair.getFirst().isCancelled()) {
            if (isStarted && timeFinished == Instant.EPOCH) {
                // We have interrupted the thread and are waiting for it to check the interrupt
                // flag and stop running.
                return CANCELING;
            }
            return CANCELED;
        }
        if (futurePair.getSecond().isDone()) {
            try {
                futurePair.getSecond().get();
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
                futurePair.getSecond().get();
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

    /**
     * @return a Future you can call cancel() on to cancel the ongoing calculation,
     * and a CompletableFuture that you can schedule an event to occur on after it's completed.
     */
    public static <T> Pair<Future<?>, CompletableFuture<T>> asFuture(Callable<? extends T> callable,
                                                                     ExecutorService executor) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        Future<?> executorFuture = executor.submit(() -> {
            try {
                completableFuture.complete(callable.call());
            } catch (Throwable t) {
                completableFuture.completeExceptionally(t);
            }
        });
        return new Pair<>(executorFuture, completableFuture);
    }
}
