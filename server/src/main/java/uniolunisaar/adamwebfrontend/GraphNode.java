package uniolunisaar.adamwebfrontend;

/**
 * Represents a node in a graph that will be displayed in our client.
 * This class is meant to be serialized using GSON and exposed to the client through our
 * JSON-HTTP interface.
 */
public abstract class GraphNode {
    private final String id;
    private final String label;
    private final GraphNodeType type;

    protected GraphNode(String id, String label, GraphNodeType type) {
        this.id = id;
        this.label = label;
        this.type = type;
    }

}
