package alkosmen;

import alkosmen.audio.SoundEffectPlayer;
import alkosmen.gfx.SpriteSheet;
import alkosmen.interfaces.IGameObject;
import alkosmen.maps.LevelLoader;
import alkosmen.objects.GameMap;
import alkosmen.objects.Player;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public final class Game extends Canvas implements Runnable {
    private int currentLevel = 1;
    private static boolean running;
    private IGameObject[] objects;
    private BufferStrategy strategy;
    private Image[][] playerSprites;
    private Player player;
    private GameMap gameMap;
    private char[][] levelMap;
    private static final String[] LEVELS = {
            "/alkosmen/maps/level1.txt",
            "/alkosmen/maps/level2.txt",
            "/alkosmen/maps/level3.txt"
    };

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;
    private boolean jumpQueued;
    private int playerDir = 1; // 0 up, 1 right, 2 down, 3 left
    private int animFrame = 0;
    private long lastAnim = 0;
    private SoundEffectPlayer stepSound;
    private SpriteSheet sheet;
    private Image tileFloor;
    private Image tileWall;
    private Image tileExit;

    private float cameraX;
    private float cameraY;

    private static final double MOVE_SPEED = 0.12;
    private static final double GRAVITY = 0.035;
    private static final double JUMP_SPEED = -0.62;
    private static final double MAX_FALL_SPEED = 0.9;

    @Override
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
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Constants.Width, Constants.Height);
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
        strategy = getBufferStrategy();

        sheet = new SpriteSheet(
                "/alkosmen/images/grass_tileset_16x16/grass_tileset_16x16.png",
                16
        );
        tileFloor = sheet.tile(0, 0);
        tileWall = sheet.tile(1, 0);
        tileExit = sheet.tile(2, 0);
        stepSound = new SoundEffectPlayer("/alkosmen/sounds/step.wav");
        playerSprites = getAlkobotImages();

        loadLevel(1);
    }

    private void update() {
        if (player == null || levelMap == null) {
            return;
        }

        double targetVx = 0.0;
        if (leftPressed && !rightPressed) {
            targetVx = -MOVE_SPEED;
            playerDir = 3;
        } else if (rightPressed && !leftPressed) {
            targetVx = MOVE_SPEED;
            playerDir = 1;
        }
        player.vx = targetVx;

        if (jumpQueued && player.onGround) {
            player.vy = JUMP_SPEED;
            player.onGround = false;
        }
        jumpQueued = false;

        player.vy = Math.min(MAX_FALL_SPEED, player.vy + GRAVITY);

        moveHorizontal(player.vx);
        moveVertical(player.vy);

        animatePlayer();
        updateCamera();

        int px = (int) Math.floor(player.x);
        int py = (int) Math.floor(player.y);
        if (isInsideMap(px, py) && levelMap[py][px] == 'E') {
            try {
                nextLevel();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void moveHorizontal(double dx) {
        if (dx == 0.0) {
            return;
        }

        double nx = player.x + dx;
        int tx = (int) Math.floor(nx);
        int ty = (int) Math.floor(player.y);

        if (isSolid(tx, ty)) {
            if (dx > 0) {
                player.x = tx - 0.001;
            } else {
                player.x = tx + 1.001;
            }
            player.vx = 0.0;
        } else {
            player.x = nx;
        }
    }

    private void moveVertical(double dy) {
        if (dy == 0.0) {
            return;
        }

        double ny = player.y + dy;
        int tx = (int) Math.floor(player.x);
        int ty = (int) Math.floor(ny);

        if (isSolid(tx, ty)) {
            if (dy > 0) {
                player.y = ty - 0.001;
                player.onGround = true;
            } else {
                player.y = ty + 1.001;
            }
            player.vy = 0.0;
        } else {
            player.y = ny;
            player.onGround = false;
        }
    }

    private void animatePlayer() {
        boolean isWalking = Math.abs(player.vx) > 0.0001 && player.onGround;
        long now = System.currentTimeMillis();
        if (isWalking && now - lastAnim > 120) {
            animFrame = 1 - animFrame;
            lastAnim = now;
            stepSound.play();
        }
        if (!isWalking) {
            animFrame = 0;
        }
    }

    private void updateCamera() {
        int cell = Constants.Size;
        float worldPx = (float) (player.x * cell);
        float worldPy = (float) (player.y * cell);

        int mapPxW = levelMap[0].length * cell;
        int mapPxH = levelMap.length * cell;

        cameraX = worldPx - getWidth() / 2f + cell / 2f;
        cameraY = worldPy - getHeight() / 2f + cell / 2f;

        float maxX = Math.max(0, mapPxW - getWidth());
        float maxY = Math.max(0, mapPxH - getHeight());
        cameraX = clamp(cameraX, 0, maxX);
        cameraY = clamp(cameraY, 0, maxY);
    }

    private void render() {
        BufferStrategy bs = strategy;
        if (bs == null) {
            return;
        }

        Graphics g = bs.getDrawGraphics();

        g.setColor(new Color(120, 190, 255));
        g.fillRect(0, 0, getWidth(), getHeight());

        if (levelMap != null) {
            int cell = Constants.Size;
            int firstTileX = Math.max(0, (int) (cameraX / cell));
            int firstTileY = Math.max(0, (int) (cameraY / cell));
            int visibleX = getWidth() / cell + 3;
            int visibleY = getHeight() / cell + 3;
            int lastTileX = Math.min(levelMap[0].length, firstTileX + visibleX);
            int lastTileY = Math.min(levelMap.length, firstTileY + visibleY);

            for (int y = firstTileY; y < lastTileY; y++) {
                for (int x = firstTileX; x < lastTileX; x++) {
                    char c = levelMap[y][x];
                    if (c == '.') {
                        continue;
                    }

                    Image img = c == '#' ? tileWall : c == 'E' ? tileExit : tileFloor;
                    int drawX = x * cell - (int) cameraX;
                    int drawY = y * cell - (int) cameraY;
                    g.drawImage(img, drawX, drawY, cell, cell, null);
                }
            }
        }

        if (player != null && playerSprites != null) {
            int cell = Constants.Size;
            int drawX = (int) Math.round(player.x * cell - cameraX);
            int drawY = (int) Math.round(player.y * cell - cameraY);
            Image img = playerSprites[playerDir][animFrame];
            g.drawImage(img, drawX, drawY, cell, cell, null);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.BOLD, 20));
        g.drawString("LEVEL: " + currentLevel, 14, 26);
        g.drawString("A/D or arrows + SPACE", 14, 50);

        g.dispose();
        bs.show();
    }

    private void enableKeys() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_A, KeyEvent.VK_LEFT -> leftPressed = true;
                    case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> rightPressed = true;
                    case KeyEvent.VK_SPACE, KeyEvent.VK_W, KeyEvent.VK_UP -> {
                        jumpPressed = true;
                        jumpQueued = true;
                    }
                    case KeyEvent.VK_N -> {
                        try {
                            nextLevel();
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    default -> {
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_A, KeyEvent.VK_LEFT -> leftPressed = false;
                    case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> rightPressed = false;
                    case KeyEvent.VK_SPACE, KeyEvent.VK_W, KeyEvent.VK_UP -> jumpPressed = false;
                    default -> {
                    }
                }
            }
        });
    }

    private void nextLevel() throws Exception {
        int next = currentLevel + 1;
        if (next > LEVELS.length) {
            running = false;
            return;
        }
        loadLevel(next);
    }

    public void start() {
        running = true;
        new Thread(this).start();
    }

    public static void stopGame() {
        running = false;
    }

    private boolean isInsideMap(int x, int y) {
        return y >= 0 && y < levelMap.length && x >= 0 && x < levelMap[0].length;
    }

    private boolean isSolid(int x, int y) {
        if (!isInsideMap(x, y)) {
            return true;
        }
        return levelMap[y][x] == '#';
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private Image getImage(String fileName) {
        BufferedImage sourceImage;

        try {
            String full = "alkosmen/images/" + fileName;
            URL url = this.getClass().getClassLoader().getResource(full);
            if (url == null) {
                throw new RuntimeException("Image not found: " + full);
            }
            sourceImage = ImageIO.read(url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Toolkit.getDefaultToolkit().createImage(sourceImage.getSource());
    }

    private Image[][] getAlkobotImages() {
        Image[][] ar = new Image[4][2];
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
        URL url = LevelLoader.class.getResource(path);
        if (url == null) {
            throw new RuntimeException("Level resource not found: " + path);
        }

        levelMap = LevelLoader.load(path);
        if (levelMap == null) {
            throw new RuntimeException("LevelLoader.load returned NULL for: " + path);
        }
        if (levelMap.length == 0 || levelMap[0].length == 0) {
            throw new RuntimeException("Loaded empty map for: " + path);
        }

        currentLevel = level;
        player = null;

        for (int y = 0; y < levelMap.length; y++) {
            for (int x = 0; x < levelMap[0].length; x++) {
                if (levelMap[y][x] == 'P') {
                    player = new Player(x, y);
                    levelMap[y][x] = '.';
                    break;
                }
            }
            if (player != null) {
                break;
            }
        }

        if (player == null) {
            throw new RuntimeException("No 'P' (player start) in map: " + path);
        }

        playerDir = 1;
        animFrame = 0;
        cameraX = 0;
        cameraY = 0;
        jumpPressed = false;
        jumpQueued = false;

        objects = new IGameObject[levelMap.length * levelMap[0].length];
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) {
            w.pack();
        }
    }
}
