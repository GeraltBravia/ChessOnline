package Chess;

/**
 * Board class quản lý 8x8 ô và khởi tạo quân cờ.
 */
public class Board {
    private Spot[][] boxes;

    public Board() {
        resetBoard();
    }

    public Spot getBox(int x, int y) {
        if (x < 0 || x > 7 || y < 0 || y > 7) {
            throw new IllegalArgumentException("Index out of bound");
        }
        return boxes[x][y];
    }

    public void resetBoard() {
        boxes = new Spot[8][8];

        // Khởi tạo quân đen (hàng 0–1)
        boxes[0][0] = new Spot(0, 0, new Rook(false));
        boxes[0][1] = new Spot(0, 1, new Knight(false));
        boxes[0][2] = new Spot(0, 2, new Bishop(false));
        boxes[0][3] = new Spot(0, 3, new Queen(false));
        boxes[0][4] = new Spot(0, 4, new King(false));
        boxes[0][5] = new Spot(0, 5, new Bishop(false));
        boxes[0][6] = new Spot(0, 6, new Knight(false));
        boxes[0][7] = new Spot(0, 7, new Rook(false));
        for (int j = 0; j < 8; j++) {
            boxes[1][j] = new Spot(1, j, new Pawn(false));
        }

        // Khởi tạo quân trắng (hàng 6–7)
        for (int j = 0; j < 8; j++) {
            boxes[6][j] = new Spot(6, j, new Pawn(true));
        }
        boxes[7][0] = new Spot(7, 0, new Rook(true));
        boxes[7][1] = new Spot(7, 1, new Knight(true));
        boxes[7][2] = new Spot(7, 2, new Bishop(true));
        boxes[7][3] = new Spot(7, 3, new Queen(true));
        boxes[7][4] = new Spot(7, 4, new King(true));
        boxes[7][5] = new Spot(7, 5, new Bishop(true));
        boxes[7][6] = new Spot(7, 6, new Knight(true));
        boxes[7][7] = new Spot(7, 7, new Rook(true));

        // Khởi tạo các ô trống (hàng 2–5)
        for (int i = 2; i <= 5; i++) {
            for (int j = 0; j < 8; j++) {
                boxes[i][j] = new Spot(i, j, null);
            }
        }
    }
}
