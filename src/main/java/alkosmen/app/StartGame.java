package alkosmen.app;

import alkosmen.Game;
import alkosmen.app.menu.settings.MenuSettingsApplier;
import alkosmen.app.menu.settings.MenuSettingsDialog;
import alkosmen.app.menu.settings.MenuSettingsState;
import alkosmen.audio.MidiPlayer;
import alkosmen.audio.SoundEffectPlayer;
import alkosmen.settings.Constants;
import alkosmen.ui.PixelButton;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
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
import java.awt.AlphaComposite;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class StartGame {
    static final System.Logger LOGGER = System.getLogger(StartGame.class.getName());

    private static final String MENU_TRACK = "/alkosmen/sounds/Golden-Brown.mid";
    private static final String MENU_LOGO = "/alkosmen/ui/menu/alkosmeny_title_logo_v1_transparent.png";
    private static final String EBOBO_WALK_RIGHT_PATH = "/alkosmen/ui/intro/ebobo/walk_right";
    private static final String EBOBO_WALK_LEFT_PATH = "/alkosmen/ui/intro/ebobo/walk_left";
    private static final boolean LEVELS_TEMP_DISABLED = true;
    private static Properties uiTexts = new Properties();
    private static Properties introTexts = new Properties();

    public static void main(String[] args) {
        LOGGER.log(System.Logger.Level.INFO, "Start game...");

        try {
            Properties properties = loadProperties("/alkosmen/config.properties");
            uiTexts = loadProperties("/alkosmen/ui-texts.properties");
            introTexts = loadProperties("/alkosmen/intro.properties");
            applyConstants(properties);

            MidiPlayer midi = new MidiPlayer();
            applyMenuMusic(midi);
            JFrame frame = createMenuFrame(midi);

            PixelButton start = new PixelButton(text("menu.button.start", "Старт"));
            PixelButton settings = new PixelButton(text("menu.button.settings", "Настройки"));
            PixelButton exit = new PixelButton(text("menu.button.exit", "Выход"));
            JPanel menuButtons = createMenuButtonsPanel(start, settings, exit);

            start.addActionListener(e -> {
                if (LEVELS_TEMP_DISABLED) {
                    showTemporaryLoadingScreen(frame, midi);
                    return;
                }
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

        menuButtons.setAlignmentX(Component.LEFT_ALIGNMENT);

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
            JLabel fallback = new JLabel(text("menu.logo.fallback", "АЛКОСМЕНЫ"));
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

    private static Properties loadProperties(String resourcePath) throws Exception {
        try (var in = StartGame.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new RuntimeException("Config not found: " + resourcePath);
            }
            Properties properties = new Properties();
            try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            return properties;
        }
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
        MenuSettingsState updatedSettings = MenuSettingsDialog.show(frame, currentSettings, uiTexts);
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

    private static String text(String key, String fallback) {
        return uiTexts.getProperty(key, fallback);
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

    private static void showTemporaryLoadingScreen(JFrame owner, MidiPlayer menuMidi) {
        JDialog loading = new JDialog(owner, introText("intro.windowTitle", "Интро"), false);
        loading.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        loading.setUndecorated(true);

        boolean resumeMenuMusic = Constants.MenuMusicEnabled;
        menuMidi.stop();

        SoundEffectPlayer introVoice = loadIntroVoice();
        if (introVoice != null) {
            introVoice.play();
        }

        final String[] crawlLines = introCrawlLines();
        final int[] crawlOffset = {0};
        final int scrollStep = introInt("intro.scroll.step", 2);
        final int scrollDelayMs = introInt("intro.scroll.delay.ms", 33);
        final List<IntroSubtitleCue> subtitleCues = introSubtitleCues();
        final String[] activeSubtitle = {subtitleCues.isEmpty() ? "" : subtitleCues.get(0).text()};
        final int[] subtitleIndex = {0};
        final boolean subtitlesEnabled = introBool("intro.subtitles.enabled", false);
        final long introStartedAt = System.currentTimeMillis();
        final Image[] eboboRight = loadAnimationTrack(EBOBO_WALK_RIGHT_PATH);
        final Image[] eboboLeft = loadAnimationTrack(EBOBO_WALK_LEFT_PATH);
        final int eboboMargin = introInt("intro.ebobo.margin.px", 20);
        final int[] eboboX = {introInt("intro.ebobo.start.x.px", eboboMargin + 2)};
        final int[] eboboDirection = {1};
        final int[] eboboFrame = {0};
        final int[] eboboTick = {0};
        final int eboboStepPx = introInt("intro.ebobo.step.px", Math.max(2, owner.getWidth() / 280));
        final double eboboHeightRatio = introDouble("intro.ebobo.height.ratio", 0.21);
        final int eboboSubtitleGap = introInt("intro.ebobo.subtitle.gap.px", 52);
        final int eboboJumpAmplitude = introInt("intro.ebobo.jump.amplitude.px", 16);
        final double eboboJumpFrequency = introDouble("intro.ebobo.jump.frequency", 0.45);
        final int eboboFrameStep = Math.max(1, introInt("intro.ebobo.frame.step", 1));
        final boolean eboboRandomJumpEnabled = introBool("intro.ebobo.random.enabled", true);
        final int eboboRandomIntervalTicks = Math.max(1, introInt("intro.ebobo.random.interval.ticks", 9));
        final int eboboRandomYRangePx = Math.max(0, introInt("intro.ebobo.random.y.range.px", 44));
        final int[] eboboRandomYOffset = {0};
        final Random eboboRandom = new Random();
        final boolean eboboLaneEnabled = introBool("intro.ebobo.lane.enabled", true);
        final double eboboLaneHeightRatio = introDouble("intro.ebobo.lane.height.ratio", 0.17);
        final int eboboLaneBottomMargin = introInt("intro.ebobo.lane.bottom.margin.px", 26);
        final int eboboLaneAlpha = clamp(introInt("intro.ebobo.lane.alpha", 135), 0, 255);

        JPanel crawlPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int h = getHeight();
                drawIntroBackdrop(g2, w, h, eboboTick[0]);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int laneH = Math.max(1, (int) Math.round(h * eboboLaneHeightRatio));
                int laneYBase = h - laneH - eboboLaneBottomMargin;
                int laneY = Math.min(h - laneH, Math.max(0, laneYBase + (int) Math.round(laneH * 0.25)));

                int startY = h + 120;
                int lineGap = 48;

                for (int i = 0; i < crawlLines.length; i++) {
                    String line = crawlLines[i];
                    int y = startY - crawlOffset[0] + i * lineGap;
                    if (y < -80 || y > h + 120) {
                        continue;
                    }

                    float depth = Math.max(0f, Math.min(1f, y / (float) h));
                    int fontSize = (int) (16 + 14 * depth);
                    Font font = new Font("Monospaced", Font.BOLD, fontSize);
                    g2.setFont(font);

                    int textW = g2.getFontMetrics().stringWidth(line);
                    int x = (w - textW) / 2;

                    g2.setColor(new Color(8, 14, 34, 220));
                    g2.drawString(line, x + 2, y + 2);
                    g2.setColor(new Color(126, 230, 255));
                    g2.drawString(line, x, y);
                }

                if (eboboLaneEnabled) {
                    drawEboboLane(g2, w, laneY, laneH, eboboLaneAlpha);
                }

                if (subtitlesEnabled && !activeSubtitle[0].isBlank()) {
                    int boxH = 44;
                    int boxW = Math.min(w - 36, (int) Math.round(w * 0.72));
                    int boxX = (w - boxW) / 2;
                    int boxY = eboboLaneEnabled
                            ? Math.max(10, laneY + laneH - boxH - 10)
                            : h - boxH - 18;
                    g2.setColor(new Color(0, 0, 0, 188));
                    g2.fillRoundRect(boxX, boxY, boxW, boxH, 14, 14);
                    g2.setColor(new Color(255, 216, 82, 150));
                    g2.drawRoundRect(boxX, boxY, boxW, boxH, 14, 14);

                    g2.setFont(new Font("Dialog", Font.BOLD, 20));
                    int subW = g2.getFontMetrics().stringWidth(activeSubtitle[0]);
                    int subX = Math.max(boxX + 12, boxX + (boxW - subW) / 2);
                    int subY = boxY + 29;

                    g2.setColor(new Color(0, 0, 0, 220));
                    g2.drawString(activeSubtitle[0], subX + 1, subY + 1);
                    g2.setColor(new Color(255, 233, 160));
                    g2.drawString(activeSubtitle[0], subX, subY);
                }

                drawIntroEbobo(
                        g2,
                        w,
                        h,
                        eboboRight,
                        eboboLeft,
                        eboboFrame[0],
                        eboboDirection[0],
                        eboboX[0],
                        eboboTick[0],
                        eboboHeightRatio,
                        eboboSubtitleGap,
                        eboboJumpAmplitude,
                        eboboJumpFrequency,
                        eboboMargin,
                        eboboRandomYOffset[0],
                        eboboLaneEnabled,
                        laneY,
                        laneH
                );

                g2.dispose();
            }
        };

        loading.setContentPane(crawlPanel);
        loading.setSize(owner.getWidth(), owner.getHeight());
        loading.setLocationRelativeTo(owner);

        Timer introTimer = new Timer(scrollDelayMs, e -> {
            crawlOffset[0] += scrollStep;
            if (subtitlesEnabled && !subtitleCues.isEmpty()) {
                long elapsed = System.currentTimeMillis() - introStartedAt;
                while (subtitleIndex[0] + 1 < subtitleCues.size()
                        && elapsed >= subtitleCues.get(subtitleIndex[0] + 1).timeMs()) {
                    subtitleIndex[0]++;
                }
                activeSubtitle[0] = subtitleCues.get(subtitleIndex[0]).text();
            }

            int trackSize = Math.max(trackLength(eboboRight), trackLength(eboboLeft));
            if (trackSize > 0) {
                if (eboboTick[0] % eboboFrameStep == 0) {
                    eboboFrame[0] = (eboboFrame[0] + 1) % trackSize;
                }
                int step = Math.max(1, eboboStepPx);
                int eboboH = Math.max(1, (int) Math.round(owner.getHeight() * eboboHeightRatio));
                Image probe = frameAt(eboboDirection[0] > 0 ? eboboRight : eboboLeft, eboboFrame[0]);
                if (probe != null) {
                    int srcW = Math.max(1, probe.getWidth(null));
                    int srcH = Math.max(1, probe.getHeight(null));
                    int eboboW = Math.max(1, (int) Math.round((double) srcW * eboboH / srcH));
                    int minX = eboboMargin;
                    int maxX = Math.max(minX, owner.getWidth() - eboboW - eboboMargin);
                    if (eboboRandomJumpEnabled && eboboTick[0] % eboboRandomIntervalTicks == 0) {
                        int span = Math.max(1, maxX - minX + 1);
                        int randomX = minX + eboboRandom.nextInt(span);
                        eboboDirection[0] = randomX >= eboboX[0] ? 1 : -1;
                        eboboX[0] = randomX;
                        int ySpan = eboboRandomYRangePx * 2 + 1;
                        eboboRandomYOffset[0] = ySpan > 1 ? eboboRandom.nextInt(ySpan) - eboboRandomYRangePx : 0;
                    } else {
                        eboboX[0] += step * eboboDirection[0];
                        if (eboboX[0] <= minX) {
                            eboboX[0] = minX;
                            eboboDirection[0] = 1;
                        } else if (eboboX[0] >= maxX) {
                            eboboX[0] = maxX;
                            eboboDirection[0] = -1;
                        }
                    }
                }
                eboboTick[0]++;
            }
            crawlPanel.repaint();

            int startY = crawlPanel.getHeight() + 120;
            int lineGap = 48;
            int lastLineY = startY - crawlOffset[0] + (crawlLines.length - 1) * lineGap;
            if (lastLineY < -100) {
                ((Timer) e.getSource()).stop();
                if (introVoice != null) {
                    introVoice.stop();
                }
                if (loading.isDisplayable()) {
                    loading.dispose();
                }
                if (resumeMenuMusic) {
                    menuMidi.playLoop(MENU_TRACK);
                }
            }
        });

        Runnable skipIntro = () -> {
            if (introTimer.isRunning()) {
                introTimer.stop();
            }
            if (introVoice != null) {
                introVoice.stop();
            }
            if (loading.isDisplayable()) {
                loading.dispose();
            }
            if (resumeMenuMusic) {
                menuMidi.playLoop(MENU_TRACK);
            }
        };

        loading.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                skipIntro.run();
            }
        });
        crawlPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                skipIntro.run();
            }
        });
        loading.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                skipIntro.run();
            }
        });

        introTimer.start();
        loading.setVisible(true);
        loading.requestFocus();
    }

    private static List<IntroSubtitleCue> introSubtitleCues() {
        List<IntroSubtitleCue> cues = new ArrayList<>();
        for (int i = 1; i <= 99; i++) {
            String timeRaw = introTexts.getProperty("intro.sub." + i + ".timeMs");
            String text = introTexts.getProperty("intro.sub." + i + ".text");
            if (timeRaw == null && text == null) {
                continue;
            }
            if (timeRaw == null) {
                continue;
            }
            try {
                int timeMs = Integer.parseInt(timeRaw.trim());
                cues.add(new IntroSubtitleCue(timeMs, text == null ? "" : text));
            } catch (NumberFormatException ignored) {
            }
        }
        cues.sort(Comparator.comparingInt(IntroSubtitleCue::timeMs));
        return cues;
    }

    private record IntroSubtitleCue(int timeMs, String text) {
    }

    private static SoundEffectPlayer loadIntroVoice() {
        String[] candidates = {
                "/alkosmen/sounds/raw/intro.wav",
                "/alkosmen/sounds/raw/intro.mp3",
                "/alkosmen/sounds/raw/audio_2026-03-09_19-29-48.ogg"
        };
        for (String path : candidates) {
            try {
                return new SoundEffectPlayer(path);
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }
    private static String introText(String key, String fallback) {
        return introTexts.getProperty(key, fallback);
    }

    private static int introInt(String key, int fallback) {
        String raw = introTexts.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double introDouble(String key, double fallback) {
        String raw = introTexts.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean introBool(String key, boolean fallback) {
        String raw = introTexts.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static String[] introCrawlLines() {
        List<String> keys = new ArrayList<>();
        for (String key : introTexts.stringPropertyNames()) {
            if (key.startsWith("intro.crawl.")) {
                keys.add(key);
            }
        }
        if (keys.isEmpty()) {
            return new String[]{
                    "ЭПИЗОД I",
                    "АЛКОСМЕН: НОЧНОЙ ЗАХОД",
                    "",
                    "Ночной город не спит.",
                    "Фонари трещат, район шумит.",
                    "Менты уже на патруле.",
                    "",
                    "Первый уровень",
                    "временно отключен."
            };
        }

        keys.sort(Comparator.comparingInt(k -> {
            String suffix = k.substring("intro.crawl.".length());
            try {
                return Integer.parseInt(suffix);
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }));

        List<String> lines = new ArrayList<>(keys.size());
        for (String key : keys) {
            lines.add(introTexts.getProperty(key, ""));
        }
        return lines.toArray(new String[0]);
    }

    private static Image loadImage(String path) {
        URL url = StartGame.class.getResource(path);
        if (url == null) {
            return null;
        }
        return new ImageIcon(url).getImage();
    }

    private static void drawIntroEbobo(
            Graphics2D g2,
            int w,
            int h,
            Image[] rightTrack,
            Image[] leftTrack,
            int frame,
            int direction,
            int x,
            int tick,
            double heightRatio,
            int subtitleGap,
            int jumpAmplitude,
            double jumpFrequency,
            int margin,
            int randomYOffset,
            boolean laneEnabled,
            int laneY,
            int laneH
    ) {
        Image sprite = frameAt(direction > 0 ? rightTrack : leftTrack, frame);
        if (sprite == null) {
            return;
        }
        int srcW = Math.max(1, sprite.getWidth(null));
        int srcH = Math.max(1, sprite.getHeight(null));
        int targetH = Math.max(1, (int) Math.round(h * heightRatio));
        int targetW = Math.max(1, (int) Math.round((double) srcW * targetH / srcH));

        int jumpOffset = (int) Math.round(Math.abs(Math.sin(tick * jumpFrequency)) * jumpAmplitude);
        int y;
        if (laneEnabled) {
            y = laneY + laneH - targetH - 6 - jumpOffset - randomYOffset;
        } else {
            y = h - targetH - subtitleGap - jumpOffset - randomYOffset;
        }
        int clampedX = Math.max(margin, Math.min(x, Math.max(margin, w - targetW - margin)));

        // Neon aura behind Ebobo.
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(new Color(66, 216, 255, 82));
        g2.fillOval(clampedX - targetW / 5, y - targetH / 6, targetW + targetW / 3, targetH + targetH / 3);
        g2.setColor(new Color(255, 82, 214, 72));
        g2.fillOval(clampedX - targetW / 7, y - targetH / 8, targetW + targetW / 4, targetH + targetH / 4);

        g2.drawImage(sprite, clampedX, y, targetW, targetH, null);

        // Neon underglow on the lane/floor.
        int glowY = y + targetH - 4;
        g2.setColor(new Color(76, 222, 255, 120));
        g2.fillRoundRect(clampedX + targetW / 7, glowY, targetW - targetW / 4, 4, 4, 4);
        g2.setColor(new Color(255, 94, 226, 92));
        g2.fillRoundRect(clampedX + targetW / 5, glowY + 2, targetW - targetW / 3, 3, 3, 3);
    }

    private static void drawEboboLane(Graphics2D g2, int w, int laneY, int laneH, int alpha) {
        GradientPaint lanePaint = new GradientPaint(
                0,
                laneY,
                new Color(12, 12, 18, alpha),
                0,
                laneY + laneH,
                new Color(6, 6, 10, Math.min(255, alpha + 30))
        );
        g2.setPaint(lanePaint);
        g2.fillRoundRect(0, laneY, w, laneH, 18, 18);

        g2.setColor(new Color(255, 216, 82, Math.min(255, alpha + 45)));
        g2.drawLine(0, laneY + 1, w, laneY + 1);
        g2.setColor(new Color(0, 0, 0, Math.min(255, alpha + 40)));
        g2.drawLine(0, laneY + laneH - 1, w, laneY + laneH - 1);
    }

    private static void drawIntroBackdrop(Graphics2D g2, int w, int h, int tick) {
        GradientPaint bg = new GradientPaint(
                0,
                0,
                new Color(8, 12, 28),
                0,
                h,
                new Color(16, 24, 42)
        );
        g2.setPaint(bg);
        g2.fillRect(0, 0, w, h);

        int cell = 30;
        g2.setColor(new Color(76, 120, 170, 46));
        for (int x = 0; x <= w; x += cell) {
            g2.drawLine(x, 0, x, h);
        }
        for (int y = 0; y <= h; y += cell) {
            g2.drawLine(0, y, w, y);
        }

        Color[] palette = {
                new Color(0, 240, 245, 170),
                new Color(246, 215, 80, 170),
                new Color(212, 104, 255, 170),
                new Color(88, 255, 132, 170),
                new Color(255, 120, 96, 170)
        };

        for (int i = 0; i < 8; i++) {
            int type = i % 5;
            int laneX = (int) Math.round((i + 1) * (w / 9.0));
            int speed = 5 + (i % 4) * 2;
            int y = ((tick * speed) + i * 140) % (h + cell * 8) - cell * 4;
            drawTetrisPiece(g2, laneX, y, cell - 3, palette[type], type);
        }
    }

    private static void drawTetrisPiece(Graphics2D g2, int x, int y, int cell, Color color, int type) {
        int[][] offsets;
        switch (type) {
            case 0 -> offsets = new int[][]{{0, 0}, {1, 0}, {-1, 0}, {2, 0}}; // I
            case 1 -> offsets = new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}}; // O
            case 2 -> offsets = new int[][]{{0, 0}, {-1, 0}, {1, 0}, {0, 1}}; // T
            case 3 -> offsets = new int[][]{{0, 0}, {1, 0}, {0, 1}, {-1, 1}}; // S
            default -> offsets = new int[][]{{0, 0}, {-1, 0}, {0, 1}, {1, 1}}; // Z
        }
        for (int[] d : offsets) {
            int bx = x + d[0] * (cell + 2);
            int by = y + d[1] * (cell + 2);
            g2.setColor(color);
            g2.fillRoundRect(bx, by, cell, cell, 4, 4);
            g2.setColor(new Color(255, 255, 255, 90));
            g2.drawLine(bx + 1, by + 1, bx + cell - 2, by + 1);
            g2.drawLine(bx + 1, by + 1, bx + 1, by + cell - 2);
            g2.setColor(new Color(0, 0, 0, 110));
            g2.drawLine(bx + cell - 1, by + 1, bx + cell - 1, by + cell - 1);
            g2.drawLine(bx + 1, by + cell - 1, bx + cell - 1, by + cell - 1);
        }
    }

    private static Image[] loadAnimationTrack(String folderPath) {
        List<Image> track = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Image frame = loadImage(folderPath + "/" + String.format("%02d", i) + ".png");
            if (frame == null) {
                break;
            }
            track.add(frame);
        }
        return track.toArray(new Image[0]);
    }

    private static Image frameAt(Image[] track, int index) {
        if (track == null || track.length == 0) {
            return null;
        }
        int safeIndex = Math.floorMod(index, track.length);
        return track[safeIndex];
    }

    private static int trackLength(Image[] track) {
        return track == null ? 0 : track.length;
    }
}
