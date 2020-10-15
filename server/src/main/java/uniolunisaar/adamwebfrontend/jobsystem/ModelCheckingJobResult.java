package uniolunisaar.adamwebfrontend.jobsystem;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.modelchecking.results.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

public class ModelCheckingJobResult {
    private final ModelCheckingResult modelCheckingResult;
    private final AdamCircuitFlowLTLMCStatistics statistics;
    private final PetriNet modelCheckingNet;
    private final PetriNet inputNet;

    public ModelCheckingJobResult(ModelCheckingResult modelCheckingResult, AdamCircuitFlowLTLMCStatistics statistics, PetriNet modelCheckingNet, PetriNetWithTransits inputNet) {
        this.modelCheckingResult = modelCheckingResult;
        this.statistics = statistics;
        this.modelCheckingNet = modelCheckingNet;
        this.inputNet = inputNet;
    }

    public ModelCheckingResult getModelCheckingResult() {
        return modelCheckingResult;
    }

    public AdamCircuitFlowLTLMCStatistics getStatistics() {
        return statistics;
    }

    public PetriNet getModelCheckingNet() {
        return modelCheckingNet;
    }

    public PetriNet getInputNet() {
        return inputNet;
    }
}
