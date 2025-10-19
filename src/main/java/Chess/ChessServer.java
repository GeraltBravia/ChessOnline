

package Chess;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author ACER LAPTOP
 */
public class ChessServer {
    private static final int PORT = 12345;
    static List<ClientHandler> clients = new ArrayList<>();
    static List<GameSession> games = new ArrayList<>(); // package-private so other classes in package can access

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            System.out.println("Starting Chess Server on port " + PORT + "...");
            serverSocket = new ServerSocket(PORT);
            System.out.println("Chess Server is running on port " + PORT);
            System.out.println("Waiting for players to connect...");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket);

                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    synchronized (clients) {
                        clients.add(clientHandler);
                    }
                    new Thread(clientHandler).start();

                    if (clients.size() >= 2) {
                        ClientHandler player1 = clients.get(clients.size() - 2);
                        ClientHandler player2 = clients.get(clients.size() - 1);
                        GameSession game = new GameSession(player1, player2);
                        synchronized (games) {
                            games.add(game);
                        }
                        game.startGame();
                    }
                } catch (IOException e) {
                    System.out.println("Error handling client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + PORT);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing server socket: " + e.getMessage());
                }
            }
        }
    }
}
