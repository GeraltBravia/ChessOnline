
package Chess;

public class Knight extends Piece {
    public Knight(boolean white) {
        super(white);
    }

    @Override
    public boolean canMove(Board board, Spot start, Spot end) {
        // Check if moving to same spot
        if (isSameSpot(start, end)) {
            logMove("Knight", start, end, false, "Cannot move to same spot");
            return false;
        }

        // Check if destination has friendly piece
        if (isSpotOccupiedByAlly(end)) {
            logMove("Knight", start, end, false, "Destination occupied by friendly piece");
            return false;
        }

        int x = Math.abs(start.getX() - end.getX());
        int y = Math.abs(start.getY() - end.getY());
        boolean isValidKnightMove = x * y == 2;

        if (isValidKnightMove) {
            logMove("Knight", start, end, true, "Valid L-shaped move");
        } else {
            logMove("Knight", start, end, false, "Invalid move pattern - must be L-shaped");
        }

        return isValidKnightMove;
    }
}
