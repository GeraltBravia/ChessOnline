package Chess;

import org.mindrot.jbcrypt.BCrypt;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;

public class AuthService {
    private String jdbcUrl;
    private String dbUser;
    private String dbPass;

    public AuthService() {
        Properties props = new Properties();
        // 1) Try resources/db.properties in project root
        Path p = Paths.get("resources", "db.properties");
        try {
            if (Files.exists(p)) {
                try (InputStream is = new FileInputStream(p.toFile())) {
                    props.load(is);
                }
            } else {
                // 2) Try to load from classpath (e.g., packaged into jar)
                try (InputStream is = AuthService.class.getClassLoader().getResourceAsStream("db.properties")) {
                    if (is != null) props.load(is);
                }
            }
        } catch (Exception ex) {
            // ignore and fallback to env
        }

        String host = props.getProperty("db.host", System.getenv().getOrDefault("CHESS_DB_HOST", "localhost"));
        String port = props.getProperty("db.port", System.getenv().getOrDefault("CHESS_DB_PORT", "3306"));
        String db = props.getProperty("db.name", System.getenv().getOrDefault("CHESS_DB_NAME", "chess"));
        dbUser = props.getProperty("db.user", System.getenv().getOrDefault("CHESS_DB_USER", "root"));
        dbPass = props.getProperty("db.pass", System.getenv().getOrDefault("CHESS_DB_PASS", ""));

        // Use Java standard encoding name UTF-8 (some drivers do not accept 'utf8mb4' here)
        jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC", host, port, db);
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    private Connection getConn() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
    }

