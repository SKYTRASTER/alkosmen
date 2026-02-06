package alkosmen;

import alkosmen.gfx.SpriteSheet;
import alkosmen.interfaces.IGameObject;
import alkosmen.maps.LevelLoader;
import alkosmen.maps.MapScaler;
import alkosmen.objects.GameMap;
import alkosmen.objects.Player;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public final class Game extends Canvas implements Runnable {
    private int currentLevel = 1;
    private static int SCORES = 0;
    private static boolean running;
    private IGameObject objects[];
    private BufferStrategy strategy;
    private Image[][] playerSprites;
    Graphics2D g2d;
    private Player player;
    private char[][] map;
    private GameMap gameMap;   // если реально нужен
    private char[][] levelMap; // ЭТО карта уровня из txt
    private static final String[] LEVELS = {
            "/alkosmen/maps/level1.txt",
            "/alkosmen/maps/level2.txt",
            "/alkosmen/maps/level3.txt"
    };
    private int playerDir = 1;   // 0 up, 1 right, 2 down, 3 left
    private int animFrame = 0;
    private long lastAnim = 0;
    private SpriteSheet sheet;
    private Image tileFloor, tileWall, tileExit;

    public void run() {
        try {
            init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        while (running) {
            update();
            render();
            try {
                Thread.sleep(16);
            } catch (InterruptedException ignored) {
            }

        }

        theEnd();

    }

    @Override
    public Dimension getPreferredSize() {
        if (levelMap == null) return new Dimension(400, 300);
        return new Dimension(levelMap[0].length * Constants.Size,
                levelMap.length * Constants.Size);
    }

    private void render() {
        BufferStrategy bs = getBufferStrategy();
        if (bs == null) return;

        Graphics g = bs.getDrawGraphics();

        int w = getWidth();
        int h = getHeight();

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);


        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.BOLD, 24));
        g.drawString("LEVEL: " + currentLevel, 20, 40);
        if (levelMap != null) {
            int cell = Constants.Size;

            for (int y = 0; y < levelMap.length; y++) {
                for (int x = 0; x < levelMap[0].length; x++) {
                    char c = levelMap[y][x];

                    Image img =
                            (c == '#') ? tileWall :
                                    (c == 'E') ? tileExit :
                                            tileFloor;

                    g.drawImage(img, x * cell, y * cell, cell, cell, null);
                }
            }
        }
        // анимация 0<->1 раз в 150 мс
        long now = System.currentTimeMillis();
        if (now - lastAnim > 150) {
            animFrame = 1 - animFrame;
            lastAnim = now;
        }

        if (player != null && playerSprites != null) {
            int cell = Constants.Size;
            Image img = playerSprites[playerDir][animFrame];
            g.drawImage(img, player.x * cell, player.y * cell, cell, cell, null);
        }
        g.dispose();
        bs.show();
    }

    private void update() {
    }


    private void init() throws Exception {
        running = true;

        setFocusable(true);
        requestFocus();
        enableKeys();
        while (!isDisplayable()) {
            Thread.yield();
        }


        createBufferStrategy(2);
        sheet = new alkosmen.gfx.SpriteSheet(
                "/alkosmen/images/grass_tileset_16x16/grass_tileset_16x16.png",
                16
        );
        tileFloor = sheet.tile(0, 0);
        tileWall  = sheet.tile(1, 0);
        tileExit  = sheet.tile(2, 0);
        playerSprites = getAlkobotImages();
        enableNextLevelKey();
        loadLevel(1);

    }

    private void enableNextLevelKey() {
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_N) {
                    try {
                        nextLevel();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
    }

    private void nextLevel() throws Exception {
        int next = currentLevel + 1;

        if (next > LEVELS.length) {
            running = false; // или показать "победа"
            return;
        }

        loadLevel(next);
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

    private Image getImage(String fileName) {
        BufferedImage sourceImage;

        try {
            String full = "alkosmen/images/" + fileName; // <-- ВОТ ОНО
            URL url = this.getClass().getClassLoader().getResource(full);
            if (url == null) throw new RuntimeException("Image not found: " + full);

            sourceImage = ImageIO.read(url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Toolkit.getDefaultToolkit().createImage(sourceImage.getSource());
    }

    private void enableKeys() {
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                switch (e.getKeyCode()) {

                    // next level
                    case java.awt.event.KeyEvent.VK_N -> {
                        try {
                            nextLevel();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    // movement (WASD + arrows)
                    case java.awt.event.KeyEvent.VK_W, java.awt.event.KeyEvent.VK_UP -> {
                        playerDir = 0; // up
                        alkosmen.service.Movement.move(player, levelMap, 0, -1);
                    }

                    case java.awt.event.KeyEvent.VK_D, java.awt.event.KeyEvent.VK_RIGHT -> {
                        playerDir = 1; // right
                        alkosmen.service.Movement.move(player, levelMap, 1, 0);
                    }

                    case java.awt.event.KeyEvent.VK_S, java.awt.event.KeyEvent.VK_DOWN -> {
                        playerDir = 2; // down
                        alkosmen.service.Movement.move(player, levelMap, 0, 1);
                    }

                    case java.awt.event.KeyEvent.VK_A, java.awt.event.KeyEvent.VK_LEFT -> {
                        playerDir = 3; // left
                        alkosmen.service.Movement.move(player, levelMap, -1, 0);
                    }

                    default -> {
                        // ничего
                    }
                }
            }
        });
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

        ar[1][0] = getImage("alkright0.png");
        ar[1][1] = getImage("alkright1.png");

        ar[2][0] = getImage("alkdown0.png");
        ar[2][1] = getImage("alkdown1.png");

        ar[3][0] = getImage("alkleft0.png");
        ar[3][1] = getImage("alkleft1.png");

        return ar;
    }

    private void loadLevel(int level) throws Exception {
        if (level < 1 || level > LEVELS.length) {
            throw new IllegalArgumentException("Bad level: " + level);
        }

        String path = LEVELS[level - 1];

        // Проверяем что ресурс реально существует
        URL url = LevelLoader.class.getResource(path);
        System.out.println("Loading level " + level + " from: " + path);
        System.out.println("Resource URL = " + url);

        if (url == null) {
            throw new RuntimeException("Level resource not found: " + path);
        }

        // Грузим карту
        levelMap = LevelLoader.load(path);
        if (levelMap == null) {
            throw new RuntimeException("LevelLoader.load returned NULL for: " + path);
        }
        if (levelMap.length == 0 || levelMap[0].length == 0) {
            throw new RuntimeException("Loaded empty map for: " + path);
        }
        levelMap = MapScaler.scale(levelMap, 3);
        // Ставим текущий уровень
        currentLevel = level;

        // Ищем 'P' и создаём игрока
        player = null;
        for (int y = 0; y < levelMap.length; y++) {
            for (int x = 0; x < levelMap[0].length; x++) {
                if (levelMap[y][x] == 'P') {
                    player = new Player(x, y);
                    levelMap[y][x] = '.'; // превращаем старт в пол
                    break;
                }
            }
            if (player != null) break;
        }

        if (player == null) {
            throw new RuntimeException("No 'P' (player start) in map: " + path);
        }

        // Массив объектов (пока просто создаём, можно позже заполнять)
        objects = new IGameObject[levelMap.length * levelMap[0].length];

        System.out.println("Map size: " + levelMap.length + " x " + levelMap[0].length);
        System.out.println("Player start: " + player.x + "," + player.y);
        java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (w != null) w.pack();

    }


}
