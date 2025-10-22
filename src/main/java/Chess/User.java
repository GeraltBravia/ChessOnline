package Chess;

public class User {
    private int id;
    private String username;
    private String email;
    private int eloRating;

    public User(int id, String username, String email, int eloRating) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.eloRating = eloRating;
    }
    public void setEloRating(int eloRating) {
        this.eloRating = eloRating;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public int getEloRating() { return eloRating; }
}
