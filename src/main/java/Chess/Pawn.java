package Chess;

public class Pawn extends Piece {

    public Pawn(boolean white) {
        super(white);
    }

    @Override
    public boolean canMove(Board board, Spot start, Spot end) {
        int startX = start.getX();
        int startY = start.getY();
        int endX = end.getX();
        int endY = end.getY();

        // Quân trắng đi lên (-1), quân đen đi xuống (+1)
        int direction = this.isWhite() ? -1 : 1;
        
        // Log move attempt
        logMove("Pawn", start, end, true, "Checking move...");

        // Validate basic movement
        if (startX + direction == endX) {
            // Di chuyển thẳng 1 ô
            if (startY == endY && end.getPiece() == null) {
                logMove("Pawn", start, end, true, "Valid forward move");
                return true;
            }
            // Ăn chéo
            if (Math.abs(startY - endY) == 1) {
                if (end.getPiece() != null && end.getPiece().isWhite() != this.isWhite()) {
                    logMove("Pawn", start, end, true, "Valid diagonal capture");
                    return true;
                }
            }
        }
        // Di chuyển 2 ô lần đầu
        else if ((this.isWhite() && startX == 6) || (!this.isWhite() && startX == 1)) {
            if (startY == endY && endX == startX + 2 * direction) {
                // Kiểm tra không có quân cản đường
                if (end.getPiece() == null && 
                    board.getBox(startX + direction, startY).getPiece() == null) {
                    logMove("Pawn", start, end, true, "Valid initial two-square move");
                    return true;
                }
            }
        }

        // Log invalid move
        logMove("Pawn", start, end, false, "Invalid move pattern");
        return false;
    }
}
