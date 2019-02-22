package uniolunisaar.adamwebfrontend;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.*;

import static uniolunisaar.adamwebfrontend.CalculationStatus.*;

/**
 * This class represents a possibly long-running calculation that should run in its own thread.
 * It can be canceled.
 * You can check its status.  If it's finished, you can access the result.
 *
 * @param <T> The result type of the calculation
 */
public class Calculation<T> {
    private final Callable<T> callable;
    private Future<T> future = null;
    private boolean isStarted = false;
    private Instant timeStarted = Instant.EPOCH;
    private Instant timeFinished = Instant.EPOCH;

    public Calculation(Callable<T> v) {
        this.callable = v;
    }

    public void queue(ExecutorService executorService) {
        if (this.future != null) {
            return;
        }
        this.future = executorService.submit(() -> {
            isStarted = true;
            timeStarted = Instant.now();
            try {
                T result = callable.call();
                timeFinished = Instant.now();
                return result;
            } catch (Throwable e) {
                timeFinished = Instant.now();
                throw e;
            }
        });
    }

    public void cancel() {
        this.future.cancel(true);
    }

    public boolean isFinished() {
        return future.isDone();
    }

    public T getResult() throws ExecutionException, InterruptedException {
        return future.get();
    }

    public T getResult(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException,
            TimeoutException {
        return future.get(timeout, unit);
    }

    public CalculationStatus getStatus() {
        if (future == null) {
            return NOT_STARTED;
        }
        if (future.isCancelled()) {
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

    public Instant getTimeStarted() {
        return timeStarted;
    }

    public Instant getTimeFinished() {
        return timeFinished;
    }

}
