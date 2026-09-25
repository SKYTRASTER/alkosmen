package alkosmen.service;

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
   private static final String WALK_RESOURCE = "/alkosmen/ui/sprites/alkosmen/walk_atlas_v1.png";
   private static final String EBOBO_DANCE_RESOURCE = "/alkosmen/ui/menu/ebobo_dance24";

   private static final int DANCE_FRAME_COUNT = 24;
   private static final int DANCE_FRAME_MS = 105;

   private static final double ALKOSMEN_GROUND_Y_RATIO = 0.655;
   private static final double EBOBO_GROUND_Y_RATIO = 0.645;
   private static final double ALKOSMEN_BASELINE_LIFT_RATIO = 0.018;
   private static final double EBOBO_BASELINE_LIFT_RATIO = 0.010;

   /*
    * 4 x 5 atlas = 20 real frames. The sequence deliberately walks through
    * several rows so the menu dance visibly changes pose and facing instead
    * of recycling the same four images.
    */
   private static final int[] ALKOSMEN_SEQUENCE = {
      12, 13, 14, 15,
      8, 9, 10, 11,
      16, 17, 18, 19,
      4, 5, 6, 7,
      0, 1, 2, 3,
      12, 14, 13, 15
   };


   /*
    * 24-beat dance choreography for Ebobo.
    * Frames come from idle / walk / laugh / attack / hurt assets, but the
    * position is intentionally kept near one spot so he dances instead of
    * sliding left-right like a pendulum.
    */
   private static final int[] EBOBO_SEQUENCE = {
      0, 1, 2, 3,
      4, 5, 6, 7,
      8, 9, 10, 11,
      16, 17, 18, 19,
      12, 13, 14, 15,
      8, 10, 9, 0
   };

   private static final int[] EBOBO_X_OFFSETS = {
      0, -2, -5, -8,
      -5, -2, 0, 3,
      6, 3, 0, -3,
      -6, -3, 0, 4,
      8, 4, 0, -4,
      -7, -3, 0, 0
   };

   private static final int[] EBOBO_JUMP_OFFSETS = {
      0, 0, 4, 12,
      22, 12, 4, 0,
      0, 6, 16, 28,
      16, 6, 0, 0,
      5, 14, 24, 14,
      6, 2, 0, 0
   };

   private static final int[] EBOBO_TILT_DEGREES = {
      0, -5, -10, -4,
      6, 12, 5, -2,
      -8, -3, 7, 14,
      6, -4, -12, -5,
      4, 10, 4, -7,
      -12, -5, 2, 0
   };

   private static final double[] EBOBO_SCALE_X = {
      1.00, 1.03, 1.08, 1.12,
      1.08, 1.03, 0.98, 0.94,
      0.90, 0.96, 1.04, 1.12,
      1.05, 0.98, 0.92, 0.96,
      1.04, 1.12, 1.06, 0.98,
      0.92, 0.97, 1.02, 1.00
   };

   private static final double[] EBOBO_SCALE_Y = {
      1.00, 0.96, 0.90, 0.84,
      1.06, 1.15, 1.08, 0.98,
      0.88, 0.96, 1.08, 1.18,
      1.10, 0.98, 0.88, 0.96,
      1.04, 1.14, 1.08, 0.96,
      0.90, 0.96, 1.03, 1.00
   };

   private final BufferedImage background;
   private final BufferedImage[] danceFrames;
   private final BufferedImage[] eboboDanceFrames;
   private final Timer danceTimer;
   private int danceFrame;

   public BackgroundPanel() {
      URL resource = BackgroundPanel.class.getResource(BACKGROUND_RESOURCE);
      if (resource == null) {
         throw new IllegalStateException("Menu background not found: " + BACKGROUND_RESOURCE);
      }

      try {
         this.background = ImageIO.read(resource);
         this.danceFrames = flattenAtlas(CharacterSpriteAssets.loadGridAtlas(WALK_RESOURCE, 4, 5, 30));
         this.eboboDanceFrames = loadFrames(EBOBO_DANCE_RESOURCE, DANCE_FRAME_COUNT);
      } catch (IOException error) {
         throw new IllegalStateException("Could not load menu art", error);
      }

      this.danceTimer = new Timer(DANCE_FRAME_MS, event -> {
         this.danceFrame = (this.danceFrame + 1) % DANCE_FRAME_COUNT;
         repaint();
      });
   }

   @Override
   public void addNotify() {
      super.addNotify();
      danceTimer.start();
   }

   @Override
   public void removeNotify() {
      danceTimer.stop();
      super.removeNotify();
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      Graphics2D g = (Graphics2D)graphics.create();
      g.setColor(new Color(8, 12, 22));
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

      double scale = Math.max(
         (double)this.getWidth() / (double)this.background.getWidth(),
         (double)this.getHeight() / (double)this.background.getHeight()
      );
      int width = (int)Math.ceil((double)this.background.getWidth() * scale);
      int height = (int)Math.ceil((double)this.background.getHeight() * scale);
      g.drawImage(
         this.background,
         (this.getWidth() - width) / 2,
         (this.getHeight() - height) / 2,
         width,
         height,
         this
      );

      drawAlkosmenDance(g);
      drawEboboDance(g);
      g.dispose();
   }

   private void drawAlkosmenDance(Graphics2D g) {
      int frame = this.danceFrame;
      BufferedImage pose = this.danceFrames[ALKOSMEN_SEQUENCE[frame]];

      double phase = Math.PI * 2.0 * frame / DANCE_FRAME_COUNT;
      int targetHeight = (int)Math.round(this.getHeight() * 0.225);
      int targetWidth = (int)Math.round(targetHeight * pose.getWidth() / (double)pose.getHeight());

      int sway = (int)Math.round(Math.sin(phase) * 8.0 + Math.sin(phase * 2.0) * 3.0);
      int bob = (int)Math.round(Math.abs(Math.sin(phase * 2.0)) * 6.0);
      double tilt = Math.sin(phase) * 0.028;

      double anchorX = this.getWidth() * 0.605 + sway;
      double groundY = this.getHeight() * ALKOSMEN_GROUND_Y_RATIO;
      double baselineLift = this.getHeight() * ALKOSMEN_BASELINE_LIFT_RATIO;
      double spriteY = groundY - baselineLift - bob;

      drawGroundShadow(g, anchorX, groundY, targetWidth);

      Graphics2D dancer = (Graphics2D)g.create();
      dancer.setRenderingHint(
         RenderingHints.KEY_INTERPOLATION,
         RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
      );
      dancer.translate(anchorX, spriteY);
      dancer.rotate(tilt);
      dancer.drawImage(pose, -targetWidth / 2, -targetHeight, targetWidth, targetHeight, this);
      dancer.dispose();
   }

   private void drawEboboDance(Graphics2D g) {
      int beat = (this.danceFrame + 5) % DANCE_FRAME_COUNT;
      BufferedImage ebobo = this.eboboDanceFrames[EBOBO_SEQUENCE[beat]];

      int targetHeight = (int)Math.round(this.getHeight() * 0.185);
      int targetWidth = (int)Math.round(targetHeight * ebobo.getWidth() / (double)ebobo.getHeight());

      double motionScale = Math.max(0.75, this.getHeight() / 640.0);
      double anchorX = this.getWidth() * 0.745 + EBOBO_X_OFFSETS[beat] * motionScale;
      double groundY = this.getHeight() * EBOBO_GROUND_Y_RATIO;
      double baselineLift = this.getHeight() * EBOBO_BASELINE_LIFT_RATIO;
      double spriteY = groundY - baselineLift - EBOBO_JUMP_OFFSETS[beat] * motionScale;
      double tilt = Math.toRadians(EBOBO_TILT_DEGREES[beat]);
      double scaleX = EBOBO_SCALE_X[beat];
      double scaleY = EBOBO_SCALE_Y[beat];

      double jumpRatio = EBOBO_JUMP_OFFSETS[beat] / 28.0;
      drawGroundShadow(g, anchorX, groundY, targetWidth, 1.0 - jumpRatio * 0.30);

      Graphics2D dancer = (Graphics2D)g.create();
      dancer.setRenderingHint(
         RenderingHints.KEY_INTERPOLATION,
         RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
      );
      dancer.translate(anchorX, spriteY);
      dancer.rotate(tilt);
      dancer.scale(scaleX, scaleY);
      dancer.drawImage(ebobo, -targetWidth / 2, -targetHeight, targetWidth, targetHeight, this);
      dancer.dispose();
   }

   private void drawGroundShadow(Graphics2D g, double x, double y, int dancerWidth) {
      drawGroundShadow(g, x, y, dancerWidth, 1.0);
   }

   private void drawGroundShadow(Graphics2D g, double x, double y, int dancerWidth, double scale) {
      Graphics2D shadow = (Graphics2D)g.create();
      int shadowWidth = Math.max(18, (int)Math.round(dancerWidth * 0.58 * scale));
      int shadowHeight = Math.max(4, shadowWidth / 8);
      shadow.setColor(new Color(0, 0, 0, 72));
      shadow.fillOval(
         (int)Math.round(x - shadowWidth / 2.0),
         (int)Math.round(y - shadowHeight / 2.0),
         shadowWidth,
         shadowHeight
      );
      shadow.dispose();
   }

   private static BufferedImage[] flattenAtlas(BufferedImage[][] atlas) {
      int count = 0;
      for (BufferedImage[] row : atlas) {
         count += row.length;
      }

      BufferedImage[] result = new BufferedImage[count];
      int index = 0;
      for (BufferedImage[] row : atlas) {
         for (BufferedImage frame : row) {
            result[index++] = frame;
         }
      }
      return result;
   }

   private static BufferedImage[] loadFrames(String directory, int count) throws IOException {
      BufferedImage[] frames = new BufferedImage[count];
      for (int i = 0; i < count; ++i) {
         String path = directory + "/" + String.format("%02d.png", i);
         URL resource = BackgroundPanel.class.getResource(path);
         if (resource == null) {
            throw new IOException("Animation frame not found: " + path);
         }
         frames[i] = ImageIO.read(resource);
      }
      return frames;
   }
}
