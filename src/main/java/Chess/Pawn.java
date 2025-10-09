/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Chess;

/**
 *
 * @author ACER LAPTOP
 */
public class Pawn extends Piece{
    public Pawn(boolean white) {
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
        int direction = this.isWhite() ? 1 : -1;

        // Di chuyển thẳng
        if (startY == endY && end.getPiece() == null) {
            if (endX == startX + direction) {
                return true;
            }
            // Di chuyển 2 ô lần đầu
            if ((this.isWhite() && startX == 1) || (!this.isWhite() && startX == 6)) {
                if (endX == startX + 2 * direction && board.getBox(startX + direction, startY).getPiece() == null) {
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
