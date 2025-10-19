package Chess;

public class DbCheck {
    public static void main(String[] args) {
        AuthService a = new AuthService();
    System.out.println("JDBC URL used: " + a.getJdbcUrl());
        boolean ok = a.testConnection();
        if (ok) System.out.println("DB connection OK");
        else System.out.println("DB connection FAILED");
    }
}
