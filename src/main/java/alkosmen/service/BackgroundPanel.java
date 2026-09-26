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
   private static final String ALKOSMEN_DANCE_RESOURCE = "/alkosmen/ui/menu/alkosmen_dance24_sheet.png";
   private static final String EBOBO_DANCE_RESOURCE = "/alkosmen/ui/menu/ebobo_dance24_sheet.png";

   private static final int DANCE_COLUMNS = 6;
   private static final int DANCE_ROWS = 4;
   private static final int DANCE_FRAME_COUNT = DANCE_COLUMNS * DANCE_ROWS;
   private static final int DANCE_FRAME_MS = 120;

   private static final int[] ALKOSMEN_SEQUENCE = {
      0, 1, 2, 3, 4, 5,
      6, 7, 8, 9, 10, 11,
      12, 13, 14, 15, 16, 17,
      18, 19, 20, 21, 22, 23
   };

   private static final int[] EBOBO_SEQUENCE = {
      0, 1, 2, 3, 4, 5,
      6, 7, 8, 9, 10, 11,
      12, 13, 16, 18, 19, 20,
      21, 22, 23, 22, 21, 20
   };

   private static final double ALKOSMEN_X_RATIO = 0.605;
   private static final double EBOBO_X_RATIO = 0.745;
   private static final double ALKOSMEN_GROUND_Y_RATIO = 0.655;
   private static final double EBOBO_GROUND_Y_RATIO = 0.645;
   private static final double ALKOSMEN_BASELINE_LIFT_RATIO = 0.018;
   private static final double EBOBO_BASELINE_LIFT_RATIO = 0.010;
   private static final double ALKOSMEN_HEIGHT_RATIO = 0.225;
   private static final double EBOBO_HEIGHT_RATIO = 0.185;

   private final BufferedImage background;
   private final BufferedImage[] alkosmenDanceFrames;
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
         this.alkosmenDanceFrames = normalizeFrames(
            flattenAtlas(
               CharacterSpriteAssets.loadGridAtlas(
                  ALKOSMEN_DANCE_RESOURCE,
                  DANCE_COLUMNS,
                  DANCE_ROWS
               )
            )
         );
         this.eboboDanceFrames = normalizeFrames(
            flattenAtlas(
               CharacterSpriteAssets.loadGridAtlas(
                  EBOBO_DANCE_RESOURCE,
                  DANCE_COLUMNS,
                  DANCE_ROWS
               )
            )
         );
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
      g.fillRect(0, 0, getWidth(), getHeight());
      g.setRenderingHint(
         RenderingHints.KEY_INTERPOLATION,
         RenderingHints.VALUE_INTERPOLATION_BILINEAR
      );

      double scale = Math.max(
         (double)getWidth() / background.getWidth(),
         (double)getHeight() / background.getHeight()
      );
      int backgroundWidth = (int)Math.ceil(background.getWidth() * scale);
      int backgroundHeight = (int)Math.ceil(background.getHeight() * scale);

      g.drawImage(
         background,
         (getWidth() - backgroundWidth) / 2,
         (getHeight() - backgroundHeight) / 2,
         backgroundWidth,
         backgroundHeight,
         this
      );

      drawDancer(
         g,
         alkosmenDanceFrames[ALKOSMEN_SEQUENCE[danceFrame]],
         getWidth() * ALKOSMEN_X_RATIO,
         getHeight() * ALKOSMEN_GROUND_Y_RATIO,
         getHeight() * ALKOSMEN_BASELINE_LIFT_RATIO,
         (int)Math.round(getHeight() * ALKOSMEN_HEIGHT_RATIO)
      );

      int eboboFrame = (danceFrame + 4) % DANCE_FRAME_COUNT;
      drawDancer(
         g,
         eboboDanceFrames[EBOBO_SEQUENCE[eboboFrame]],
         getWidth() * EBOBO_X_RATIO,
         getHeight() * EBOBO_GROUND_Y_RATIO,
         getHeight() * EBOBO_BASELINE_LIFT_RATIO,
         (int)Math.round(getHeight() * EBOBO_HEIGHT_RATIO)
      );

      g.dispose();
   }

   private void drawDancer(
      Graphics2D g,
      BufferedImage frame,
      double anchorX,
      double groundY,
      double baselineLift,
      int targetHeight
   ) {
      int targetWidth = (int)Math.round(
         targetHeight * frame.getWidth() / (double)frame.getHeight()
      );
      double spriteBottom = groundY - baselineLift;

      drawGroundShadow(g, anchorX, groundY, targetWidth);

      Graphics2D dancer = (Graphics2D)g.create();
      dancer.setRenderingHint(
         RenderingHints.KEY_INTERPOLATION,
         RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
      );
      dancer.drawImage(
         frame,
         (int)Math.round(anchorX - targetWidth / 2.0),
         (int)Math.round(spriteBottom - targetHeight),
         targetWidth,
         targetHeight,
         this
      );
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

   private static BufferedImage[] normalizeFrames(BufferedImage[] frames) {
      BufferedImage[] normalized = new BufferedImage[frames.length];
      for (int i = 0; i < frames.length; ++i) {
         normalized[i] = normalizeFrame(frames[i]);
      }
      return normalized;
   }

   private static BufferedImage normalizeFrame(BufferedImage source) {
      int minX = source.getWidth();
      int minY = source.getHeight();
      int maxX = -1;
      int maxY = -1;

      for (int y = 0; y < source.getHeight(); ++y) {
         for (int x = 0; x < source.getWidth(); ++x) {
            int alpha = source.getRGB(x, y) >>> 24;
            if (alpha > 8) {
               minX = Math.min(minX, x);
               minY = Math.min(minY, y);
               maxX = Math.max(maxX, x);
               maxY = Math.max(maxY, y);
            }
         }
      }

      if (maxX < minX || maxY < minY) {
         return source;
      }

      int contentCenterX = (minX + maxX) / 2;
      int targetCenterX = source.getWidth() / 2;
      int targetBottomY = source.getHeight() - 2;

      int offsetX = targetCenterX - contentCenterX;
      int offsetY = targetBottomY - maxY;

      BufferedImage result = new BufferedImage(
         source.getWidth(),
         source.getHeight(),
         BufferedImage.TYPE_INT_ARGB
      );

      Graphics2D graphics = result.createGraphics();
      graphics.setRenderingHint(
         RenderingHints.KEY_INTERPOLATION,
         RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
      );
      graphics.drawImage(source, offsetX, offsetY, null);
      graphics.dispose();

      return result;
   }

   private static BufferedImage[] flattenAtlas(BufferedImage[][] atlas) {
      BufferedImage[] result = new BufferedImage[DANCE_FRAME_COUNT];
      int index = 0;

      for (BufferedImage[] row : atlas) {
         for (BufferedImage frame : row) {
            result[index++] = frame;
         }
      }

      if (index != DANCE_FRAME_COUNT) {
         throw new IllegalStateException(
            "Expected " + DANCE_FRAME_COUNT + " dance frames, got " + index
         );
      }

      return result;
   }
}
