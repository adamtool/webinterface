package uniolunisaar.adamwebfrontend;

public class GraphLink {
    private final String source;
    private final String target;

    public GraphLink(String source, String target) {
        this.source = source;
        this.target = target;
    }
    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }
}
