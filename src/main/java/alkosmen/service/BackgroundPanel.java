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
    * Menu dance must stay front-facing. The fourth atlas row is the front
    * animation, so never mix in side/back walking rows here.
    */
   private static final int[] ALKOSMEN_SEQUENCE = {
      12, 13, 14, 15, 14, 13,
      12, 13, 14, 15, 14, 13,
      12, 13, 14, 15, 14, 13,
      12, 13, 14, 15, 14, 13
   };


   /*
    * Ebobo stays on one ground point. The dance is driven by pose changes,
    * hops and mirrored lean frames, not by sliding the whole sprite left-right.
    */
   private static final int[] EBOBO_SEQUENCE = {
      8, 9, 10, 11,
      12, 13, 14, 13,
      12, 11, 10, 9,
      8, 9, 10, 11,
      12, 13, 14, 13,
      12, 10, 9, 8
   };

   private static final int[] EBOBO_JUMP_OFFSETS = {
      0, 3, 8, 14,
      8, 3, 0, 4,
      10, 16, 10, 4,
      0, 4, 10, 16,
      10, 4, 0, 3,
      8, 5, 2, 0
   };

   private static final boolean[] EBOBO_MIRROR = {
      false, false, false, false,
      false, false, false, true,
      true, true, true, true,
      false, false, false, false,
      true, true, true, false,
      false, true, false, false
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
      double anchorX = this.getWidth() * 0.745;
      double groundY = this.getHeight() * EBOBO_GROUND_Y_RATIO;
      double baselineLift = this.getHeight() * EBOBO_BASELINE_LIFT_RATIO;
      double spriteY = groundY - baselineLift - EBOBO_JUMP_OFFSETS[beat] * motionScale;

      double jumpRatio = EBOBO_JUMP_OFFSETS[beat] / 16.0;
      drawGroundShadow(g, anchorX, groundY, targetWidth, 1.0 - jumpRatio * 0.10);

      Graphics2D dancer = (Graphics2D)g.create();
      dancer.setRenderingHint(
         RenderingHints.KEY_INTERPOLATION,
         RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
      );
      dancer.translate(anchorX, spriteY);
      if (EBOBO_MIRROR[beat]) {
         dancer.scale(-1.0, 1.0);
      }
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
