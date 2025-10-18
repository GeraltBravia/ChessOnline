package Chess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChessClientSwing extends JFrame {
    private static final int DEFAULT_PORT = 12345;

    private JButton[][] boardButtons = new JButton[8][8];
    private JTextArea chatArea;
    private JTextField chatInput;
    private JTextField hostField;
    private JTextField portField;
    private JButton connectBtn, undoBtn, surrenderBtn, newGameBtn;
    private JLabel statusLabel;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String[][] boardState = new String[8][8];
    private boolean myTurn = false;
    private boolean isWhite = false;

    private java.util.List<String> moveHistory = new ArrayList<>();
    private Map<String, ImageIcon> pieceIcons = new HashMap<>();

    // Highlight hợp lệ
    private java.util.List<String> highlightedSquares = new ArrayList<>();

    // Click selection (use click to select a piece)
    private int selectedRow = -1, selectedCol = -1;
    // tracks the last square we requested LEGAL moves for (algebraic)
    private String lastRequestedFrom = null;
    private Image draggingPiece = null;
    private int dragX, dragY;

    public ChessClientSwing() {
        super("Chess Client (Socket + Drag-Drop)");
        loadPieceIcons();
        initUI();
        initBoardState();
        renderBoard();
        setSize(950, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void loadPieceIcons() {
        String[] colors = {"w", "b"};
        String[] pieces = {"p", "r", "n", "b", "q", "k"};
        for (String c : colors) {
            for (String p : pieces) {
                String key = c + p;
                try {
                    ImageIcon icon = new ImageIcon("resources/pieces/" + key + ".png");
                    Image scaled = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                    pieceIcons.put(key, new ImageIcon(scaled));
                } catch (Exception e) {
                    System.out.println("Missing icon for: " + key);
                }
            }
        }
    }

    private void initUI() {
        JPanel left = new JPanel(new BorderLayout());
        JPanel boardPanel = new JPanel(new GridLayout(8, 8));

        Font btnFont = new Font(Font.MONOSPACED, Font.BOLD, 18);
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JButton b = new JButton();
                b.setFont(btnFont);
                b.setFocusPainted(false);
                boardButtons[r][c] = b;
                if ((r + c) % 2 == 0) b.setBackground(new Color(240, 217, 181));
                else b.setBackground(new Color(181, 136, 99));
                final int fr = r, fc = c;
                // Add mouse listener for precise left-click selection and moves
                b.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            // If it's not our turn, ignore selection
                            if (!myTurn) {
                                JOptionPane.showMessageDialog(null, "Chưa đến lượt bạn");
                                return;
                            }

                            // Handle clicking on any piece
                            if (hasPiece(fr, fc)) {
                                String code = boardState[fr][fc];
                                boolean pieceIsWhite = code != null && code.startsWith("w");
                                
                                // Check if the clicked piece belongs to the current player
                                if (pieceIsWhite != isWhite) {
                                    // If we have a piece selected and click on opponent's piece, try to capture
                                    if (selectedRow != -1 && selectedCol != -1) {
                                        handleMove(selectedRow, selectedCol, fr, fc);
                                        // Clear selection and highlights after move attempt
                                        clearHighlights();
                                        selectedRow = -1;
                                        selectedCol = -1;
                                        lastRequestedFrom = null;
                                        draggingPiece = null;
                                        renderBoard();
                                    } else {
                                        // Trying to select opponent's piece when no piece is selected
                                        JOptionPane.showMessageDialog(null, "Không thể chọn quân của đối phương");
                                    }
                                    return;
                                }

                                // Clear previous selection and highlights
                                clearHighlights();
                                selectedRow = fr;
                                selectedCol = fc;
                                String code2 = boardState[fr][fc];
                                draggingPiece = pieceIcons.get(code2) != null ? pieceIcons.get(code2).getImage() : null;
                                // Highlight only the selected piece
                                boardButtons[fr][fc].setBackground(new Color(173, 216, 230)); // light blue for selected
                                lastRequestedFrom = coordToAlgebraic(fr, fc);
                                requestLegalMovesFromServer(lastRequestedFrom);
                            } else {
                                // clicking an empty square when something is selected will attempt a move
                                if (selectedRow != -1) {
                                    handleMove(selectedRow, selectedCol, fr, fc);
                                    // Clear selection and highlights before rendering
                                    selectedRow = -1;
                                    selectedCol = -1;
                                    lastRequestedFrom = null;
                                    draggingPiece = null;
                                    clearHighlights();
                                    renderBoard();
                                } else {
                                    // Click on empty with no selection - clear any stray highlights
                                    clearHighlights();
                                    renderBoard();
                                }
                            }
                        }
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        // on release, clear dragging visual if any
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            draggingPiece = null;
                            // keep selection until move attempted or user clicks elsewhere
                        }
                    }
                });
                boardPanel.add(b);
            }
        }

        // Note: per-button listeners handle selection/move; panel-level drag listeners removed

        left.add(boardPanel, BorderLayout.CENTER);

        JPanel ctrl = new JPanel(new GridLayout(5, 2, 4, 4));
        hostField = new JTextField("localhost");
        portField = new JTextField(String.valueOf(DEFAULT_PORT));
        connectBtn = new JButton("Connect");
        connectBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectToServer();
            }
        });
        undoBtn = new JButton("Undo");
        undoBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendUndoRequest();
            }
        });
        statusLabel = new JLabel("Not connected");

        ctrl.add(new JLabel("Host:"));
        ctrl.add(hostField);
        ctrl.add(new JLabel("Port:"));
        ctrl.add(portField);
        ctrl.add(connectBtn);
        ctrl.add(undoBtn);
        
        // Add New Game button
        newGameBtn = new JButton("Ván mới");
        newGameBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestNewGame();
            }
        });
        newGameBtn.setEnabled(false);
        ctrl.add(newGameBtn);
        
        // Add Surrender button
        surrenderBtn = new JButton("Đầu hàng");
        surrenderBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                surrender();
            }
        });
        surrenderBtn.setEnabled(false);
        ctrl.add(surrenderBtn);
        
        ctrl.add(statusLabel);
        left.add(ctrl, BorderLayout.SOUTH);

        // Chat
        JPanel right = new JPanel(new BorderLayout());
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        right.add(chatScroll, BorderLayout.CENTER);

        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendChat();
            }
        });
        chatInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendChat();
            }
        });
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendBtn, BorderLayout.EAST);
        right.add(chatInputPanel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(650);
        add(split, BorderLayout.CENTER);
    }

    private void initBoardState() {
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                boardState[i][j] = null;
        boardState[0] = new String[]{"br", "bn", "bb", "bq", "bk", "bb", "bn", "br"};
        Arrays.fill(boardState[1], "bp");
        boardState[7] = new String[]{"wr", "wn", "wb", "wq", "wk", "wb", "wn", "wr"};
        Arrays.fill(boardState[6], "wp");
    }

    private void renderBoard() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JButton b = boardButtons[r][c];
                String code = boardState[r][c];
                b.setIcon(pieceIcons.getOrDefault(code, null));
                if ((r + c) % 2 == 0)
                    b.setBackground(new Color(240, 217, 181));
                else
                    b.setBackground(new Color(181, 136, 99));
            }
        }
        highlightSquares();
    }

    private void handleMove(int sX, int sY, int eX, int eY) {
        String moveStr = coordToAlgebraic(sX, sY) + coordToAlgebraic(eX, eY);
        sendMove(moveStr);
    }

    private void sendMove(String moveStr) {
        if (!myTurn) {
            JOptionPane.showMessageDialog(this, "\u26A0\uFE0F Chưa đến lượt bạn");
            return;
        }
        if (socket != null && out != null) {
            out.println("MOVE " + moveStr);
            // wait for server confirmation (MOVE_OK / CAPTURE) before applying locally
            moveHistory.add(moveStr);
        }
    }

    private void sendUndoRequest() {
        if (socket != null && out != null) {
            out.println("UNDO");
            appendChat("Undo requested from server.");
        }
    }

    private void surrender() {
        if (socket != null && out != null) {
            out.println("SURRENDER");
            showGameOver(false); // Show lose message for the surrendering player
        }
    }

    private void requestNewGame() {
        if (socket != null && out != null) {
            // Ask for confirmation
            int option = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn bắt đầu ván mới?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION
            );
            if (option == JOptionPane.YES_OPTION) {
                out.println("START");
            }
        }
    }

    private void resetGame() {
        // Reset board state
        initBoardState();
        // Reset UI state
        selectedRow = -1;
        selectedCol = -1;
        lastRequestedFrom = null;
        draggingPiece = null;
        highlightedSquares.clear();
        // Reset game state
        myTurn = false;
        // Re-render the board
        renderBoard();
        // Enable/disable controls appropriately
        surrenderBtn.setEnabled(false);
        undoBtn.setEnabled(false);
        // Keep new game button enabled if connected
        newGameBtn.setEnabled(socket != null && socket.isConnected());
        statusLabel.setText("Game reset");
    }

    private void showGameOver(boolean won) {
        if (won) {
            JOptionPane.showMessageDialog(this, "Bạn đã chiến thắng!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Bạn đã thua!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
        }
        resetGame();
    }

    private void applyMoveToLocalBoard(String moveStr) {
        if (!moveStr.matches("[a-h][1-8][a-h][1-8].*")) return;
        int sX = algebraicToX(moveStr.substring(0, 2));
        int sY = algebraicToY(moveStr.substring(0, 2));
        int eX = algebraicToX(moveStr.substring(2, 4));
        int eY = algebraicToY(moveStr.substring(2, 4));
        boardState[eX][eY] = boardState[sX][sY];
        boardState[sX][sY] = null;
        renderBoard();
    }

    private void requestLegalMovesFromServer(String from) {
        if (socket != null && out != null) {
            out.println("LEGAL " + from);
        }
    }

    private void highlightSquares() {
        // Only highlight squares if we have a selected piece
        if (selectedRow != -1 && selectedCol != -1) {
            // First highlight the selected piece in light blue
            boardButtons[selectedRow][selectedCol].setBackground(new Color(173, 216, 230));
            
            // Then highlight legal moves in yellow
            for (String sq : highlightedSquares) {
                int x = algebraicToX(sq);
                int y = algebraicToY(sq);
                if (x >= 0 && x < 8 && y >= 0 && y < 8) {
                    boardButtons[x][y].setBackground(Color.YELLOW);
                }
            }
        }
    }

    private void clearHighlights() {
        highlightedSquares.clear();
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                if ((i + j) % 2 == 0)
                    boardButtons[i][j].setBackground(new Color(240, 217, 181));
                else
                    boardButtons[i][j].setBackground(new Color(181, 136, 99));
    }

    private boolean hasPiece(int row, int col) {
        return boardState[row][col] != null;
    }

    private String coordToAlgebraic(int x, int y) {
        int rank = 8 - x;
        char file = (char) ('a' + y);
        return "" + file + rank;
    }

    private int algebraicToX(String sq) { return 8 - (sq.charAt(1) - '0'); }
    private int algebraicToY(String sq) { return sq.charAt(0) - 'a'; }

    private void connectToServer() {
        String host = hostField.getText().trim();
        int port = DEFAULT_PORT;
        try { port = Integer.parseInt(portField.getText().trim()); } catch (NumberFormatException ignored) {}
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            statusLabel.setText("Connected");
            appendChat("Connected to server at " + host + ":" + port);
            new Thread(this::serverReaderLoop).start();
            connectBtn.setEnabled(false);
        } catch (IOException e) {
            appendChat("Connection failed: " + e.getMessage());
            statusLabel.setText("Connect failed");
        }
    }

    private void serverReaderLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                final String msg = line;
                SwingUtilities.invokeLater(() -> handleServerMessage(msg));
            }
        } catch (IOException e) {
            appendChat("Disconnected.");
            statusLabel.setText("Disconnected");
            connectBtn.setEnabled(true);
        }
    }

    private void handleServerMessage(String msg) {
        // Treat LEGAL_MOVES as an internal UI message (don't append to chat)
        if (msg.startsWith("LEGAL_MOVES ")) {
            // only apply LEGAL_MOVES for the currently selected piece
            if (selectedRow == -1 || selectedCol == -1 || lastRequestedFrom == null) {
                return;
            }
            String currentSelected = coordToAlgebraic(selectedRow, selectedCol);
            if (!currentSelected.equals(lastRequestedFrom)) {
                // this LEGAL response isn't for the currently selected piece
                return;
            }
            String data = msg.substring("LEGAL_MOVES ".length());
            String[] arr = data.split(",");
            highlightedSquares.clear();
            for (String move : arr)
                if (move.trim().length() == 2)
                    highlightedSquares.add(move.trim());
            highlightSquares();
            return;
        }

        appendChat("<server> " + msg);

        if (msg.startsWith("START")) {
            // START WHITE or START BLACK
            isWhite = msg.contains("WHITE");
            myTurn = isWhite; // white starts
            statusLabel.setText(myTurn ? "Your turn" : "Opponent's turn");
            // Enable game control buttons
            surrenderBtn.setEnabled(true);
            undoBtn.setEnabled(true);
            newGameBtn.setEnabled(true);
        } else if (msg.equals("YOUR_TURN")) {
            myTurn = true;
            statusLabel.setText("Your turn");
        } else if (msg.startsWith("MOVE ")) {
            // Opponent move
            String move = msg.substring(5).trim();
            applyMoveToLocalBoard(move);
            myTurn = true; // after opponent moves, it's our turn
            statusLabel.setText("Your turn");
        } else if (msg.startsWith("MOVE_OK ")) {
            // Our move was accepted by server
            String move = msg.substring("MOVE_OK ".length()).trim();
            applyMoveToLocalBoard(move);
            myTurn = false;
            statusLabel.setText("Move accepted");
        } else if (msg.startsWith("CAPTURE ")) {
            // CAPTURE <move> <WHITE|BLACK>
            String[] parts = msg.split(" ");
            if (parts.length >= 2) {
                String move = parts[1].trim();
                // Check if a king was captured
                int eX = algebraicToX(move.substring(2, 4));
                int eY = algebraicToY(move.substring(2, 4));
                String capturedPiece = boardState[eX][eY];
                
                applyMoveToLocalBoard(move);
                
                // If a king was captured, end the game
                if (capturedPiece != null && capturedPiece.endsWith("k")) {
                    boolean iWon = (capturedPiece.startsWith("b") && isWhite) || 
                                 (capturedPiece.startsWith("w") && !isWhite);
                    showGameOver(iWon);
                    return;
                }
            }
            myTurn = false;
            statusLabel.setText("Captured");
        } else if (msg.startsWith("LEGAL_MOVES ")) {
            String data = msg.substring("LEGAL_MOVES ".length());
            String[] arr = data.split(",");
            highlightedSquares.clear();
            for (String move : arr)
                if (move.trim().length() == 2)
                    highlightedSquares.add(move.trim());
            highlightSquares();
        } else if (msg.equals("NOT_YOUR_TURN")) {
            JOptionPane.showMessageDialog(this, "\u26A0\uFE0F Không phải lượt của bạn");
        } else if (msg.startsWith("INVALID_MOVE")) {
            // Show invalid move reason (server uses several codes)
            JOptionPane.showMessageDialog(this, "\u274c Lỗi: " + msg);
        } else if (msg.equals("INVALID_MOVE_NOT_YOUR_PIECE")) {
            JOptionPane.showMessageDialog(this, "❌ Không thể di chuyển quân không thuộc quyền của bạn!");
        } else if (msg.equals("OPPONENT_SURRENDERED")) {
            showGameOver(true); // Show win message for the remaining player
        } else if (msg.startsWith("START")) {
            resetGame();
            initBoardState();
            // Update white/black based on server message
            isWhite = msg.contains("WHITE");
            myTurn = isWhite; // white starts
            surrenderBtn.setEnabled(true);
            undoBtn.setEnabled(true);
            newGameBtn.setEnabled(true);
            statusLabel.setText(myTurn ? "Your turn" : "Opponent's turn");
            JOptionPane.showMessageDialog(this, "Ván mới bắt đầu!");
        }
    }

    private void sendChat() {
        String txt = chatInput.getText().trim();
        if (txt.isEmpty()) return;
        if (out != null) {
            out.println("CHAT " + txt);
            chatInput.setText("");
        } else appendChat("Not connected");
    }

    private void appendChat(String s) {
        chatArea.append(s + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (draggingPiece != null) {
            int boardLeft = boardButtons[0][0].getLocationOnScreen().x - getLocationOnScreen().x;
            int boardTop = boardButtons[0][0].getLocationOnScreen().y - getLocationOnScreen().y;
            g.drawImage(draggingPiece, dragX + boardLeft - 30, dragY + boardTop - 30, this);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChessClientSwing().setVisible(true));
    }
}
