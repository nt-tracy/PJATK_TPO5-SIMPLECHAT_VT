/**
 *
 *  @author Tracewicz Natalia s33507
 *
 */

package zad1;

import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private final int port;
    private ServerSocket serverSocket;
    private final StringBuilder serverLog = new StringBuilder();
    private final Map<Socket, String> clientMap = new ConcurrentHashMap<>();
    private final List<PrintWriter> clientWriters = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private volatile boolean running = false;

    public ChatServer(int port) {
        this.port = port;
    }

    public void startServer() {
        running = true;
        Thread.ofPlatform().start(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Server started");
                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        executor.submit(() -> handleClient(socket));
                    } catch (IOException e) {
                        if (running) e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        });
    }

    private void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            clientWriters.add(out);
            String line;
            while ((line = in.readLine()) != null) {
                parseCommand(socket, out, line);
            }
        } catch (IOException e) {
            // Połączenie przerwane
        } finally {
            String id = clientMap.remove(socket);
            if (id != null) {
                String msg = id + " logged out";
                broadcast(msg);
                addToLog(msg);
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void parseCommand(Socket socket, PrintWriter out, String input) {
        if (input.startsWith("LOGIN ")) {
            String id = input.substring(6);
            clientMap.put(socket, id);
            String msg = id + " logged in";
            broadcast(msg);
            addToLog(msg);
        } else if (input.equals("LOGOUT")) {
            String id = clientMap.get(socket);
            if (id != null) {
                String msg = id + " logged out";
                broadcast(msg);
                addToLog(msg);
                clientMap.remove(socket);
            }
        } else {
            String id = clientMap.get(socket);
            if (id != null) {
                String msg = id + ": " + input;
                broadcast(msg);
                addToLog(msg);
            }
        }
    }

    private void broadcast(String message) {
        for (PrintWriter writer : clientWriters) {
            writer.println(message);
        }
    }

    private synchronized void addToLog(String message) {
        String ts = LocalTime.now().format(formatter);
        serverLog.append(ts).append(" ").append(message).append("\n");
    }

    public void stopServer() {
        running = false;
        String closeMsg = "ChatServer: chat closed";
        broadcast(closeMsg);
        addToLog(closeMsg);

        try {
            if (serverSocket != null) serverSocket.close();
            executor.shutdownNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server stopped");
    }

    public String getServerLog() {
        return serverLog.toString();
    }
}