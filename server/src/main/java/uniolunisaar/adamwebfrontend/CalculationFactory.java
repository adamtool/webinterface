package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonObject;
import uniolunisaar.adam.ds.petrigame.PetriGame;

/**
 * Created by Ann on 4/1/19.
 */
@FunctionalInterface
public interface CalculationFactory<T> {
    Calculation<T> createCalculation(PetriGame p, JsonObject params);
}
