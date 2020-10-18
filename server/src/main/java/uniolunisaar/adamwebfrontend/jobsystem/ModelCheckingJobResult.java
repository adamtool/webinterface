package uniolunisaar.adamwebfrontend.jobsystem;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.modelchecking.cex.CounterExample;
import uniolunisaar.adam.ds.modelchecking.cex.ReducedCounterExample;
import uniolunisaar.adam.ds.modelchecking.results.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

public class ModelCheckingJobResult {

    private final ModelCheckingResult modelCheckingResult;
    private final AdamCircuitFlowLTLMCStatistics statistics;
    private final CounterExample counterExample;
    private final ReducedCounterExample reducedCexMc;
    private final ReducedCounterExample reducedCexInputNet;
    private final String formulaRepresentation;
    private PetriNet modelCheckingNet;
    private PetriNetWithTransits inputNet;

    public ModelCheckingJobResult(ModelCheckingResult modelCheckingResult,
                                  AdamCircuitFlowLTLMCStatistics statistics,
                                  CounterExample counterExample, ReducedCounterExample reducedCexMc,
                                  ReducedCounterExample reducedCexInputNet,
                                  String formulaRepresenation,
                                  PetriNet modelCheckingNet, PetriNetWithTransits inputNet) {
        this.modelCheckingResult = modelCheckingResult;
        this.statistics = statistics;
        this.counterExample = counterExample;
        this.reducedCexMc = reducedCexMc;
        this.reducedCexInputNet = reducedCexInputNet;
        this.formulaRepresentation = formulaRepresenation;
        this.modelCheckingNet = modelCheckingNet;
        this.inputNet = inputNet;
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

    public String getFormulaRepresentation() {
        return formulaRepresentation;
    }

    public PetriNet getModelCheckingNet() {
        return modelCheckingNet;
    }

    public PetriNetWithTransits getInputNet() {
        return inputNet;
    }
}
