package src.main.java.alkosmen.objects;

import src.main.java.alkosmen.Constants;
import src.main.java.alkosmen.Point;
import src.main.java.alkosmen.Sprite;

public class Texture {
    protected int x, y;
    protected Sprite sprite;
    protected String text;

    public Texture(Point p) {
        x = p.x * Constants.Size;
        y = p.y * Constants.Size;
    }
}
