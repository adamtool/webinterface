package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.exceptions.pg.NotSupportedGameException;
import uniolunisaar.adam.symbolic.bddapproach.graph.BDDGraph;

/**
 * Different kinds of jobs that can be requested by the user to run on the server
 */
public enum JobType {
    EXISTS_WINNING_STRATEGY {
        JsonElement serialize(Object result) {
            return new JsonPrimitive((boolean) result);
        }
    }, WINNING_STRATEGY {
        JsonElement serialize(Object result) {
            return PetriNetD3.ofNetWithoutObjective((PetriGame) result);
        }
    }, GRAPH_STRATEGY_BDD {
        JsonElement serialize(Object result) {
            return BDDGraphD3.ofWholeBddGraph((BDDGraph) result);
        }
    }, MODEL_CHECKING_RESULT {
        JsonElement serialize(Object result) {
            // TODO serialize the counterexample as well if it is present
            ModelCheckingResult mcResult = (ModelCheckingResult) result;
            return new JsonPrimitive(mcResult.getSatisfied().toString());
        }
    }, MODEL_CHECKING_NET {
        JsonElement serialize(Object result) {
            try {
                return PetriNetD3.ofPetriGame(new PetriGame((PetriNet) result));
            } catch (NotSupportedGameException e) {
                throw new SerializationException(e);
            }
        }
    }, GRAPH_GAME_BDD {
        JsonElement serialize(Object result) {
            return ((BDDGraphExplorer) result).getVisibleGraph();
        }
    };

    abstract JsonElement serialize(Object result) throws SerializationException;
}