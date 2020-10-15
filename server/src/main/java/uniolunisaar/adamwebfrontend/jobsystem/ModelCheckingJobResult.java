package uniolunisaar.adamwebfrontend.jobsystem;

import uniolunisaar.adam.ds.modelchecking.results.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;

public class ModelCheckingJobResult {
    private final ModelCheckingResult modelCheckingResult;
    private final AdamCircuitFlowLTLMCStatistics statistics;

    public ModelCheckingJobResult(ModelCheckingResult modelCheckingResult, AdamCircuitFlowLTLMCStatistics statistics) {
        this.modelCheckingResult = modelCheckingResult;
        this.statistics = statistics;
    }

    public ModelCheckingResult getModelCheckingResult() {
        return modelCheckingResult;
    }

    public AdamCircuitFlowLTLMCStatistics getStatistics() {
        return statistics;
    }
}
