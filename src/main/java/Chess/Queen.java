/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Chess;

/**
 *
 * @author ACER LAPTOP
 */
public class Queen extends Piece{
    public Queen(boolean white) {
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

        // Di chuyển ngang hoặc dọc (như Rook)
        if (startX == endX || startY == endY) {
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

        // Di chuyển chéo (như Bishop)
        if (deltaX == deltaY) {
            int xStep = startX < endX ? 1 : -1;
            int yStep = startY < endY ? 1 : -1;
            for (int i = 1; i < deltaX; i++) {
                if (board.getBox(startX + i * xStep, startY + i * yStep).getPiece() != null) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }
}
