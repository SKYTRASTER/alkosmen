package alkosmen.Maps;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.util.Random;
import java.awt.image.BufferStrategy;
import javax.imageio.ImageIO;

import java.awt.Toolkit;
import java.awt.Graphics2D;
import alkosmen.Constants;
import alkosmen.Point;
import alkosmen.Interfaces.IGameObject;
import alkosmen.Objects.GameMap;
import alkosmen.Objects.Grass;
import alkosmen.Objects.Ground;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class Map_Level_1 {
    public static void generateMap(BufferStrategy bs, int Width, int Height, Graphics2D g, IGameObject objects[],
            GameMap map) {
        System.out.println("Загрузка ресурсов");
        Integer objindex = 0;
        int start = 1;
        g.setColor(Color.black);

        System.out.println(start + Constants.Size + 10);
        map = new GameMap();

        for (int i = 0; i < Width / Constants.Size; i++) {
            for (int j = 0; j < (Height / Constants.Size); j++) {
                Random random = new Random();
                Point p = new Point(i, j);
                new Ground(p, getGround()).draw(g);
                if (random.nextBoolean() && i % 2 == 0 && j > 1) {
                    map.setGrassAt(p);
                    objects[objindex] = new Grass(p, getTextures(), objindex.toString());
                    objects[objindex].draw(g);
                }

            }

            objindex++;
        }

        g.setFont(new Font(Constants.Font, 0, Constants.Size));
        g.drawLine(1, start, Width, start);
        g.drawLine(1, start + Constants.Size + 10, Width, start + Constants.Size + 10);
        g.drawString("Ебанина Map_Level_1", 20, 30);
        System.out.println("Загрузка завершена");
    }

    private static Image getTextures() {

        return getImage(
                "alkosmen/images/Textures/Grass/grass_2.png");

    }

    private static Image getGround() {
        return getImage(
                "alkosmen/images/Textures/Ground/dirt_160x130.png");
    }

    private static Image getImage(String path, int x, int y, int width, int height) {
        BufferedImage sourceImage = null;

        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(path);
            sourceImage = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Image result = Toolkit.getDefaultToolkit()
                .createImage(sourceImage.getSubimage(x, y, width, height).getSource());
        return result;
    }

    private static void setFontTextures() {

    }

    private static Image getImage(String path) {
        BufferedImage sourceImage = null;

        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(path);
            sourceImage = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Image result = Toolkit.getDefaultToolkit().createImage(sourceImage.getSource());
        return result;
    }
}
