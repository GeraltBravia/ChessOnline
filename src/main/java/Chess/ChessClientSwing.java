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
    private JButton connectBtn, undoBtn, surrenderBtn, newGameBtn, exitBtn;
    private JButton createRoomBtn, joinRoomBtn;
    private JLabel statusLabel;
    private JLabel roomCodeLabel;
    private JLabel opponentEloLabel; // Hiển thị Elo của đối thủ
    private String currentRoomCode;
    private GameMenu parentMenu;
    private boolean isQuickPlayMode = false; // Theo dõi chế độ chơi nhanh

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String[][] boardState = new String[8][8];
    // Chưa xác định màu quân và lượt đi cho đến khi nhận thông báo từ server
    private boolean myTurn;
    private boolean isWhite;

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

    public ChessClientSwing(GameMenu parentMenu) {
        super("Cờ Vua Trực Tuyến");
        this.parentMenu = parentMenu;
        loadPieceIcons();
        initUI();
        initBoardState();
        renderBoard();
        setSize(950, 750);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Add window listener to show menu when closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Refresh Elo information in parent menu before showing it
                if (parentMenu != null) {
                    parentMenu.refreshUserInfo();
                    parentMenu.setVisible(true);
                }
            }
        });
    }
    
    public ChessClientSwing() {
        this(null);
    }

    public void autoConnect(String host, int port) {
        hostField.setText(host);
        portField.setText(String.valueOf(port));
        appendChat("Đang kết nối tới máy chủ " + host + ":" + port);
        
        // Kết nối sau khi UI đã sẵn sàng
        SwingUtilities.invokeLater(() -> {
            connectToServer();
        });
    }
    
    public void autoConnectAndQuickPlay(String host, int port) {
        isQuickPlayMode = true; // Đánh dấu đang chơi chế độ Quick Play
        hostField.setText(host);
        portField.setText(String.valueOf(port));
        connectToServer();
        appendChat("Đang kết nối tới máy chủ " + host + ":" + port);
        
        // Đợi một chút để kết nối hoàn tất, sau đó tự động tham gia quick play
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(500); // Đợi kết nối hoàn tất
                if (socket != null && socket.isConnected() && out != null) {
                    // Gửi thông tin user trước khi tham gia quick play
                    if (parentMenu != null && parentMenu.getCurrentUser() != null) {
                        User currentUser = parentMenu.getCurrentUser();
                        out.println("SET_USER " + currentUser.getId() + " " + currentUser.getUsername());
                    }
                    out.println("QUICK_PLAY");
                    statusLabel.setText("Đang tìm đối thủ...");
                    appendChat("Đang tìm đối thủ cho chế độ Chơi Nhanh...");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
    
    public void autoConnectAndCreateRoom(String host, int port) {
        hostField.setText(host);
        portField.setText(String.valueOf(port));
        connectToServer();
        appendChat("Đang kết nối tới máy chủ " + host + ":" + port);
        
        // Đợi một chút để kết nối hoàn tất, sau đó tự động tạo phòng
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(500); // Đợi kết nối hoàn tất
                if (socket != null && socket.isConnected()) {
                    createRoom();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
    
    public void autoConnectAndJoinRoom(String host, int port, String roomCode) {
        hostField.setText(host);
        portField.setText(String.valueOf(port));
        connectToServer();
        appendChat("Đang kết nối tới máy chủ " + host + ":" + port);
        
        // Đợi một chút để kết nối hoàn tất, sau đó tự động vào phòng
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(500); // Đợi kết nối hoàn tất
                if (socket != null && socket.isConnected() && out != null) {
                    out.println("JOIN_ROOM " + roomCode);
                    statusLabel.setText("Đang vào phòng " + roomCode + "...");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
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

        JPanel ctrl = new JPanel(new GridLayout(7, 2, 4, 4));
        hostField = new JTextField("localhost");
        portField = new JTextField(String.valueOf(DEFAULT_PORT));
        
        ctrl.add(new JLabel("Host:"));
        ctrl.add(hostField);
        ctrl.add(new JLabel("Port:"));
        ctrl.add(portField);
        
        connectBtn = new JButton("Kết nối");
        connectBtn.addActionListener(e -> connectToServer());
        ctrl.add(connectBtn);
        
        // Thêm label để hiển thị mã phòng
        roomCodeLabel = new JLabel("");
        roomCodeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        roomCodeLabel.setForeground(new Color(0, 100, 0));
        ctrl.add(roomCodeLabel);
        
        // Thêm nút tạo phòng
        createRoomBtn = new JButton("Tạo phòng");
        createRoomBtn.addActionListener(e -> createRoom());
        createRoomBtn.setEnabled(false);
        ctrl.add(createRoomBtn);
        
        // Thêm nút vào phòng
        joinRoomBtn = new JButton("Vào phòng");
        joinRoomBtn.addActionListener(e -> joinRoom());
        joinRoomBtn.setEnabled(false);
        ctrl.add(joinRoomBtn);
        
        undoBtn = new JButton("Đi lại");
        undoBtn.addActionListener(e -> sendUndoRequest());
        undoBtn.setEnabled(false);
        ctrl.add(undoBtn);
        
        statusLabel = new JLabel("Chưa kết nối");
        ctrl.add(statusLabel);
        
        // Thêm label hiển thị Elo của đối thủ
        opponentEloLabel = new JLabel("Đối thủ: Chưa xác định");
        opponentEloLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        ctrl.add(opponentEloLabel);
        
        // Add New Game button
        newGameBtn = new JButton("Ván mới");
        newGameBtn.addActionListener(e -> requestNewGame());
        newGameBtn.setEnabled(false);
        ctrl.add(newGameBtn);
        
        // Add Surrender button
        surrenderBtn = new JButton("Đầu hàng");
        surrenderBtn.addActionListener(e -> {
            int option = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn đầu hàng?",
                "Xác nhận đầu hàng",
                JOptionPane.YES_NO_OPTION
            );
            if (option == JOptionPane.YES_OPTION) {
                surrender();
            }
        });
        surrenderBtn.setEnabled(false);
        ctrl.add(surrenderBtn);
        
        // Add Exit button
        exitBtn = new JButton("Thoát");
        exitBtn.addActionListener(e -> {
            if (socket != null && socket.isConnected()) {
                int option = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có chắc muốn thoát? Ván đấu sẽ bị hủy.",
                    "Xác nhận thoát",
                    JOptionPane.YES_NO_OPTION
                );
                if (option == JOptionPane.YES_OPTION) {
                    surrender(); // Thông báo cho đối thủ
                    dispose();
                    if (parentMenu != null) {
                        parentMenu.setVisible(true);
                    }
                }
            } else {
                dispose();
                if (parentMenu != null) {
                    parentMenu.setVisible(true);
                }
            }
        });
        ctrl.add(exitBtn);
        
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
            // Không gọi showGameOver ở đây nữa, đợi server phản hồi
            // Server sẽ gửi thông báo GAME_OVER với kết quả phù hợp
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
                out.println("NEW_GAME");
                // Vô hiệu hóa nút cho đến khi nhận được phản hồi từ server
                newGameBtn.setEnabled(false);
            }
        }
    }

    private void updateStatusLabel() {
        if (socket == null || !socket.isConnected()) {
            statusLabel.setText("Chưa kết nối");
            return;
        }
        
        if (myTurn) {
            statusLabel.setText(isWhite ? "Bạn chơi quân trắng - Lượt của bạn" : "Bạn chơi quân đen - Lượt của bạn");
        } else {
            statusLabel.setText(isWhite ? "Bạn chơi quân trắng - Lượt đối thủ" : "Bạn chơi quân đen - Lượt đối thủ");
        }
    }

    private void resetGame() {
        // Reset game state
        initBoardState();
        
        // Reset UI state
        selectedRow = -1;
        selectedCol = -1;
        lastRequestedFrom = null;
        draggingPiece = null;
        highlightedSquares.clear();
        
        // Re-render the board
        renderBoard();
        
        // Reset game mode
        isQuickPlayMode = false; // Reset về chế độ bình thường
        
        // Enable/disable controls appropriately
        surrenderBtn.setEnabled(false);
        undoBtn.setEnabled(false);
        
        // Update button states based on connection
        boolean isConnected = socket != null && socket.isConnected();
        newGameBtn.setEnabled(isConnected);
        connectBtn.setEnabled(!isConnected);
        connectBtn.setText("Kết nối");
        
        // Reset room controls
        createRoomBtn.setEnabled(isConnected);
        joinRoomBtn.setEnabled(isConnected);
        currentRoomCode = null;
        roomCodeLabel.setText("");
        opponentEloLabel.setText("Đối thủ: Chưa xác định");
        
        // Update connection fields
        hostField.setEnabled(!isConnected);
        portField.setEnabled(!isConnected);
        
        // Update status
        if (isConnected) {
            statusLabel.setText("Đã kết nối, chọn Tạo phòng hoặc Vào phòng");
        } else {
            statusLabel.setText("Chưa kết nối");
        }
    }

    private void showGameOver(boolean won) {
        if (won) {
            JOptionPane.showMessageDialog(this, "Bạn đã chiến thắng!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Bạn đã thua!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
        }
        resetGame();
    }

    /**
     * Xử lý kết thúc game - tự động quay về menu nếu là Quick Play
     */
    private void handleGameEnd(boolean won) {
        if (isQuickPlayMode) {
            // Chế độ Quick Play: tự động quay về menu sau 2 giây
            String message = won ? "Bạn đã chiến thắng!" : "Bạn đã thua!";
            appendChat("🎯 " + message);
            
            // Hiển thị thông báo ngắn
            JOptionPane.showMessageDialog(this, message + "\n\nTự động quay về menu chính...", 
                                        "Game Over", JOptionPane.INFORMATION_MESSAGE);
            
            // Tự động đóng cửa sổ và quay về menu
            SwingUtilities.invokeLater(() -> {
                try {
                    Thread.sleep(1000); // Đợi 1 giây để user thấy thông báo
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Đóng cửa sổ game - window listener sẽ tự động refresh Elo và show menu
                dispose();
            });
        } else {
            // Chế độ phòng: hiển thị dialog như bình thường
            showGameOver(won);
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
        if (socket != null && socket.isConnected()) {
            // Nếu đã kết nối, nút này sẽ hoạt động như nút "Ván mới"
            requestNewGame();
            return;
        }
        
        String host = hostField.getText().trim();
        int port = DEFAULT_PORT;
        try { port = Integer.parseInt(portField.getText().trim()); } catch (NumberFormatException ignored) {}
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Reset game state
            isWhite = false; // Không set màu trước, đợi server thông báo
            myTurn = false;
            highlightedSquares.clear();
            moveHistory.clear();
            selectedRow = -1;
            selectedCol = -1;
            currentRoomCode = null;
            
            // Update UI elements
            hostField.setEnabled(false);
            portField.setEnabled(false);
            connectBtn.setEnabled(false);
            createRoomBtn.setEnabled(true);
            joinRoomBtn.setEnabled(true);
            statusLabel.setText("Đã kết nối, chọn Tạo phòng hoặc Vào phòng");
            
            // Start listening for server messages
            new Thread(this::serverReaderLoop).start();
            
        } catch (IOException e) {
            appendChat("Kết nối thất bại: " + e.getMessage());
            statusLabel.setText("Kết nối thất bại");
            
            // Re-enable connection fields on failure
            hostField.setEnabled(true);
            portField.setEnabled(true);
            connectBtn.setEnabled(true);
            connectBtn.setText("Kết nối");
        }
    }
    
    private void createRoom() {
        if (socket != null && out != null) {
            out.println("CREATE_ROOM");
            statusLabel.setText("Đang tạo phòng...");
            createRoomBtn.setEnabled(false);
            joinRoomBtn.setEnabled(false);
        }
    }
    
    private void joinRoom() {
        String roomCode = JOptionPane.showInputDialog(this, "Nhập mã phòng:", "Vào phòng", JOptionPane.PLAIN_MESSAGE);
        if (roomCode != null && !roomCode.trim().isEmpty()) {
            roomCode = roomCode.trim().toUpperCase();
            if (socket != null && out != null) {
                out.println("JOIN_ROOM " + roomCode);
                statusLabel.setText("Đang vào phòng " + roomCode + "...");
                createRoomBtn.setEnabled(false);
                joinRoomBtn.setEnabled(false);
            }
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
        
        // Xử lý tin nhắn về phòng
        if (msg.startsWith("ROOM_CREATED ")) {
            currentRoomCode = msg.substring(13).trim();
            roomCodeLabel.setText("Mã phòng: " + currentRoomCode);
            statusLabel.setText("Phòng đã tạo! Đang chờ đối thủ...");
            appendChat("Phòng đã được tạo với mã: " + currentRoomCode);
            appendChat("Chia sẻ mã này cho đối thủ của bạn!");
            return;
        }
        
        if (msg.equals("ROOM_NOT_FOUND")) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy phòng với mã này!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Phòng không tồn tại");
            createRoomBtn.setEnabled(true);
            joinRoomBtn.setEnabled(true);
            return;
        }
        
        if (msg.equals("ROOM_FULL")) {
            JOptionPane.showMessageDialog(this, "Phòng đã đầy!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Phòng đã đầy");
            createRoomBtn.setEnabled(true);
            joinRoomBtn.setEnabled(true);
            return;
        }
        
        if (msg.startsWith("ROOM_JOINED ")) {
            currentRoomCode = msg.substring(12).trim();
            roomCodeLabel.setText("Mã phòng: " + currentRoomCode);
            statusLabel.setText("Đã vào phòng " + currentRoomCode);
            appendChat("Đã vào phòng: " + currentRoomCode);
            return;
        }
        
        // Xử lý tin nhắn Quick Play
        if (msg.equals("WAITING_FOR_OPPONENT")) {
            statusLabel.setText("Đang chờ đối thủ...");
            appendChat("⏳ Đang tìm đối thủ...");
            appendChat("Vui lòng chờ trong giây lát...");
            createRoomBtn.setEnabled(false);
            joinRoomBtn.setEnabled(false);
            return;
        }
        
        if (msg.equals("QUICK_PLAY_CANCELLED")) {
            statusLabel.setText("Đã hủy tìm trận");
            appendChat("❌ Đã hủy tìm trận");
            createRoomBtn.setEnabled(true);
            joinRoomBtn.setEnabled(true);
            return;
        }
        
        if (msg.startsWith("OPPONENT_ELO ")) {
            String eloStr = msg.substring(13).trim();
            System.out.println("DEBUG: Received OPPONENT_ELO: " + eloStr);
            try {
                int opponentElo = Integer.parseInt(eloStr);
                opponentEloLabel.setText("Đối thủ: " + opponentElo + " Elo");
                System.out.println("DEBUG: Set opponentEloLabel to: " + opponentElo + " Elo");
            } catch (NumberFormatException e) {
                opponentEloLabel.setText("Đối thủ: Chưa xác định");
                System.out.println("DEBUG: Failed to parse Elo: " + eloStr);
            }
            return;
        }
        
        // Xử lý tin nhắn START trước tiên vì nó thiết lập trạng thái ban đầu của game
        if (msg.startsWith("START")) {
            String color = msg.substring(5).trim(); // Lấy phần "BLACK" hoặc "WHITE" từ tin nhắn
            System.out.println("Received START with color: " + color); // Debug log
            
            // Reset game trước khi set màu mới
            resetGame();
            
            // Kiểm tra chính xác màu quân
            if (color.equals("WHITE") || color.startsWith("WHITE RESTART")) {
                isWhite = true;
                myTurn = true; // Quân trắng đi trước
                System.out.println("Set as WHITE player with myTurn = true"); // Debug log
            } else if (color.equals("BLACK") || color.startsWith("BLACK RESTART")) {
                isWhite = false;
                myTurn = false; // Quân đen chờ
                System.out.println("Set as BLACK player with myTurn = false"); // Debug log
            } else {
                System.out.println("Invalid color received: " + color);
                return;
            }
            
            // Cập nhật giao diện
            surrenderBtn.setEnabled(true);
            undoBtn.setEnabled(true);
            newGameBtn.setEnabled(true);
            connectBtn.setText("Ván mới");
            connectBtn.setEnabled(false);
            
            // Cập nhật trạng thái hiển thị
            updateStatusLabel();
            
            // Thông báo
            appendChat(String.format("<server> Bạn chơi quân %s", isWhite ? "trắng" : "đen"));
            if (msg.contains("RESTART")) {
                appendChat("Ván đấu mới đã bắt đầu!");
            } else {
                JOptionPane.showMessageDialog(this, "Ván đấu mới đã bắt đầu!");
            }
            return;
        }
        
        // Treat LEGAL_MOVES and UNDO_MOVE as internal UI messages (don't append to chat)
        if (msg.startsWith("UNDO_MOVE ")) {
            String moveStr = msg.substring(10);
            if (moveStr.matches("[a-h][1-8][a-h][1-8]")) {
                int endX = algebraicToX(moveStr.substring(2, 4));
                int endY = algebraicToY(moveStr.substring(2, 4));
                int startX = algebraicToX(moveStr.substring(0, 2));
                int startY = algebraicToY(moveStr.substring(0, 2));
                
                // Di chuyển quân cờ về vị trí cũ
                boardState[startX][startY] = boardState[endX][endY];
                boardState[endX][endY] = null;
                renderBoard();
            }
            return;
        }
        
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

        // Append tin nhắn vào chat (trừ tin nhắn START đã được xử lý ở trên)
        appendChat("<server> " + msg);

        // Các xử lý khác cho các tin nhắn
        if (msg.equals("YOUR_TURN")) {
            myTurn = true;
            System.out.println("Received YOUR_TURN. myTurn set to true. isWhite=" + isWhite); // Debug log
            updateStatusLabel();
            // Enable game control buttons
            surrenderBtn.setEnabled(true);
            undoBtn.setEnabled(true);
            newGameBtn.setEnabled(true);
        } else if (msg.startsWith("MOVE ")) {
            // Opponent move
            String move = msg.substring(5).trim();
            applyMoveToLocalBoard(move);
            myTurn = true; // after opponent moves, it's our turn
            System.out.println("Received opponent MOVE. myTurn set to true. isWhite=" + isWhite); // Debug log
            updateStatusLabel();
        } else if (msg.startsWith("MOVE_OK ")) {
            // Our move was accepted by server
            String move = msg.substring("MOVE_OK ".length()).trim();
            applyMoveToLocalBoard(move);
            myTurn = false;
            System.out.println("Move accepted by server. myTurn set to false. isWhite=" + isWhite); // Debug log
            updateStatusLabel();
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
                    handleGameEnd(iWon);
                    return;
                }
            }
            myTurn = false;
            statusLabel.setText("Đã ăn quân cờ");
        } else if (msg.startsWith("LEGAL_MOVES ")) {
            String data = msg.substring("LEGAL_MOVES ".length());
            String[] arr = data.split(",");
            highlightedSquares.clear();
            for (String move : arr)
                if (move.trim().length() == 2)
                    highlightedSquares.add(move.trim());
            highlightSquares();
        } else if (msg.equals("NOT_YOUR_TURN")) {
            JOptionPane.showMessageDialog(this, "\u26A0\uFE0F Chưa đến lượt của bạn");
        } else if (msg.startsWith("INVALID_MOVE")) {
            // Show invalid move reason (server uses several codes)
            JOptionPane.showMessageDialog(this, "\u274c Nước đi không hợp lệ: " + msg);
        } else if (msg.equals("INVALID_MOVE_NOT_YOUR_PIECE")) {
            JOptionPane.showMessageDialog(this, "❌ Không thể di chuyển quân của đối thủ!");
        } else if (msg.equals("OPPONENT_SURRENDERED")) {
            handleGameEnd(true); // Show win message for the remaining player
        } else if (msg.startsWith("GAME_OVER ")) {
            String result = msg.substring(10).trim();
            boolean won = result.equals("WHITE_WIN") ? isWhite : !isWhite;
            handleGameEnd(won);
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
        // Thay thế các từ tiếng Anh bằng tiếng Việt trong tin nhắn chat
        s = s.replace("Connected to server", "Đã kết nối tới máy chủ");
        s = s.replace("Disconnected", "Đã ngắt kết nối");
        s = s.replace("Connection failed", "Kết nối thất bại");
        s = s.replace("Not connected", "Chưa kết nối");
        s = s.replace("You:", "Bạn:");
        s = s.replace("Opponent:", "Đối thủ:");
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
