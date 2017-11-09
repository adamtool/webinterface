package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.util.AdamExtensions;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the data needed to display a PetriNet in our graph editor.
 * This class is meant to be serialized using GSON and fed directly into our D3 code.
 */
public class PetriNetD3 {
    private final List<PetriNetLink> links;
    private final List<PetriNetNode> nodes;

    private PetriNetD3(List<PetriNetLink> links, List<PetriNetNode> nodes) {
        this.links = links;
        this.nodes = nodes;
    }

    /**
     * Extract all the information needed to display a PetriNet in our graph editor.
     * @param net - A PetriNet
     * @return A serializable object containing the relevant information from the PetriNet
     * <p>
     * See https://github.com/d3/d3-force
     */
    public static JsonElement of(PetriNet net) {
        List<PetriNetLink> links = new ArrayList<>();
        List<PetriNetNode> nodes = new ArrayList<>();

        for (Place place : net.getPlaces()) {
            boolean isBad = AdamExtensions.isBad(place);
            if (AdamExtensions.isEnvironment(place)) {
                nodes.add(PetriNetNode.envPlace(place.getId(), place.getId(), isBad));
            } else {
                nodes.add(PetriNetNode.sysPlace(place.getId(), place.getId(), isBad));
            }

            // TODO: Can this be done neater using a loop like "for (Flow edge : net.getEdges()) {...}"?
            for (Transition preTransition : place.getPreset()) {
                PetriNetLink link = PetriNetLink.of(preTransition, place);
                links.add(link);
            }
            for (Transition postTransition : place.getPostset()) {
                PetriNetLink link = PetriNetLink.of(place, postTransition);
                links.add(link);
            }
        }

        for (Transition transition : net.getTransitions()) {
            PetriNetNode transitionNode = PetriNetNode.transition(transition.getId(), transition.getLabel());
            nodes.add(transitionNode);
        }

        PetriNetD3 petriNetD3 = new PetriNetD3(links, nodes);
        return new Gson().toJsonTree(petriNetD3);
    }

    static class PetriNetLink extends GraphLink {
        private PetriNetLink(String source, String target) {
            super(source, target);
        }
        static PetriNetLink of(Place place, Transition transition) {
            return new PetriNetLink(place.getId(), transition.getId());
        }
        static PetriNetLink of(Transition transition, Place place) {
            return new PetriNetLink(transition.getId(), place.getId());
        }
    }

    static class PetriNetNode extends GraphNode {
        private final boolean isBad;

        PetriNetNode(String id, String label, GraphNodeType type, boolean isBad) {
            super(id, label, type);
            this.isBad = isBad;
        }

        static PetriNetNode transition(String id, String label) {
            // Transitions are never bad
            return new PetriNetNode(id, label, GraphNodeType.TRANSITION, false);
        }

        static PetriNetNode envPlace(String id, String label, boolean isBad) {
            return new PetriNetNode(id, label, GraphNodeType.ENVPLACE, isBad);
        }

        static PetriNetNode sysPlace(String id, String label, boolean isBad) {
            return new PetriNetNode(id, label, GraphNodeType.SYSPLACE, isBad);
        }
    }
}
