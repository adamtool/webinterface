package uniolunisaar.adamwebfrontend;

/**
 * Represents a link in a graph corresponding to a PetriNet.
 * This class is meant to be serialized using GSON and exposed to the client through our
 * JSON-HTTP interface.
 */
public class GraphLink {
    private final String source;
    private final String target;

    public GraphLink(String source, String target) {
        this.source = source;
        this.target = target;
    }
}
