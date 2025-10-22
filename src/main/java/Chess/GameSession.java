package Chess;

import java.util.ArrayList;
import java.util.List;

public class GameSession {

    private ClientHandler player1;
    private ClientHandler player2;
    private Board board;
    private boolean isPlayer1Turn;
    private Player whitePlayer;
    private Player blackPlayer;
    private boolean isQuickPlay; // true nếu là chế độ Quick Play (cập nhật Elo)
    
    // Lưu trữ lịch sử nước đi
    private List<Move> moveHistory;
    private List<Piece> capturedPieces;

    public GameSession(ClientHandler whitePlayer, ClientHandler blackPlayer) {
        this(whitePlayer, blackPlayer, false);
    }

    public GameSession(ClientHandler whitePlayer, ClientHandler blackPlayer, boolean isQuickPlay) {
        this.player1 = whitePlayer;
        this.player2 = blackPlayer;
        this.board = new Board();
        this.isPlayer1Turn = true; // White player (player1) goes first
        this.whitePlayer = new HumanPlayer(true);
        this.blackPlayer = new HumanPlayer(false);
        this.isQuickPlay = isQuickPlay;
        whitePlayer.setPlayer(this.whitePlayer);
        blackPlayer.setPlayer(this.blackPlayer);
        this.moveHistory = new ArrayList<>();
        this.capturedPieces = new ArrayList<>();
    }

    public void handleUndo(ClientHandler requester) {
        // Kiểm tra xem có nước đi nào để undo không
        if (moveHistory.isEmpty()) {
            requester.sendMessage("CANNOT_UNDO_NO_MOVES");
            return;
        }

        // Lấy nước đi cuối cùng
        Move lastMove = moveHistory.remove(moveHistory.size() - 1);
        
        // Khôi phục vị trí quân cờ
        Spot start = lastMove.getStart();
        Spot end = lastMove.getEnd();
        
        // Di chuyển quân cờ về vị trí cũ
        start.setPiece(end.getPiece());
        
        // Nếu có quân bị ăn, đặt lại quân đó
        if (lastMove.getPieceKilled() != null) {
            end.setPiece(lastMove.getPieceKilled());
            lastMove.getPieceKilled().setKilled(false);
        } else {
            end.setPiece(null);
        }

        // Đổi lượt
        isPlayer1Turn = !isPlayer1Turn;
        
        // Gửi thông báo undo cho cả hai người chơi
        String moveStr = coordToString(start) + coordToString(end);
        player1.sendMessage("UNDO_MOVE " + moveStr);
        player2.sendMessage("UNDO_MOVE " + moveStr);
        
        // Gửi lượt đi cho người chơi tiếp theo
        if (isPlayer1Turn) {
            player1.sendMessage("YOUR_TURN");
        } else {
            player2.sendMessage("YOUR_TURN");
        }
    }

    private String coordToString(Spot spot) {
        char file = (char)('a' + spot.getY());
        int rank = 8 - spot.getX();
        return "" + file + rank;
    }

    public void startGame() {
        // Send START messages with color assignments
        player1.sendMessage("START WHITE"); // First player is always WHITE
        player2.sendMessage("START BLACK"); // Second player is always BLACK
        
        // Add debug logs
        System.out.println("Game started:");
        System.out.println("Player 1 (WHITE): " + player1.getPlayerId());
        System.out.println("Player 2 (BLACK): " + player2.getPlayerId());
        
        // Set initial turn
        player1.sendMessage("YOUR_TURN"); // White moves first
        player2.sendMessage("OPPONENT_TURN"); // Black waits
    }
    
    public void restartGame(ClientHandler requester) {
        // Tạo bàn cờ mới
        this.board = new Board();
        
        // Người chơi yêu cầu restart sẽ được chơi quân trắng
        if (requester == player1) {
            this.isPlayer1Turn = true;
            player1.sendMessage("START WHITE RESTART");
            player2.sendMessage("START BLACK RESTART");
            player1.sendMessage("YOUR_TURN");
        } else {
            this.isPlayer1Turn = false;
            player2.sendMessage("START WHITE RESTART");
            player1.sendMessage("START BLACK RESTART");
            player2.sendMessage("YOUR_TURN");
        }
        
        // Đặt lại người chơi
        if (requester == player1) {
            player1.setPlayer(new HumanPlayer(true));
            player2.setPlayer(new HumanPlayer(false));
        } else {
            player2.setPlayer(new HumanPlayer(true));
            player1.setPlayer(new HumanPlayer(false));
        }
    }

