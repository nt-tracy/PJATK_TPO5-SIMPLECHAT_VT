/**
 *
 *  @author Tracewicz Natalia s33507
 *
 */

package zad1;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatClient {
    private final String host;
    private final int port;
    private final String id;
    private Socket socket;
    private PrintWriter out;
    private final StringBuilder chatView = new StringBuilder();
    private final ExecutorService receiverExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public ChatClient(String host, int port, String id) {
        this.host = host;
        this.port = port;
        this.id = id;
    }

    public void login() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);

            receiverExecutor.submit(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        chatView.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            });

            send("LOGIN " + id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        send("LOGOUT");
    }

    public void send(String req) {
        if (out != null) {
            out.println(req);
        }
    }

    public String getChatView() {
        return chatView.toString();
    }

    public String getId(){
        return id;
    }
}

