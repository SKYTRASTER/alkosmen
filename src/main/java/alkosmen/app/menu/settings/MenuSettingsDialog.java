package alkosmen.app.menu.settings;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.GridLayout;

public final class MenuSettingsDialog {
    private static final String[] RESOLUTIONS = {
            "1024x576",
            "1280x720",
            "1366x768",
            "1600x900",
            "1920x1080"
    };

    private MenuSettingsDialog() {
    }

    public static MenuSettingsState show(JFrame frame, MenuSettingsState current) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Разрешение"));

        JComboBox<String> resolutionBox = new JComboBox<>(RESOLUTIONS);
        resolutionBox.setSelectedItem(current.width() + "x" + current.height());
        panel.add(resolutionBox);

        JCheckBox menuMusicBox = new JCheckBox("Музыка в меню", current.menuMusicEnabled());
        panel.add(menuMusicBox);

        JCheckBox gameMusicBox = new JCheckBox("Фоновая музыка в игре", current.gameMusicEnabled());
        panel.add(gameMusicBox);

        int result = JOptionPane.showConfirmDialog(
                frame,
                panel,
                "Настройки",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        String selected = (String) resolutionBox.getSelectedItem();
        int width = current.width();
        int height = current.height();
        if (selected != null && selected.contains("x")) {
            String[] parts = selected.split("x");
            width = Integer.parseInt(parts[0]);
            height = Integer.parseInt(parts[1]);
        }

        return new MenuSettingsState(
                width,
                height,
                menuMusicBox.isSelected(),
                gameMusicBox.isSelected()
        );
    }
}