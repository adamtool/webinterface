package uniolunisaar.adamwebfrontend;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static uniolunisaar.adamwebfrontend.CalculationStatus.*;

/**
 * Test the class Calculation
 */
public class CalculationTest {
    // Just make sure that a single calculation acts like it should, being run in a separate thread.
    @Test
    public void testCalculation() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Calculation<String> stringCalculation = new Calculation<>(() -> {

            Thread.sleep(1000);
            return "This String was calculated in another thread";
        }, "Fake ID");

        // We queue up the calculation, and it should immediately start to run in a separate thread.
        stringCalculation.queue(executorService);

        int timeWaitedInMainThread = 0;
        while (!stringCalculation.isFinished()) {
            Thread.sleep(100);
            timeWaitedInMainThread += 100;
            System.out.println("Waiting... (" + timeWaitedInMainThread + " ms)");
        }
        System.out.println("The calculation is finished.  Result: '" + stringCalculation.getResult() + "'");

        assertEquals("The calculation's status should be 'completed'",
                COMPLETED, stringCalculation.getStatus());
        assertTrue("The main thread should have waited roughly 1 second",
                timeWaitedInMainThread > 500 && timeWaitedInMainThread < 1500);
    }

    // In this test, we start running a calculation that will take 1 second, and then we cancel it
    // after half a second.  The calculation thread should be interrupted.
    @Test
    public void testInterruption() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        AtomicBoolean didCalculationStart = new AtomicBoolean(false);
        AtomicBoolean didCalculationfinish = new AtomicBoolean(false);
        Calculation<String> stringCalculation = new Calculation<>(() -> {
            didCalculationStart.set(true);
            Thread.sleep(1000);

            didCalculationfinish.set(true);
            return "This String was calculated in another thread";
        }, "Fake ID");

        stringCalculation.queue(executorService);
        int timeWaited = 0;
        while (!stringCalculation.isFinished()) {
            Thread.sleep(100);
            timeWaited += 100;
            System.out.println("Waiting... (" + timeWaited + " ms)");

            if (timeWaited >= 500) {
                stringCalculation.cancel();
                break;
            }
        }

        assertTrue("The calculation thread should have been started, and this flag should have " +
                "been set", didCalculationStart.get());
        assertFalse("The calculation thread should have been interrupted, so this flag should not" +
                " have been set to true", didCalculationfinish.get());
        assertEquals("The calculation's status should be 'canceled'",
                CANCELED, stringCalculation.getStatus());
    }


    // After queue() is called, the calculation is merely queued up to be run by the given
    // ExecutorService.  We want to be able to tell if it is actually being run yet.
    // The ExecutorService API offers no obvious way to do this.
    // A possibility would be to extend the ExecutorService class -- see this article:
    // https://www.richardnichols.net/2012/01/how-to-get-the-running-tasks-for-a-java-executor/
    // -- but I have chosen to simply use a boolean flag that gets set to true at the start of my
    // Callable tasks.
    @Test
    public void testDistinguishBetweenQueuedAndRunningCalculations() throws InterruptedException, ExecutionException {
         ExecutorService executorService = Executors.newSingleThreadExecutor();

        Calculation<String> calculation1 = new Calculation<>(() -> {
            Thread.sleep(1000);
            return "First calculation result";
        }, "Fake ID");
        Calculation<String> calculation2 = new Calculation<>(() -> {
            Thread.sleep(1000);
            return "Second calculation result";
        }, "Fake ID");

        // Queue up these two calculations.  Using a singleThreadExecutor, only one of them
        // should be running at a time, so at first, the first calcluation should be running, but
        // the second should just be "queued".
        calculation1.queue(executorService);
        calculation2.queue(executorService);
        Thread.sleep(100);
        assertEquals(RUNNING, calculation1.getStatus());
        assertEquals(QUEUED, calculation2.getStatus());

        Thread.sleep(1000);
        // After 1 second, the first calculation should be finished, and the second one should
        // have been started.
        assertEquals(COMPLETED, calculation1.getStatus());
        assertEquals(RUNNING, calculation2.getStatus());

        Thread.sleep(1000);
        // Finally, both calculations should be done.
        assertEquals(COMPLETED, calculation1.getStatus());
        assertEquals(COMPLETED, calculation2.getStatus());
    }

}
