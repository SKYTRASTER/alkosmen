package alkosmen.Objects;

import alkosmen.Constants;
import alkosmen.Point;

public class Texture {
    public int x, y;

    public Texture(Point p) {
        x = p.x * Constants.Size;
        y = p.y * Constants.Size;
    }
}
