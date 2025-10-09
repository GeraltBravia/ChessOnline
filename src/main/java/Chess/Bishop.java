/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Chess;

/**
 *
 * @author ACER LAPTOP
 */
public class Bishop extends Piece {
    public Bishop(boolean white) {
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

        int deltaX = Math.abs(startX - endX);
        int deltaY = Math.abs(startY - endY);

        // Chỉ di chuyển chéo
        if (deltaX != deltaY) {
            return false;
        }

        // Kiểm tra đường đi không bị cản
        int xStep = startX < endX ? 1 : -1;
        int yStep = startY < endY ? 1 : -1;
        for (int i = 1; i < deltaX; i++) {
            if (board.getBox(startX + i * xStep, startY + i * yStep).getPiece() != null) {
                return false;
            }
        }

        return true;
    }
}
