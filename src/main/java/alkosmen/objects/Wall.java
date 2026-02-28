package alkosmen.objects;

import java.awt.Graphics;

import alkosmen.settings.Constants;
import alkosmen.Point;
import alkosmen.interfaces.IGameObject;

public class Wall implements IGameObject {
    private int x, y;

    public Wall(Point p) {
        x = p.x * 30;
        y = p.y * 30;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(Constants.WallColor);
        g.fillRect(x, y, 30, 30);
    }

}

