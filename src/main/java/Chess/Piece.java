
package Chess;

import java.util.logging.Logger;
import java.util.logging.Level;

public abstract class Piece {
    private static final Logger LOGGER = Logger.getLogger(Piece.class.getName());
    private boolean killed = false;
    private boolean white = false;

    public Piece(boolean white) {
        this.setWhite(white);
    }

    public boolean isWhite() {
        return this.white;
    }

    public void setWhite(boolean white) {
        this.white = white;
    }

    public boolean isKilled() {
        return this.killed;
    }

    public void setKilled(boolean killed) {
        this.killed = killed;
    }

    protected void logMove(String pieceName, Spot start, Spot end, boolean isValid, String reason) {
        LOGGER.log(Level.INFO, "{0} move from ({1},{2}) to ({3},{4}): {5} - {6}",
            new Object[]{pieceName, start.getX(), start.getY(), end.getX(), end.getY(), 
            isValid ? "VALID" : "INVALID", reason});
    }

    protected boolean isSameSpot(Spot start, Spot end) {
        return start.getX() == end.getX() && start.getY() == end.getY();
    }

    protected boolean isSpotOccupiedByAlly(Spot spot) {
        return spot.getPiece() != null && spot.getPiece().isWhite() == this.isWhite();
    }

    protected boolean isPathClear(Board board, Spot start, Spot end) {
        // This should be implemented by pieces that need path checking (Rook, Bishop, Queen)
        return true;
    }

    public abstract boolean canMove(Board board, Spot start, Spot end);
}
