package alkosmen.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PixelButton extends JButton {

    private boolean hover = false;
    private boolean pressed = false;

    public PixelButton(String text) {
        super(text);

        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);

        setForeground(new Color(235, 245, 235));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // размеры/отступы
        setMargin(new Insets(10, 18, 10, 18));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; pressed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        int w = getWidth();
        int h = getHeight();

        // базовый цвет кнопки
        Color base = new Color(20, 30, 30, 160);      // тёмный прозрачный
        Color border = new Color(120, 180, 120, 220); // зелёная рамка

        if (hover) {
            base = new Color(30, 50, 35, 185);
            border = new Color(170, 240, 170, 235);
        }
        if (pressed) {
            base = new Color(10, 20, 20, 200);
            border = new Color(90, 150, 90, 235);
        }

        // “пиксельная” рамка (без сглаживания)
        g2.setColor(base);
        g2.fillRect(0, 0, w, h);

        g2.setColor(border);
        g2.drawRect(0, 0, w - 1, h - 1);
        g2.drawRect(2, 2, w - 5, h - 5);

        // тень текста
        String text = getText();
        FontMetrics fm = g2.getFontMetrics(getFont());
        int tx = (w - fm.stringWidth(text)) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();

        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(text, tx + 2, ty + 2);

        g2.setColor(getForeground());
        g2.drawString(text, tx, ty);

        g2.dispose();
    }
}
