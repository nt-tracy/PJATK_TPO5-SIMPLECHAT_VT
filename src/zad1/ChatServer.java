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
    private final Map<Socket, PrintWriter> clientWriters = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private volatile boolean running = false;

    public ChatServer(int port) {
        this.port = port;
    }

    public void startServer() {
        running = true;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Thread.ofPlatform().start(() -> {
            System.out.println("Server started");
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    executor.submit(() -> handleClient(socket));
                } catch (IOException e) {
                    if (running) break;
                }
            }
        });
    }

    private void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                parseCommand(socket, out, line);
            }
        } catch (IOException e) {
        } finally {
            String id;
            synchronized (this) {
                id = clientMap.get(socket);
                if (id != null) {
                    String msg = id + " logged out";
                    broadcast(msg);
                    addToLog(msg);
                    clientWriters.remove(socket);
                    clientMap.remove(socket);
                }
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private synchronized void parseCommand(Socket socket, PrintWriter out, String input) {
        if (input.startsWith("LOGIN ")) {
            String id = input.substring(6);
            clientMap.put(socket, id);
            clientWriters.put(socket, out);
            String msg = id + " logged in";
            broadcast(msg);
            addToLog(msg);
        } else if (input.equals("LOGOUT")) {
            String id = clientMap.get(socket);
            if (id != null) {
                String msg = id + " logged out";
                broadcast(msg);
                addToLog(msg);
                clientWriters.remove(socket);
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

    private synchronized void broadcast(String message) {
        for (PrintWriter writer : clientWriters.values()) {
            writer.println(message);
        }
    }

    private synchronized void addToLog(String message) {
        String ts = LocalTime.now().format(formatter);
        serverLog.append(ts).append(" ").append(message).append("\n");
    }

    public void stopServer() {
        synchronized (this) {
            running = false;
            String closeMsg = "ChatServer: chat closed";
            broadcast(closeMsg);
            addToLog(closeMsg);
            clientWriters.clear();
            clientMap.clear();
        }

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