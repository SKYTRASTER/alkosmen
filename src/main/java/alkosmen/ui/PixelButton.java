package alkosmen.ui;

import javax.swing.JButton;
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

public class PixelButton extends JButton {

    private boolean hover;
    private boolean pressed;

    public PixelButton(String text) {
        super(text);

        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setMargin(new Insets(10, 20, 10, 20));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(new Color(239, 247, 244));
        setFont(new Font("Dialog", Font.BOLD, 26));
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                pressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        boolean focused = hasFocus();

        Color top = new Color(10, 20, 24, 185);
        Color bottom = new Color(5, 12, 16, 210);
        Color border = new Color(88, 195, 175, 235);
        Color inner = new Color(120, 230, 210, 130);

        if (hover || focused) {
            top = new Color(14, 30, 36, 205);
            bottom = new Color(8, 18, 24, 225);
            border = new Color(124, 231, 210, 245);
            inner = new Color(155, 250, 232, 170);
        }
        if (pressed) {
            top = new Color(6, 13, 16, 220);
            bottom = new Color(3, 8, 12, 235);
            border = new Color(76, 170, 154, 245);
            inner = new Color(120, 220, 204, 135);
        }

        g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
        g2.fillRoundRect(0, 0, w, h, 14, 14);

        g2.setColor(new Color(0, 0, 0, 65));
        g2.fillRoundRect(3, 4, Math.max(0, w - 6), Math.max(0, h - 6), 12, 12);

        g2.setColor(border);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);

        g2.setColor(inner);
        g2.setStroke(new BasicStroke(1.3f));
        g2.drawRoundRect(4, 4, w - 9, h - 9, 10, 10);

        if (focused) {
            g2.setColor(new Color(182, 255, 244, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(-2, -2, w + 3, h + 3, 16, 16);
        }

        String text = getText();
        FontMetrics fm = g2.getFontMetrics(getFont());
        int tx = (w - fm.stringWidth(text)) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();

        g2.setColor(new Color(0, 0, 0, 185));
        g2.drawString(text, tx + 1, ty + 2);
        g2.setColor(getForeground());
        g2.drawString(text, tx, ty);

        g2.dispose();
    }
}
