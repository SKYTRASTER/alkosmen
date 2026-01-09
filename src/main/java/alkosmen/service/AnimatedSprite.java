package alkosmen.service;

import java.awt.Graphics;
import java.awt.Image;

public class AnimatedSprite {
    private final Image[] frames;
    private int index = 0;
    private long last = 0;
    private final int delayMs;

    public AnimatedSprite(int delayMs, Image... frames) {
        this.frames = frames;
        this.delayMs = delayMs;
    }

    public void update() {
        long now = System.currentTimeMillis();
        if (now - last >= delayMs) {
            index = (index + 1) % frames.length;
            last = now;
        }
    }

    public void draw(Graphics g, int x, int y, int w, int h) {
        g.drawImage(frames[index], x, y, w, h, null);
    }
}
