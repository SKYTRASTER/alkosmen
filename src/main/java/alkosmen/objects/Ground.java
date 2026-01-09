package src.main.java.alkosmen.objects;


import src.main.java.alkosmen.Point;
import src.main.java.alkosmen.Sprite;
import src.main.java.alkosmen.interfaces.IGameObject;

import java.awt.*;


public class Ground extends Texture implements IGameObject {
    @Override
    public void draw(Graphics g) {
        sprite.draw(g, new Point(x, y));

    }

    public Ground(Point p, Image image) {
        super(p);

        sprite = new Sprite(image);
    }

}
