

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
    static Map<String, RoomInfo> rooms = new HashMap<>(); // Quản lý các phòng với mã phòng
    static List<ClientHandler> waitingPlayers = new ArrayList<>(); // Hàng đợi cho chế độ chơi nhanh
    
    // Phương thức tạo mã phòng ngẫu nhiên
    public static String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
    
    // Phương thức tự động ghép cặp cho chế độ chơi nhanh
    public static void joinQuickPlay(ClientHandler player) {
        synchronized (waitingPlayers) {
            if (waitingPlayers.isEmpty()) {
                // Không có người chờ, thêm vào hàng đợi
                waitingPlayers.add(player);
                player.sendMessage("WAITING_FOR_OPPONENT");
                System.out.println(player.getPlayerId() + " joined quick play queue. Waiting for opponent...");
            } else {
                // Có người chờ, ghép cặp ngay
                ClientHandler opponent = waitingPlayers.remove(0);
                
                System.out.println("Quick play match found!");
                System.out.println("Player 1 (WHITE): " + opponent.getPlayerId());
                System.out.println("Player 2 (BLACK): " + player.getPlayerId());
                
                // Tạo game session
                GameSession game = new GameSession(opponent, player);
                synchronized (games) {
                    games.add(game);
                }
                
                // Bắt đầu game
                game.startGame();
            }
        }
    }
    
    // Phương thức hủy chờ quick play
    public static void cancelQuickPlay(ClientHandler player) {
        synchronized (waitingPlayers) {
            if (waitingPlayers.remove(player)) {
                player.sendMessage("QUICK_PLAY_CANCELLED");
                System.out.println(player.getPlayerId() + " cancelled quick play.");
            }
        }
    }

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
                    
                    // Không tự động ghép cặp nữa, đợi người chơi tạo/vào phòng
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
