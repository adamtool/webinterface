package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonObject;

/**
 * We store Jobs in Map<JobKey, Job>
 * A JobKey contains the info necessary to uniquely identify a job
 * (except for the info about who the Job belongs to.  That is encapsulated in UserContext objects
 */
public class JobKey {
    private final String canonicalApt;
    private final JsonObject requestBody;

    /**
     * @param canonicalApt the 'canonical apt' representation of a Petri Game that the Job is
     *                     based on
     * @param requestBody All other parameters related to the job
     */
    public JobKey(String canonicalApt, JsonObject requestBody) {
        this.canonicalApt = canonicalApt;
        this.requestBody = requestBody;
    }

    public String getCanonicalApt() {
        return canonicalApt;
    }

    public JsonObject getRequestBody() {
        return requestBody;
    }
}
