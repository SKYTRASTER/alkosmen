package alkosmen.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class GameHudRenderer {
    private final int hudHeight;
    private final long caughtTextMs;

    public GameHudRenderer(int hudHeight, long caughtTextMs) {
        this.hudHeight = hudHeight;
        this.caughtTextMs = caughtTextMs;
    }

    public void drawHud(
            Graphics g,
            int width,
            int height,
            int level,
            int score,
            int bottleGoal,
            int lives,
            int maxLives,
            boolean hidden,
            boolean gameOver,
            String cityLine,
            long lastCaughtAt,
            long now
    ) {
        int y = height - hudHeight;

        g.setColor(new Color(28, 22, 20));
        g.fillRect(0, y, width, hudHeight);

        g.setColor(new Color(92, 74, 58));
        g.drawLine(0, y, width, y);
        g.drawLine(0, y + 1, width, y + 1);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.BOLD, 20));
        g.drawString("A/D or arrows + SPACE, S/Down to hide", 14, 50);

        g.setColor(new Color(160, 130, 96));
        g.setFont(new Font("Monospaced", Font.BOLD, 26));
        g.drawString("SCORE " + score, 16, y + 38);

        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.setColor(score >= bottleGoal ? new Color(120, 220, 120) : new Color(220, 210, 170));
        g.drawString("BOTTLES " + score + "/" + bottleGoal, 260, y + 35);

        g.setColor(lives > 1 ? new Color(140, 220, 140) : new Color(255, 140, 120));
        g.drawString("LIVES " + lives + "/" + maxLives, 500, y + 35);
        if (cityLine != null && !cityLine.isBlank()) {
            g.setColor(new Color(170, 190, 220));
            g.setFont(new Font("Dialog", Font.PLAIN, 13));
            g.drawString("ГОРОД: " + cityLine, 14, y + hudHeight - 6);
        }

        if (score >= bottleGoal) {
            g.setColor(new Color(120, 220, 120));
            g.drawString("YOU WIN!!!", 470, y + 35);
        }
        if (hidden) {
            g.setColor(new Color(150, 220, 255));
            g.drawString("HIDING", 700, y + 35);
        }
        if (now - lastCaughtAt < caughtTextMs) {
            g.setColor(new Color(255, 140, 120));
            g.drawString("CAUGHT BY COP", 790, y + 35);
        }
        if (gameOver) {
            g.setColor(new Color(255, 90, 90));
            g.drawString("GAME OVER", 790, y + 35);
        }
    }

    public void drawGameOverOverlay(Graphics g, int width, int height, boolean gameOver) {
        if (!gameOver) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 185));
        g2.fillRect(0, 0, width, height);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Impact", Font.BOLD, Math.max(72, height / 5)));
        String text = "GAME OVER";
        int textW = g2.getFontMetrics().stringWidth(text);
        int textX = (width - textW) / 2;
        int textY = height / 2;

        g2.setColor(new Color(30, 0, 0));
        g2.drawString(text, textX + 4, textY + 4);
        g2.setColor(new Color(255, 60, 60));
        g2.drawString(text, textX, textY);
        g2.dispose();
    }
}
