/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

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
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chess Server started on port " + PORT);

            while (true) {
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
