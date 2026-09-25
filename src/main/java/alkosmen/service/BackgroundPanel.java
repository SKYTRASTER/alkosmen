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
      double anchorY = this.getHeight() * 0.655 - bob;

      drawGroundShadow(g, anchorX, anchorY, targetWidth);

      Graphics2D dancer = (Graphics2D)g.create();
      dancer.setRenderingHint(
         RenderingHints.KEY_INTERPOLATION,
         RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
      );
      dancer.translate(anchorX, anchorY);
      dancer.rotate(tilt);
      dancer.drawImage(pose, -targetWidth / 2, -targetHeight, targetWidth, targetHeight, this);
      dancer.dispose();
   }

   private void drawEboboDance(Graphics2D g) {
      int frame = (this.danceFrame + 5) % DANCE_FRAME_COUNT;
      BufferedImage ebobo = this.eboboDanceFrames[frame];

      double phase = Math.PI * 2.0 * frame / DANCE_FRAME_COUNT;
      int targetHeight = (int)Math.round(this.getHeight() * 0.185);
      int targetWidth = (int)Math.round(targetHeight * ebobo.getWidth() / (double)ebobo.getHeight());

      int sway = (int)Math.round(Math.sin(phase * 1.5) * 8.0);
      int bob = (int)Math.round(Math.abs(Math.sin(phase * 2.5)) * 9.0);
      double tilt = Math.sin(phase * 1.5) * 0.045;

      double anchorX = this.getWidth() * 0.745 + sway;
      double anchorY = this.getHeight() * 0.645 - bob;

      drawGroundShadow(g, anchorX, anchorY, targetWidth);

      Graphics2D dancer = (Graphics2D)g.create();
      dancer.setRenderingHint(
         RenderingHints.KEY_INTERPOLATION,
         RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
      );
      dancer.translate(anchorX, anchorY);
      dancer.rotate(tilt);
      dancer.drawImage(ebobo, -targetWidth / 2, -targetHeight, targetWidth, targetHeight, this);
      dancer.dispose();
   }

   private void drawGroundShadow(Graphics2D g, double x, double y, int dancerWidth) {
      Graphics2D shadow = (Graphics2D)g.create();
      int shadowWidth = Math.max(18, (int)Math.round(dancerWidth * 0.58));
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
