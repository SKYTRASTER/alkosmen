package alkosmen;

public class Point {
    public int x, y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean equal(Point p) {
        if (p.x == x && p.y == y)
            return true;
        else
            return false;
    }

    public Point clone() {
        return new Point(x, y);
    }

    public boolean onOneLineWith(Point p) {
        if (p.x == x || p.y == y)
            return true;
        return false;
    }
}
