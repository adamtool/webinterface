package uniolunisaar.adamwebfrontend;

/**
 * Represents a link in a directed graph that will be displayed using D3 in our client.
 * This class is meant to be serialized using GSON and exposed to the client through our
 * JSON-HTTP interface.
 */
abstract class GraphLink {
    private final String source;
    private final String target;

    GraphLink(String source, String target) {
        this.source = source;
        this.target = target;
    }
}
