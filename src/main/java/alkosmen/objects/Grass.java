package src.main.java.alkosmen.objects;

import src.main.java.alkosmen.Point;
import src.main.java.alkosmen.Sprite;
import src.main.java.alkosmen.interfaces.IGameObject;

import java.awt.Graphics;
import java.awt.Image;

public class Grass extends Texture implements IGameObject {

    public Grass(Point p, Image image) {
        super(p);
        var sprite = new Sprite(image);
    }

    public Grass(Point p, Image image, String text) {
        super(p);
        this.text = text;
        var   sprite = new Sprite(image);
    }

    @Override
    public void draw(Graphics g) {
        if (this.text != null) {
            sprite.draw(g, new Point(x, y), text);
        } else {
            sprite.draw(g, new Point(x, y));
        }

    }

}
