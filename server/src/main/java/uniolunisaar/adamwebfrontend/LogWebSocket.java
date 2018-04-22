package uniolunisaar.adamwebfrontend;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles WebSocket connections for sending ADAM's text output to the client
 */
@WebSocket
public class LogWebSocket {
    // Store sessions if you want to, for example, broadcast a message to all users
    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();
    private static final OutputStream outputStream = new OutputStream() {
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
            for (Session session : sessions) {
                session.getRemote().sendString(message);
            }
        }
    };

    private static final PrintStream printStream = initPrintStream();

    public static PrintStream getPrintStream() {
        return printStream;
    }

    // TODO Think about initializing this some better way... Seems kind of brittle to have a System.exit()
    // call happen in the initialization of a static class.
    // TODO Figure out how to handle multiple users.  Not everyone should see everyone else's output.
    private static PrintStream initPrintStream() {
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
        System.out.println("Got: " + message);   // Print message
        session.getRemote().sendString(message); // and send it back
    }
}