    private int calculateEloChange(int winnerElo, int loserElo, boolean isQuickPlay) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (loserElo - winnerElo) / 400.0));
        int k = isQuickPlay ? 32 : 16; // K-factor: higher for quick play
        return (int) Math.round(k * (1.0 - expectedScore));
    }

    // Returns User if success, null otherwise
    public User login(String usernameOrEmail, String password) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, elo_rating FROM users WHERE username = ? OR email = ? LIMIT 1";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    if (hash != null && BCrypt.checkpw(password, hash)) {
                        return new User(rs.getInt("id"), rs.getString("username"), rs.getString("email"), rs.getInt("elo_rating"));
                    }
                }
            }
        }
        return null;
    }

    // Returns created User or null on failure (e.g., duplicate)
    public User register(String username, String email, String password) throws SQLException {
        String checkSql = "SELECT id FROM users WHERE username = ? OR email = ? LIMIT 1";
        try (Connection c = getConn(); PreparedStatement check = c.prepareStatement(checkSql)) {
            check.setString(1, username);
            check.setString(2, email);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return null; // already exists
                }
            }

            String insertSql = "INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)";
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
            try (PreparedStatement ins = c.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ins.setString(1, username);
                ins.setString(2, hashed);
                ins.setString(3, email);
                int affected = ins.executeUpdate();
                if (affected == 0) return null;
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        return new User(id, username, email, 1200);
                    }
                }
            }
        }
        return null;
    }

    // utility to test DB connection from application
    public boolean testConnection() {
        try (Connection c = getConn()) {
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lưu kết quả game vào database và cập nhật Elo (chỉ cho Quick Play)
     * @param player1Id ID người chơi 1 (trắng)
     * @param player2Id ID người chơi 2 (đen)
     * @param winnerId ID người thắng (null nếu hòa)
     * @param isQuickPlay true nếu là chế độ Quick Play
     * @return true nếu lưu thành công
     */
    public boolean saveGameResult(int player1Id, int player2Id, Integer winnerId, boolean isQuickPlay) {
        if (!isQuickPlay) {
            return true; // Không cập nhật Elo cho chế độ tạo phòng thủ công
        }

        System.out.println("DEBUG saveGameResult: player1Id=" + player1Id + ", player2Id=" + player2Id + ", winnerId=" + winnerId);

        try (Connection c = getConn()) {
            // Lấy Elo hiện tại của cả hai người chơi
            String getEloSql = "SELECT id, elo_rating FROM users WHERE id IN (?, ?)";
            int player1Elo = 1200, player2Elo = 1200;

            try (PreparedStatement ps = c.prepareStatement(getEloSql)) {
                ps.setInt(1, player1Id);
                ps.setInt(2, player2Id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if (rs.getInt("id") == player1Id) {
                            player1Elo = rs.getInt("elo_rating");
                        } else if (rs.getInt("id") == player2Id) {
                            player2Elo = rs.getInt("elo_rating");
                        }
                    }
                }
            }

            System.out.println("DEBUG: player1Elo=" + player1Elo + ", player2Elo=" + player2Elo);

            // Tính toán thay đổi Elo
            int[] eloChanges = new int[2];
            
            if (winnerId == null) {
                // Trường hợp hòa - điểm thay đổi ít hơn
                int eloChange = calculateEloChange(player1Elo, player2Elo, true) / 2;
                eloChanges[0] = eloChange;
                eloChanges[1] = -eloChange;
                System.out.println("DEBUG: Draw - eloChanges=" + eloChanges[0] + ", " + eloChanges[1]);
            } else {
                // Xác định người thắng/thua và tính điểm
                boolean isPlayer1Winner = winnerId == player1Id;
                int winnerElo = isPlayer1Winner ? player1Elo : player2Elo;
                int loserElo = isPlayer1Winner ? player2Elo : player1Elo;
                
                int eloChange = calculateEloChange(winnerElo, loserElo, true);
                
                if (isPlayer1Winner) {
                    eloChanges[0] = eloChange;
                    eloChanges[1] = -eloChange;
                    System.out.println("DEBUG: Player1 wins - eloChanges=" + eloChanges[0] + ", " + eloChanges[1]);
                } else {
                    eloChanges[0] = -eloChange;
                    eloChanges[1] = eloChange;
                    System.out.println("DEBUG: Player2 wins - eloChanges=" + eloChanges[0] + ", " + eloChanges[1]);
                }
            }

            // Cập nhật điểm Elo mới cho cả hai người chơi
            String updateEloSql = "UPDATE users SET elo_rating = CASE " +
                                "WHEN id = ? THEN elo_rating + ? " +
                                "WHEN id = ? THEN elo_rating + ? " +
                                "END WHERE id IN (?, ?)";
            try (PreparedStatement ps = c.prepareStatement(updateEloSql)) {
                ps.setInt(1, player1Id);
                ps.setInt(2, eloChanges[0]);
                ps.setInt(3, player2Id);
                ps.setInt(4, eloChanges[1]);
                ps.setInt(5, player1Id);
                ps.setInt(6, player2Id);
                ps.executeUpdate();
            }

            // Lưu kết quả game vào database
            String insertGameSql = "INSERT INTO games (player1_id, player2_id, winner_id, elo_change_player1, elo_change_player2, end_time) VALUES (?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement ps = c.prepareStatement(insertGameSql)) {
                ps.setInt(1, player1Id);
                ps.setInt(2, player2Id);
                if (winnerId != null) {
                    ps.setInt(3, winnerId);
                } else {
                    ps.setNull(3, Types.INTEGER);
                }
                ps.setInt(4, eloChanges[0]);
                ps.setInt(5, eloChanges[1]);
                ps.executeUpdate();
            }

            // In ra log để debug
            System.out.println("DEBUG: Updated Elo - player1: " + (player1Elo + eloChanges[0]) + 
                             ", player2: " + (player2Elo + eloChanges[1]));

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy điểm Elo hiện tại của user
     * @param userId ID của user
     * @return Điểm Elo hoặc 1200 nếu không tìm thấy
     */
    public int getCurrentElo(int userId) {
        String sql = "SELECT elo_rating FROM users WHERE id = ?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("elo_rating");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1200; // Default Elo
    }
}
