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

    private static final String PRIMARY_MENU_BG = "/alkosmen/ui/menu/main_menu_background.png";
    private static final String FALLBACK_MENU_BG = "/alkosmen/ui/menu/main_menu_bg.png";
    private static final String MOZOL_OVERLAY = "/alkosmen/ui/menu/mozol_overlay.png";
    private static final String EBOBO_WALK_RIGHT = "/alkosmen/ui/menu/ebobo_overlay_sheet_sprites/walk_right";
    private static final String EBOBO_WALK_LEFT = "/alkosmen/ui/menu/ebobo_overlay_sheet_sprites/walk_left";

    private Image background;
    private Image mozolOverlay;
    private Image[] eboboWalkRight;
    private Image[] eboboWalkLeft;
    private final Timer eboboTimer;
    private int eboboX = -1;
    private int eboboDirection = 1;
    private int eboboFrame = 0;
    private int eboboTick = 0;

    private static final int PHASE_BENCH_TO_WHITE = 0;
    private static final int PHASE_WHITE_BUMP_PAUSE = 1;
    private static final int PHASE_BENCH_TO_EDGE = 2;
    private static final int PHASE_JUMP_DOWN = 3;
    private static final int PHASE_GROUND_TO_OPPOSITE = 4;
    private static final int PHASE_JUMP_UP = 5;

    private int eboboPhase = PHASE_BENCH_TO_WHITE;
    private int eboboPhaseTick = 0;
    private int eboboSide = -1; // -1 left side of bench, +1 right side

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

        int targetH = Math.max(1, (int) Math.round(h * 0.20));
        int targetW = Math.max(1, (int) Math.round((double) srcW * targetH / srcH));

        PathMetrics m = pathMetrics(w, h, targetW);
        if (eboboX < 0) {
            eboboX = m.benchLeftX;
        }

        int x = eboboX - targetW / 2;
        int footY = currentFootY(m);
        int y = footY - targetH;

        x = Math.max(0, Math.min(x, Math.max(0, w - targetW)));
        y = Math.max(0, Math.min(y, Math.max(0, h - targetH)));

        g2.drawImage(eboboOverlay, x, y, targetW, targetH, this);
    }

    private void advanceEboboAnimation() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        Image[] activeTrack = eboboDirection >= 0 ? eboboWalkRight : eboboWalkLeft;
        Image probe = activeTrack != null && activeTrack.length > 0
                ? activeTrack[Math.floorMod(eboboFrame, activeTrack.length)]
                : null;
        int srcW = probe != null ? probe.getWidth(this) : 120;
        int srcH = probe != null ? probe.getHeight(this) : 160;
        if (srcW <= 0) srcW = 120;
        if (srcH <= 0) srcH = 160;
        int targetH = Math.max(1, (int) Math.round(h * 0.20));
        int targetW = Math.max(1, (int) Math.round((double) srcW * targetH / srcH));
        int halfW = targetW / 2;

        PathMetrics m = pathMetrics(w, h, targetW);
        if (eboboX < 0) {
            eboboX = eboboSide < 0 ? m.benchLeftX : m.benchRightX;
        }

        int stepBench = Math.max(2, (int) Math.round(w * 0.0038));
        int stepGround = Math.max(3, (int) Math.round(w * 0.0055));

        int whiteStopX = eboboSide < 0 ? (m.whiteLeftX - halfW) : (m.whiteRightX + halfW);
        int benchEdgeX = eboboSide < 0 ? m.benchLeftX : m.benchRightX;
        int groundTargetX = eboboSide < 0 ? m.groundRightX : m.groundLeftX;
        int jumpUpTargetX = eboboSide < 0 ? m.benchRightX : m.benchLeftX;

        switch (eboboPhase) {
            case PHASE_BENCH_TO_WHITE -> {
                eboboDirection = eboboSide < 0 ? 1 : -1;
                eboboX += stepBench * eboboDirection;
                if ((eboboDirection > 0 && eboboX >= whiteStopX) || (eboboDirection < 0 && eboboX <= whiteStopX)) {
                    eboboX = whiteStopX;
                    eboboPhase = PHASE_WHITE_BUMP_PAUSE;
                    eboboPhaseTick = 0;
                }
            }
            case PHASE_WHITE_BUMP_PAUSE -> {
                eboboDirection = eboboSide < 0 ? 1 : -1;
                eboboPhaseTick++;
                if (eboboPhaseTick >= 6) {
                    eboboPhase = PHASE_BENCH_TO_EDGE;
                    eboboPhaseTick = 0;
                }
            }
            case PHASE_BENCH_TO_EDGE -> {
                eboboDirection = eboboSide < 0 ? -1 : 1;
                eboboX += stepBench * eboboDirection;
                if ((eboboDirection < 0 && eboboX <= benchEdgeX) || (eboboDirection > 0 && eboboX >= benchEdgeX)) {
                    eboboX = benchEdgeX;
                    eboboPhase = PHASE_JUMP_DOWN;
                    eboboPhaseTick = 0;
                }
            }
            case PHASE_JUMP_DOWN -> {
                eboboDirection = eboboSide < 0 ? -1 : 1;
                eboboPhaseTick++;
                if (eboboPhaseTick >= 12) {
                    eboboPhase = PHASE_GROUND_TO_OPPOSITE;
                    eboboPhaseTick = 0;
                }
            }
            case PHASE_GROUND_TO_OPPOSITE -> {
                eboboDirection = eboboSide < 0 ? 1 : -1;
                eboboX += stepGround * eboboDirection;
                if ((eboboDirection > 0 && eboboX >= groundTargetX) || (eboboDirection < 0 && eboboX <= groundTargetX)) {
                    eboboX = groundTargetX;
                    eboboPhase = PHASE_JUMP_UP;
                    eboboPhaseTick = 0;
                }
            }
            case PHASE_JUMP_UP -> {
                eboboDirection = eboboSide < 0 ? 1 : -1;
                int total = 12;
                int t = Math.min(total, eboboPhaseTick + 1);
                eboboX = lerpInt(groundTargetX, jumpUpTargetX, t / (double) total);
                eboboPhaseTick++;
                if (eboboPhaseTick >= total) {
                    eboboX = jumpUpTargetX;
                    eboboSide = -eboboSide;
                    eboboPhase = PHASE_BENCH_TO_WHITE;
                    eboboPhaseTick = 0;
                }
            }
            default -> {
                eboboPhase = PHASE_BENCH_TO_WHITE;
                eboboPhaseTick = 0;
            }
        }

        activeTrack = eboboDirection >= 0 ? eboboWalkRight : eboboWalkLeft;
        if (activeTrack != null && activeTrack.length > 0) {
            eboboFrame = (eboboFrame + 1) % activeTrack.length;
        }
        eboboTick++;
    }

    private int currentFootY(PathMetrics m) {
        return switch (eboboPhase) {
            case PHASE_BENCH_TO_WHITE, PHASE_WHITE_BUMP_PAUSE, PHASE_BENCH_TO_EDGE -> m.benchFootY;
            case PHASE_GROUND_TO_OPPOSITE -> m.groundFootY;
            case PHASE_JUMP_DOWN -> jumpFootY(m.benchFootY, m.groundFootY, eboboPhaseTick, 12, Math.max(10, getHeight() / 24));
            case PHASE_JUMP_UP -> jumpFootY(m.groundFootY, m.benchFootY, eboboPhaseTick, 12, Math.max(10, getHeight() / 24));
            default -> m.benchFootY;
        };
    }

    private static int jumpFootY(int startY, int endY, int tick, int total, int arcHeight) {
        double t = Math.max(0.0, Math.min(1.0, tick / (double) Math.max(1, total)));
        double base = startY + (endY - startY) * t;
        double arc = -4.0 * arcHeight * t * (1.0 - t);
        return (int) Math.round(base + arc);
    }

    private static int lerpInt(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    private PathMetrics pathMetrics(int w, int h, int targetW) {
        int halfW = Math.max(1, targetW / 2);
        int benchLeft = clamp((int) Math.round(w * 0.12), halfW, Math.max(halfW, w - halfW));
        int benchRight = clamp((int) Math.round(w * 0.50), halfW, Math.max(halfW, w - halfW));
        if (benchRight <= benchLeft) {
            benchRight = Math.min(w - halfW, benchLeft + Math.max(30, w / 6));
        }

        int whiteLeft = clamp((int) Math.round(w * 0.40), benchLeft + 10, benchRight - 10);
        int whiteRight = clamp((int) Math.round(w * 0.54), whiteLeft + 10, benchRight - 5);

        int groundLeft = clamp((int) Math.round(w * 0.10), halfW, Math.max(halfW, w - halfW));
        int groundRight = clamp((int) Math.round(w * 0.62), halfW, Math.max(halfW, w - halfW));
        if (groundRight <= groundLeft) {
            groundRight = Math.min(w - halfW, groundLeft + Math.max(40, w / 5));
        }

        int benchFootY = (int) Math.round(h * 0.66);
        int groundFootY = (int) Math.round(h * 0.92);
        return new PathMetrics(benchLeft, benchRight, whiteLeft, whiteRight, groundLeft, groundRight, benchFootY, groundFootY);
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private record PathMetrics(
            int benchLeftX,
            int benchRightX,
            int whiteLeftX,
            int whiteRightX,
            int groundLeftX,
            int groundRightX,
            int benchFootY,
            int groundFootY
    ) {
    }

}
