package uniolunisaar.adamwebfrontend;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Test;
import uniol.apt.io.parser.ParseException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.AdamModelChecker;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class JobStatisticsTest {

    public String loadAptFile(String path) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL path1 = classLoader.getResource(path);
        File file = new File(path1.getFile());
        return new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset());
    }

    @Test
    public void test() throws IOException, ParseException, InterruptedException, ExecutionException {
        String aptExample = loadAptFile("somewhatSmallExampleLtl.apt");
        PetriNetWithTransits net = AdamModelChecker.getPetriNetWithTransits(aptExample);
        JsonObject params = new JsonObject();
        params.addProperty("formula", "A(p0 AND p4)");
        Job<?> job = JobType.MODEL_CHECKING_RESULT.makeJob(
                net,
                params
        );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        job.queue(executorService);
        System.out.println("Started model checking test job");
        while (job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RUNNING) {
            Thread.sleep(100);
        }
        assertEquals(JobStatus.COMPLETED, job.getStatus());

        Pair<ModelCheckingResult, AdamCircuitFlowLTLMCStatistics> result =
                (Pair<ModelCheckingResult, AdamCircuitFlowLTLMCStatistics>) job.getResult();

        System.out.println("Result: " + result.getFirst().getSatisfied().toString());
        JsonElement resultJson = JobType.MODEL_CHECKING_RESULT.serialize(result);
        JsonObject resultJsonObject = (JsonObject)resultJson;
        assertTrue(resultJsonObject.has("statistics"));
    }

}
