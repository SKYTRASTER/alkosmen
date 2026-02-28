package alkosmen.objects;

import alkosmen.settings.Constants;
import alkosmen.Point;
import alkosmen.Sprite;

public class Texture {
    protected int x, y;
    protected Sprite sprite;
    protected String text;

    public Texture(Point p) {
        x = p.x * Constants.Size;
        y = p.y * Constants.Size;
    }
}

