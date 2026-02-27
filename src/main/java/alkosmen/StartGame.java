package alkosmen;

import alkosmen.audio.MidiPlayer;
import alkosmen.ui.PixelButton;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
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

    private static boolean menuMusicEnabled = true;

    public static void main(String[] args) {
        LOGGER.log(System.Logger.Level.INFO, "Start game...");

        Properties aProperties = new Properties();
        try (var in = StartGame.class.getResourceAsStream("/alkosmen/config.properties")) {
            if (in == null) {
                throw new RuntimeException("Config not found: /alkosmen/config.properties");
            }
            aProperties.load(in);

            Constants.Height = Integer.parseInt(aProperties.getProperty("Height"));
            Constants.Width = Integer.parseInt(aProperties.getProperty("Width"));
            Constants.Size = Integer.parseInt(aProperties.getProperty("Size"));
            Constants.Font = aProperties.getProperty("Font");

            MidiPlayer midi = new MidiPlayer();
            applyMenuMusic(midi);

            JFrame frame = new JFrame(Constants.Title);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    midi.stop();
                }
            });

            frame.setContentPane(new alkosmen.service.BackgroundPanel());
            frame.getContentPane().setLayout(null);
            frame.setSize(Constants.Width, Constants.Height);
            frame.setResizable(false);

            PixelButton start = new PixelButton("Start");
            PixelButton settings = new PixelButton("Settings");
            PixelButton exit = new PixelButton("Exit");

            positionMenuButtons(start, settings, exit);

            start.addActionListener(e -> {
                midi.stop();
                frame.dispose();
                newGame();
            });

            settings.addActionListener(e -> {
                openSettings(frame, midi, start, settings, exit);
            });

            exit.addActionListener(e -> {
                midi.stop();
                frame.dispose();
            });

            frame.getContentPane().add(start);
            frame.getContentPane().add(settings);
            frame.getContentPane().add(exit);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.ERROR, "Start error", e);
            e.printStackTrace();
        }
    }

    private static void positionMenuButtons(PixelButton start, PixelButton settings, PixelButton exit) {
        int centerX = Constants.Width / 2 - 180;
        int centerY = Constants.Height / 2;
        start.setBounds(centerX, centerY - 100, 360, 70);
        settings.setBounds(centerX, centerY - 10, 360, 70);
        exit.setBounds(centerX, centerY + 80, 360, 70);
    }

    private static void openSettings(
            JFrame frame,
            MidiPlayer midi,
            PixelButton start,
            PixelButton settings,
            PixelButton exit
    ) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Resolution"));

        JComboBox<String> resolutionBox = new JComboBox<>(RESOLUTIONS);
        String current = Constants.Width + "x" + Constants.Height;
        resolutionBox.setSelectedItem(current);
        panel.add(resolutionBox);

        JCheckBox menuMusicBox = new JCheckBox("Enable menu music", menuMusicEnabled);
        panel.add(menuMusicBox);

        int result = JOptionPane.showConfirmDialog(
                frame,
                panel,
                "Settings",
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
            positionMenuButtons(start, settings, exit);
            frame.revalidate();
            frame.repaint();
            frame.setLocationRelativeTo(null);
        }

        menuMusicEnabled = menuMusicBox.isSelected();
        applyMenuMusic(midi);
    }

    private static void applyMenuMusic(MidiPlayer midi) {
        if (menuMusicEnabled) {
            midi.playLoop(MENU_TRACK);
        } else {
            midi.stop();
        }
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
                Game.stopGame();
            }
        });

        g.requestFocusInWindow();
        g.start();
    }
}
