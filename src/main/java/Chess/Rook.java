
package Chess;


public class Rook extends Piece {
    public Rook(boolean white) {
        super(white);
    }

    @Override
    public boolean canMove(Board board, Spot start, Spot end) {
        if (end.getPiece() != null && end.getPiece().isWhite() == this.isWhite()) {
            return false;
        }

        int startX = start.getX();
        int startY = start.getY();
        int endX = end.getX();
        int endY = end.getY();

        if (startX != endX && startY != endY) {
            return false;
        }

        // Kiểm tra đường đi không bị cản
        if (startX == endX) {
            int yStep = startY < endY ? 1 : -1;
            for (int y = startY + yStep; y != endY; y += yStep) {
                if (board.getBox(startX, y).getPiece() != null) {
                    return false;
                }
            }
        } else if (startY == endY) {
            int xStep = startX < endX ? 1 : -1;
            for (int x = startX + xStep; x != endX; x += xStep) {
                if (board.getBox(x, startY).getPiece() != null) {
                    return false;
                }
            }
        }
        return true;
    }
}
