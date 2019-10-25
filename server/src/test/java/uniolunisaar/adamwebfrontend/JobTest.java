package uniolunisaar.adamwebfrontend;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static uniolunisaar.adamwebfrontend.JobStatus.*;

/**
 * Test the class Job
 */
public class JobTest {
    // Just make sure that a single job acts like it should, being run in a separate thread.
    @Test
    public void testJob() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Job<String> stringJob = new Job<>(() -> {

            Thread.sleep(1000);
            return "This String was calculated in another thread";
        }, "Fake ID");

        // We queue up the job, and it should immediately start to run in a separate thread.
        stringJob.queue(executorService);

        int timeWaitedInMainThread = 0;
        while (!stringJob.isFinished()) {
            Thread.sleep(100);
            timeWaitedInMainThread += 100;
            System.out.println("Waiting... (" + timeWaitedInMainThread + " ms)");
        }
        System.out.println("The job is finished.  Result: '" + stringJob.getResult() + "'");

        assertEquals("The job's status should be 'completed'",
                COMPLETED, stringJob.getStatus());
        assertTrue("The main thread should have waited roughly 1 second",
                timeWaitedInMainThread > 500 && timeWaitedInMainThread < 1500);
    }

    // In this test, we start running a job that will take 1 second, and then we cancel it
    // after half a second.  The job thread should be interrupted.
    @Test
    public void testInterruption() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        AtomicBoolean didJobStart = new AtomicBoolean(false);
        AtomicBoolean didJobfinish = new AtomicBoolean(false);
        Job<String> stringJob = new Job<>(() -> {
            didJobStart.set(true);
            Thread.sleep(1000);

            didJobfinish.set(true);
            return "This String was calculated in another thread";
        }, "Fake ID");

        stringJob.queue(executorService);
        int timeWaited = 0;
        boolean didWeCallCanceledYet = false;
        while (timeWaited < 1500) {
            Thread.sleep(100);
            timeWaited += 100;
            System.out.println("Waiting... (" + timeWaited + " ms)");

            if (timeWaited >= 500 && !didWeCallCanceledYet) {
                stringJob.cancel();
                didWeCallCanceledYet = true;
            }
        }

        assertTrue("The job thread should have been started, and this flag should have " +
                "been set", didJobStart.get());
        assertFalse("The job thread should have been interrupted, so this flag should not" +
                " have been set to true", didJobfinish.get());
        assertEquals("The job's status should be 'canceled'",
                CANCELED, stringJob.getStatus());
    }


    // After queue() is called, the job is merely queued up to be run by the given
    // ExecutorService.  We want to be able to tell if it is actually being run yet.
    // The ExecutorService API offers no obvious way to do this.
    // A possibility would be to extend the ExecutorService class -- see this article:
    // https://www.richardnichols.net/2012/01/how-to-get-the-running-tasks-for-a-java-executor/
    // -- but I have chosen to simply use a boolean flag that gets set to true at the start of my
    // Callable tasks.
    @Test
    public void testDistinguishBetweenQueuedAndRunningJobs() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Job<String> job1 = new Job<>(() -> {
            Thread.sleep(1000);
            return "First job result";
        }, "Fake ID");
        Job<String> job2 = new Job<>(() -> {
            Thread.sleep(1000);
            return "Second job result";
        }, "Fake ID");

        // Queue up these two jobs.  Using a singleThreadExecutor, only one of them
        // should be running at a time, so at first, the first calcluation should be running, but
        // the second should just be "queued".
        job1.queue(executorService);
        job2.queue(executorService);
        Thread.sleep(100);
        assertEquals(RUNNING, job1.getStatus());
        assertEquals(QUEUED, job2.getStatus());

        Thread.sleep(1000);
        // After 1 second, the first job should be finished, and the second one should
        // have been started.
        assertEquals(COMPLETED, job1.getStatus());
        assertEquals(RUNNING, job2.getStatus());

        Thread.sleep(1000);
        // Finally, both jobs should be done.
        assertEquals(COMPLETED, job1.getStatus());
        assertEquals(COMPLETED, job2.getStatus());
    }

    /**
     * Make sure that observers of a job recieve messages at exactly the points they should
     * (NOT_STARTED, QUEUED, RUNNING, COMPLETED)
     * @throws InterruptedException
     */
    @Test
    public void TestJobStatusChangedMessages() throws InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Job<String> job1 = new Job<>(() -> {
            Thread.sleep(1000);
            return "First job result";
        }, "Fake ID");

        CopyOnWriteArrayList<JobStatus> statusSequence = new CopyOnWriteArrayList<>();
        statusSequence.add(job1.getStatus());

        job1.addObserver(new JobObserver() {
            @Override
            public synchronized void onJobChange(Job job) {
                statusSequence.add(job.getStatus());
            }
        });
        job1.queue(executorService);
        Thread.sleep(1500);
        try {
            job1.getResult();
        } catch (ExecutionException e) {
            e.printStackTrace();
            fail();
        }
        assertEquals(
                Arrays.asList(NOT_STARTED, QUEUED, RUNNING, COMPLETED),
                statusSequence);
    }
}
