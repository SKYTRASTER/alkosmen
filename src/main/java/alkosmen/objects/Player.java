package alkosmen.objects;

public class Player {
    public double x, y;
    public double vx, vy;
    public boolean onGround;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.vx = 0.0;
        this.vy = 0.0;
        this.onGround = false;
    }
}
