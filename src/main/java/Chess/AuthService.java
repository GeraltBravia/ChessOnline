package Chess;

import org.mindrot.jbcrypt.BCrypt;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.sql.*;

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
}
