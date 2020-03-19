package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class PetriGameD3 {
    final private String uuid;
    final private JsonElement net;
    final private JsonElement initialMarking;

    private PetriGameD3(JsonElement net, String uuid, JsonElement initialMarking) {
        this.uuid = uuid;
        this.net = net;
        this.initialMarking = initialMarking;
    }

    public static JsonElement of(JsonElement net, String uuid, JsonElement initialMarking) {
        PetriGameD3 petriGameD3 = new PetriGameD3(net, uuid, initialMarking);
        return new Gson().toJsonTree(petriGameD3);
    }
}
