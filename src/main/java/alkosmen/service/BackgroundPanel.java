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
   private static final String EBOBO_DANCE_RESOURCE = "/alkosmen/ui/intro/ebobo/laugh";
   private static final int[] DANCE_POSES = {0, 1, 2, 3, 2, 1, 0, 3};
   private static final int[] DANCE_SWAY = {-12, -5, 5, 12, 5, -5, -12, 5};
   private static final int[] DANCE_BOB = {0, 9, 14, 5, 0, 9, 14, 5};
   private static final int[] EBOBO_BOB = {5, 1, 6, 2, 5, 1, 6, 2};
   private static final int[] EBOBO_SWAY = {3, 0, -3, 0, 3, 0, -3, 0};
   private static final double[] DANCE_TILT = {-0.07, -0.03, 0.03, 0.07, 0.03, -0.03, -0.07, 0.03};
   private static final int DANCE_FRAME_MS = 250;
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
         this.danceFrames = CharacterSpriteAssets.loadGridAtlas(WALK_RESOURCE, 4, 5, 30)[3];
         this.eboboDanceFrames = loadFrames(EBOBO_DANCE_RESOURCE, 4);
      } catch (IOException error) {
         throw new IllegalStateException("Could not load menu art", error);
      }
      this.danceTimer = new Timer(DANCE_FRAME_MS, event -> {
         this.danceFrame = (this.danceFrame + 1) % DANCE_POSES.length;
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
      double scale = Math.max((double)this.getWidth() / (double)this.background.getWidth(), (double)this.getHeight() / (double)this.background.getHeight());
      int width = (int)Math.ceil((double)this.background.getWidth() * scale);
      int height = (int)Math.ceil((double)this.background.getHeight() * scale);
      g.drawImage(this.background, (this.getWidth() - width) / 2, (this.getHeight() - height) / 2, width, height, this);

      BufferedImage pose = this.danceFrames[DANCE_POSES[this.danceFrame]];
      int targetHeight = (int)Math.round(this.getHeight() * 0.235);
      int targetWidth = (int)Math.round(targetHeight * pose.getWidth() / (double)pose.getHeight());
      Graphics2D dancer = (Graphics2D)g.create();
      dancer.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      dancer.translate(this.getWidth() * 0.655 + DANCE_SWAY[this.danceFrame],
         this.getHeight() * 0.705 - DANCE_BOB[this.danceFrame]);
      dancer.rotate(DANCE_TILT[this.danceFrame]);
      dancer.drawImage(pose, -targetWidth / 2, -targetHeight, targetWidth, targetHeight, this);
      dancer.dispose();

      BufferedImage ebobo = this.eboboDanceFrames[this.danceFrame % this.eboboDanceFrames.length];
      int eboboHeight = (int)Math.round(this.getHeight() * 0.19);
      int eboboWidth = (int)Math.round(eboboHeight * ebobo.getWidth() / (double)ebobo.getHeight());
      Graphics2D eboboDancer = (Graphics2D)g.create();
      eboboDancer.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      eboboDancer.translate(this.getWidth() * 0.705 + EBOBO_SWAY[this.danceFrame],
         this.getHeight() * 0.705 - EBOBO_BOB[this.danceFrame]);
      eboboDancer.rotate(-DANCE_TILT[this.danceFrame] * 0.8);
      eboboDancer.drawImage(ebobo, -eboboWidth / 2, -eboboHeight, eboboWidth, eboboHeight, this);
      eboboDancer.dispose();

      g.dispose();
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
