package uniolunisaar.adamwebfrontend;

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
    private final List<GraphLink> links;
    private final List<GraphNode> nodes;

    private PetriNetD3(List<GraphLink> links, List<GraphNode> nodes) {
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
    public static PetriNetD3 of(PetriNet net) {
        List<GraphLink> links = new ArrayList<>();
        List<GraphNode> nodes = new ArrayList<>();

        for (Place place : net.getPlaces()) {
            boolean isBad = AdamExtensions.isBad(place);
            if (AdamExtensions.isEnviroment(place)) {
                nodes.add(GraphNode.envPlace(place.getId(), place.getId(), isBad));
            } else {
                nodes.add(GraphNode.sysPlace(place.getId(), place.getId(), isBad));
            }

            // TODO: Can this be done neater using a loop like "for (Flow edge : net.getEdges()) {...}"?
            for (Transition preTransition : place.getPreset()) {
                GraphLink link = new GraphLink(preTransition.getId(), place.getId());
                links.add(link);
            }
            for (Transition postTransition : place.getPostset()) {
                GraphLink link = new GraphLink(place.getId(), postTransition.getId());
                links.add(link);
            }
        }

        for (Transition transition : net.getTransitions()) {
            GraphNode transitionNode = GraphNode.transition(transition.getId(), transition.getLabel());
            nodes.add(transitionNode);
        }

        return new PetriNetD3(links, nodes);
    }

}
