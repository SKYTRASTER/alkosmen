package alkosmen.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

public class PixelButton extends JButton {
   private boolean hover;
   private boolean pressed;

   public PixelButton(String text) {
      super(text);
      this.setFocusPainted(false);
      this.setBorderPainted(false);
      this.setContentAreaFilled(false);
      this.setOpaque(false);
      this.setMargin(new Insets(10, 20, 10, 20));
      this.setCursor(Cursor.getPredefinedCursor(12));
      this.setForeground(new Color(255, 223, 177));
      this.setFont(new Font("Monospaced", 1, 27));
      this.setFocusable(true);
      this.addMouseListener(new MouseAdapter() {
         public void mouseEntered(MouseEvent e) {
            PixelButton.this.hover = true;
            PixelButton.this.repaint();
         }

         public void mouseExited(MouseEvent e) {
            PixelButton.this.hover = false;
            PixelButton.this.pressed = false;
            PixelButton.this.repaint();
         }

         public void mousePressed(MouseEvent e) {
            PixelButton.this.pressed = true;
            PixelButton.this.repaint();
         }

         public void mouseReleased(MouseEvent e) {
            PixelButton.this.pressed = false;
            PixelButton.this.repaint();
         }
      });
   }

   protected void paintComponent(Graphics g) {
      Graphics2D g2 = (Graphics2D)g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int w = this.getWidth();
      int h = this.getHeight();
      boolean focused = this.hasFocus();
      Color glow = new Color(246, 115, 50, 38);
      Color top = new Color(91, 35, 32, 235);
      Color bottom = new Color(50, 27, 28, 244);
      Color border = new Color(226, 137, 83, 235);
      Color inner = new Color(255, 196, 126, 90);
      if (this.hover || focused) {
         glow = new Color(255, 171, 60, 110);
         top = new Color(139, 48, 34, 246);
         bottom = new Color(75, 30, 25, 250);
         border = new Color(255, 195, 111, 255);
         inner = new Color(255, 217, 158, 150);
      }

      if (this.pressed) {
         glow = new Color(255, 111, 44, 70);
         top = new Color(55, 23, 23, 245);
         bottom = new Color(35, 20, 19, 250);
         border = new Color(211, 127, 71, 245);
         inner = new Color(244, 175, 110, 100);
      }

      g2.setColor(glow);
      g2.fillRoundRect(-2, -2, w + 4, h + 4, 8, 8);
      g2.setPaint(new GradientPaint(0.0F, 0.0F, top, 0.0F, (float)h, bottom));
      g2.fillRoundRect(0, 0, w, h, 6, 6);
      g2.setColor(new Color(0, 0, 0, 54));
      g2.fillRoundRect(3, 4, Math.max(0, w - 6), Math.max(0, h - 6), 4, 4);
      g2.setColor(border);
      g2.setStroke(new BasicStroke(2.0F));
      g2.drawRoundRect(1, 1, w - 3, h - 3, 6, 6);
      g2.setColor(inner);
      g2.setStroke(new BasicStroke(1.3F));
      g2.drawRoundRect(4, 4, w - 9, h - 9, 3, 3);
      if (focused) {
         g2.setColor(new Color(255, 218, 151, 195));
         g2.setStroke(new BasicStroke(2.0F));
         g2.drawRoundRect(-2, -2, w + 3, h + 3, 8, 8);
      }

      String text = this.getText();
      FontMetrics fm = g2.getFontMetrics(this.getFont());
      int tx = (w - fm.stringWidth(text)) / 2;
      int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
      g2.setColor(new Color(0, 0, 0, 170));
      g2.drawString(text, tx + 1, ty + 1);
      g2.setColor(this.getForeground());
      g2.drawString(text, tx, ty);
      g2.dispose();
   }
}
