package alkosmen.Objects;

import java.awt.Graphics;
import java.awt.Image;
import alkosmen.Constants;
import alkosmen.Point;
import alkosmen.Sprite;
import alkosmen.Interfaces.IGameObject;

public class Grass extends Texture implements IGameObject {
    private Sprite sprite;

    public Grass(Point p, Image image) {
        super(p);
        sprite = new Sprite(image);
    }

    @Override
    public void draw(Graphics g) {
        sprite.draw(g, new Point(x, y));
    }

}
