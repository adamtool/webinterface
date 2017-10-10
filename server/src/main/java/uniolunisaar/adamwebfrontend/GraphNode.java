package uniolunisaar.adamwebfrontend;

public class GraphNode {
    private final String id;
    private final String label;
    private final GraphNodeType type;

    protected GraphNode(String id, String label, GraphNodeType type) {
        this.id = id;
        this.label = label;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public GraphNodeType getType() {
        return type;
    }

    public static GraphNode transition(String id, String label) {
        return new GraphNode(id, label, GraphNodeType.TRANSITION);
    }

    public static GraphNode envPlace(String id, String label) {
        return new GraphNode(id, label, GraphNodeType.ENVPLACE);
    }

    public static GraphNode sysPlace(String id, String label) {
        return new GraphNode(id, label, GraphNodeType.SYSPLACE);
    }
}
