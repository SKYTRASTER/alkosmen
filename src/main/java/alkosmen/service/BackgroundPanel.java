package alkosmen.service;

import alkosmen.audio.MenuMusicPlayer;
import alkosmen.gfx.CharacterSpriteAssets;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

public final class BackgroundPanel extends JPanel {
   public static final String BACKGROUND_RESOURCE = "/alkosmen/ui/menu/town_square_dance_bg_v1.png";

   private static final String DANCE_FRAME_TEMPLATE = "/alkosmen/ui/menu/dance/%02d.png";
   private static final String FALLBACK_IDLE_RESOURCE = "/alkosmen/ui/menu/alkosmen_idle_menu_v2.png";
   private static final String FALLBACK_WALK_RESOURCE = "/alkosmen/ui/sprites/alkosmen/walk_atlas_v1.png";

   private static final int FRAME_COUNT = 8;
   private static final int[] DANCE_SEQUENCE = {
      0, 1, 2, 3, 4, 5, 6, 7,
      6, 5, 4, 3, 2, 1
   };

   private static final int REPAINT_INTERVAL_MS = 16;
   private static final double DEFAULT_DANCE_FPS = 9.0;

   /*
    * Dance speed timeline for each menu track.
    *
    * The first number is the song position in milliseconds,
    * the second is the animation speed in sprite frames per second.
    *
    * Add/change cues here when the final musical accents are known.
    */
   private static final SpeedCue[][] TRACK_SPEED_CUES = {
      {
         new SpeedCue(0L, 7.0),
         new SpeedCue(15_000L, 9.0),
         new SpeedCue(30_000L, 13.0),
         new SpeedCue(50_000L, 8.0),
         new SpeedCue(65_000L, 11.0)
      },
      {
         new SpeedCue(0L, 8.0),
         new SpeedCue(18_000L, 10.0),
         new SpeedCue(36_000L, 14.0),
         new SpeedCue(56_000L, 8.5),
         new SpeedCue(72_000L, 11.0)
      }
   };

   private final MenuMusicPlayer menuMusic;
   private final BufferedImage background;
   private final BufferedImage[] danceFrames;
   private final Timer animationTimer;

   private double dancePosition;
   private long lastUpdateNanos;
   private long lastMusicPositionMs = -1L;
   private int lastTrackIndex = -1;

   public BackgroundPanel(MenuMusicPlayer menuMusic) {
      this.menuMusic = menuMusic;

      URL backgroundResource = BackgroundPanel.class.getResource(BACKGROUND_RESOURCE);
      if (backgroundResource == null) {
         throw new IllegalStateException("Menu background not found: " + BACKGROUND_RESOURCE);
      }

      try {
         this.background = ImageIO.read(backgroundResource);
         this.danceFrames = loadDanceFrames();
      } catch (IOException error) {
         throw new IllegalStateException("Could not load menu art", error);
      }

      this.animationTimer = new Timer(REPAINT_INTERVAL_MS, event -> {
         updateDancePosition();
         repaint();
      });
      this.animationTimer.setCoalesce(true);
   }

   public BackgroundPanel() {
      this(null);
   }

   private static BufferedImage[] loadDanceFrames() throws IOException {
      BufferedImage[] frames = new BufferedImage[FRAME_COUNT];
      boolean completeDance = true;

      for (int i = 0; i < FRAME_COUNT; ++i) {
         String path = String.format(DANCE_FRAME_TEMPLATE, i);
         URL resource = BackgroundPanel.class.getResource(path);
         if (resource == null) {
            completeDance = false;
            break;
         }

         frames[i] = ImageIO.read(resource);
         if (frames[i] == null) {
            completeDance = false;
            break;
         }
      }

      if (completeDance) {
         return frames;
      }

      URL idleResource = BackgroundPanel.class.getResource(FALLBACK_IDLE_RESOURCE);
      if (idleResource != null) {
         return CharacterSpriteAssets.loadGridAtlas(FALLBACK_IDLE_RESOURCE, 8, 1)[0];
      }

      return CharacterSpriteAssets.loadGridAtlas(FALLBACK_WALK_RESOURCE, 4, 5, 30)[3];
   }

