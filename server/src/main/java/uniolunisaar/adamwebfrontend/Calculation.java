package uniolunisaar.adamwebfrontend;

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
    private Future<T> future;

    public Calculation(Callable<T> v) {
        this.callable = v;
    }

    public void start(ExecutorService executorService) {
        if (this.future != null) {
            return;
        }
        this.future = executorService.submit(callable);
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

    public CalculationStatus getStatus() {
        if (future == null) {
            return NOT_STARTED;
        }
        if (future.isCancelled()) {
            return CANCELED;
        }
        if (future.isDone()) {
            return COMPLETED;
        }
        return RUNNING; // TODO Differentiate between "queued" and "running".
    }

}
