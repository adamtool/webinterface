package uniolunisaar.adamwebfrontend;

// Some routes (e.g. /fireTransition) may operate upon one of several different types of nets.
// This enum has one entry for each possible type.
public enum NetType {
    PETRI_NET, PETRI_NET_WITH_TRANSITS, PETRI_GAME
}
