import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.http.*;
import java.util.prefs.Preferences;
import com.google.gson.*;

public class LTEBandControlApp {
    // RouterControl class remains the same as before
    static class RouterControl {
        // ... (previous RouterControl implementation)
    }

    public static class ModernDarkUI {
        // Custom colors for dark theme
        static final Color BACKGROUND = new Color(32, 33, 36);
        static final Color SURFACE = new Color(41, 42, 45);
        static final Color PRIMARY = new Color(138, 180, 248);
        static final Color SECONDARY = new Color(241, 243, 244);
        static final Color ERROR = new Color(242, 139, 130);
        static final Color SUCCESS = new Color(129, 201, 149);
    }

    public static class LTEBandControlGUI {
        private RouterControl router;
        private JFrame frame;
        private JTextField ipField;
        private JTextField usernameField;
        private JPasswordField passwordField;
        private JCheckBox[] bandCheckBoxes;
        private JLabel currentBandsLabel;
        private JButton connectButton, changeBandsButton;
        private JCheckBox rememberCredentials;
        private String[] availableBands = {"EUTRAN_BAND1", "EUTRAN_BAND3", "EUTRAN_BAND5", "EUTRAN_BAND38", "EUTRAN_BAND41"};
        private Preferences prefs;

        public LTEBandControlGUI() {
            setupPreferences();
            createAndShowGUI();
            loadSavedCredentials();
        }

        private void setupPreferences() {
            prefs = Preferences.userNodeForPackage(LTEBandControlApp.class);
        }

        private void createAndShowGUI() {
            frame = new JFrame("LTE Band Control");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setBackground(ModernDarkUI.BACKGROUND);
            frame.setSize(500, 600);

            // Main panel with padding
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBackground(ModernDarkUI.BACKGROUND);
            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            // Add components
            addConnectionPanel(mainPanel);
            addBandSelectionPanel(mainPanel);
            addStatusPanel(mainPanel);

            // Scroll pane for responsiveness
            JScrollPane scrollPane = new JScrollPane(mainPanel);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
            frame.add(scrollPane);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }

