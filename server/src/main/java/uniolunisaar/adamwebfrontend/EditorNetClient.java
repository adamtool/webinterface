package uniolunisaar.adamwebfrontend;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class EditorNetClient {
    final private String uuid;
    final private JsonElement net;
    final private JsonElement initialMarking;

    private EditorNetClient(JsonElement net, String uuid, JsonElement initialMarking) {
        this.uuid = uuid;
        this.net = net;
        this.initialMarking = initialMarking;
    }

    public static JsonElement of(JsonElement net, String uuid, JsonElement initialMarking) {
        EditorNetClient editorNetClient = new EditorNetClient(net, uuid, initialMarking);
        return new Gson().toJsonTree(editorNetClient);
    }
}
