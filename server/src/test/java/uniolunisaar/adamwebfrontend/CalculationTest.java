package uniolunisaar.adamwebfrontend;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static uniolunisaar.adamwebfrontend.CalculationStatus.CANCELED;
import static uniolunisaar.adamwebfrontend.CalculationStatus.COMPLETED;

/**
 * Unit test for simple App.
 */
public class CalculationTest {
    @Test
    public void testCalculation() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Calculation<String> stringCalculation = new Calculation<>(() -> {
            Thread.sleep(1000);
            return "This String was calculated in another thread";
        });

        stringCalculation.start(executorService);
        int timeWaited = 0;
        while (!stringCalculation.isFinished()) {
            Thread.sleep(100);
            timeWaited += 100;
            System.out.println("Waiting... (" + timeWaited + " ms)");
        }
        System.out.println("The calculation is finished.  Result: '" + stringCalculation.getResult() + "'");

        assertEquals("The calculation's status should be 'completed'",
                COMPLETED, stringCalculation.getStatus());
        assertTrue("The main thread should have waited roughly 1 second",
                timeWaited > 500 && timeWaited < 1500);
    }

    // In this test, we start running a calculation that will take 1 second, and then we cancel it
    // after half a second.  The calculation thread should be interrupted.
    @Test
    public void testInterruption() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        AtomicBoolean didCalculationfinish = new AtomicBoolean(false);
        Calculation<String> stringCalculation = new Calculation<>(() -> {
            Thread.sleep(1000);

            didCalculationfinish.set(true);
            return "This String was calculated in another thread";
        });

        stringCalculation.start(executorService);
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

        assertFalse("The calculation thread should have been interrupted, so this flag should not" +
                " have been set to true", didCalculationfinish.get());
        assertEquals("The calculation's status should be 'canceled'",
                CANCELED, stringCalculation.getStatus());
    }
}
