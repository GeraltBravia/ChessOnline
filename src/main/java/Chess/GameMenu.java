package Chess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GameMenu extends JFrame {
    private JButton quickPlayBtn;
    private JTextField hostField;
    private JTextField portField;
    
    public GameMenu() {
        super("Cờ Vua Trực Tuyến - Menu Chính");
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
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Nút Chơi Nhanh
        quickPlayBtn = new JButton("Bắt đầu chơi");
        quickPlayBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        quickPlayBtn.setMaximumSize(new Dimension(200, 40));
        quickPlayBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quickPlay();
            }
        });
        mainPanel.add(quickPlayBtn);
        
        add(mainPanel, BorderLayout.CENTER);
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
        ChessClientSwing gameWindow = new ChessClientSwing(this);
        gameWindow.setVisible(true);
        gameWindow.autoConnect(host, port);
        this.setVisible(false);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameMenu menu = new GameMenu();
            menu.setVisible(true);
        });
    }
}