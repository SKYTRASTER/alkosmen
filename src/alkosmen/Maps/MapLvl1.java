package alkosmen.Maps;

import java.awt.Image;
import java.util.Random;

import javax.imageio.ImageIO;

import java.awt.Toolkit;
import java.awt.Graphics2D;
import alkosmen.Constants;
import alkosmen.Point;
import alkosmen.Interfaces.IGameObject;
import alkosmen.Objects.GameMap;
import alkosmen.Objects.Grass;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class MapLvl1 {
    public static void generateMap(int Width, int Height, Graphics2D g, IGameObject objects[], GameMap map) {
        System.out.println("Загрузка ресурсов");
        int objindex = 0;
        for (int i = 0; i < Width / Constants.Size; i++) {
            for (int j = 0; j < Height / Constants.Size; j++) {
                Random random = new Random();
                Point p = new Point(i, j);
                if (random.nextBoolean() && j % 2 == 0) {
                    map.setGrassAt(p);
                    objects[objindex] = new Grass(p, getTextures());
                    objects[objindex].draw(g);
                }

            }

            objindex++;
        }
        System.out.println("Загрузка завершена");
    }

    private static Image getTextures() {

        return getImage(
                "alkosmen/images/Textures/Grass/grass_1.png");

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
