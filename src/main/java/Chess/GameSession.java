/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Chess;

/**
 *
 * @author ACER LAPTOP
 */
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

        public void processMove(String moveStr, ClientHandler sender) {
            // Kiểm tra lượt
            if ((sender == player1 && !isPlayer1Turn) || (sender == player2 && isPlayer1Turn)) {
                sender.sendMessage("NOT_YOUR_TURN");
                return;
            }

            // Chuyển đổi nước đi từ chuỗi (e2e4) sang tọa độ
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

                // Kiểm tra nước đi hợp lệ
                Piece sourcePiece = start.getPiece();
                if (sourcePiece == null) {
                    sender.sendMessage("INVALID_MOVE_NO_PIECE");
                    return;
                }
                if (sourcePiece.isWhite() != currentPlayer.isWhiteSide()) {
                    sender.sendMessage("INVALID_MOVE_NOT_YOUR_PIECE");
                    return;
                }
                if (!sourcePiece.canMove(board, start, end)) {
                    sender.sendMessage("INVALID_MOVE_RULES");
                    return;
                }

                // Thực hiện nước đi
                Move move = new Move(currentPlayer, start, end);
                if (end.getPiece() != null) {
                    end.getPiece().setKilled(true);
                    move.setPieceKilled(end.getPiece());
                }
                end.setPiece(sourcePiece);
                start.setPiece(null);

                // Gửi nước đi đến đối thủ
                ClientHandler opponent = (sender == player1) ? player2 : player1;
                opponent.sendMessage("MOVE " + moveStr);

                // Chuyển lượt
                isPlayer1Turn = !isPlayer1Turn;
                opponent.sendMessage("YOUR_TURN");

                // Kiểm tra kết thúc game (ví dụ: bắt Vua)
                if (end.getPiece() instanceof King) {
                    String winner = currentPlayer.isWhiteSide() ? "WHITE_WIN" : "BLACK_WIN";
                    player1.sendMessage("GAME_OVER " + winner);
                    player2.sendMessage("GAME_OVER " + winner);
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage("INVALID_MOVE_OUT_OF_BOUNDS");
            }
        }
}
