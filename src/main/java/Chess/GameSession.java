package Chess;

public class GameSession {

    private ClientHandler player1;
    private ClientHandler player2;
    private Board board;
    private boolean isPlayer1Turn;
    private Player whitePlayer;
    private Player blackPlayer;

    public GameSession(ClientHandler player1, ClientHandler player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.board = new Board();
        this.isPlayer1Turn = true;
        this.whitePlayer = new HumanPlayer(true);
        this.blackPlayer = new HumanPlayer(false);
        player1.setPlayer(whitePlayer);
        player2.setPlayer(blackPlayer);
    }

    public void startGame() {
        player1.sendMessage("START WHITE");
        player2.sendMessage("START BLACK");
        player1.sendMessage("YOUR_TURN");
    }

    public boolean containsPlayer(ClientHandler player) {
        return player == player1 || player == player2;
    }

    public void broadcastChat(ClientHandler sender, String chatMsg) {
        ClientHandler opponent = (sender == player1) ? player2 : player1;
        sender.sendMessage("CHAT You: " + chatMsg);
        opponent.sendMessage("CHAT Opponent: " + chatMsg);
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
            }

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

}