   @Override
   public void addNotify() {
      super.addNotify();
      this.lastUpdateNanos = System.nanoTime();
      this.lastMusicPositionMs = -1L;
      this.lastTrackIndex = -1;
      this.animationTimer.start();
   }

   @Override
   public void removeNotify() {
      this.animationTimer.stop();
      super.removeNotify();
   }

   private void updateDancePosition() {
      long nowNanos = System.nanoTime();
      if (this.lastUpdateNanos == 0L) {
         this.lastUpdateNanos = nowNanos;
         return;
      }

      double deltaSeconds = Math.min(0.1, Math.max(0.0, (nowNanos - this.lastUpdateNanos) / 1_000_000_000.0));
      this.lastUpdateNanos = nowNanos;

      int trackIndex = this.menuMusic == null ? -1 : this.menuMusic.getCurrentTrackIndex();
      long songPositionMs = this.menuMusic == null ? -1L : this.menuMusic.getPlaybackPositionMs();

      if (trackIndex >= 0 && songPositionMs >= 0L) {
         boolean trackChanged = trackIndex != this.lastTrackIndex;
         boolean songRestarted = this.lastMusicPositionMs >= 0L && songPositionMs + 250L < this.lastMusicPositionMs;

         if (trackChanged || songRestarted) {
            this.dancePosition = 0.0;
         }

         this.lastTrackIndex = trackIndex;
         this.lastMusicPositionMs = songPositionMs;
         this.dancePosition += deltaSeconds * danceFps(trackIndex, songPositionMs);
      } else {
         this.dancePosition += deltaSeconds * DEFAULT_DANCE_FPS;
      }
   }

   private static double danceFps(int trackIndex, long positionMs) {
      if (trackIndex < 0 || trackIndex >= TRACK_SPEED_CUES.length) {
         return DEFAULT_DANCE_FPS;
      }

      SpeedCue[] cues = TRACK_SPEED_CUES[trackIndex];
      double speed = cues[0].framesPerSecond();

      for (SpeedCue cue : cues) {
         if (positionMs < cue.startMs()) {
            break;
         }
         speed = cue.framesPerSecond();
      }

      return speed;
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);

      Graphics2D g = (Graphics2D)graphics.create();
      try {
         drawBackground(g);
         drawDancer(g);
      } finally {
         g.dispose();
      }
   }

   private void drawBackground(Graphics2D g) {
      g.setColor(new Color(8, 12, 22));
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

      double scale = Math.max(
         (double)this.getWidth() / (double)this.background.getWidth(),
         (double)this.getHeight() / (double)this.background.getHeight()
      );

      int width = (int)Math.ceil(this.background.getWidth() * scale);
      int height = (int)Math.ceil(this.background.getHeight() * scale);
      int x = (this.getWidth() - width) / 2;
      int y = (this.getHeight() - height) / 2;

      g.drawImage(this.background, x, y, width, height, this);
   }

   private void drawDancer(Graphics2D g) {
      int sequenceIndex = Math.floorMod((int)Math.floor(this.dancePosition), DANCE_SEQUENCE.length);
      int frameIndex = DANCE_SEQUENCE[sequenceIndex] % this.danceFrames.length;
      BufferedImage pose = this.danceFrames[frameIndex];

      double phase = this.dancePosition / DANCE_SEQUENCE.length * Math.PI * 2.0;
      double sway = Math.sin(phase) * 2.5;
      double bob = Math.abs(Math.sin(phase)) * 2.0;
      double tilt = Math.sin(phase) * 0.012;

      int targetHeight = (int)Math.round(this.getHeight() * 0.43);
      int targetWidth = (int)Math.round(targetHeight * pose.getWidth() / (double)pose.getHeight());

      Graphics2D dancer = (Graphics2D)g.create();
      try {
         dancer.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
         dancer.translate(this.getWidth() * 0.65 + sway, this.getHeight() * 0.91 - bob);
         dancer.rotate(tilt);
         dancer.drawImage(pose, -targetWidth / 2, -targetHeight, targetWidth, targetHeight, this);
      } finally {
         dancer.dispose();
      }
   }

   private record SpeedCue(long startMs, double framesPerSecond) {
   }
}
