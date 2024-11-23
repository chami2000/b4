import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LTEBandControlApp {

    static class RouterControl {
        private String routerIp;
        private String username;
        private String password;
        private String sessionId;

        public RouterControl(String routerIp, String username, String password) {
            this.routerIp = routerIp;
            this.username = username;
            this.password = password;
            this.sessionId = null;
        }

        public String getSessionId() {
            try {
                HttpClient client = HttpClient.newHttpClient();
                URI uri = new URI("http://" + routerIp + "/cgi-bin/lua.cgi");
                JsonObject data = new JsonObject();
                data.addProperty("cmd", 100);
                data.addProperty("method", "POST");
                data.addProperty("username", username);
                data.addProperty("passwd", password);

                HttpRequest request = HttpRequest.newBuilder(uri)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (result.get("success").getAsBoolean()) {
                        sessionId = result.get("sessionId").getAsString();
                        return sessionId;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public boolean changeLTEBands(String[] bands) {
            if (sessionId == null && getSessionId() == null) {
                return false;
            }

            try {
                HttpClient client = HttpClient.newHttpClient();
                URI uri = new URI("http://" + routerIp + "/cgi-bin/lua.cgi");
                JsonObject data = new JsonObject();
                data.addProperty("cmd", 166);
                data.addProperty("method", "POST");
                data.addProperty("sessionId", sessionId);
                data.add("band", JsonParser.parseString(java.util.Arrays.toString(bands)));

                HttpRequest request = HttpRequest.newBuilder(uri)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200 && JsonParser.parseString(response.body())
                        .getAsJsonObject()
                        .get("success")
                        .getAsBoolean();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        public String[] getCurrentBands() {
            if (sessionId == null && getSessionId() == null) {
                return null;
            }

            try {
                HttpClient client = HttpClient.newHttpClient();
                URI uri = new URI("http://" + routerIp + "/cgi-bin/lua.cgi");
                JsonObject data = new JsonObject();
                data.addProperty("cmd", 165);
                data.addProperty("method", "GET");
                data.addProperty("sessionId", sessionId);

                HttpRequest request = HttpRequest.newBuilder(uri)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (result.get("success").getAsBoolean()) {
                        String[] bands = result.getAsJsonArray("lockband")
                                .toString()
                                .replace("[", "")
                                .replace("]", "")
                                .replace("\"", "")
                                .split(",");
                        return bands;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static class LTEBandControlGUI {
        private RouterControl router;
        private JFrame frame;
        private JTextField ipField, usernameField, passwordField;
        private JCheckBox[] bandCheckBoxes;
        private JLabel currentBandsLabel;
        private JButton connectButton, changeBandsButton;
        private String[] availableBands = {"EUTRAN_BAND1", "EUTRAN_BAND3", "EUTRAN_BAND5", "EUTRAN_BAND38", "EUTRAN_BAND41"};

        public LTEBandControlGUI() {
            frame = new JFrame("LTE Band Control");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 400);
            frame.setLayout(new GridLayout(10, 2));

            frame.add(new JLabel("Router IP:"));
            ipField = new JTextField("192.168.1.1");
            frame.add(ipField);

            frame.add(new JLabel("Username:"));
            usernameField = new JTextField("administrator");
            frame.add(usernameField);

            frame.add(new JLabel("Password:"));
            passwordField = new JPasswordField();
            frame.add(passwordField);

            connectButton = new JButton("Connect");
            connectButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    connectRouter();
                }
            });
            frame.add(connectButton);

            frame.add(new JLabel("Select LTE Bands:"));
            JPanel bandPanel = new JPanel();
            bandPanel.setLayout(new GridLayout(5, 1));
            bandCheckBoxes = new JCheckBox[availableBands.length];
            for (int i = 0; i < availableBands.length; i++) {
                bandCheckBoxes[i] = new JCheckBox("Band " + availableBands[i].split("_")[1]);
                bandPanel.add(bandCheckBoxes[i]);
            }
            frame.add(bandPanel);

            changeBandsButton = new JButton("Change Bands");
            changeBandsButton.setEnabled(false);
            changeBandsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    changeBands();
                }
            });
            frame.add(changeBandsButton);

            currentBandsLabel = new JLabel("Current Bands: None");
            frame.add(currentBandsLabel);

            frame.setVisible(true);
        }

        private void connectRouter() {
            router = new RouterControl(ipField.getText(), usernameField.getText(), passwordField.getText());
            if (router.getSessionId() != null) {
                JOptionPane.showMessageDialog(frame, "Connected to Router!", "Success", JOptionPane.INFORMATION_MESSAGE);
                changeBandsButton.setEnabled(true);
                updateCurrentBands();
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to Connect!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void changeBands() {
            String[] selectedBands = java.util.Arrays.stream(bandCheckBoxes)
                    .filter(JCheckBox::isSelected)
                    .map(cb -> cb.getText().replace("Band ", "EUTRAN_BAND"))
                    .toArray(String[]::new);
            if (router.changeLTEBands(selectedBands)) {
                JOptionPane.showMessageDialog(frame, "Bands Changed Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                updateCurrentBands();
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to Change Bands!", "Error", JOptionPane.ERROR_MESSAGE);
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
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LTEBandControlGUI());
    }
}
