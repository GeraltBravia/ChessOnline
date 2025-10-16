package Chess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GameMenu extends JFrame {
    private JButton quickPlayBtn;
    private JButton createRoomBtn;
    private JTextField hostField;
    private JTextField portField;
    
    public GameMenu() {
        super("Chess Game Menu");
        initUI();
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }
    
    private void initUI() {
        // Sử dụng BoxLayout để sắp xếp các components theo chiều dọc
        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Tiêu đề
        JLabel titleLabel = new JLabel("Chess Online");
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
        connectionPanel.add(new JLabel("Host:"));
        connectionPanel.add(hostField);
        connectionPanel.add(new JLabel("Port:"));
        connectionPanel.add(portField);
        mainPanel.add(connectionPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Nút Chơi Nhanh
        quickPlayBtn = new JButton("Chơi Nhanh");
        quickPlayBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        quickPlayBtn.setMaximumSize(new Dimension(200, 40));
        quickPlayBtn.addActionListener(e -> quickPlay());
        mainPanel.add(quickPlayBtn);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        // Nút Tạo Phòng
        createRoomBtn = new JButton("Tạo Phòng");
        createRoomBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        createRoomBtn.setMaximumSize(new Dimension(200, 40));
        createRoomBtn.addActionListener(e -> createRoom());
        mainPanel.add(createRoomBtn);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private void quickPlay() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port không hợp lệ!");
            return;
        }
        ChessClientSwing gameWindow = new ChessClientSwing();
        gameWindow.setVisible(true);
        this.dispose(); // Đóng cửa sổ menu
    }
    
    private void createRoom() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port không hợp lệ!");
            return;
        }
        String roomId = JOptionPane.showInputDialog(this, 
            "Nhập mã phòng (để trống để tạo mã tự động):", 
            "Tạo Phòng", 
            JOptionPane.PLAIN_MESSAGE);
        ChessClientSwing gameWindow = new ChessClientSwing();
        gameWindow.setVisible(true);
        this.dispose(); // Đóng cửa sổ menu
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameMenu menu = new GameMenu();
            menu.setVisible(true);
        });
    }
}