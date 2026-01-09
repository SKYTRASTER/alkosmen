package alkosmen;

import alkosmen.interfaces.IGameObject;
import alkosmen.maps.LevelLoader;
import alkosmen.maps.Map_Level_1;
import alkosmen.objects.GameMap;
import alkosmen.objects.Player;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public final class Game extends Canvas implements Runnable {
    private static int SCORES = 0;
    private static boolean running;
    private IGameObject objects[];
    private BufferStrategy strategy;
    Graphics2D g2d;
    private Player player;
    private char[][] map;
    private GameMap gameMap;   // если реально нужен
    private char[][] levelMap; // ЭТО карта уровня из txt
    public void run() {
        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private void init() throws Exception {
        //System.out.println("map size: " + levelMap.length + " x " + levelMap[0].length);
        //System.out.println("player: " + player.x + "," + player.y);
        running = true;

        createBufferStrategy(2);
        BufferStrategy bs = getBufferStrategy();
        Graphics2D g = (Graphics2D) bs.getDrawGraphics();

        g.setColor(Color.white);
        g.fillRect(0, 0, Constants.Width, Constants.Height);
        System.out.println("URL level1 = " +
                alkosmen.maps.LevelLoader.class.getResource("/alkosmen/maps/level1.txt"));

        levelMap = LevelLoader.load("/alkosmen/maps/level1.txt");
        System.out.println("levelMap = " + (levelMap == null ? "NULL" : levelMap.length + " rows"));

        // создаём массив объектов карты
        objects = new IGameObject[this.getWidth() * this.getHeight() / (Constants.Size * Constants.Size)];

        // 1) грузим level1.txt
        levelMap = LevelLoader.load("/alkosmen/maps/level1.txt");

        // 2) ищем P и ставим игрока
        for (int y = 0; y < levelMap.length; y++) {
            for (int x = 0; x < levelMap[0].length; x++) {
                if (levelMap[y][x] == 'P') {
                    player = new Player(x, y);
                    levelMap[y][x] = '.'; // превращаем старт в пол
                }
            }
        }

        // 3) создаём объекты уровня из символов
        Map_Level_1.generateMapFromChars(objects, levelMap);

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
