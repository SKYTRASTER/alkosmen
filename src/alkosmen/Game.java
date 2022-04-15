package alkosmen;

import java.awt.Color;
import java.awt.Font;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.awt.Toolkit;
import javax.imageio.ImageIO;

import alkosmen.Interfaces.IGameObject;
import alkosmen.Objects.GameMap;
import alkosmen.Objects.Grass;

public final class Game extends Canvas implements Runnable {
    private static int SCORES = 0;
    private GameMap map;
    private static boolean running;
    private IGameObject objects[];

    public void run() {
        init();
        while (running) {
            update();
            // render();
        }

        theEnd();

    }

    private void render() {
        BufferStrategy bs = getBufferStrategy();
        Graphics g = bs.getDrawGraphics();

        g.setColor(Color.yellow);
        g.fillRect(0, 0, 30, 30);
        g.setColor(Color.red);
        g.setFont(new Font("Serif", 0, 36));
        g.drawString(Integer.toString(SCORES), 10, 35);
        g.dispose();
        bs.show();
    }

    private void update() {
    }

    private void init() {
        running = true;
        createBufferStrategy(2);
        // requestFocus();
        BufferStrategy bs = getBufferStrategy();
        Graphics g = bs.getDrawGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, Constants.Width, Constants.Height);
        g.setColor(Color.MAGENTA);
        g.setFont(new Font("Serif", 0, 36));
        g.drawString("Ебанина", 10, 35);
        g.drawLine(1, 1, 50, 10);
        map = new GameMap();
        System.out.println((this.getWidth() / Constants.Size) + " " + this.getHeight() / Constants.Size);
        objects = new IGameObject[this.getWidth() * this.getHeight() / Constants.Size];
        int objindex = 0;

        for (int i = 0; i < this.getWidth() / Constants.Size; i++) {
            for (int j = 0; j < this.getWidth() / Constants.Size; j++) {
                Random random = new Random();
                Point p = new Point(i, j);
                if (random.nextBoolean()) {
                    map.setGrassAt(p);
                    objects[objindex] = new Grass(p);
                    objects[objindex].draw(g);
                }

            }

            objindex++;
        }

        g.dispose();
        bs.show();
    }

    private void theEnd() {
    }

    public void start() {
        running = true;
        new Thread(this).start();
    }

    public static void stopGame() {
        Game.running = false;
    }

    private Image getTextures() {

        return null;

    }

    private Image getImage(String path) {
        BufferedImage sourceImage = null;

        try {
            URL url = this.getClass().getClassLoader().getResource(path);
            sourceImage = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Image result = Toolkit.getDefaultToolkit().createImage(sourceImage.getSource());
        return result;
    }

    private Image[][] getAlkobotImages() {
        Image ar[][] = new Image[4][2];
        ar[0][0] = getImage("pacman_u_o.png");
        ar[0][1] = getImage("pacman_u_c.png");
        ar[1][0] = getImage("pacman_r_o.png");
        ar[1][1] = getImage("pacman_r_c.png");
        ar[2][0] = getImage("pacman_d_o.png");
        ar[2][1] = getImage("pacman_d_c.png");
        ar[3][0] = getImage("pacman_l_o.png");
        ar[3][1] = getImage("pacman_l_c.png");
        return ar;
    }
}
