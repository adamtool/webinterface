package uniolunisaar.adamwebfrontend;

import uniol.apt.adt.pn.PetriNet;

public class PetriGameD3 {
    final private PetriNetD3 net;
    final private String uuid;

    private PetriGameD3(PetriNetD3 net, String uuid) {
        this.net = net;
        this.uuid = uuid;
    }

    public static PetriGameD3 of(PetriNet net, String uuid) {
        return new PetriGameD3(PetriNetD3.of(net), uuid);
    }
}
