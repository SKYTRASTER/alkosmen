package alkosmen.service;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BackgroundPanel extends JPanel {

    // TODO: Replace with final art keeping the same aspect ratio for best framing.
    private static final String PRIMARY_MENU_BG = "/alkosmen/ui/versions/img.png";
    private static final String FALLBACK_MENU_BG = "/alkosmen/ui/menu_background.png";
    private static final String MOZOL_OVERLAY = "/alkosmen/ui/menu/mozol_overlay.png";
    private static final String EBOBO_WALK_RIGHT = "/alkosmen/ui/intro/ebobo/walk_right";
    private static final String EBOBO_WALK_LEFT = "/alkosmen/ui/intro/ebobo/walk_left";

    private Image background;
    private Image mozolOverlay;
    private Image[] eboboWalkRight;
    private Image[] eboboWalkLeft;
    private final Timer eboboTimer;
    private int eboboX = -1;
    private int eboboDirection = 1;
    private int eboboFrame = 0;

    public BackgroundPanel() {
        background = loadBackground(PRIMARY_MENU_BG);
        if (background == null) {
            background = loadBackground(FALLBACK_MENU_BG);
        }
        mozolOverlay = loadBackground(MOZOL_OVERLAY);
        eboboWalkRight = loadAnimationTrack(EBOBO_WALK_RIGHT);
        eboboWalkLeft = loadAnimationTrack(EBOBO_WALK_LEFT);

        eboboTimer = new Timer(95, e -> {
            advanceEboboAnimation();
            repaint();
        });
        eboboTimer.setRepeats(true);
    }

    private static Image loadBackground(String path) {
        try {
            return ImageIO.read(BackgroundPanel.class.getResource(path));
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    private static Image[] loadAnimationTrack(String folderPath) {
        List<Image> frames = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            Image frame = loadBackground(folderPath + "/" + String.format("%02d", i) + ".png");
            if (frame == null) {
                break;
            }
            frames.add(frame);
        }
        return frames.toArray(new Image[0]);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!eboboTimer.isRunning()) {
            eboboTimer.start();
        }
    }

    @Override
    public void removeNotify() {
        eboboTimer.stop();
        super.removeNotify();
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
        drawMozolOverlay(g2, w, h);
        drawEboboOverlay(g2, w, h);

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

        int vignetteW = (int) (w * 0.22);
        GradientPaint edgeVignette = new GradientPaint(
                0,
                0,
                new Color(0, 0, 0, 95),
                vignetteW,
                0,
                new Color(0, 0, 0, 0)
        );
        g2.setPaint(edgeVignette);
        g2.fillRect(0, 0, vignetteW, h);
    }

    private void drawMozolOverlay(Graphics2D g2, int w, int h) {
        if (mozolOverlay == null) {
            return;
        }

        int srcW = mozolOverlay.getWidth(this);
        int srcH = mozolOverlay.getHeight(this);
        if (srcW <= 0 || srcH <= 0) {
            return;
        }

        int margin = 20;
        int targetH = Math.max(1, (int) Math.round(h * 0.22));
        int targetW = Math.max(1, (int) Math.round((double) srcW * targetH / srcH));
        int x = w - targetW - margin;
        int y = h - targetH - margin;

        g2.drawImage(mozolOverlay, x, y, targetW, targetH, this);
    }

    private void drawEboboOverlay(Graphics2D g2, int w, int h) {
        Image[] activeTrack = eboboDirection >= 0 ? eboboWalkRight : eboboWalkLeft;
        if (activeTrack == null || activeTrack.length == 0) {
            return;
        }
        Image eboboOverlay = activeTrack[Math.floorMod(eboboFrame, activeTrack.length)];

        int srcW = eboboOverlay.getWidth(this);
        int srcH = eboboOverlay.getHeight(this);
        if (srcW <= 0 || srcH <= 0) {
            return;
        }

        int margin = 20;
        int targetH = Math.max(1, (int) Math.round(h * 0.18));
        int targetW = Math.max(1, (int) Math.round((double) srcW * targetH / srcH));
        int benchMinX = (int) Math.round(w * 0.18);
        int benchMaxX = (int) Math.round(w * 0.45);
        if (eboboX < 0) {
            eboboX = benchMinX;
        }
        int benchSeatY = (int) Math.round(h * 0.60);
        int x = eboboX - targetW / 2;
        int y = benchSeatY - targetH;

        int minX = Math.max(margin, benchMinX - targetW / 2);
        int maxX = Math.min(Math.max(margin, w - targetW - margin), benchMaxX - targetW / 2);
        x = Math.max(minX, Math.min(x, maxX));
        y = Math.max(margin, Math.min(y, Math.max(margin, h - targetH - margin)));

        g2.drawImage(eboboOverlay, x, y, targetW, targetH, this);
    }

    private void advanceEboboAnimation() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        int benchMinX = (int) Math.round(w * 0.18);
        int benchMaxX = (int) Math.round(w * 0.45);
        if (eboboX < 0) {
            eboboX = benchMinX;
        }

        int step = Math.max(2, (int) Math.round(w * 0.0032));
        eboboX += step * eboboDirection;
        if (eboboX <= benchMinX) {
            eboboX = benchMinX;
            eboboDirection = 1;
        } else if (eboboX >= benchMaxX) {
            eboboX = benchMaxX;
            eboboDirection = -1;
        }

        Image[] activeTrack = eboboDirection >= 0 ? eboboWalkRight : eboboWalkLeft;
        if (activeTrack != null && activeTrack.length > 0) {
            eboboFrame = (eboboFrame + 1) % activeTrack.length;
        }
    }

}
