
package Chess;


public class King extends Piece {
    private boolean castlingDone = false;

    public King(boolean white) {
        super(white);
    }

    public boolean isCastlingDone() {
        return this.castlingDone;
    }

    public void setCastlingDone(boolean castlingDone) {
        this.castlingDone = castlingDone;
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
        int deltaX = Math.abs(startX - endX);
        int deltaY = Math.abs(startY - endY);

        // Di chuyển bình thường (1 ô)
        if (deltaX + deltaY == 1 || (deltaX == 1 && deltaY == 1)) {
            return true;
        }

        // Kiểm tra nhập thành
        return this.isValidCastling(board, start, end);
    }

    public boolean isValidCastling(Board board, Spot start, Spot end) {
        if (this.isCastlingDone()) {
            return false;
        }

        int startX = start.getX();
        int startY = start.getY();
        int endX = end.getX();
        int endY = end.getY();

        // Kiểm tra nhập thành (king di chuyển 2 ô theo hàng ngang)
        if (startX != endX || Math.abs(startY - endY) != 2) {
            return false;
        }

        // Kiểm tra Vua chưa di chuyển
        if (this.isCastlingDone()) {
            return false;
        }

        // Kiểm tra nhập thành bên vua (king-side) hoặc bên hậu (queen-side)
        int rookY = (endY > startY) ? 7 : 0; // 7: king-side, 0: queen-side
        Spot rookSpot = board.getBox(startX, rookY);
        Piece rook = rookSpot.getPiece();

        // Kiểm tra quân Xe tồn tại và chưa di chuyển
        if (!(rook instanceof Rook) || rook.isWhite() != this.isWhite()) {
            return false;
        }
        // Giả sử Rook có thuộc tính `moved` để kiểm tra đã di chuyển chưa
        // (Nếu không, bạn cần thêm thuộc tính này vào lớp Rook)

        // Kiểm tra các ô giữa Vua và Xe trống
        int yStep = (rookY > startY) ? 1 : -1;
        for (int y = startY + yStep; y != rookY; y += yStep) {
            if (board.getBox(startX, y).getPiece() != null) {
                return false;
            }
        }

        // Note: do not change castling state here; caller that performs the move should set it.
        return true;
    }
}
