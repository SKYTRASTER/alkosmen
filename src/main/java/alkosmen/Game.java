package alkosmen;

import alkosmen.audio.MidiPlayer;
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
    private int score = 0;
    private int bottleGoal = 8;
    private static boolean running;
    private IGameObject[] objects;
    private BufferStrategy strategy;
    private Image[][] playerSprites;
    private Player player;
    private GameMap gameMap;
    private char[][] levelMap;
    private static final String[] LEVELS = {
            "/alkosmen/maps/demo_level.txt"
    };
    private static final String[] LEVEL_BACKGROUNDS = {
            "/alkosmen/images/objects/maps/demo_map.png"
    };

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;
    private boolean jumpQueued;
    private int playerDir = 1; // 0 up, 1 right, 2 down, 3 left
    private int animFrame = 0;
    private long lastAnim = 0;
    private SoundEffectPlayer stepSound;
    private SoundEffectPlayer jumpSound;
    private SoundEffectPlayer bottleCollectSound;
    private SpriteSheet sheet;
    private Image tileFloor;
    private Image tileWall;
    private Image levelBackground;
    private Image bottleSprite;
    private Image npcBoy1Sprite;
    private Image npcBoy2Sprite;
    private Image npcCopSprite;
    private MidiPlayer levelMidi;

    private float cameraX;
    private float cameraY;

    private static final double MOVE_SPEED = 0.12;
    private static final double GRAVITY = 0.035;
    private static final double JUMP_SPEED = -0.68;
    private static final double MAX_FALL_SPEED = 0.9;
    // While jump key is held and player is moving up, gravity is reduced.
    private static final double JUMP_HOLD_GRAVITY_MULT = 0.55;
    private static final double PLAYER_SCALE = 1.95;
    private static final double BOTTLE_SCALE = 2.2;
    private static final double NPC_SCALE = 1.7;
    // Demo task is intentionally small to keep level accessible.
    private static final int DEFAULT_BOTTLE_GOAL = 8;
    // Bottom HUD height; gameplay camera/render should not overlap this zone.
    private static final int HUD_HEIGHT = 56;

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

        if (levelMidi != null) {
            levelMidi.stop();
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
        bottleSprite = loadImageResource("/alkosmen/images/objects/bottle/bottle_tich_gold.png");
        npcBoy1Sprite = loadImageResource("/alkosmen/images/objects/boy/boy1.png");
        npcBoy2Sprite = loadImageResource("/alkosmen/images/objects/boy/boy2.png");
        npcCopSprite = loadImageResource("/alkosmen/images/objects/cop/copdown0.png");
        stepSound = new SoundEffectPlayer("/alkosmen/sounds/step.wav");
        jumpSound = new SoundEffectPlayer("/alkosmen/sounds/jump.wav");
        bottleCollectSound = new SoundEffectPlayer("/alkosmen/sounds/scratch_bottle.wav");
        levelMidi = new MidiPlayer();
        levelMidi.playLoop("/alkosmen/sounds/Caribbean-Blue.mid", 70);
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
            // Initial jump impulse applies only from ground.
            player.vy = JUMP_SPEED;
            player.onGround = false;
            jumpSound.play();
        }
        jumpQueued = false;

        double gravityStep = GRAVITY;
        if (jumpPressed && player.vy < 0) {
            // Variable jump height: hold key to jump a bit higher.
            gravityStep *= JUMP_HOLD_GRAVITY_MULT;
        }
        player.vy = Math.min(MAX_FALL_SPEED, player.vy + gravityStep);

        moveHorizontal(player.vx);
        moveVertical(player.vy);

        animatePlayer();
        updateCamera();

        int px = (int) Math.floor(player.x);
        int py = (int) Math.floor(player.y);
        if (isInsideMap(px, py) && levelMap[py][px] == 'B') {
            // Collect bottle on overlap: remove tile, add score, play SFX.
            levelMap[py][px] = '.';
            score++;
            bottleCollectSound.play();
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
        int gameplayHeight = Math.max(1, getHeight() - HUD_HEIGHT);

        int mapPxW = levelMap[0].length * cell;
        int mapPxH = levelMap.length * cell;

        cameraX = worldPx - getWidth() / 2f + cell / 2f;
        cameraY = worldPy - gameplayHeight / 2f + cell / 2f;

        float maxX = Math.max(0, mapPxW - getWidth());
        float maxY = Math.max(0, mapPxH - gameplayHeight);
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
            if (levelBackground != null) {
                int mapPxW = levelMap[0].length * cell;
                int mapPxH = levelMap.length * cell;
                g.drawImage(levelBackground, -(int) cameraX, -(int) cameraY, mapPxW, mapPxH, null);
            }

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

                    if (levelBackground != null && c != 'B' && !isNpcTile(c)) {
                        continue;
                    }

                    int drawX = x * cell - (int) cameraX;
                    int drawY = y * cell - (int) cameraY;

                    if (c == 'B' && bottleSprite != null) {
                        int bottleW = (int) Math.round(cell * BOTTLE_SCALE);
                        int bottleH = (int) Math.round(cell * BOTTLE_SCALE);
                        int bottleX = drawX - (bottleW - cell) / 2;
                        int bottleY = drawY - (bottleH - cell);
                        g.drawImage(bottleSprite, bottleX, bottleY, bottleW, bottleH, null);
                        continue;
                    }

                    if (isNpcTile(c)) {
                        Image npc = npcImageFor(c);
                        if (npc != null) {
                            int npcW = (int) Math.round(cell * NPC_SCALE);
                            int npcH = (int) Math.round(cell * NPC_SCALE);
                            int npcX = drawX - (npcW - cell) / 2;
                            int npcY = drawY - (npcH - cell);
                            g.drawImage(npc, npcX, npcY, npcW, npcH, null);
                        }
                        continue;
                    }

                    Image img = c == '#' ? tileWall : tileFloor;
                    g.drawImage(img, drawX, drawY, cell, cell, null);
                }
            }
        }

        if (player != null && playerSprites != null) {
            int cell = Constants.Size;
            int playerW = (int) Math.round(cell * PLAYER_SCALE);
            int playerH = (int) Math.round(cell * PLAYER_SCALE);
            int drawX = (int) Math.round(player.x * cell - cameraX - (playerW - cell) / 2.0);
            int drawY = (int) Math.round(player.y * cell - cameraY - (playerH - cell));
            int maxPlayerY = getHeight() - HUD_HEIGHT - playerH;
            if (drawY > maxPlayerY) {
                drawY = maxPlayerY;
            }
            Image img = playerSprites[playerDir][animFrame];
            g.drawImage(img, drawX, drawY, playerW, playerH, null);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Serif", Font.BOLD, 20));
        g.drawString("LEVEL: " + currentLevel, 14, 26);
        g.drawString("A/D or arrows + SPACE", 14, 50);
        drawDoomStyleHud(g);

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
                        // Demo mode: single level only.
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
            String full = "alkosmen/images/objects/alkoman/" + fileName;
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

    private Image loadImageResource(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        URL url = this.getClass().getClassLoader().getResource(normalized);
        if (url == null) {
            throw new RuntimeException("Image not found: " + path);
        }
        return Toolkit.getDefaultToolkit().createImage(url);
    }

    private void drawDoomStyleHud(Graphics g) {
        // Simple retro HUD bar anchored to the bottom of the viewport.
        int y = getHeight() - HUD_HEIGHT;

        g.setColor(new Color(28, 22, 20));
        g.fillRect(0, y, getWidth(), HUD_HEIGHT);

        g.setColor(new Color(92, 74, 58));
        g.drawLine(0, y, getWidth(), y);
        g.drawLine(0, y + 1, getWidth(), y + 1);

        g.setColor(new Color(160, 130, 96));
        g.setFont(new Font("Monospaced", Font.BOLD, 26));
        g.drawString("SCORE " + score, 16, y + 38);

        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.setColor(score >= bottleGoal ? new Color(120, 220, 120) : new Color(220, 210, 170));
        g.drawString("BOTTLES " + score + "/" + bottleGoal, 260, y + 35);
        if (score >= bottleGoal) {
            g.drawString("TASK DONE", 470, y + 35);
        }
    }

    private boolean isNpcTile(char c) {
        return c == 'N' || c == 'M' || c == 'C';
    }

    private Image npcImageFor(char c) {
        return switch (c) {
            case 'N' -> npcBoy1Sprite;
            case 'M' -> npcBoy2Sprite;
            case 'C' -> npcCopSprite;
            default -> null;
        };
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

        String backgroundPath = LEVEL_BACKGROUNDS[level - 1];
        levelBackground = backgroundPath == null ? null : loadImageResource(backgroundPath);

        currentLevel = level;
        score = 0;
        bottleGoal = Math.min(DEFAULT_BOTTLE_GOAL, countTiles('B'));
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

    private int countTiles(char target) {
        int count = 0;
        for (int y = 0; y < levelMap.length; y++) {
            for (int x = 0; x < levelMap[0].length; x++) {
                if (levelMap[y][x] == target) {
                    count++;
                }
            }
        }
        return count;
    }
}

