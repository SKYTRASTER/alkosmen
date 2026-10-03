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
   private static final String MENU_IDLE_RESOURCE = "/alkosmen/ui/menu/alkosmen_idle_menu_v2.png";
   private static final String FALLBACK_WALK_RESOURCE = "/alkosmen/ui/sprites/alkosmen/walk_atlas_v1.png";

   private static final int[] IDLE_POSES = {0, 1, 2, 3, 4, 5, 6, 7};
   private static final int[] IDLE_SWAY = {-2, -1, 0, 1, 2, 1, 0, -1};
   private static final int[] IDLE_BOB = {0, 1, 2, 1, 0, 1, 2, 1};
   private static final double[] IDLE_TILT = {-0.01, -0.005, 0.0, 0.005, 0.01, 0.005, 0.0, -0.005};
   private static final int IDLE_FRAME_MS = 180;

   private final BufferedImage background;
   private final BufferedImage[] idleFrames;
   private final Timer idleTimer;
   private int idleFrame;

   public BackgroundPanel() {
      URL resource = BackgroundPanel.class.getResource(BACKGROUND_RESOURCE);
      if (resource == null) {
         throw new IllegalStateException("Menu background not found: " + BACKGROUND_RESOURCE);
      }

      try {
         this.background = ImageIO.read(resource);
         this.idleFrames = loadMenuIdleFrames();
      } catch (IOException error) {
         throw new IllegalStateException("Could not load menu art", error);
      }

      this.idleTimer = new Timer(IDLE_FRAME_MS, event -> {
         this.idleFrame = (this.idleFrame + 1) % IDLE_POSES.length;
         repaint();
      });
   }

   private static BufferedImage[] loadMenuIdleFrames() throws IOException {
      URL idleResource = BackgroundPanel.class.getResource(MENU_IDLE_RESOURCE);
      if (idleResource != null) {
         return CharacterSpriteAssets.loadGridAtlas(MENU_IDLE_RESOURCE, 8, 1)[0];
      }

      // Keep the menu working until the new generated idle atlas is added to resources.
      return CharacterSpriteAssets.loadGridAtlas(FALLBACK_WALK_RESOURCE, 4, 5, 30)[3];
   }

   @Override
   public void addNotify() {
      super.addNotify();
      idleTimer.start();
   }

   @Override
   public void removeNotify() {
      idleTimer.stop();
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

      int poseIndex = IDLE_POSES[this.idleFrame] % this.idleFrames.length;
      BufferedImage pose = this.idleFrames[poseIndex];
      int targetHeight = (int)Math.round(this.getHeight() * 0.43);
      int targetWidth = (int)Math.round(targetHeight * pose.getWidth() / (double)pose.getHeight());

      Graphics2D character = (Graphics2D)g.create();
      character.translate(
         this.getWidth() * 0.65 + IDLE_SWAY[this.idleFrame],
         this.getHeight() * 0.91 - IDLE_BOB[this.idleFrame]
      );
      character.rotate(IDLE_TILT[this.idleFrame]);
      character.drawImage(pose, -targetWidth / 2, -targetHeight, targetWidth, targetHeight, this);
      character.dispose();
      g.dispose();
   }
}
