package uniolunisaar.adamwebfrontend.jobsystem;

import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * We store Jobs in Map<JobKey, Job>
 * A JobKey contains the info necessary to uniquely identify a job
 * (except for the info about who the Job belongs to.  That is encapsulated in UserContext objects
 */
public class JobKey {
    private final String canonicalApt;
    private final JsonObject requestParams;
    private final JobType jobType;

    /**
     * @param canonicalApt the 'canonical apt' representation of a Petri Game that the Job is
     *                     based on.  Obtained using PNWTTools.getApt()
     * @param requestParams All other parameters related to the job
     * @param jobType What kind of job (e.g. winning strategy, model checking result, ...)
     */
    public JobKey(String canonicalApt, JsonObject requestParams, JobType jobType) {
        this.canonicalApt = canonicalApt;
        this.requestParams = requestParams;
        this.jobType = jobType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobKey jobKey = (JobKey) o;
        return canonicalApt.equals(jobKey.canonicalApt) &&
                requestParams.equals(jobKey.requestParams) &&
                jobType.equals(jobKey.jobType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(canonicalApt, requestParams, jobType);
    }

    public JobType getJobType() {
        return this.jobType;
    }

    public String getApt() {
        return this.canonicalApt;
    }

    public JsonObject getParams() {
        return this.requestParams;
    }
}
