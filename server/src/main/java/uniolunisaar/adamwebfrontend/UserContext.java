package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import uniolunisaar.adam.ds.petrigame.PetriGame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import static uniolunisaar.adamwebfrontend.CalculationStatus.COMPLETED;
import static uniolunisaar.adamwebfrontend.CalculationStatus.FAILED;
import static uniolunisaar.adamwebfrontend.CalculationType.*;

/**
 * Stores data related to a single user session
 */
public class UserContext {
    // When we calculate a graph game BDD from a petri game, we convert the petri game to APT, then
    // put the (APT, BDDGraphExplorer) pair in here
    private final Map<String, Calculation<BDDGraphExplorer>> bddGraphsOfApts = new ConcurrentHashMap<>();
    // Store the results of "existsWinningStrategy"
    private final Map<String, Calculation<Boolean>> existsWinningStrategyOfApts =
            new ConcurrentHashMap<>();
    // Store the results of "getStrategyBdd"
    private final Map<String, Calculation<PetriGame>> strategyBddsOfApts =
            new ConcurrentHashMap<>();

    public JsonArray getCalculationList() {
        JsonArray result = new JsonArray();
        for (String aptOfPetriGame : this.bddGraphsOfApts.keySet()) {
            Calculation<BDDGraphExplorer> calculation = this.bddGraphsOfApts.get(aptOfPetriGame);
            JsonObject entry = calculationListEntry(calculation, aptOfPetriGame, GRAPH_GAME_BDD);
            result.add(entry);
        }
        for (String canonicalApt : this.existsWinningStrategyOfApts.keySet()) {
            Calculation<Boolean> calculation = this.existsWinningStrategyOfApts.get(canonicalApt);
            JsonObject entry = calculationListEntry(
                    calculation, canonicalApt, EXISTS_WINNING_STRATEGY);
            if (calculation.getStatus() == COMPLETED) {
                try {
                    entry.addProperty("result", calculation.getResult());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    throw new RuntimeException("calculation.getResult() threw an exception, " +
                            "although calculation.getStatus() == COMPLETED.  This is a bug.  " +
                            "Please let Ann know about it. :)");
                }
            }
            result.add(entry);
        }
        for (String canonicalApt : this.strategyBddsOfApts.keySet()) {
            Calculation<PetriGame> calculation = this.strategyBddsOfApts.get(canonicalApt);
            JsonObject entry = calculationListEntry(
                    calculation, canonicalApt, WINNING_STRATEGY);
            result.add(entry);
        }
        return result;
    }

    private static JsonObject calculationListEntry(Calculation calculation,
                                            String canonicalApt,
                                            CalculationType calculationType) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", calculationType.toString());
        entry.addProperty("canonicalApt", canonicalApt);
        entry.addProperty("calculationStatus", calculation.getStatus().toString());
        entry.addProperty("timeStarted", calculation.getTimeStarted().getEpochSecond());
        entry.addProperty("timeFinished", calculation.getTimeFinished().getEpochSecond());
        if (calculation.getStatus() == FAILED) {
            entry.addProperty("failureReason", calculation.getFailedReason());
        }
        return entry;
    }
}
