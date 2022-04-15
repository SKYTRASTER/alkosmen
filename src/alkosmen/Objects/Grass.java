package alkosmen.Objects;

import java.awt.Graphics;

import alkosmen.Constants;
import alkosmen.Point;
import alkosmen.Interfaces.IGameObject;

public class Grass extends Texture implements IGameObject {

    public Grass(Point p) {
        super(p);
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(Constants.WallColor);
        g.fillRect(x, y, Constants.Size, Constants.Size);

    }

}
