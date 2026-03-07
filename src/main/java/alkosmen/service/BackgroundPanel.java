package alkosmen.service;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.IOException;

public class BackgroundPanel extends JPanel {

    // TODO: Replace with final art keeping the same aspect ratio for best framing.
    private static final String PRIMARY_MENU_BG = "/alkosmen/ui/menu/main_menu_bg.png";
    private static final String FALLBACK_MENU_BG = "/alkosmen/ui/menu_background.png";

    private Image background;

    public BackgroundPanel() {
        background = loadBackground(PRIMARY_MENU_BG);
        if (background == null) {
            background = loadBackground(FALLBACK_MENU_BG);
        }
    }

    private static Image loadBackground(String path) {
        try {
            return ImageIO.read(BackgroundPanel.class.getResource(path));
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();

        drawBackgroundCover(g2, w, h);
        drawAtmospherePass(g2, w, h);
        drawMenuGlassPanel(g2, w, h);

        g2.dispose();
    }

    private void drawBackgroundCover(Graphics2D g2, int w, int h) {
        if (background == null) {
            g2.setColor(new Color(10, 16, 22));
            g2.fillRect(0, 0, w, h);
            return;
        }

        int imgW = background.getWidth(this);
        int imgH = background.getHeight(this);
        if (imgW <= 0 || imgH <= 0) {
            return;
        }

        double scale = Math.max((double) w / imgW, (double) h / imgH);
        int drawW = (int) Math.ceil(imgW * scale);
        int drawH = (int) Math.ceil(imgH * scale);

        int drawX = (w - drawW) / 2;
        int drawY = (h - drawH) / 2 + (int) (h * 0.02);

        g2.drawImage(background, drawX, drawY, drawW, drawH, this);
    }

    private void drawAtmospherePass(Graphics2D g2, int w, int h) {
        g2.setComposite(AlphaComposite.SrcOver);

        GradientPaint topFade = new GradientPaint(
                0,
                0,
                new Color(6, 12, 20, 155),
                0,
                h * 0.36f,
                new Color(6, 12, 20, 25)
        );
        g2.setPaint(topFade);
        g2.fillRect(0, 0, w, (int) (h * 0.38));

        GradientPaint edgeVignette = new GradientPaint(
                0,
                0,
                new Color(0, 0, 0, 95),
                w,
                0,
                new Color(0, 0, 0, 25)
        );
        g2.setPaint(edgeVignette);
        g2.fillRect(0, 0, (int) (w * 0.22), h);
    }

    private void drawMenuGlassPanel(Graphics2D g2, int w, int h) {
        int panelW = clamp((int) (w * 0.34), 350, 470);
        int panelH = clamp((int) (h * 0.60), 360, 500);
        int panelX = Math.max(22, (int) (w * 0.03));
        int panelY = clamp((int) (h * 0.14), 80, h - panelH - 26);

        GradientPaint glassFill = new GradientPaint(
                panelX,
                panelY,
                new Color(8, 18, 24, 130),
                panelX,
                panelY + panelH,
                new Color(6, 12, 18, 185)
        );

        g2.setPaint(glassFill);
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 28, 28);

        g2.setColor(new Color(110, 210, 196, 165));
        g2.drawRoundRect(panelX, panelY, panelW - 1, panelH - 1, 28, 28);

        g2.setColor(new Color(150, 235, 223, 70));
        g2.drawRoundRect(panelX + 3, panelY + 3, panelW - 7, panelH - 7, 24, 24);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
