package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonElement;

public interface BDDGraphExplorer {
    JsonElement getVisibleGraph();

    void toggleStatePostset(int stateId);

    void toggleStatePreset(int stateId);
}
