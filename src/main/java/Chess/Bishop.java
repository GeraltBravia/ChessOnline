
package Chess;


public class Bishop extends Piece {
    public Bishop(boolean white) {
        super(white);
    }

    @Override
    public boolean canMove(Board board, Spot start, Spot end) {
        if (isSameSpot(start, end)) {
            logMove("Bishop", start, end, false, "Cannot move to same spot");
            return false;
        }

        if (isSpotOccupiedByAlly(end)) {
            logMove("Bishop", start, end, false, "Cannot capture friendly piece");
            return false;
        }

        int startX = start.getX();
        int startY = start.getY();
        int endX = end.getX();
        int endY = end.getY();

        int deltaX = Math.abs(startX - endX);
        int deltaY = Math.abs(startY - endY);

        // Chỉ di chuyển chéo
        if (deltaX != deltaY) {
            logMove("Bishop", start, end, false, "Must move diagonally");
            return false;
        }

        // Kiểm tra đường đi không bị cản
        int xStep = startX < endX ? 1 : -1;
        int yStep = startY < endY ? 1 : -1;
        
        for (int i = 1; i < deltaX; i++) {
            if (board.getBox(startX + i * xStep, startY + i * yStep).getPiece() != null) {
                logMove("Bishop", start, end, false, "Path is blocked");
                return false;
            }
        }

        logMove("Bishop", start, end, true, "Valid diagonal move");
        return true;
    }
}
