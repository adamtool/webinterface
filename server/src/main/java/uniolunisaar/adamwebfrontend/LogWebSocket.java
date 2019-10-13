package uniolunisaar.adamwebfrontend;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import uniol.apt.util.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Handles WebSocket connections for sending ADAM's text output to the client
 */
@WebSocket
public class LogWebSocket {
    // Store sessions if you want to, for example, broadcast a message to all users
    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();
    private static final JsonParser parser = new JsonParser();
    private static final ConcurrentHashMap<Session, UUID> sessionUuids = new ConcurrentHashMap<>();
    // Queue of messages meant to be pushed to clients: Pair<UserContext UUID, message>.
    // The messages need to be sent in their own thread, asynchronously, because, for example, if
    // the messages are sent in the same thread that a Job is running in, that Thread can get
    // interrupted if the Job gets canceled, and that causes the Websocket to be closed with a
    // 'EofException', since Jetty recognizes that its Thread has been interrupted.
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

    private static void sendPingMessage() {
        JsonObject message = new JsonObject();
        message.addProperty("type", "ping");
        for (Session session: sessions) {
            try {
                session.getRemote().sendString(message.toString());
            } catch (IOException e) {
                System.err.println("IOException occurred when sending a websocket message");
                e.printStackTrace();
            }
        }
    }

    /**
     * Start a thread that, every 15 seconds, sends a ping message to all connected websocket
     * clients in order to stop the connections from being dropped.
     */
    public static void startPingThread() {
        new Thread(() -> {
            while (true) {
                sendPingMessage();
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }).start();
    }

    /**
     * Start a thread that continuously blocks on the websocket message queue and broadcasts
     * messages to the appropriate clients as soon as they are enqueued
     */
    public static void startMessageQueueThread() {
        new Thread(() -> {
            while (true) {
                try {
                    Pair<UUID, JsonElement> messageSpec = messageQueue.take();
                    sendWebsocketMessage(messageSpec.getFirst(), messageSpec.getSecond());
                } catch (InterruptedException e) {
                    System.err.println("Message queue thread interrupted.  Stack trace: ");
                    e.printStackTrace();
                }
            }
        }).start();
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

        if (messageJson.has("browserUuid")) {
            String browserUuidString = messageJson.get("browserUuid").getAsString();
            UUID browserUuid = UUID.fromString(browserUuidString);
            sessionUuids.put(session, browserUuid);
        } else if (messageJson.has("type") && messageJson.get("type").getAsString().equals("pong")) {
//            System.out.println("Got pong from client");
        } else {
            throw new IllegalArgumentException("Got an unrecognizable message over a websocket.\n" +
                    "Message: " + message + "\nSession: " + session.getRemoteAddress().toString());
        }
    }

    @OnWebSocketError
    public void error(Session session, Throwable error) {
        System.err.println("OnWebSocketError.  Error stack trace: ");
        error.printStackTrace();
        System.err.println("OnWebSocketError.  Error cause stack trace: ");
        error.getCause().printStackTrace();
    }

}
