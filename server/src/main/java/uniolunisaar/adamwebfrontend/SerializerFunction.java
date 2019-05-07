package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonElement;

/**
 * This interface represents a function that we can call to serialize an object of a given
 * class into a JSON representation that our browser client can work with.
 */
public interface SerializerFunction<T> {
    JsonElement apply(T t) throws Exception;
}
