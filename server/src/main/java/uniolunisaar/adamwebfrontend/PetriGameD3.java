package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class PetriGameD3 {
    final private String uuid;
    final private JsonElement net;

    private PetriGameD3(JsonElement net, String uuid) {
        this.uuid = uuid;
        this.net = net;
    }

    public static JsonElement of(JsonElement net, String uuid) {
        PetriGameD3 petriGameD3 = new PetriGameD3(net, uuid);
        return new Gson().toJsonTree(petriGameD3);
    }
}
