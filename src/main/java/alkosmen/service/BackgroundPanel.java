package alkosmen.service;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public final class BackgroundPanel extends JPanel {
   public static final String BACKGROUND_RESOURCE = "/alkosmen/ui/menu/town_square_menu_hero_v2.png";
   private final BufferedImage background;

   public BackgroundPanel() {
      URL resource = BackgroundPanel.class.getResource("/alkosmen/ui/menu/town_square_menu_hero_v2.png");
      if (resource == null) {
         throw new IllegalStateException("Menu background not found: /alkosmen/ui/menu/town_square_menu_hero_v2.png");
      } else {
         try {
            this.background = ImageIO.read(resource);
         } catch (IOException error) {
            throw new IllegalStateException("Could not load menu background", error);
         }
      }
   }

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
      g.dispose();
   }
}
