package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jetty.websocket.api.CloseException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import uniol.apt.util.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Handles all WebSocket connections.
 * ADAM's log output is pushed over websocket, as are the notifications when jobs start and finish.
 */
@WebSocket
public class WebSocketHandler {
    // Store sessions if you want to, for example, broadcast a message to all users
    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();
    private static final JsonParser parser = new JsonParser();
    // Map from websocket Sessions to client UUIDs.  Each session will only receive messages
    // addressed to the clientUuid under which it is registered.
    // Clients can register themselves by sending a message { clientUuid: ... } via websocket.
    private static final ConcurrentHashMap<Session, UUID> sessionUuids = new ConcurrentHashMap<>();
    // Queue of messages which should be pushed to clients: For example, log output from ADAM or
    // notifications when jobs are complete.
    // The messages need to be sent in a separate thread like this because, for example, if
    // the messages were sent in the very same thread that a Job was running in, that Thread would
    // get interrupted if the Job got canceled, and that would cause the Websocket to be closed
    // with a 'EofException' by Jetty.
    private static final BlockingQueue<Pair<UUID, JsonElement>> messageQueue =
            new LinkedBlockingQueue<>();

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
                queueWebsocketMessage(userContextUuid, messageJson);
            }
        };

    }

    public static void queueWebsocketMessage(UUID userContextUuid, JsonObject messageJson) {
        messageQueue.add(new Pair<>(userContextUuid, messageJson));
    }

    /**
     * Broadcast a message to all sessions associated with the given user context.
     */
    private static void sendWebsocketMessage(UUID userContextUuid, JsonElement message) {
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

    /**
     * Start a thread that continuously blocks on the websocket message queue and broadcasts
     * messages to the appropriate clients as soon as they are enqueued
     */
    public static void startMessageQueueThread() {
        Thread messageQueueThread = new Thread(() -> {
            while (true) {
                try {
                    Pair<UUID, JsonElement> messageSpec = messageQueue.take();
                    sendWebsocketMessage(messageSpec.getFirst(), messageSpec.getSecond());
                } catch (InterruptedException e) {
                    System.err.println("Message queue thread interrupted.  Stack trace: ");
                    e.printStackTrace();
                }
            }
        });

        messageQueueThread.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
            System.err.println("Message queue thread crashed with an uncaught exception. Stack " +
                    "trace: ");
            e.printStackTrace();
        });

        messageQueueThread.start();
    }

    @OnWebSocketConnect
    public void connected(Session session) {
        sessions.add(session);
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        System.out.println("Websocket closed.  Statuscode: " + statusCode + ", reason: " + reason);
        sessions.remove(session);
    }

    @OnWebSocketMessage
    public void message(Session session, String message) {
        JsonObject messageJson = parser.parse(message).getAsJsonObject();

        if (messageJson.has("clientUuid")) {
            // Associate the client's session with the given clientUuid
            String clientUuidString = messageJson.get("clientUuid").getAsString();
            UUID clientUuid = UUID.fromString(clientUuidString);
            sessionUuids.put(session, clientUuid);
        } else if (messageJson.has("type") &&
                messageJson.get("type").getAsString().equals("heartbeat")) {
            return; // We don't have to do anything.
        } else {
            throw new IllegalArgumentException("Got an unrecognized message over a websocket.\n" +
                    "Message: " + message + "\nSession: " + session.getRemoteAddress().toString());
        }
    }

    @OnWebSocketError
    public void error(Session session, Throwable error) {
        // Reduce log spam by not logging 'CloseException' due to timeout
        if (error instanceof CloseException && error.getCause() instanceof TimeoutException) {
            return;
        }
        System.err.println("OnWebSocketError.  Error stack trace: ");
        error.printStackTrace();
        if (error.getCause() != null) {
            System.err.println("OnWebSocketError.  Error cause stack trace: ");
            error.getCause().printStackTrace();
        }
    }

}
