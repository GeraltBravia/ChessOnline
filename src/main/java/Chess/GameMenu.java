package Chess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GameMenu extends JFrame {
    private JButton quickPlayBtn, createRoomBtn, joinRoomBtn;
    private JTextField hostField;
    private JTextField portField;
    private User currentUser = null;
    
    public GameMenu() {
        super("Cờ Vua Trực Tuyến - Menu Chính");
        setSize(450, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }
    
    public GameMenu(User user) {
        this();
        this.currentUser = user;
        if (user != null) {
            setTitle("Cờ Vua Trực Tuyến - Người dùng: " + user.getUsername());
        } else {
            setTitle("Cờ Vua Trực Tuyến - Khách");
        }
        initUI();
    }
    
    private void initUI() {
        // Sử dụng BoxLayout để sắp xếp các components theo chiều dọc
        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Tiêu đề
        JLabel titleLabel = new JLabel("Cờ Vua Trực Tuyến");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Panel cho host và port
        JPanel connectionPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        connectionPanel.setMaximumSize(new Dimension(300, 60));
        connectionPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        hostField = new JTextField("localhost");
        portField = new JTextField("12345");
        connectionPanel.add(new JLabel("Máy chủ:"));
        connectionPanel.add(hostField);
        connectionPanel.add(new JLabel("Cổng:"));
        connectionPanel.add(portField);
        mainPanel.add(connectionPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        
        // Nhãn hướng dẫn
        JLabel instructionLabel = new JLabel("Chọn chế độ chơi:");
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        mainPanel.add(instructionLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Nút Tạo Phòng
        createRoomBtn = new JButton("🏠 Tạo Phòng");
        createRoomBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        createRoomBtn.setMaximumSize(new Dimension(200, 40));
        createRoomBtn.setFont(new Font("Arial", Font.BOLD, 14));
        createRoomBtn.addActionListener(e -> createRoom());
        mainPanel.add(createRoomBtn);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Nút Vào Phòng
        joinRoomBtn = new JButton("🚪 Vào Phòng");
        joinRoomBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        joinRoomBtn.setMaximumSize(new Dimension(200, 40));
        joinRoomBtn.setFont(new Font("Arial", Font.BOLD, 14));
        joinRoomBtn.addActionListener(e -> joinRoom());
        mainPanel.add(joinRoomBtn);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Nút Chơi Nhanh (Tự động ghép cặp)
        quickPlayBtn = new JButton("⚡ Chơi Nhanh (Tự động ghép)");
        quickPlayBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        quickPlayBtn.setMaximumSize(new Dimension(200, 40));
        quickPlayBtn.setFont(new Font("Arial", Font.BOLD, 14));
        quickPlayBtn.addActionListener(e -> quickPlay());
        mainPanel.add(quickPlayBtn);
        
        add(mainPanel, BorderLayout.CENTER);
    }

    // Không cần ensureLoggedIn nữa vì đã đăng nhập trước khi vào menu

    private void createRoom() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cổng không hợp lệ!");
            return;
        }
        
        // Tạo cửa sổ game và kết nối
        ChessClientSwing gameWindow = new ChessClientSwing(this);
        gameWindow.setVisible(true);
        this.setVisible(false);
        
        // Kết nối và tự động tạo phòng
        gameWindow.autoConnectAndCreateRoom(host, port);
    }
    
    private void joinRoom() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cổng không hợp lệ!");
            return;
        }
        
        // Hiển thị dialog nhập mã phòng
        String roomCode = JOptionPane.showInputDialog(this, "Nhập mã phòng:", "Vào Phòng", JOptionPane.PLAIN_MESSAGE);
        if (roomCode == null || roomCode.trim().isEmpty()) {
            return; // User cancelled
        }
        
        roomCode = roomCode.trim().toUpperCase();
        
        // Tạo cửa sổ game và kết nối
        ChessClientSwing gameWindow = new ChessClientSwing(this);
        gameWindow.setVisible(true);
        this.setVisible(false);
        
        // Kết nối và tự động vào phòng
        gameWindow.autoConnectAndJoinRoom(host, port, roomCode);
    }
    
    private void quickPlay() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cổng không hợp lệ!");
            return;
        }
        
        // Tạo cửa sổ game và kết nối
        ChessClientSwing gameWindow = new ChessClientSwing(this);
        gameWindow.setVisible(true);
        this.setVisible(false);
        
        // Kết nối và tự động tham gia Quick Play (tự động ghép cặp)
        gameWindow.autoConnectAndQuickPlay(host, port);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Hiển thị màn hình đăng nhập trước
            LoginRegisterDialog loginDialog = new LoginRegisterDialog(null);
            loginDialog.setVisible(true);
            
            // Lấy user sau khi đăng nhập
            User user = loginDialog.getAuthenticatedUser();
            
            if (user != null) {
                // Đăng nhập thành công
                GameMenu menu = new GameMenu(user);
                menu.setVisible(true);
            } else {
                // Hỏi có muốn chơi với tư cách khách không
                int option = JOptionPane.showConfirmDialog(
                    null,
                    "Bạn chưa đăng nhập. Tiếp tục với tư cách khách?",
                    "Chưa đăng nhập",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (option == JOptionPane.YES_OPTION) {
                    // Chơi với tư cách khách
                    GameMenu menu = new GameMenu(null);
                    menu.setVisible(true);
                } else {
                    // Thoát ứng dụng
                    System.exit(0);
                }
            }
        });
    }
}