package alkosmen.Objects;

import java.awt.Graphics;
import java.awt.Image;
import alkosmen.Constants;
import alkosmen.Point;
import alkosmen.Sprite;
import alkosmen.Interfaces.IGameObject;

public class Grass extends Texture implements IGameObject {

    public Grass(Point p, Image image) {
        super(p);
        sprite = new Sprite(image);
    }

    public Grass(Point p, Image image, String text) {
        super(p);
        this.text = text;
        sprite = new Sprite(image);
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
