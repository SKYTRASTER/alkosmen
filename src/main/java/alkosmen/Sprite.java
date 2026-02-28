package alkosmen;

import alkosmen.settings.Constants;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;

public class Sprite {
    private Image image;

    public Sprite(Image image) {
        this.image = image;
    }

    public void draw(Graphics g, Point coord) {
        // System.out.println(coord.x + " " + coord.y);
        g.drawImage(this.image, coord.x, coord.y, null);
    }

    public void draw(Graphics g, Point coord, String text) {
        // System.out.println(coord.x + " " + coord.y);
        g.setFont(new Font(Constants.Font, 0, 10));
        g.drawImage(this.image, coord.x, coord.y, null);

        g.drawString(text, coord.x + 5, coord.y + 15);

    }
}

