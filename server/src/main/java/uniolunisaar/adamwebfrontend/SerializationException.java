package uniolunisaar.adamwebfrontend;

public class SerializationException extends RuntimeException {
   public SerializationException(Exception reason) {
       super(reason);
   }
}
