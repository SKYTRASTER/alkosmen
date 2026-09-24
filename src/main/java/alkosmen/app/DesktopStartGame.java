package alkosmen.app;

import alkosmen.Game;
import alkosmen.app.menu.settings.MenuSettingsApplier;
import alkosmen.app.menu.settings.MenuSettingsDialog;
import alkosmen.app.menu.settings.MenuSettingsState;
import alkosmen.audio.MenuMusicPlayer;
import alkosmen.audio.SoundEffectPlayer;
import alkosmen.service.BackgroundPanel;
import alkosmen.settings.Constants;
import alkosmen.ui.PixelButton;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

public class DesktopStartGame {
   static final System.Logger LOGGER = System.getLogger(DesktopStartGame.class.getName());
   private static final String MENU_LOGO_SOURCE = "/alkosmen/ui/menu/town_square_menu_hero_v2.png";
   private static final int MENU_ART_WIDTH = 1672;
   private static final int MENU_ART_HEIGHT = 941;
   private static final int LOGO_SOURCE_X = 60;
   private static final int LOGO_SOURCE_Y = 95;
   private static final int LOGO_SOURCE_WIDTH = 620;
   private static final int LOGO_SOURCE_HEIGHT = 275;
   private static final double BUTTON_X_RATIO = 0.064;
   private static final double BUTTON_Y_RATIO = 0.465;
   private static final int BUTTON_WIDTH = 296;
   private static final String EBOBO_WALK_RIGHT_PATH = "/alkosmen/ui/intro/ebobo/walk_right";
   private static final String EBOBO_WALK_LEFT_PATH = "/alkosmen/ui/intro/ebobo/walk_left";
   private static final boolean LEVELS_TEMP_DISABLED = false;
   private static Properties uiTexts = new Properties();
   private static Properties introTexts = new Properties();

   public static void main(String[] args) {
      LOGGER.log(Level.INFO, "Start game...");

      try {
         Properties properties = loadProperties("/alkosmen/config.properties");
         uiTexts = loadProperties("/alkosmen/ui-texts.properties");
         introTexts = loadProperties("/alkosmen/intro.properties");
         applyConstants(properties);
         MenuMusicPlayer menuMusic = new MenuMusicPlayer();
         JFrame frame = createMenuFrame(menuMusic);
         PixelButton start = new PixelButton(text("menu.button.start", "Старт"));
         PixelButton settings = new PixelButton(text("menu.button.settings", "Настройки"));
         PixelButton exit = new PixelButton(text("menu.button.exit", "Выход"));
         JPanel menuButtons = createMenuButtonsPanel(start, settings, exit);
         start.addActionListener((ex) -> {
            menuMusic.stop();
            frame.dispose();
            newGame();
         });
         settings.addActionListener((ex) -> openSettings(frame, menuMusic));
         exit.addActionListener((ex) -> {
            menuMusic.stop();
            frame.dispose();
         });
         frame.getContentPane().add(createMenuOverlay(menuButtons), "Center");
         showFrame(frame);
         applyMenuMusic(menuMusic);
      } catch (Exception e) {
         LOGGER.log(Level.ERROR, "Start error", e);
         e.printStackTrace();
      }

   }

