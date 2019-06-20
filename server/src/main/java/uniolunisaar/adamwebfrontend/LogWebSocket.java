package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles WebSocket connections for sending ADAM's text output to the client
 */
@WebSocket
public class LogWebSocket {
    // Store sessions if you want to, for example, broadcast a message to all users
    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();
    private static final JsonParser parser = new JsonParser();
    private static final ConcurrentHashMap<Session, UUID> sessionUuids = new ConcurrentHashMap<>();

    /**
     * @return A PrintStream that will only send output to Websocket clients that have associated
     * themselves with the given UserContext via its UUID.
     */
    public static PrintStream makePrintStream(int loggingLevel, UUID userContextUuid) {
        OutputStream outputStream = makeOutputStream(loggingLevel, userContextUuid);
        try {
            return new PrintStream(outputStream, false, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("Had a problem initializing the print stream for ADAM's Logging " +
                    "functionality: This system doesn't support the encoding UTF-8.  " +
                    "Suggestion: Refactor the Logging class to use PrintWriter instead of PrintStream?" +
                    "You can convert System.out into a PrintWriter using new PrintWriter(System.out).");
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Warning: Using System.out!  Logs won't be sent to the web UI.");
        return new PrintStream(System.out); // This should be unreachable, because we call System.exit() in the catch block

    }

    private static OutputStream makeOutputStream(int loggingLevel, UUID userContextUuid) {
        return new OutputStream() {
            StringBuffer sb = new StringBuffer();

            @Override
            public void write(int b) throws IOException {
                byte bb = (byte) b;
                String s = new String(new byte[]{bb}, "UTF-8");
                sb.append(s);
                System.out.print(s);
                if (b == '\n') {
                    this.flush();
                }
            }

            @Override
            public void flush() throws IOException {
                String message = sb.toString();
                sb.setLength(0);
                JsonObject messageJson = new JsonObject();
                messageJson.addProperty("type", "serverLogMessage");
                messageJson.addProperty("level", loggingLevel);
                messageJson.addProperty("message", message);
                sendWebsocketMessage(userContextUuid, messageJson);
            }
        };

    }

    /**
     * Broadcast a message to all sessions associated with the given user context.
     */
    public static void sendWebsocketMessage(UUID userContextUuid, JsonElement message) {
        for (Session session : sessions) {
            UUID sessionUuid = sessionUuids.get(session);
            if (userContextUuid.equals(sessionUuid)) {
                try {
                    session.getRemote().sendString(message.toString());
                } catch (IOException e) {
                    System.err.println("IOException occurred when sending a websocket message");
                    e.printStackTrace();
                }
            }
        }

    }

    @OnWebSocketConnect
    public void connected(Session session) {
        sessions.add(session);
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        sessions.remove(session);
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        JsonObject messageJson = parser.parse(message).getAsJsonObject();

        if (messageJson.has("browserUuid")) {
            String browserUuidString = messageJson.get("browserUuid").getAsString();
            UUID browserUuid = UUID.fromString(browserUuidString);
            sessionUuids.put(session, browserUuid);
        } else {
            throw new IllegalArgumentException("Got an unrecognizable message over a websocket.\n" +
                    "Message: " + message + "\nSession: " + session.getRemoteAddress().toString());
        }
    }

    @OnWebSocketError
    public void error(Session session, Throwable error) {
        error.printStackTrace();
    }

}