    public boolean containsPlayer(ClientHandler player) {
        return player == player1 || player == player2;
    }

    public void broadcastChat(ClientHandler sender, String chatMsg) {
        ClientHandler opponent = (sender == player1) ? player2 : player1;
        sender.sendMessage("CHAT You: " + chatMsg);
        opponent.sendMessage("CHAT Opponent: " + chatMsg);
    }

    public void handleSurrender(ClientHandler surrenderingPlayer) {
        ClientHandler winner = (surrenderingPlayer == player1) ? player2 : player1;
        winner.sendMessage("OPPONENT_SURRENDERED");
        surrenderingPlayer.sendMessage("GAME_OVER " + (winner == player1 ? "WHITE_WIN" : "BLACK_WIN"));
        winner.sendMessage("GAME_OVER " + (winner == player1 ? "WHITE_WIN" : "BLACK_WIN"));

        // Lưu kết quả game
        endGame(winner == player1 ? player1 : player2, null);
    }

    public void processMove(String moveStr, ClientHandler sender) {
        System.out.println("[GameSession] processMove called with move='" + moveStr + "' from " + sender);
        // Kiểm tra lượt
        if ((sender == player1 && !isPlayer1Turn) || (sender == player2 && isPlayer1Turn)) {
            sender.sendMessage("NOT_YOUR_TURN");
            return;
        }

        // Kiểm tra định dạng
        if (!moveStr.matches("[a-h][1-8][a-h][1-8]")) {
            sender.sendMessage("INVALID_MOVE_FORMAT");
            return;
        }

        int startX = 8 - (moveStr.charAt(1) - '0');
        int startY = moveStr.charAt(0) - 'a';
        int endX = 8 - (moveStr.charAt(3) - '0');
        int endY = moveStr.charAt(2) - 'a';

        try {
            Spot start = board.getBox(startX, startY);
            Spot end = board.getBox(endX, endY);
            Player currentPlayer = sender.getPlayer();
            Piece sourcePiece = start.getPiece();

            System.out.println("[GameSession] start=(" + startX + "," + startY + ") end=(" + endX + "," + endY + ")");
            System.out.println("[GameSession] sourcePiece=" + (sourcePiece == null ? "null" : sourcePiece.getClass().getSimpleName()) +
                               " destPiece=" + (end.getPiece() == null ? "null" : end.getPiece().getClass().getSimpleName()));

            // Kiểm tra nước đi hợp lệ
            if (sourcePiece == null) {
                sender.sendMessage("INVALID_MOVE_NO_PIECE");
                System.out.println("[GameSession] Move rejected: no piece at start");
                return;
            }
            if (sourcePiece.isWhite() != currentPlayer.isWhiteSide()) {
                sender.sendMessage("INVALID_MOVE_NOT_YOUR_PIECE");
                System.out.println("[GameSession] Move rejected: piece color mismatch. source.isWhite=" + sourcePiece.isWhite() + " player.isWhite=" + currentPlayer.isWhiteSide());
                return;
            }
            if (!sourcePiece.canMove(board, start, end)) {
                sender.sendMessage("INVALID_MOVE_RULES");
                System.out.println("[GameSession] Move rejected: piece.canMove returned false");
                return;
            }

            // Lưu quân bị ăn (nếu có)
            Piece captured = end.getPiece();
            if (captured != null) {
                captured.setKilled(true);
            }

            // Thực hiện nước đi
            end.setPiece(sourcePiece);
            start.setPiece(null);

            Move move = new Move(currentPlayer, start, end);
            if (captured != null) {
                move.setPieceKilled(captured);
                capturedPieces.add(captured);
            }

            // Lưu nước đi vào lịch sử
            moveHistory.add(move);

            ClientHandler opponent = (sender == player1) ? player2 : player1;

            // Gửi nước đi cho cả sender và opponent
            if (captured != null) {
                sender.sendMessage("CAPTURE " + moveStr + " " + (captured.isWhite() ? "WHITE" : "BLACK"));
                opponent.sendMessage("CAPTURE " + moveStr + " " + (captured.isWhite() ? "WHITE" : "BLACK"));
            } else {
                sender.sendMessage("MOVE_OK " + moveStr);
                opponent.sendMessage("MOVE " + moveStr);
            }

            // Chuyển lượt
            isPlayer1Turn = !isPlayer1Turn;
            opponent.sendMessage("YOUR_TURN");

            // Kiểm tra kết thúc game
            if (captured instanceof King) {
                String winner = currentPlayer.isWhiteSide() ? "WHITE_WIN" : "BLACK_WIN";
                player1.sendMessage("GAME_OVER " + winner);
                player2.sendMessage("GAME_OVER " + winner);

                // Lưu kết quả game - sender là người vừa thắng
                endGame(sender, null);
            }

        } catch (IllegalArgumentException e) {
            sender.sendMessage("INVALID_MOVE_OUT_OF_BOUNDS");
            System.out.println("[GameSession] Move rejected: out of bounds: " + moveStr);
        }
    }

