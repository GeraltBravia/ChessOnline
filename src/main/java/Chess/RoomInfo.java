package Chess;

public class RoomInfo {
    private final String roomCode;
    private final ClientHandler host;
    private ClientHandler joiner;
    private final String preferredColor;

    public RoomInfo(String roomCode, ClientHandler host, String preferredColor) {
        this.roomCode = roomCode;
        System.out.println("Creating new room with code: " + roomCode + ", host and color: " + preferredColor); // Log tạo phòng mới
        this.host = host;
        // Đảm bảo preferredColor là một trong các giá trị hợp lệ
        if (preferredColor.equals("WHITE") || preferredColor.equals("BLACK") || preferredColor.equals("RANDOM")) {
            this.preferredColor = preferredColor;
        } else {
            System.out.println("Invalid color preference, defaulting to RANDOM"); // Log lỗi màu không hợp lệ
            this.preferredColor = "RANDOM";
        }
    }

    public boolean isFull() {
        return joiner != null;
    }

    public void addPlayer(ClientHandler player) {
        if (!isFull()) {
            this.joiner = player;
        }
    }

    public ClientHandler getHost() {
        return host;
    }
    
    public ClientHandler getJoiner() {
        return joiner;
    }
    
    public String getRoomCode() {
        return roomCode;
    }

    public String getPreferredColor() {
        return preferredColor;
    }
}