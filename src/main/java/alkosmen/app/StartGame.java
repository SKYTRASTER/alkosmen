package alkosmen.app;

import alkosmen.Game;
import alkosmen.audio.MidiPlayer;
import alkosmen.settings.Constants;
import alkosmen.ui.PixelButton;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Properties;

public class StartGame {
    static final System.Logger LOGGER = System.getLogger(StartGame.class.getName());

    private static final String MENU_TRACK = "/alkosmen/sounds/Golden-Brown.mid";
    private static final String[] RESOLUTIONS = {
            "1024x576",
            "1280x720",
            "1366x768",
            "1600x900",
            "1920x1080"
    };

    public static void main(String[] args) {
        LOGGER.log(System.Logger.Level.INFO, "Start game...");

        try (var in = StartGame.class.getResourceAsStream("/alkosmen/config.properties")) {
            Properties properties = loadProperties(in);
            applyConstants(properties);

            MidiPlayer midi = new MidiPlayer();
            applyMenuMusic(midi);
            JFrame frame = createMenuFrame(midi);

            PixelButton start = new PixelButton("Старт");
            PixelButton settings = new PixelButton("Настройки");
            PixelButton exit = new PixelButton("Выход");
            JPanel menuPanel = createMenuPanel(start, settings, exit);

            start.addActionListener(e -> {
                midi.stop();
                frame.dispose();
                newGame();
            });
            settings.addActionListener(e -> openSettings(frame, midi));
            exit.addActionListener(e -> {
                midi.stop();
                frame.dispose();
            });

            frame.getContentPane().add(createLeftMenuOverlay(menuPanel), BorderLayout.CENTER);
            showFrame(frame);

        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.ERROR, "Start error", e);
            e.printStackTrace();
        }
    }

    private static Properties loadProperties(java.io.InputStream in) throws Exception {
        if (in == null) {
            throw new RuntimeException("Config not found: /alkosmen/config.properties");
        }
        Properties properties = new Properties();
        properties.load(in);
        return properties;
    }

    private static void applyConstants(Properties properties) {
        Constants.Height = Integer.parseInt(properties.getProperty("Height"));
        Constants.Width = Integer.parseInt(properties.getProperty("Width"));
        Constants.Size = Integer.parseInt(properties.getProperty("Size"));
        Constants.Font = properties.getProperty("Font");
        Constants.MenuMusicEnabled = Boolean.parseBoolean(
                properties.getProperty("MenuMusicEnabled", String.valueOf(Constants.MenuMusicEnabled))
        );
        Constants.GameMusicEnabled = Boolean.parseBoolean(
                properties.getProperty("GameMusicEnabled", String.valueOf(Constants.GameMusicEnabled))
        );
    }

    private static JFrame createMenuFrame(MidiPlayer midi) {
        JFrame frame = new JFrame(Constants.Title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                midi.stop();
            }
        });
        frame.setContentPane(new alkosmen.service.BackgroundPanel());
        frame.getContentPane().setLayout(new BorderLayout());
        frame.setSize(Constants.Width, Constants.Height);
        frame.setResizable(false);
        return frame;
    }

    private static JPanel createLeftMenuOverlay(JPanel menuPanel) {
        JPanel leftRail = new JPanel();
        leftRail.setOpaque(false);
        leftRail.setLayout(new BoxLayout(leftRail, BoxLayout.Y_AXIS));

        JPanel overlay = new JPanel(null) {
            @Override
            public void doLayout() {
                int w = getWidth();
                int h = getHeight();

                int menuX = clamp((int) Math.round(w * 0.055), 50, 80);
                int menuY = clamp((int) Math.round(h * 0.25), 150, 180);
                int menuWidth = clamp((int) Math.round(w * 0.31), 380, 420);
                int menuHeight = Math.max(330, h - menuY - 34);

                leftRail.setBounds(menuX, menuY, menuWidth, menuHeight);
            }
        };
        overlay.setOpaque(false);

        JLabel title = new JLabel("АЛКОСМЕН");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setForeground(new Color(214, 246, 240));
        title.setFont(new Font("Dialog", Font.BOLD, 40));

        JLabel subtitle = new JLabel("Ночной патруль похмельного героя");
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setForeground(new Color(155, 206, 196));
        subtitle.setFont(new Font("Dialog", Font.BOLD, 16));

        menuPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        leftRail.add(title);
        leftRail.add(Box.createVerticalStrut(8));
        leftRail.add(subtitle);
        leftRail.add(Box.createVerticalStrut(28));
        leftRail.add(menuPanel);
        leftRail.add(Box.createVerticalGlue());

        overlay.add(leftRail);
        return overlay;
    }

    private static JPanel createMenuPanel(PixelButton start, PixelButton settings, PixelButton exit) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Dimension size = new Dimension(340, 66);
        start.setMaximumSize(size);
        settings.setMaximumSize(size);
        exit.setMaximumSize(size);
        start.setPreferredSize(size);
        settings.setPreferredSize(size);
        exit.setPreferredSize(size);

        panel.add(start);
        panel.add(Box.createVerticalStrut(12));
        panel.add(settings);
        panel.add(Box.createVerticalStrut(12));
        panel.add(exit);
        return panel;
    }

    private static void openSettings(JFrame frame, MidiPlayer midi) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Разрешение"));

        JComboBox<String> resolutionBox = new JComboBox<>(RESOLUTIONS);
        String current = Constants.Width + "x" + Constants.Height;
        resolutionBox.setSelectedItem(current);
        panel.add(resolutionBox);

        JCheckBox menuMusicBox = new JCheckBox("Музыка в меню", Constants.MenuMusicEnabled);
        panel.add(menuMusicBox);

        JCheckBox gameMusicBox = new JCheckBox("Фоновая музыка в игре", Constants.GameMusicEnabled);
        panel.add(gameMusicBox);

        int result = JOptionPane.showConfirmDialog(
                frame,
                panel,
                "Настройки",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String selected = (String) resolutionBox.getSelectedItem();
        if (selected != null && selected.contains("x")) {
            String[] parts = selected.split("x");
            Constants.Width = Integer.parseInt(parts[0]);
            Constants.Height = Integer.parseInt(parts[1]);
            frame.setSize(Constants.Width, Constants.Height);
            for (Component component : frame.getContentPane().getComponents()) {
                component.revalidate();
            }
            frame.revalidate();
            frame.repaint();
            frame.setLocationRelativeTo(null);
        }

        Constants.MenuMusicEnabled = menuMusicBox.isSelected();
        Constants.GameMusicEnabled = gameMusicBox.isSelected();
        applyMenuMusic(midi);
    }

    private static void applyMenuMusic(MidiPlayer midi) {
        if (Constants.MenuMusicEnabled) {
            midi.playLoop(MENU_TRACK);
        } else {
            midi.stop();
        }
    }

    private static void showFrame(JFrame frame) {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void newGame() {
        JFrame frame = new JFrame(" ");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setResizable(false);

        Game g = new Game();
        frame.add(g, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                g.stopGame();
            }
        });

        g.requestFocusInWindow();
        g.start();
    }
}