    public void sendLegalMoves(String fromAlgebraic, ClientHandler requester) {
        // Convert algebraic to board coordinates
        if (fromAlgebraic == null || fromAlgebraic.length() != 2) {
            requester.sendMessage("LEGAL_MOVES ");
            return;
        }
        int startX = 8 - (fromAlgebraic.charAt(1) - '0');
        int startY = fromAlgebraic.charAt(0) - 'a';
        try {
            Spot start = board.getBox(startX, startY);
            Piece source = start.getPiece();
            System.out.println("[GameSession] sendLegalMoves for " + fromAlgebraic + " requester=" + requester + " source=" + (source == null ? "null" : source.getClass().getSimpleName()));
            if (source == null) {
                requester.sendMessage("LEGAL_MOVES ");
                return;
            }
            Player currentPlayer = requester.getPlayer();
            // Only allow requesting legal moves for your own piece
            if (source.isWhite() != currentPlayer.isWhiteSide()) {
                requester.sendMessage("LEGAL_MOVES ");
                return;
            }

            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    try {
                        Spot end = board.getBox(x, y);
                        if (source.canMove(board, start, end)) {
                            String sq = "" + (char)('a' + y) + (8 - x);
                            if (!first) sb.append(",");
                            sb.append(sq);
                            first = false;
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            requester.sendMessage("LEGAL_MOVES " + sb.toString());
        } catch (IllegalArgumentException e) {
            requester.sendMessage("LEGAL_MOVES ");
        }
    }

    /**
     * Xử lý kết thúc game và lưu kết quả vào database (chỉ cho Quick Play)
     * @param winner Người thắng (null nếu hòa)
     * @param draw true nếu hòa
     */
    private void endGame(ClientHandler winner, Boolean draw) {
        if (!isQuickPlay) {
            return; // Không lưu kết quả cho chế độ tạo phòng thủ công
        }

        try {
            // Lấy User ID từ ClientHandler (cần thêm phương thức getUserId vào ClientHandler)
            Integer winnerId = null;
            if (winner != null && winner.getUser() != null) {
                winnerId = winner.getUser().getId();
            }

            Integer player1Id = (player1.getUser() != null) ? player1.getUser().getId() : null;
            Integer player2Id = (player2.getUser() != null) ? player2.getUser().getId() : null;

            System.out.println("DEBUG endGame: winner=" + (winner != null ? winner.getPlayerId() : "null") + 
                             ", winnerId=" + winnerId + ", player1Id=" + player1Id + ", player2Id=" + player2Id);
            System.out.println("DEBUG: player1 is WHITE, player2 is BLACK");
            System.out.println("DEBUG: winner should be the sender who made the winning move");

            // Chỉ lưu nếu cả hai người chơi đều có tài khoản
            if (player1Id != null && player2Id != null) {
                AuthService authService = new AuthService();
                boolean success = authService.saveGameResult(player1Id, player2Id, winnerId, isQuickPlay);
                if (success) {
                    System.out.println("Game result saved to database for Quick Play match");
                } else {
                    System.out.println("Failed to save game result to database");
                }
            } else {
                System.out.println("DEBUG: Cannot save game result - one or both players are guests");
            }
        } catch (Exception e) {
            System.out.println("Error saving game result: " + e.getMessage());
        }
    }

}
