package Chess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

public class LoginRegisterDialog extends JDialog {
    private AuthService authService = new AuthService();
    private User authenticatedUser = null;

    public LoginRegisterDialog(Frame owner) {
        super(owner, "Đăng nhập / Đăng ký", true);
        initUI();
        setSize(420, 300);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        JTabbedPane tabs = new JTabbedPane();

        // Login panel
        JPanel loginPanel = new JPanel(new BorderLayout());
        JPanel loginForm = new JPanel(new GridLayout(3, 2, 6, 6));
        JTextField loginUser = new JTextField();
        JPasswordField loginPass = new JPasswordField();
        loginForm.add(new JLabel("Username or Email:"));
        loginForm.add(loginUser);
        loginForm.add(new JLabel("Password:"));
        loginForm.add(loginPass);
        JButton loginBtn = new JButton("Đăng nhập");
        loginForm.add(new JLabel());
        loginForm.add(loginBtn);
        loginPanel.add(loginForm, BorderLayout.CENTER);

        // Register panel
        JPanel regPanel = new JPanel(new BorderLayout());
        JPanel regForm = new JPanel(new GridLayout(4, 2, 6, 6));
        JTextField regUser = new JTextField();
        JTextField regEmail = new JTextField();
        JPasswordField regPass = new JPasswordField();
        regForm.add(new JLabel("Username:"));
        regForm.add(regUser);
        regForm.add(new JLabel("Email:"));
        regForm.add(regEmail);
        regForm.add(new JLabel("Password:"));
        regForm.add(regPass);
        JButton regBtn = new JButton("Đăng ký");
        regForm.add(new JLabel());
        regForm.add(regBtn);
        regPanel.add(regForm, BorderLayout.CENTER);

        tabs.addTab("Đăng nhập", loginPanel);
        tabs.addTab("Đăng ký", regPanel);

        add(tabs, BorderLayout.CENTER);

        // Actions
        loginBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String user = loginUser.getText().trim();
                String pass = new String(loginPass.getPassword());
                if (user.isEmpty() || pass.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginRegisterDialog.this, "Vui lòng nhập đầy đủ thông tin");
                    return;
                }
                try {
                    User u = authService.login(user, pass);
                    if (u != null) {
                        authenticatedUser = u;
                        JOptionPane.showMessageDialog(LoginRegisterDialog.this, "Đăng nhập thành công: " + u.getUsername());
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(LoginRegisterDialog.this, "Đăng nhập thất bại");
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(LoginRegisterDialog.this, "Lỗi kết nối DB: " + ex.getMessage());
                }
            }
        });

        regBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String user = regUser.getText().trim();
                String email = regEmail.getText().trim();
                String pass = new String(regPass.getPassword());
                if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginRegisterDialog.this, "Vui lòng nhập đầy đủ thông tin");
                    return;
                }
                try {
                    User u = authService.register(user, email, pass);
                    if (u != null) {
                        authenticatedUser = u;
                        JOptionPane.showMessageDialog(LoginRegisterDialog.this, "Đăng ký thành công: " + u.getUsername());
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(LoginRegisterDialog.this, "Đăng ký thất bại: username hoặc email đã tồn tại");
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(LoginRegisterDialog.this, "Lỗi kết nối DB: " + ex.getMessage());
                }
            }
        });
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }
}