        private void addConnectionPanel(JPanel parent) {
            JPanel panel = createPanelWithTitle("Connection Settings");
            
            // Grid for form fields
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(ModernDarkUI.SURFACE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            // Router IP
            gbc.gridx = 0; gbc.gridy = 0;
            formPanel.add(createLabel("Router IP:"), gbc);
            gbc.gridx = 1;
            ipField = createTextField("192.168.1.1");
            formPanel.add(ipField, gbc);

            // Username
            gbc.gridx = 0; gbc.gridy = 1;
            formPanel.add(createLabel("Username:"), gbc);
            gbc.gridx = 1;
            usernameField = createTextField("administrator");
            formPanel.add(usernameField, gbc);

            // Password
            gbc.gridx = 0; gbc.gridy = 2;
            formPanel.add(createLabel("Password:"), gbc);
            gbc.gridx = 1;
            passwordField = createPasswordField();
            formPanel.add(passwordField, gbc);

            // Remember credentials checkbox
            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
            rememberCredentials = new JCheckBox("Remember credentials");
            styleCheckBox(rememberCredentials);
            formPanel.add(rememberCredentials, gbc);

            // Connect button
            gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
            connectButton = createButton("Connect");
            connectButton.addActionListener(e -> connectRouter());
            formPanel.add(connectButton, gbc);

            panel.add(formPanel);
            parent.add(panel);
            parent.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        private void addBandSelectionPanel(JPanel parent) {
            JPanel panel = createPanelWithTitle("LTE Band Selection");
            
            JPanel bandPanel = new JPanel();
            bandPanel.setLayout(new BoxLayout(bandPanel, BoxLayout.Y_AXIS));
            bandPanel.setBackground(ModernDarkUI.SURFACE);
            
            bandCheckBoxes = new JCheckBox[availableBands.length];
            for (int i = 0; i < availableBands.length; i++) {
                bandCheckBoxes[i] = new JCheckBox("Band " + availableBands[i].split("_")[1]);
                styleCheckBox(bandCheckBoxes[i]);
                bandPanel.add(bandCheckBoxes[i]);
                bandPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }

            changeBandsButton = createButton("Change Bands");
            changeBandsButton.setEnabled(false);
            changeBandsButton.addActionListener(e -> changeBands());
            
            panel.add(bandPanel);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
            panel.add(changeBandsButton);
            
            parent.add(panel);
            parent.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        private void addStatusPanel(JPanel parent) {
            JPanel panel = createPanelWithTitle("Status");
            
            currentBandsLabel = createLabel("Current Bands: None");
            currentBandsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(currentBandsLabel);
            
            parent.add(panel);
        }

        private JPanel createPanelWithTitle(String title) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(ModernDarkUI.SURFACE);
            panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(ModernDarkUI.PRIMARY),
                    title,
                    TitledBorder.LEFT,
                    TitledBorder.TOP,
                    null,
                    ModernDarkUI.PRIMARY
                ),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            return panel;
        }

        private JLabel createLabel(String text) {
            JLabel label = new JLabel(text);
            label.setForeground(ModernDarkUI.SECONDARY);
            return label;
        }

        private JTextField createTextField(String defaultText) {
            JTextField field = new JTextField(defaultText);
            field.setBackground(ModernDarkUI.BACKGROUND);
            field.setForeground(ModernDarkUI.SECONDARY);
            field.setCaretColor(ModernDarkUI.SECONDARY);
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernDarkUI.PRIMARY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
            return field;
        }

        private JPasswordField createPasswordField() {
            JPasswordField field = new JPasswordField();
            field.setBackground(ModernDarkUI.BACKGROUND);
            field.setForeground(ModernDarkUI.SECONDARY);
            field.setCaretColor(ModernDarkUI.SECONDARY);
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernDarkUI.PRIMARY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
            return field;
        }

        private JButton createButton(String text) {
            JButton button = new JButton(text);
            button.setBackground(ModernDarkUI.PRIMARY);
            button.setForeground(Color.BLACK);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return button;
        }

        private void styleCheckBox(JCheckBox checkBox) {
            checkBox.setBackground(ModernDarkUI.SURFACE);
            checkBox.setForeground(ModernDarkUI.SECONDARY);
            checkBox.setFocusPainted(false);
        }

        private void saveCredentials() {
            if (rememberCredentials.isSelected()) {
                prefs.put("routerIp", ipField.getText());
                prefs.put("username", usernameField.getText());
                prefs.put("password", new String(passwordField.getPassword()));
                prefs.putBoolean("rememberCredentials", true);
            } else {
                clearSavedCredentials();
            }
        }

        private void loadSavedCredentials() {
            if (prefs.getBoolean("rememberCredentials", false)) {
                ipField.setText(prefs.get("routerIp", "192.168.1.1"));
                usernameField.setText(prefs.get("username", "administrator"));
                passwordField.setText(prefs.get("password", ""));
                rememberCredentials.setSelected(true);
            }
        }

        private void clearSavedCredentials() {
            prefs.remove("routerIp");
            prefs.remove("username");
            prefs.remove("password");
            prefs.remove("rememberCredentials");
        }

        private void connectRouter() {
            router = new RouterControl(ipField.getText(), usernameField.getText(), new String(passwordField.getPassword()));
            if (router.getSessionId() != null) {
                showMessage("Connected to Router!", "Success", JOptionPane.INFORMATION_MESSAGE);
                changeBandsButton.setEnabled(true);
                updateCurrentBands();
                saveCredentials();
            } else {
                showMessage("Failed to Connect!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void changeBands() {
            String[] selectedBands = java.util.Arrays.stream(bandCheckBoxes)
                    .filter(JCheckBox::isSelected)
                    .map(cb -> cb.getText().replace("Band ", "EUTRAN_BAND"))
                    .toArray(String[]::new);
            if (router.changeLTEBands(selectedBands)) {
                showMessage("Bands Changed Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                updateCurrentBands();
            } else {
                showMessage("Failed to Change Bands!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void updateCurrentBands() {
            String[] currentBands = router.getCurrentBands();
            if (currentBands != null) {
                currentBandsLabel.setText("Current Bands: " + String.join(", ", currentBands));
                for (JCheckBox cb : bandCheckBoxes) {
                    cb.setSelected(java.util.Arrays.asList(currentBands).contains(cb.getText().replace("Band ", "EUTRAN_BAND")));
                }
            } else {
                currentBandsLabel.setText("Failed to Fetch Current Bands");
            }
        }

        private void showMessage(String message, String title, int messageType) {
            UIManager.put("OptionPane.background", ModernDarkUI.SURFACE);
            UIManager.put("Panel.background", ModernDarkUI.SURFACE);
            UIManager.put("OptionPane.messageForeground", ModernDarkUI.SECONDARY);
            JOptionPane.showMessageDialog(frame, message, title, messageType);
        }
    }

    // Custom ScrollBar UI for dark theme
    static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = ModernDarkUI.PRIMARY;
            this.trackColor = ModernDarkUI.SURFACE;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            return button;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> new LTEBandControlGUI());
    }
}
