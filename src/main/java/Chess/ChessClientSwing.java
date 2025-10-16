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
    private JButton connectBtn, undoBtn;
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

    // Drag and Drop
    private int dragStartRow = -1, dragStartCol = -1;
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
                boardPanel.add(b);
            }
        }

        // ===== Drag-and-drop listener =====
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int col = e.getX() / (boardPanel.getWidth() / 8);
                int row = e.getY() / (boardPanel.getHeight() / 8);
                if (row >= 0 && row < 8 && col >= 0 && col < 8 && hasPiece(row, col)) {
                    dragStartRow = row;
                    dragStartCol = col;
                    String code = boardState[row][col];
                    draggingPiece = pieceIcons.get(code) != null ? pieceIcons.get(code).getImage() : null;
                    // Gửi yêu cầu LEGAL tới server
                    requestLegalMovesFromServer(coordToAlgebraic(row, col));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingPiece != null) {
                    int col = e.getX() / (boardPanel.getWidth() / 8);
                    int row = e.getY() / (boardPanel.getHeight() / 8);
                    if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                        handleMove(dragStartRow, dragStartCol, row, col);
                    }
                    draggingPiece = null;
                    clearHighlights();
                    renderBoard();
                }
            }
        });

        boardPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                dragX = e.getX();
                dragY = e.getY();
                repaint();
            }
        });

        left.add(boardPanel, BorderLayout.CENTER);

        JPanel ctrl = new JPanel(new GridLayout(5, 2, 4, 4));
        hostField = new JTextField("localhost");
        portField = new JTextField(String.valueOf(DEFAULT_PORT));
        connectBtn = new JButton("Connect");
        connectBtn.addActionListener(e -> connectToServer());
        undoBtn = new JButton("Undo");
        undoBtn.addActionListener(e -> sendUndoRequest());
        statusLabel = new JLabel("Not connected");

        ctrl.add(new JLabel("Host:"));
        ctrl.add(hostField);
        ctrl.add(new JLabel("Port:"));
        ctrl.add(portField);
        ctrl.add(connectBtn);
        ctrl.add(undoBtn);
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
        sendBtn.addActionListener(e -> sendChat());
        chatInput.addActionListener(e -> sendChat());
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
        if (socket != null && out != null) {
            out.println("MOVE " + moveStr);
            moveHistory.add(moveStr);
        }
    }

    private void sendUndoRequest() {
        if (socket != null && out != null) {
            out.println("UNDO");
            appendChat("Undo requested from server.");
        }
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
        for (String sq : highlightedSquares) {
            int x = algebraicToX(sq);
            int y = algebraicToY(sq);
            if (x >= 0 && x < 8 && y >= 0 && y < 8)
                boardButtons[x][y].setBackground(Color.YELLOW);
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
        appendChat("<server> " + msg);

        if (msg.startsWith("START")) {
            myTurn = msg.contains("WHITE");
        } else if (msg.equals("YOUR_TURN")) {
            myTurn = true;
            statusLabel.setText("Your turn");
        } else if (msg.startsWith("MOVE ")) {
            String move = msg.substring(5).trim();
            applyMoveToLocalBoard(move);
            statusLabel.setText("Opponent moved");
        } else if (msg.startsWith("LEGAL_MOVES ")) {
            String data = msg.substring("LEGAL_MOVES ".length());
            String[] arr = data.split(",");
            highlightedSquares.clear();
            for (String move : arr)
                if (move.trim().length() == 2)
                    highlightedSquares.add(move.trim());
            highlightSquares();
        } else if (msg.equals("INVALID_MOVE_NOT_YOUR_PIECE")) {
            JOptionPane.showMessageDialog(this, "❌ Không thể di chuyển quân không thuộc quyền của bạn!");
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
