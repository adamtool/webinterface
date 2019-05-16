package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonObject;
import uniolunisaar.adam.ds.petrigame.PetriGame;

/**
 * Created by Ann on 4/1/19.
 */
@FunctionalInterface
public interface JobFactory<T> {
    Job<T> createJob(PetriGame p, JsonObject params);
}
