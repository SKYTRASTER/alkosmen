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
import java.awt.Toolkit;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import alkosmen.Interfaces.IGameObject;
import alkosmen.Maps.MapLvl1;
import alkosmen.Objects.GameMap;
import alkosmen.Objects.Grass;

public final class Game extends Canvas implements Runnable {
    private static int SCORES = 0;
    private GameMap map;
    private static boolean running;
    private IGameObject objects[];
    private BufferStrategy strategy;
    Graphics2D g2d;

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
        // Graphics2D graphics = (Graphics2D) strategy.getDrawGraphics();
        // Graphics g = bs.getDrawGraphics();
        Graphics2D g = (Graphics2D) bs.getDrawGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, Constants.Width, Constants.Height);
        g.setColor(Color.black);
        g.setFont(new Font("Serif", 0, 36));
        g.drawString("Ебанина", 10, 35);
        map = new GameMap();
        System.out.println((this.getWidth() / Constants.Size) + " " + this.getHeight() / Constants.Size);
        objects = new IGameObject[this.getWidth() * this.getHeight() / Constants.Size];

        MapLvl1.generateMap(this.getWidth(), this.getHeight(), g, objects, map);
        g.drawLine(1, 1, this.getWidth(), 1);
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

    private Image getImage(String path, int x, int y, int width, int height) {
        BufferedImage sourceImage = null;

        try {
            URL url = this.getClass().getClassLoader().getResource(path);
            sourceImage = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Image result = Toolkit.getDefaultToolkit()
                .createImage(sourceImage.getSubimage(x, y, width, height).getSource());
        return result;
    }

    private Image[][] getAlkobotImages() {
        Image ar[][] = new Image[4][2];
        ar[0][0] = getImage("alkup0.png");
        ar[0][1] = getImage("alkup1.png");
        ar[1][0] = getImage("pacman_r_o.png");
        ar[1][1] = getImage("pacman_r_c.png");
        ar[2][0] = getImage("pacman_d_o.png");
        ar[2][1] = getImage("pacman_d_c.png");
        ar[3][0] = getImage("pacman_l_o.png");
        ar[3][1] = getImage("pacman_l_c.png");
        return ar;
    }
}
