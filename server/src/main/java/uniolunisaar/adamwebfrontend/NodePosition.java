package uniolunisaar.adamwebfrontend;

/**
 * This represents the x/y coordinates of a node in our graph viewer UI.
 * It's meant to be converted to/from JSON.
 */
public class NodePosition {
    final double x;
    final double y;

    public NodePosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
