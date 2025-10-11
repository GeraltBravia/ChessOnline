package Chess;

public class Pawn extends Piece {

    public Pawn(boolean white) {
        super(white);
    }

    @Override
    public boolean canMove(Board board, Spot start, Spot end) {
        // Không thể ăn quân cùng màu
        if (end.getPiece() != null && end.getPiece().isWhite() == this.isWhite()) {
            return false;
        }

        int startX = start.getX();
        int startY = start.getY();
        int endX = end.getX();
        int endY = end.getY();

        // Quân trắng đi lên (-1), quân đen đi xuống (+1) vì hàng 0 là trên cùng
        int direction = this.isWhite() ? -1 : 1;

        // Di chuyển thẳng
        if (startY == endY) {
            // 1 ô
            if (endX == startX + direction && end.getPiece() == null) {
                return true;
            }
            // 2 ô lần đầu
            if ((this.isWhite() && startX == 6) || (!this.isWhite() && startX == 1)) {
                if (endX == startX + 2 * direction
                        && end.getPiece() == null
                        && board.getBox(startX + direction, startY).getPiece() == null) {
                    return true;
                }
            }
        }

        // Ăn chéo
        if (Math.abs(startY - endY) == 1 && endX == startX + direction && end.getPiece() != null) {
            return true;
        }

        return false;
    }
}
