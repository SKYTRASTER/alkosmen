package alkosmen.service;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

public final class BackgroundPanel extends JPanel {
   public static final String BACKGROUND_RESOURCE = "/alkosmen/ui/menu/town_square_dance_bg_v1.png";

   private static final String[] DANCE_ATLAS_PARTS = {
      "/alkosmen/ui/menu/menu_dance_26.b64.00",
      "/alkosmen/ui/menu/menu_dance_26.b64.01",
      "/alkosmen/ui/menu/menu_dance_26.b64.02",
      "/alkosmen/ui/menu/menu_dance_26.b64.03"
   };

   private static final int DANCE_FRAME_COUNT = 26;
   private static final int DANCE_FRAME_MS = 105;
   private static final int ATLAS_COLUMNS = 13;
   private static final int ATLAS_TOTAL_ROWS = 4;
   private static final int ALKOSMEN_START_ROW = 0;
   private static final int EBOBO_START_ROW = 2;

   private static final double ALKOSMEN_GROUND_Y_RATIO = 0.655;
   private static final double EBOBO_GROUND_Y_RATIO = 0.645;
   private static final double FRAME_BOTTOM_MARGIN_RATIO = 1.0 / 40.0;

   private final BufferedImage background;
   private final BufferedImage[] danceFrames;
   private final BufferedImage[] eboboDanceFrames;
   private final Timer danceTimer;
   private int danceFrame;

   public BackgroundPanel() {
      try {
         this.background = loadRequiredImage(BACKGROUND_RESOURCE);

         BufferedImage danceAtlas = loadEmbeddedDanceAtlas();
         this.danceFrames = loadCharacterFrames(danceAtlas, ALKOSMEN_START_ROW);
         this.eboboDanceFrames = loadCharacterFrames(danceAtlas, EBOBO_START_ROW);
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
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

      double scale = Math.max(
         (double)getWidth() / background.getWidth(),
         (double)getHeight() / background.getHeight()
      );
      int width = (int)Math.ceil(background.getWidth() * scale);
      int height = (int)Math.ceil(background.getHeight() * scale);

      g.drawImage(
         background,
         (getWidth() - width) / 2,
         (getHeight() - height) / 2,
         width,
         height,
         this
      );

      drawAlkosmenDance(g);
      drawEboboDance(g);
      g.dispose();
   }

   private void drawAlkosmenDance(Graphics2D g) {
      BufferedImage pose = danceFrames[danceFrame];

      int targetHeight = (int)Math.round(getHeight() * 0.225);
      int targetWidth = (int)Math.round(targetHeight * pose.getWidth() / (double)pose.getHeight());

      double anchorX = getWidth() * 0.605;
      double groundY = getHeight() * ALKOSMEN_GROUND_Y_RATIO;
      double spriteBottomY = groundY + targetHeight * FRAME_BOTTOM_MARGIN_RATIO;

      drawGroundShadow(g, anchorX, groundY, targetWidth);
      drawFrame(g, pose, anchorX, spriteBottomY, targetWidth, targetHeight);
   }

   private void drawEboboDance(Graphics2D g) {
      int frame = (danceFrame + 7) % DANCE_FRAME_COUNT;
      BufferedImage pose = eboboDanceFrames[frame];

      int targetHeight = (int)Math.round(getHeight() * 0.185);
      int targetWidth = (int)Math.round(targetHeight * pose.getWidth() / (double)pose.getHeight());

      double anchorX = getWidth() * 0.745;
      double groundY = getHeight() * EBOBO_GROUND_Y_RATIO;
      double spriteBottomY = groundY + targetHeight * FRAME_BOTTOM_MARGIN_RATIO;

      drawGroundShadow(g, anchorX, groundY, targetWidth);
      drawFrame(g, pose, anchorX, spriteBottomY, targetWidth, targetHeight);
   }

   private void drawFrame(
      Graphics2D g,
      BufferedImage frame,
      double anchorX,
      double spriteBottomY,
      int targetWidth,
      int targetHeight
   ) {
      Graphics2D dancer = (Graphics2D)g.create();
      dancer.setRenderingHint(
         RenderingHints.KEY_INTERPOLATION,
         RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
      );
      dancer.drawImage(
         frame,
         (int)Math.round(anchorX - targetWidth / 2.0),
         (int)Math.round(spriteBottomY - targetHeight),
         targetWidth,
         targetHeight,
         this
      );
      dancer.dispose();
   }

   private void drawGroundShadow(Graphics2D g, double x, double y, int dancerWidth) {
      Graphics2D shadow = (Graphics2D)g.create();
      int shadowWidth = Math.max(18, (int)Math.round(dancerWidth * 0.56));
      int shadowHeight = Math.max(4, shadowWidth / 8);

      shadow.setColor(new Color(0, 0, 0, 68));
      shadow.fillOval(
         (int)Math.round(x - shadowWidth / 2.0),
         (int)Math.round(y - shadowHeight / 2.0),
         shadowWidth,
         shadowHeight
      );
      shadow.dispose();
   }

   private static BufferedImage[] loadCharacterFrames(BufferedImage atlas, int startRow) throws IOException {
      if (atlas.getWidth() % ATLAS_COLUMNS != 0 || atlas.getHeight() % ATLAS_TOTAL_ROWS != 0) {
         throw new IOException("Invalid menu dance atlas dimensions: " + atlas.getWidth() + "x" + atlas.getHeight());
      }

      int cellWidth = atlas.getWidth() / ATLAS_COLUMNS;
      int cellHeight = atlas.getHeight() / ATLAS_TOTAL_ROWS;
      BufferedImage[] frames = new BufferedImage[DANCE_FRAME_COUNT];

      for (int i = 0; i < DANCE_FRAME_COUNT; ++i) {
         int column = i % ATLAS_COLUMNS;
         int row = startRow + i / ATLAS_COLUMNS;

         frames[i] = atlas.getSubimage(
            column * cellWidth,
            row * cellHeight,
            cellWidth,
            cellHeight
         );
      }

      return frames;
   }

   private static BufferedImage loadEmbeddedDanceAtlas() throws IOException {
      StringBuilder encoded = new StringBuilder(30000);

      for (String path : DANCE_ATLAS_PARTS) {
         try (InputStream input = BackgroundPanel.class.getResourceAsStream(path)) {
            if (input == null) {
               throw new IOException("Dance atlas part not found: " + path);
            }
            encoded.append(new String(input.readAllBytes(), StandardCharsets.US_ASCII).trim());
         }
      }

      byte[] pngBytes = Base64.getDecoder().decode(encoded.toString());
      try (ByteArrayInputStream input = new ByteArrayInputStream(pngBytes)) {
         BufferedImage atlas = ImageIO.read(input);
         if (atlas == null) {
            throw new IOException("Could not decode embedded menu dance atlas");
         }
         return atlas;
      }
   }

   private static BufferedImage loadRequiredImage(String path) throws IOException {
      try (InputStream input = BackgroundPanel.class.getResourceAsStream(path)) {
         if (input == null) {
            throw new IOException("Image not found: " + path);
         }

         BufferedImage image = ImageIO.read(input);
         if (image == null) {
            throw new IOException("Could not decode image: " + path);
         }
         return image;
      }
   }
}