   private static JFrame createMenuFrame(final MenuMusicPlayer menuMusic) {
      JFrame frame = new JFrame(Constants.Title);
      frame.setDefaultCloseOperation(3);
      frame.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            menuMusic.stop();
         }
      });
      BackgroundPanel background = new BackgroundPanel();
      background.setPreferredSize(new Dimension(Constants.Width, Constants.Height));
      frame.setContentPane(background);
      frame.getContentPane().setLayout(new BorderLayout());
      frame.pack();
      frame.setResizable(false);
      return frame;
   }

   private static JPanel createMenuOverlay(final JPanel menuButtons) {
      final JLabel logoLabel = createMenuLogoLabel();
      JPanel overlay = new JPanel((LayoutManager)null) {
         public void doLayout() {
            int w = this.getWidth();
            int h = this.getHeight();
            int menuX = (int)Math.round((double)w * 0.064);
            int menuY = (int)Math.round((double)h * 0.465);
            menuButtons.setBounds(menuX, menuY, 296, menuButtons.getPreferredSize().height);
            int logoX = Math.round((float)(w * 60 / 1672));
            int logoY = Math.round((float)(h * 95 / 941));
            int logoW = Math.round((float)(w * 620 / 1672));
            int logoH = DesktopStartGame.updateLogoIconSize(logoLabel, logoW);
            logoLabel.setBounds(logoX, logoY, logoW, logoH);
         }
      };
      overlay.setOpaque(false);
      overlay.add(menuButtons);
      overlay.add(logoLabel);
      return overlay;
   }

   private static JPanel createMenuShell(JPanel menuButtons) {
      JPanel menuShell = new JPanel() {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = this.getWidth();
            int h = this.getHeight();
            GradientPaint fill = new GradientPaint(0.0F, 0.0F, new Color(10, 8, 26, 165), 0.0F, (float)h, new Color(4, 6, 18, 220));
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
      menuShell.setLayout(new BoxLayout(menuShell, 1));
      menuShell.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
      menuButtons.setAlignmentX(0.0F);
      menuShell.add(menuButtons);
      menuShell.add(Box.createVerticalGlue());
      menuShell.setPreferredSize(new Dimension(156, 180));
      return menuShell;
   }

   private static JPanel createMenuButtonsPanel(PixelButton start, PixelButton settings, PixelButton exit) {
      JPanel panel = new JPanel();
      panel.setOpaque(false);
      panel.setLayout(new BoxLayout(panel, 1));
      panel.setBorder(BorderFactory.createEmptyBorder());
      Dimension size = new Dimension(296, 56);
      start.setMaximumSize(size);
      settings.setMaximumSize(size);
      exit.setMaximumSize(size);
      start.setPreferredSize(size);
      settings.setPreferredSize(size);
      exit.setPreferredSize(size);
      panel.add(start);
      panel.add(Box.createVerticalStrut(16));
      panel.add(settings);
      panel.add(Box.createVerticalStrut(16));
      panel.add(exit);
      return panel;
   }

   private static JLabel createMenuLogoLabel() {
      URL logoUrl = DesktopStartGame.class.getResource("/alkosmen/ui/menu/town_square_menu_hero_v2.png");
      if (logoUrl == null) {
         JLabel fallback = new JLabel(text("menu.logo.fallback", "АЛКОСМЕНЫ"));
         fallback.setForeground(new Color(226, 244, 240));
         fallback.setFont(new Font("Dialog", 1, 36));
         return fallback;
      } else {
         Image source;
         try {
            BufferedImage art = ImageIO.read(logoUrl);
            source = art.getSubimage(60, 95, 620, 275);
         } catch (IOException error) {
            throw new IllegalStateException("Could not load menu logo", error);
         }

         JLabel label = new JLabel();
         label.putClientProperty("logoSource", source);
         label.setHorizontalAlignment(0);
         return label;
      }
   }

   private static int updateLogoIconSize(JLabel logoLabel, int targetW) {
      Object sourceObj = logoLabel.getClientProperty("logoSource");
      if (sourceObj instanceof Image source) {
         int srcW = Math.max(1, source.getWidth((ImageObserver)null));
         int srcH = Math.max(1, source.getHeight((ImageObserver)null));
         int targetH = Math.max(1, (int)Math.round((double)srcH * (double)targetW / (double)srcW));
         Image scaled = source.getScaledInstance(targetW, targetH, 4);
         logoLabel.setIcon(new ImageIcon(scaled));
         logoLabel.setPreferredSize(new Dimension(targetW, targetH));
         return targetH;
      } else {
         return logoLabel.getPreferredSize().height;
      }
   }

   private static Properties loadProperties(String resourcePath) throws Exception {
      InputStream in = DesktopStartGame.class.getResourceAsStream(resourcePath);

      Properties var10;
      try {
         if (in == null) {
            throw new RuntimeException("Config not found: " + resourcePath);
         }

         Properties properties = new Properties();
         InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);

         try {
            properties.load(reader);
         } catch (Throwable var8) {
            try {
               reader.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         reader.close();
         var10 = properties;
      } catch (Throwable var9) {
         if (in != null) {
            try {
               in.close();
            } catch (Throwable var6) {
               var9.addSuppressed(var6);
            }
         }

         throw var9;
      }

      if (in != null) {
         in.close();
      }

      return var10;
   }

   private static void applyConstants(Properties properties) {
      Constants.Height = Integer.parseInt(properties.getProperty("Height"));
      Constants.Width = Integer.parseInt(properties.getProperty("Width"));
      Constants.Size = Integer.parseInt(properties.getProperty("Size"));
      Constants.Font = properties.getProperty("Font");
      Constants.MenuMusicEnabled = Boolean.parseBoolean(properties.getProperty("MenuMusicEnabled", String.valueOf(Constants.MenuMusicEnabled)));
      Constants.GameMusicEnabled = Boolean.parseBoolean(properties.getProperty("GameMusicEnabled", String.valueOf(Constants.GameMusicEnabled)));
   }

   private static void openSettings(JFrame frame, MenuMusicPlayer menuMusic) {
      MenuSettingsState currentSettings = new MenuSettingsState(Constants.Width, Constants.Height, Constants.MenuMusicEnabled, Constants.GameMusicEnabled);
      MenuSettingsState updatedSettings = MenuSettingsDialog.show(frame, currentSettings, uiTexts);
      if (updatedSettings != null) {
         MenuSettingsApplier.apply(updatedSettings, frame);
         applyMenuMusic(menuMusic);
      }
   }

   private static void applyMenuMusic(MenuMusicPlayer menuMusic) {
      if (Constants.MenuMusicEnabled) {
         menuMusic.playLoop();
      } else {
         menuMusic.stop();
      }

   }

   private static void showFrame(JFrame frame) {
      frame.setLocationRelativeTo((Component)null);
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
      frame.setDefaultCloseOperation(3);
      frame.setLayout(new BorderLayout());
      frame.setResizable(false);
      final Game g = new Game();
      frame.add(g, "Center");
      frame.pack();
      frame.setLocationRelativeTo((Component)null);
      frame.setVisible(true);
      frame.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e) {
            g.stopGame();
         }
      });
      g.requestFocusInWindow();
      g.start();
   }

   private static void showTemporaryLoadingScreen(JFrame owner, MenuMusicPlayer menuMusic) {
      JDialog loading = new JDialog(owner, introText("intro.windowTitle", "Интро"), false);
      loading.setDefaultCloseOperation(2);
      loading.setUndecorated(true);
      boolean resumeMenuMusic = Constants.MenuMusicEnabled;
      menuMusic.stop();
      SoundEffectPlayer introVoice = introBool("intro.voice.enabled", false) ? loadIntroVoice() : null;
      if (introVoice != null) {
         introVoice.play();
      }

      final String[] crawlLines = introCrawlLines();
      final int[] crawlOffset = new int[]{0};
      int scrollStep = introInt("intro.scroll.step", 2);
      int scrollDelayMs = introInt("intro.scroll.delay.ms", 33);
      List<IntroSubtitleCue> subtitleCues = introSubtitleCues();
      final String[] activeSubtitle = new String[]{subtitleCues.isEmpty() ? "" : ((IntroSubtitleCue)subtitleCues.get(0)).text()};
      int[] subtitleIndex = new int[]{0};
      final boolean subtitlesEnabled = introBool("intro.subtitles.enabled", false);
      int subtitlesOffsetMs = introInt("intro.sub.sync.offset.ms", 0);
      double subtitlesSyncSpeed = introDouble("intro.sub.sync.speed", (double)1.0F);
      long introStartedAt = System.currentTimeMillis();
      final Image[] eboboRight = loadAnimationTrack("/alkosmen/ui/intro/ebobo/walk_right");
      final Image[] eboboLeft = loadAnimationTrack("/alkosmen/ui/intro/ebobo/walk_left");
      final int eboboMargin = introInt("intro.ebobo.margin.px", 20);
      final int[] eboboX = new int[]{introInt("intro.ebobo.start.x.px", eboboMargin + 2)};
      final int[] eboboDirection = new int[]{1};
      final int[] eboboFrame = new int[]{0};
      final int[] eboboTick = new int[]{0};
      int eboboStepPx = introInt("intro.ebobo.step.px", Math.max(2, owner.getWidth() / 280));
      final double eboboHeightRatio = introDouble("intro.ebobo.height.ratio", 0.21);
      final int eboboSubtitleGap = introInt("intro.ebobo.subtitle.gap.px", 52);
      final int eboboJumpAmplitude = introInt("intro.ebobo.jump.amplitude.px", 16);
      final double eboboJumpFrequency = introDouble("intro.ebobo.jump.frequency", 0.45);
      int eboboFrameStep = Math.max(1, introInt("intro.ebobo.frame.step", 1));
      boolean eboboRandomJumpEnabled = introBool("intro.ebobo.random.enabled", true);
      int eboboRandomIntervalTicks = Math.max(1, introInt("intro.ebobo.random.interval.ticks", 9));
      int eboboRandomYRangePx = Math.max(0, introInt("intro.ebobo.random.y.range.px", 44));
      final int[] eboboRandomYOffset = new int[]{0};
      Random eboboRandom = new Random();
      final boolean eboboLaneEnabled = introBool("intro.ebobo.lane.enabled", true);
      final double eboboLaneHeightRatio = introDouble("intro.ebobo.lane.height.ratio", 0.17);
      final int eboboLaneBottomMargin = introInt("intro.ebobo.lane.bottom.margin.px", 26);
      final int eboboLaneAlpha = clamp(introInt("intro.ebobo.lane.alpha", 135), 0, 255);
      JPanel crawlPanel = new JPanel() {
         protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g.create();
            int w = this.getWidth();
            int h = this.getHeight();
            DesktopStartGame.drawIntroBackdrop(g2, w, h, eboboTick[0]);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int laneH = Math.max(1, (int)Math.round((double)h * eboboLaneHeightRatio));
            int laneYBase = h - laneH - eboboLaneBottomMargin;
            int laneY = Math.min(h - laneH, Math.max(0, laneYBase + (int)Math.round((double)laneH * (double)0.25F)));
            int startY = h + 120;
            int lineGap = 48;

            for(int i = 0; i < crawlLines.length; ++i) {
               String line = crawlLines[i];
               int y = startY - crawlOffset[0] + i * lineGap;
               if (y >= -80 && y <= h + 120) {
                  float depth = Math.max(0.0F, Math.min(1.0F, (float)y / (float)h));
                  int fontSize = (int)(16.0F + 14.0F * depth);
                  Font font = new Font("Monospaced", 1, fontSize);
                  g2.setFont(font);
                  int textW = g2.getFontMetrics().stringWidth(line);
                  int x = (w - textW) / 2;
                  g2.setColor(new Color(8, 14, 34, 220));
                  g2.drawString(line, x + 2, y + 2);
                  g2.setColor(new Color(126, 230, 255));
                  g2.drawString(line, x, y);
               }
            }

            if (eboboLaneEnabled) {
               DesktopStartGame.drawEboboLane(g2, w, laneY, laneH, eboboLaneAlpha);
            }

            if (subtitlesEnabled && !activeSubtitle[0].isBlank()) {
               int boxH = 44;
               int boxW = Math.min(w - 36, (int)Math.round((double)w * 0.72));
               int boxX = (w - boxW) / 2;
               int boxY = eboboLaneEnabled ? Math.max(10, laneY + laneH - boxH - 10) : h - boxH - 18;
               g2.setColor(new Color(0, 0, 0, 188));
               g2.fillRoundRect(boxX, boxY, boxW, boxH, 14, 14);
               g2.setColor(new Color(255, 216, 82, 150));
               g2.drawRoundRect(boxX, boxY, boxW, boxH, 14, 14);
               g2.setFont(new Font("Dialog", 1, 20));
               int subW = g2.getFontMetrics().stringWidth(activeSubtitle[0]);
               int subX = Math.max(boxX + 12, boxX + (boxW - subW) / 2);
               int subY = boxY + 29;
               g2.setColor(new Color(0, 0, 0, 220));
               g2.drawString(activeSubtitle[0], subX + 1, subY + 1);
               g2.setColor(new Color(255, 233, 160));
               g2.drawString(activeSubtitle[0], subX, subY);
            }

            DesktopStartGame.drawIntroEbobo(g2, w, h, eboboRight, eboboLeft, eboboFrame[0], eboboDirection[0], eboboX[0], eboboTick[0], eboboHeightRatio, eboboSubtitleGap, eboboJumpAmplitude, eboboJumpFrequency, eboboMargin, eboboRandomYOffset[0], eboboLaneEnabled, laneY, laneH);
            g2.dispose();
         }
      };
      loading.setContentPane(crawlPanel);
      loading.setSize(owner.getWidth(), owner.getHeight());
      loading.setLocationRelativeTo(owner);
      Timer introTimer = new Timer(scrollDelayMs, (e) -> {
         crawlOffset[0] += scrollStep;
         if (subtitlesEnabled && !subtitleCues.isEmpty()) {
            long elapsedRaw = System.currentTimeMillis() - introStartedAt;

            int var10002;
            for(long elapsed = Math.max(0L, Math.round((double)elapsedRaw * subtitlesSyncSpeed) + (long)subtitlesOffsetMs); subtitleIndex[0] + 1 < subtitleCues.size() && elapsed >= (long)((IntroSubtitleCue)subtitleCues.get(subtitleIndex[0] + 1)).timeMs(); var10002 = subtitleIndex[0]++) {
            }

            activeSubtitle[0] = ((IntroSubtitleCue)subtitleCues.get(subtitleIndex[0])).text();
         }

         int trackSize = Math.max(trackLength(eboboRight), trackLength(eboboLeft));
         if (trackSize > 0) {
            if (eboboTick[0] % eboboFrameStep == 0) {
               eboboFrame[0] = (eboboFrame[0] + 1) % trackSize;
            }

            int step = Math.max(1, eboboStepPx);
            int eboboH = Math.max(1, (int)Math.round((double)owner.getHeight() * eboboHeightRatio));
            Image probe = frameAt(eboboDirection[0] > 0 ? eboboRight : eboboLeft, eboboFrame[0]);
            if (probe != null) {
               int srcW = Math.max(1, probe.getWidth((ImageObserver)null));
               int srcH = Math.max(1, probe.getHeight((ImageObserver)null));
               int eboboW = Math.max(1, (int)Math.round((double)srcW * (double)eboboH / (double)srcH));
               int maxX = Math.max(eboboMargin, owner.getWidth() - eboboW - eboboMargin);
               if (eboboRandomJumpEnabled && eboboTick[0] % eboboRandomIntervalTicks == 0) {
                  int span = Math.max(1, maxX - eboboMargin + 1);
                  int randomX = eboboMargin + eboboRandom.nextInt(span);
                  eboboDirection[0] = randomX >= eboboX[0] ? 1 : -1;
                  eboboX[0] = randomX;
                  int ySpan = eboboRandomYRangePx * 2 + 1;
                  eboboRandomYOffset[0] = ySpan > 1 ? eboboRandom.nextInt(ySpan) - eboboRandomYRangePx : 0;
               } else {
                  eboboX[0] += step * eboboDirection[0];
                  if (eboboX[0] <= eboboMargin) {
                     eboboX[0] = eboboMargin;
                     eboboDirection[0] = 1;
                  } else if (eboboX[0] >= maxX) {
                     eboboX[0] = maxX;
                     eboboDirection[0] = -1;
                  }
               }
            }

            int var52 = eboboTick[0]++;
         }

         crawlPanel.repaint();
         int startY = crawlPanel.getHeight() + 120;
         int lineGap = 48;
         int lastLineY = startY - crawlOffset[0] + (crawlLines.length - 1) * lineGap;
         if (lastLineY < -100) {
            ((Timer)e.getSource()).stop();
            if (introVoice != null) {
               introVoice.stop();
            }

            if (loading.isDisplayable()) {
               loading.dispose();
            }

            if (resumeMenuMusic) {
               applyMenuMusic(menuMusic);
            }
         }

      });
      final Runnable skipIntro = () -> {
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
            applyMenuMusic(menuMusic);
         }

      };
      loading.addMouseListener(new MouseAdapter() {
         public void mousePressed(MouseEvent e) {
            skipIntro.run();
         }
      });
      crawlPanel.addMouseListener(new MouseAdapter() {
         public void mousePressed(MouseEvent e) {
            skipIntro.run();
         }
      });
      loading.addKeyListener(new KeyAdapter() {
         public void keyPressed(KeyEvent e) {
            skipIntro.run();
         }
      });
      introTimer.start();
      loading.setVisible(true);
      loading.requestFocus();
   }

   private static List introSubtitleCues() {
      List<IntroSubtitleCue> cues = new ArrayList();

      for(int i = 1; i <= 99; ++i) {
         String timeRaw = introTexts.getProperty("intro.sub." + i + ".timeMs");
         String text = introTexts.getProperty("intro.sub." + i + ".text");
         if ((timeRaw != null || text != null) && timeRaw != null) {
            try {
               int timeMs = Integer.parseInt(timeRaw.trim());
               cues.add(new IntroSubtitleCue(timeMs, text == null ? "" : text));
            } catch (NumberFormatException var5) {
            }
         }
      }

      cues.sort(Comparator.comparingInt(IntroSubtitleCue::timeMs));
      return cues;
   }

   private static SoundEffectPlayer loadIntroVoice() {
      String[] candidates = new String[]{"/alkosmen/sounds/raw/intro.wav", "/alkosmen/sounds/raw/intro.mp3", "/alkosmen/sounds/raw/audio_2026-03-09_19-29-48.ogg"};

      for(String path : candidates) {
         try {
            return new SoundEffectPlayer(path);
         } catch (RuntimeException var6) {
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
      } else {
         try {
            return Integer.parseInt(raw.trim());
         } catch (NumberFormatException var4) {
            return fallback;
         }
      }
   }

   private static double introDouble(String key, double fallback) {
      String raw = introTexts.getProperty(key);
      if (raw == null) {
         return fallback;
      } else {
         try {
            return Double.parseDouble(raw.trim());
         } catch (NumberFormatException var5) {
            return fallback;
         }
      }
   }

   private static boolean introBool(String key, boolean fallback) {
      String raw = introTexts.getProperty(key);
      return raw == null ? fallback : Boolean.parseBoolean(raw.trim());
   }

   private static String[] introCrawlLines() {
      List<String> keys = new ArrayList();

      for(String key : introTexts.stringPropertyNames()) {
         if (key.startsWith("intro.crawl.")) {
            keys.add(key);
         }
      }

      if (keys.isEmpty()) {
         return new String[]{"ЭПИЗОД I", "АЛКОСМЕН: НОЧНОЙ ЗАХОД", "", "Ночной город не спит.", "Фонари трещат, район шумит.", "Менты уже на патруле.", "", "Первый уровень", "временно отключен."};
      } else {
         keys.sort(Comparator.comparingInt((k) -> {
            String suffix = k.substring("intro.crawl.".length());

            try {
               return Integer.parseInt(suffix);
            } catch (NumberFormatException var3) {
               return Integer.MAX_VALUE;
            }
         }));
         List<String> lines = new ArrayList(keys.size());

         for(String key : keys) {
            lines.add(introTexts.getProperty(key, ""));
         }

         return (String[])lines.toArray(new String[0]);
      }
   }

   private static Image loadImage(String path) {
      URL url = DesktopStartGame.class.getResource(path);
      return url == null ? null : (new ImageIcon(url)).getImage();
   }

   private static void drawIntroEbobo(Graphics2D g2, int w, int h, Image[] rightTrack, Image[] leftTrack, int frame, int direction, int x, int tick, double heightRatio, int subtitleGap, int jumpAmplitude, double jumpFrequency, int margin, int randomYOffset, boolean laneEnabled, int laneY, int laneH) {
      Image sprite = frameAt(direction > 0 ? rightTrack : leftTrack, frame);
      if (sprite != null) {
         int srcW = Math.max(1, sprite.getWidth((ImageObserver)null));
         int srcH = Math.max(1, sprite.getHeight((ImageObserver)null));
         int targetH = Math.max(1, (int)Math.round((double)h * heightRatio));
         int targetW = Math.max(1, (int)Math.round((double)srcW * (double)targetH / (double)srcH));
         int jumpOffset = (int)Math.round(Math.abs(Math.sin((double)tick * jumpFrequency)) * (double)jumpAmplitude);
         int y;
         if (laneEnabled) {
            y = laneY + laneH - targetH - 6 - jumpOffset - randomYOffset;
         } else {
            y = h - targetH - subtitleGap - jumpOffset - randomYOffset;
         }

         int clampedX = Math.max(margin, Math.min(x, Math.max(margin, w - targetW - margin)));
         g2.setComposite(AlphaComposite.SrcOver);
         g2.setColor(new Color(66, 216, 255, 82));
         g2.fillOval(clampedX - targetW / 5, y - targetH / 6, targetW + targetW / 3, targetH + targetH / 3);
         g2.setColor(new Color(255, 82, 214, 72));
         g2.fillOval(clampedX - targetW / 7, y - targetH / 8, targetW + targetW / 4, targetH + targetH / 4);
         g2.drawImage(sprite, clampedX, y, targetW, targetH, (ImageObserver)null);
         int glowY = y + targetH - 4;
         g2.setColor(new Color(76, 222, 255, 120));
         g2.fillRoundRect(clampedX + targetW / 7, glowY, targetW - targetW / 4, 4, 4, 4);
         g2.setColor(new Color(255, 94, 226, 92));
         g2.fillRoundRect(clampedX + targetW / 5, glowY + 2, targetW - targetW / 3, 3, 3, 3);
      }
   }

   private static void drawEboboLane(Graphics2D g2, int w, int laneY, int laneH, int alpha) {
      GradientPaint lanePaint = new GradientPaint(0.0F, (float)laneY, new Color(12, 12, 18, alpha), 0.0F, (float)(laneY + laneH), new Color(6, 6, 10, Math.min(255, alpha + 30)));
      g2.setPaint(lanePaint);
      g2.fillRoundRect(0, laneY, w, laneH, 18, 18);
      g2.setColor(new Color(255, 216, 82, Math.min(255, alpha + 45)));
      g2.drawLine(0, laneY + 1, w, laneY + 1);
      g2.setColor(new Color(0, 0, 0, Math.min(255, alpha + 40)));
      g2.drawLine(0, laneY + laneH - 1, w, laneY + laneH - 1);
   }

   private static void drawIntroBackdrop(Graphics2D g2, int w, int h, int tick) {
      GradientPaint bg = new GradientPaint(0.0F, 0.0F, new Color(8, 12, 28), 0.0F, (float)h, new Color(16, 24, 42));
      g2.setPaint(bg);
      g2.fillRect(0, 0, w, h);
      int cell = 30;
      g2.setColor(new Color(76, 120, 170, 46));

      for(int x = 0; x <= w; x += cell) {
         g2.drawLine(x, 0, x, h);
      }

      for(int y = 0; y <= h; y += cell) {
         g2.drawLine(0, y, w, y);
      }

      Color[] palette = new Color[]{new Color(0, 240, 245, 170), new Color(246, 215, 80, 170), new Color(212, 104, 255, 170), new Color(88, 255, 132, 170), new Color(255, 120, 96, 170)};

      for(int i = 0; i < 8; ++i) {
         int type = i % 5;
         int laneX = (int)Math.round((double)(i + 1) * ((double)w / (double)9.0F));
         int speed = 5 + i % 4 * 2;
         int y = (tick * speed + i * 140) % (h + cell * 8) - cell * 4;
         drawTetrisPiece(g2, laneX, y, cell - 3, palette[type], type);
      }

   }

   private static void drawTetrisPiece(Graphics2D g2, int x, int y, int cell, Color color, int type) {
      int[][] offsets;
      switch (type) {
         case 0 -> offsets = new int[][]{{0, 0}, {1, 0}, {-1, 0}, {2, 0}};
         case 1 -> offsets = new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}};
         case 2 -> offsets = new int[][]{{0, 0}, {-1, 0}, {1, 0}, {0, 1}};
         case 3 -> offsets = new int[][]{{0, 0}, {1, 0}, {0, 1}, {-1, 1}};
         default -> offsets = new int[][]{{0, 0}, {-1, 0}, {0, 1}, {1, 1}};
      }

      for(int[] d : offsets) {
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
      List<Image> track = new ArrayList();

      for(int i = 0; i < 100; ++i) {
         Image frame = loadImage(folderPath + "/" + String.format("%02d", i) + ".png");
         if (frame == null) {
            break;
         }

         track.add(frame);
      }

      return (Image[])track.toArray(new Image[0]);
   }

   private static Image frameAt(Image[] track, int index) {
      if (track != null && track.length != 0) {
         int safeIndex = Math.floorMod(index, track.length);
         return track[safeIndex];
      } else {
         return null;
      }
   }

   private static int trackLength(Image[] track) {
      return track == null ? 0 : track.length;
   }

   private static record IntroSubtitleCue(int timeMs, String text) {
   }
}
