package alkosmen.Objects;

import java.awt.Graphics;
import java.awt.Image;

import alkosmen.Point;
import alkosmen.Sprite;
import alkosmen.Interfaces.IGameObject;

public class Ground extends Texture implements IGameObject {

    public Ground(Point p, Image image) {
        super(p);

        sprite = new Sprite(image);
    }

    @Override
    public void draw(Graphics g) {
        sprite.draw(g, new Point(x, y));
    }

}
