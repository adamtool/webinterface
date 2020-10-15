package uniolunisaar.adamwebfrontend.jobsystem;

import uniolunisaar.adam.ds.modelchecking.cex.CounterExample;
import uniolunisaar.adam.ds.modelchecking.cex.ReducedCounterExample;
import uniolunisaar.adam.ds.modelchecking.results.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;

public class ModelCheckingJobResult {
    private final ModelCheckingResult modelCheckingResult;
    private final AdamCircuitFlowLTLMCStatistics statistics;
    private final CounterExample counterExample;
    private final ReducedCounterExample reducedCexMc;
    private final ReducedCounterExample reducedCexInputNet;

    public ModelCheckingJobResult(ModelCheckingResult modelCheckingResult,
                                  AdamCircuitFlowLTLMCStatistics statistics,
                                  CounterExample counterExample, ReducedCounterExample reducedCexMc,
                                  ReducedCounterExample reducedCexInputNet) {
        this.modelCheckingResult = modelCheckingResult;
        this.statistics = statistics;
        this.counterExample = counterExample;
        this.reducedCexMc = reducedCexMc;
        this.reducedCexInputNet = reducedCexInputNet;
    }

    public ModelCheckingResult getModelCheckingResult() {
        return modelCheckingResult;
    }

    public AdamCircuitFlowLTLMCStatistics getStatistics() {
        return statistics;
    }

    public CounterExample getCounterExample() {
        return counterExample;
    }

    public ReducedCounterExample getReducedCexInputNet() {
        return reducedCexInputNet;
    }

    public ReducedCounterExample getReducedCexMc() {
        return reducedCexMc;
    }
}