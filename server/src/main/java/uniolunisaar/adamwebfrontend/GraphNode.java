package uniolunisaar.adamwebfrontend;

/**
 * Represents a node in a graph corresponding to a PetriNet.
 * This class is meant to be serialized using GSON and exposed to the client through our
 * JSON-HTTP interface.
 */
public class GraphNode {
    private final String id;
    private final String label;
    private final GraphNodeType type;
    private final boolean isBad;

    protected GraphNode(String id, String label, GraphNodeType type, boolean isBad) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.isBad = isBad;
    }

    public static GraphNode transition(String id, String label) {
        // Transitions are never bad
        return new GraphNode(id, label, GraphNodeType.TRANSITION, false);
    }

    public static GraphNode envPlace(String id, String label, boolean isBad) {
        return new GraphNode(id, label, GraphNodeType.ENVPLACE, isBad);
    }

    public static GraphNode sysPlace(String id, String label, boolean isBad) {
        return new GraphNode(id, label, GraphNodeType.SYSPLACE, isBad);
    }
}
