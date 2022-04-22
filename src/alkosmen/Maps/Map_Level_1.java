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
import alkosmen.Service.Ellers;

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

        /*
         * Пока закоментировано
         * 
         * for (int i = 0; i < Width / Constants.Size; i++) {
         * for (int j = 0; j < (Height / Constants.Size); j++) {
         * Random random = new Random();
         * Point p = new Point(i, j);
         * new Ground(p, getGround()).draw(g);
         * if (random.nextBoolean() && j > 1) {
         * map.setGrassAt(p);
         * objects[objindex] = new Grass(p, getTextures(), objindex.toString());
         * objects[objindex].draw(g);
         * }
         * objindex++;
         * }
         * 
         * }
         */
        /************************************************************** */
        Ellers ell = new Ellers(24, 24);
        ell.makeMaze();
        ell.printMaze();
        int obj[][] = new int[Height / Constants.Size - 2][Width / Constants.Size - 1];
        g.setFont(new Font(Constants.Font, 0, 20));
        for (int j = 0; j < Height / Constants.Size - 2; j++) {
            int last = 0;
            /*
             * g.drawLine(1 * Constants.Size, (j + 2) * Constants.Size, 1 * Constants.Size,
             * (j + 2) * Constants.Size + Constants.Size);
             */

            Boolean lastwall = false;
            Boolean downwall = false;
            for (int i = 0; i < Width / Constants.Size - 1; i++) {
                Random random = new Random();
                Point p = new Point(i + 1, j + 2);
                /*
                 * g.drawLine((i + 1) * Constants.Size + 2, (j + 2) * Constants.Size,
                 * (i + 1) * Constants.Size - 2 + Constants.Size,
                 * (j + 2) * Constants.Size);
                 */
                // map.setGrassAt(p);
                // objects[objindex] = new Grass(p, getTextures(), objindex.toString());
                // objects[objindex].draw(g);

                last = (p.x) * Constants.Size + Constants.Size;
                if (random.nextBoolean()) {

                    g.drawLine(last, (p.y) * Constants.Size, last,
                            (p.y) * Constants.Size + Constants.Size);

                    // objects[objindex] = new Grass(p, getTextures(), objindex.toString());
                    // objects[objindex].draw(g);
                    if (!lastwall) {
                        if (i == 0) {
                            obj[j][i] = i + 1;
                        } else {
                            obj[j][i] = obj[j][i - 1];
                        }
                    } else {
                        obj[j][i] = i + 1;

                    }
                    lastwall = true;
                } else {
                    if (lastwall) {
                        obj[j][i] = i + 1;
                        lastwall = false;
                    } else if (!lastwall) {
                        if (i == 0) {
                            obj[j][i] = i + 1;
                        } else {
                            obj[j][i] = obj[j][i - 1];
                        }
                    }
                }

                g.drawString("" + obj[j][i], p.x * Constants.Size + 5, p.y * Constants.Size + Constants.Size);
                if (random.nextBoolean()) {

                    g.drawLine(p.x * Constants.Size + 2, p.y * Constants.Size + Constants.Size,
                            p.x * Constants.Size - 2 + Constants.Size,
                            p.y * Constants.Size + Constants.Size);

                }

                bs.show();
                /*
                 * g.drawLine(last, (j + 2) * Constants.Size, last,
                 * (j + 2) * Constants.Size + Constants.Size);
                 */
            }

            objindex++;
            break;
        }

        /********************************* */
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
