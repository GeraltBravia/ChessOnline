/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Chess;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private Player[] players;
    private Board board;
    private Player currentTurn;
    private GameStatus status;
    private List<Move> movesPlayed;

    public Game() {
        players = new Player[2];
        board = new Board();
        movesPlayed = new ArrayList<>();
        status = GameStatus.ACTIVE;
    }

    public void initialize(Player p1, Player p2) {
        players[0] = p1;
        players[1] = p2;
        board.resetBoard();
        if (p1.isWhiteSide()) {
            this.currentTurn = p1;
        } else {
            this.currentTurn = p2;
        }
        movesPlayed.clear();
    }

    public boolean isEnd() {
        return this.getStatus() != GameStatus.ACTIVE;
    }

    public GameStatus getStatus() {
        return this.status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public boolean playerMove(Player player, int startX, int startY, int endX, int endY) {
        try {
            Spot startBox = board.getBox(startX, startY);
            Spot endBox = board.getBox(endX, endY);
            Move move = new Move(player, startBox, endBox);
            return this.makeMove(move, player);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean makeMove(Move move, Player player) {
        Piece sourcePiece = move.getStart().getPiece();
        if (sourcePiece == null) {
            return false;
        }

        // Kiểm tra lượt hợp lệ
        if (player != currentTurn) {
            return false;
        }

        if (sourcePiece.isWhite() != player.isWhiteSide()) {
            return false;
        }

        // Kiểm tra nước đi hợp lệ
        if (!sourcePiece.canMove(board, move.getStart(), move.getEnd())) {
            return false;
        }

        // Xử lý nhập thành
        // Handle castling: king moves two squares horizontally and the move is valid per piece rules
        if (sourcePiece instanceof King && Math.abs(move.getStart().getY() - move.getEnd().getY()) == 2
                && sourcePiece.canMove(board, move.getStart(), move.getEnd())) {
            move.setCastlingMove(true);
            // Di chuyển quân Xe khi nhập thành
            int startY = move.getStart().getY();
            int endY = move.getEnd().getY();
            if (Math.abs(startY - endY) == 2) {
                int rookY = (endY > startY) ? 7 : 0;
                int newRookY = (endY > startY) ? endY - 1 : endY + 1;
                Spot rookSpot = board.getBox(move.getStart().getX(), rookY);
                Spot newRookSpot = board.getBox(move.getStart().getX(), newRookY);
                newRookSpot.setPiece(rookSpot.getPiece());
                rookSpot.setPiece(null);
            }
        }

        // Xử lý ăn quân
        Piece destPiece = move.getEnd().getPiece();
        if (destPiece != null) {
            destPiece.setKilled(true);
            move.setPieceKilled(destPiece);
        }

        // Lưu nước đi
        movesPlayed.add(move);

        // Di chuyển quân cờ
        move.getEnd().setPiece(move.getStart().getPiece());
        move.getStart().setPiece(null);

        // Kiểm tra kết thúc game
        if (destPiece != null && destPiece instanceof King) {
            if (player.isWhiteSide()) {
                this.setStatus(GameStatus.WHITE_WIN);
            } else {
                this.setStatus(GameStatus.BLACK_WIN);
            }
        }

        // Chuyển lượt
        if (this.currentTurn == players[0]) {
            this.currentTurn = players[1];
        } else {
            this.currentTurn = players[0];
        }

        return true;
    }
}
