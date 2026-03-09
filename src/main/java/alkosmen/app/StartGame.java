package alkosmen.app;

import alkosmen.Game;
import alkosmen.app.menu.settings.MenuSettingsApplier;
import alkosmen.app.menu.settings.MenuSettingsDialog;
import alkosmen.app.menu.settings.MenuSettingsState;
import alkosmen.audio.MidiPlayer;
import alkosmen.settings.Constants;
import alkosmen.ui.PixelButton;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.net.URL;
import java.util.Properties;

public class StartGame {
    static final System.Logger LOGGER = System.getLogger(StartGame.class.getName());

    private static final String MENU_TRACK = "/alkosmen/sounds/Golden-Brown.mid";
    private static final String MENU_LOGO = "/alkosmen/ui/menu/alkosmeny_title_logo_v1_transparent.png";

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
            JPanel menuButtons = createMenuButtonsPanel(start, settings, exit);

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

            frame.getContentPane().add(createMenuOverlay(menuButtons), BorderLayout.CENTER);
            showFrame(frame);

        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.ERROR, "Start error", e);
            e.printStackTrace();
        }
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

    private static JPanel createMenuOverlay(JPanel menuButtons) {
        JPanel menuShell = createMenuShell(menuButtons);
        JLabel logoLabel = createMenuLogoLabel();

        JPanel overlay = new JPanel(null) {
            @Override
            public void doLayout() {
                int w = getWidth();
                int h = getHeight();

                int menuX = clamp((int) Math.round(w * 0.035), 35, 50);
                int menuY = clamp((int) Math.round(h * 0.21), 135, 160);
                int menuW = clamp((int) Math.round(w * 0.18), 180, 220);
                int menuH = Math.max(menuShell.getPreferredSize().height, 168);
                menuShell.setBounds(menuX, menuY, menuW, menuH);

                int logoW = clamp((int) Math.round(w * 0.29), 340, 390);
                int logoY = clamp((int) Math.round(h * 0.035), 16, 34);
                int logoH = updateLogoIconSize(logoLabel, logoW);
                int logoX = clamp((w - logoW) / 2 - (int) Math.round(w * 0.03), 0, Math.max(0, w - logoW - 24));
                logoLabel.setBounds(logoX, logoY, logoW, logoH);
            }
        };

        overlay.setOpaque(false);
        overlay.add(menuShell);
        overlay.add(logoLabel);
        return overlay;
    }

    private static JPanel createMenuShell(JPanel menuButtons) {
        JPanel menuShell = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                GradientPaint fill = new GradientPaint(
                        0, 0, new Color(10, 8, 26, 165),
                        0, h, new Color(4, 6, 18, 220)
                );
                g2.setPaint(fill);
                g2.fillRoundRect(0, 0, w, h, 26, 26);

                g2.setColor(new Color(126, 238, 255, 225));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 26, 26);
                g2.setColor(new Color(214, 146, 255, 112));
                g2.drawRoundRect(3, 3, w - 7, h - 7, 22, 22);
                g2.dispose();

                super.paintComponent(g);
            }
        };

        menuShell.setOpaque(false);
        menuShell.setLayout(new BoxLayout(menuShell, BoxLayout.Y_AXIS));
        menuShell.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel subtitle = new JLabel("Ночной патруль похмельного героя");
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setForeground(new Color(148, 178, 214));
        subtitle.setFont(new Font("Dialog", Font.BOLD, 16));

        menuButtons.setAlignmentX(Component.LEFT_ALIGNMENT);

        menuShell.add(subtitle);
        menuShell.add(Box.createVerticalStrut(10));
        menuShell.add(menuButtons);
        menuShell.add(Box.createVerticalGlue());

        menuShell.setPreferredSize(new Dimension(156, 180));
        return menuShell;
    }

    private static JPanel createMenuButtonsPanel(PixelButton start, PixelButton settings, PixelButton exit) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 0));

        Dimension size = new Dimension(172, 34);
        start.setMaximumSize(size);
        settings.setMaximumSize(size);
        exit.setMaximumSize(size);
        start.setPreferredSize(size);
        settings.setPreferredSize(size);
        exit.setPreferredSize(size);

        panel.add(start);
        panel.add(Box.createVerticalStrut(8));
        panel.add(settings);
        panel.add(Box.createVerticalStrut(8));
        panel.add(exit);
        return panel;
    }

    private static JLabel createMenuLogoLabel() {
        URL logoUrl = StartGame.class.getResource(MENU_LOGO);
        if (logoUrl == null) {
            JLabel fallback = new JLabel("АЛКОСМЕНЫ");
            fallback.setForeground(new Color(226, 244, 240));
            fallback.setFont(new Font("Dialog", Font.BOLD, 36));
            return fallback;
        }

        Image source = new ImageIcon(logoUrl).getImage();
        JLabel label = new JLabel();
        label.putClientProperty("logoSource", source);
        label.setHorizontalAlignment(JLabel.CENTER);
        return label;
    }

    private static int updateLogoIconSize(JLabel logoLabel, int targetW) {
        Object sourceObj = logoLabel.getClientProperty("logoSource");
        if (!(sourceObj instanceof Image source)) {
            return logoLabel.getPreferredSize().height;
        }

        int srcW = Math.max(1, source.getWidth(null));
        int srcH = Math.max(1, source.getHeight(null));
        int targetH = Math.max(1, (int) Math.round((double) srcH * targetW / srcW));

        Image scaled = source.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
        logoLabel.setIcon(new ImageIcon(scaled));
        logoLabel.setPreferredSize(new Dimension(targetW, targetH));
        return targetH;
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

    private static void openSettings(JFrame frame, MidiPlayer midi) {
        MenuSettingsState currentSettings = new MenuSettingsState(
                Constants.Width,
                Constants.Height,
                Constants.MenuMusicEnabled,
                Constants.GameMusicEnabled
        );
        MenuSettingsState updatedSettings = MenuSettingsDialog.show(frame, currentSettings);
        if (updatedSettings == null) {
            return;
        }

        MenuSettingsApplier.apply(updatedSettings, frame);
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