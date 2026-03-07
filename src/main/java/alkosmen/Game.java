package alkosmen;

import alkosmen.audio.MidiPlayer;
import alkosmen.settings.Constants;
import alkosmen.audio.SoundEffectPlayer;
import alkosmen.game.CopSystem;
import alkosmen.game.GameHudRenderer;
import alkosmen.gfx.SpriteSheet;
import alkosmen.maps.LevelLoader;
import alkosmen.objects.Player;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ThreadLocalRandom;

public final class Game extends Canvas implements Runnable {
    private int currentLevel = 1;
    private int score = 0;
    private int bottleGoal = 8;
    private volatile boolean running;
    private BufferStrategy strategy;
    private Image[][] playerSprites;
    private Player player;
    private char[][] levelMap;
    private static final String[] LEVELS = {
            "/alkosmen/maps/demo_level.txt"
    };
    private static final String[] LEVEL_BACKGROUNDS = {
            "/alkosmen/images/objects/maps/demo_map_grid_48x32.png"
    };

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;
    private boolean jumpQueued;
    private long jumpBufferUntil;
    private long lastOnGroundAt;
    private int playerDir = 2; // 0 left, 1 right, 2 idle/dance
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
    private Image[] copWalkLeftFrames;
    private Image[] copWalkRightFrames;
    private MidiPlayer levelMidi;

    private float cameraX;
    private float cameraY;
    private final GameHudRenderer hudRenderer = new GameHudRenderer(HUD_HEIGHT, COP_CAUGHT_TEXT_MS);
    private final CopSystem copSystem = new CopSystem(COP_SPEED, COP_DROP_STEP, COP_VIEW_DISTANCE, COP_CAUGHT_COOLDOWN_MS);
    private int playerSpawnX;
    private int playerSpawnY;
    private boolean hidePressed;
    private boolean gameOver;
    private int lives = MAX_LIVES;
    private boolean spectatorMode;

    private static final double MOVE_SPEED = 0.12;
    private static final double GRAVITY = 0.035;
    private static final double JUMP_SPEED = -0.68;
    private static final double MAX_FALL_SPEED = 0.9;
    private static final long JUMP_BUFFER_MS = 140;
    private static final long COYOTE_TIME_MS = 120;
    // While jump key is held and player is moving up, gravity is reduced.
    private static final double JUMP_HOLD_GRAVITY_MULT = 0.55;
    private static final double PLAYER_SCALE = 1.95;
    private static final double BOTTLE_SCALE = 2.2;
    private static final double NPC_SCALE = 1.7;
    // Bottom HUD height; gameplay camera/render should not overlap this zone.
    private static final int HUD_HEIGHT = 56;
    // Cop patrol tuning: horizontal speed, drop distance on turn, and sight range.
    private static final double COP_SPEED = 0.045;
    private static final double COP_DROP_STEP = 1.0;
    private static final double COP_VIEW_DISTANCE = 4.5;
    private static final long COP_CAUGHT_COOLDOWN_MS = 900;
    private static final long COP_CAUGHT_TEXT_MS = 1200;
    private static final long FRAME_DELAY_MS = 16L;
    private static final int MAX_LIVES = 3;
    private static final int DEMO_RANDOM_COPS = 12;
    private static final int DEMO_RANDOM_COP_ATTEMPTS = 800;
    private static final int COP_WALK_FRAME_COUNT = 8;

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
                Thread.sleep(FRAME_DELAY_MS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        stopAudio();
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
        renderLoadingScreen("Loading assets...");

        sheet = new SpriteSheet(
                "/alkosmen/images/grass_tileset_16x16/grass_tileset_16x16.png",
                16
        );
        tileFloor = sheet.tile(0, 0);
        tileWall = sheet.tile(1, 0);
        bottleSprite = loadImageResource("/alkosmen/images/objects/bottle/bottle_tich_gold.png");
        npcBoy1Sprite = loadFirstExistingImage(
                "/alkosmen/images/objects/glack/boy1.png",
                "/alkosmen/images/objects/boy/boy1.png"
        );
        npcBoy2Sprite = loadFirstExistingImage(
                "/alkosmen/images/objects/glack/boy2.png",
                "/alkosmen/images/objects/boy/boy2.png"
        );
        npcCopSprite = loadImageResource("/alkosmen/images/objects/cop/copdown0.png");
        copWalkLeftFrames = loadCopTrackFrames("walk_left");
        copWalkRightFrames = loadCopTrackFrames("walk_right");
        stepSound = new SoundEffectPlayer("/alkosmen/sounds/step.wav");
        jumpSound = new SoundEffectPlayer("/alkosmen/sounds/jump.wav");
        bottleCollectSound = new SoundEffectPlayer("/alkosmen/sounds/scratch_bottle.wav");
        levelMidi = new MidiPlayer();
        if (Constants.GameMusicEnabled) {
            levelMidi.playLoop("/alkosmen/sounds/Caribbean-Blue.mid", 70);
        }
        playerSprites = getAlkobotImages();
        renderLoadingScreen("Loading level...");

        loadLevel(1);
    }

    private void update() {
        if (player == null || levelMap == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (player.onGround) {
            lastOnGroundAt = now;
        }

        if (!spectatorMode) {
            double targetVx = 0.0;
            if (leftPressed && !rightPressed) {
                targetVx = -MOVE_SPEED;
                playerDir = 0;
            } else if (rightPressed && !leftPressed) {
                targetVx = MOVE_SPEED;
                playerDir = 1;
            } else {
                playerDir = 2;
            }
            player.vx = targetVx;

            boolean hasBufferedJump = jumpQueued || now <= jumpBufferUntil;
            boolean canJumpNow = player.onGround || (now - lastOnGroundAt <= COYOTE_TIME_MS);
            if (hasBufferedJump && canJumpNow) {
                // Initial jump impulse applies only from ground.
                player.vy = JUMP_SPEED;
                player.onGround = false;
                jumpSound.play();
                lastOnGroundAt = 0L;
                jumpBufferUntil = 0L;
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
        }

        animatePlayer();
        CopSystem.Outcome copOutcome = copSystem.tick(levelMap, player, isPlayerHidden(), now, spectatorMode);
        if (copOutcome == CopSystem.Outcome.GAME_OVER) {
            lives = 0;
            gameOver = true;
            running = false;
            return;
        }
        if (copOutcome == CopSystem.Outcome.CAUGHT) {
            lives = Math.max(0, lives - 1);
            if (lives == 0) {
                gameOver = true;
                running = false;
                return;
            }
            respawnPlayer();
        }
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
        int frameCount = playerSprites != null && playerDir >= 0 && playerDir < playerSprites.length
                ? playerSprites[playerDir].length
                : 0;
        if (frameCount <= 0) {
            return;
        }
        if (isWalking && now - lastAnim > 90) {
            animFrame = (animFrame + 1) % frameCount;
            lastAnim = now;
            if ((animFrame % 3) == 0) {
                stepSound.play();
            }
        }
        if (!isWalking && now - lastAnim > 110) {
            animFrame = (animFrame + 1) % frameCount;
            lastAnim = now;
        }
    }

    private void updateCamera() {
        int cell = Constants.Size;
        double focusX = spectatorMode && copSystem.hasCops() ? copSystem.getFocusX() : player.x;
        double focusY = spectatorMode && copSystem.hasCops() ? copSystem.getFocusY() : player.y;
        float worldPx = (float) (focusX * cell);
        float worldPy = (float) (focusY * cell);
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

        if (levelMap != null) {
            int cell = Constants.Size;
            copSystem.draw(
                    g,
                    cell,
                    NPC_SCALE,
                    npcCopSprite,
                    copWalkLeftFrames,
                    copWalkRightFrames,
                    cameraX,
                    cameraY,
                    System.currentTimeMillis()
            );
        }

        if (!spectatorMode && player != null && playerSprites != null) {
            int cell = Constants.Size;
            int playerW = (int) Math.round(cell * PLAYER_SCALE);
            int playerH = (int) Math.round(cell * PLAYER_SCALE);
            int drawX = (int) Math.round(player.x * cell - cameraX - (playerW - cell) / 2.0);
            int drawY = (int) Math.round(player.y * cell - cameraY - (playerH - cell));
            int maxPlayerY = getHeight() - HUD_HEIGHT - playerH;
            if (drawY > maxPlayerY) {
                drawY = maxPlayerY;
            }
            Image[] track = playerSprites[playerDir];
            Image img = track[Math.floorMod(animFrame, track.length)];
            // Hidden player remains visible with low alpha for gameplay readability.
            if (isPlayerHidden()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.33f));
                g2.drawImage(img, drawX, drawY, playerW, playerH, null);
                g2.dispose();
            } else {
                g.drawImage(img, drawX, drawY, playerW, playerH, null);
            }
        }

        hudRenderer.drawHud(
                g,
                getWidth(),
                getHeight(),
                currentLevel,
                score,
                bottleGoal,
                lives,
                MAX_LIVES,
                isPlayerHidden(),
                gameOver,
                copSystem.getLastCaughtAt(),
                System.currentTimeMillis()
        );
        hudRenderer.drawGameOverOverlay(g, getWidth(), getHeight(), gameOver);

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
                    case KeyEvent.VK_SPACE -> {
                        jumpPressed = true;
                        jumpQueued = true;
                        jumpBufferUntil = System.currentTimeMillis() + JUMP_BUFFER_MS;
                    }
                    // Hold S/Down to hide from cop detection.
                    case KeyEvent.VK_S, KeyEvent.VK_DOWN -> hidePressed = true;
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
                    case KeyEvent.VK_SPACE -> jumpPressed = false;
                    case KeyEvent.VK_S, KeyEvent.VK_DOWN -> hidePressed = false;
                    default -> {
                    }
                }
            }
        });
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        Thread loopThread = new Thread(this, "alkosmen-game-loop");
        loopThread.start();
    }

    public void stopGame() {
        running = false;
    }

    private void stopAudio() {
        if (levelMidi != null) {
            levelMidi.stop();
        }
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

        return sourceImage;
    }

    private Image loadImageResource(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        URL url = this.getClass().getClassLoader().getResource(normalized);
        if (url == null) {
            throw new RuntimeException("Image not found: " + path);
        }
        try {
            return ImageIO.read(url);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image: " + path, e);
        }
    }

    private Image loadFirstExistingImage(String... paths) {
        for (String path : paths) {
            String normalized = path.startsWith("/") ? path.substring(1) : path;
            URL url = this.getClass().getClassLoader().getResource(normalized);
            if (url != null) {
                try {
                    return ImageIO.read(url);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read image: " + path, e);
                }
            }
        }
        throw new RuntimeException("Image not found. Tried: " + String.join(", ", paths));
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
        try {
            return new Image[][]{
                    loadTrackFrames("left"),
                    loadTrackFrames("right"),
                    loadTrackFrames("idle")
            };
        } catch (RuntimeException ex) {
            // Fallback to legacy 4x2 sprites if frame sequence is missing.
            Image[][] ar = new Image[3][2];
            ar[0][0] = getImage("alkleft0.png");
            ar[0][1] = getImage("alkleft1.png");

            ar[1][0] = getImage("alkright0.png");
            ar[1][1] = getImage("alkright1.png");

            ar[2][0] = getImage("alkdown0.png");
            ar[2][1] = getImage("alkdown1.png");
            return ar;
        }
    }

    private Image[] loadTrackFrames(String trackName) {
        Image[] frames = new Image[10];
        for (int i = 0; i < frames.length; i++) {
            String framePath = String.format("/alkosmen/images/objects/alkoman/frames/alk_%s_%02d.png", trackName, i);
            frames[i] = loadImageResource(framePath);
        }
        return frames;
    }

    private Image[] loadCopTrackFrames(String trackName) {
        Image[] frames = new Image[COP_WALK_FRAME_COUNT];
        for (int i = 0; i < frames.length; i++) {
            String framePath = String.format("/alkosmen/images/objects/cop/male/%s/%02d.png", trackName, i);
            frames[i] = loadImageResource(framePath);
        }
        return frames;
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
        bottleGoal = countTiles('B');
        player = null;
        copSystem.reset();

        // Scan full map: spawn player and convert all cop markers into dynamic NPCs.
        for (int y = 0; y < levelMap.length; y++) {
            for (int x = 0; x < levelMap[0].length; x++) {
                if (levelMap[y][x] == 'P') {
                    playerSpawnX = x;
                    playerSpawnY = y;
                    player = new Player(x, y);
                    levelMap[y][x] = '.';
                } else if (levelMap[y][x] == 'C') {
                    copSystem.addCop(x, y);
                    levelMap[y][x] = '.';
                }
            }
        }

        if (player == null) {
            throw new RuntimeException("No 'P' (player start) in map: " + path);
        }

        spectatorMode = !copSystem.hasCops();
        if (spectatorMode) {
            spawnRandomCops(DEMO_RANDOM_COPS);
        }

        playerDir = 2;
        animFrame = 0;
        cameraX = 0;
        cameraY = 0;
        jumpPressed = false;
        jumpQueued = false;
        jumpBufferUntil = 0L;
        lastOnGroundAt = 0L;
        hidePressed = false;
        gameOver = false;
        lives = MAX_LIVES;

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

    private void respawnPlayer() {
        // After getting spotted, reset position and clear movement/hide inputs.
        player.x = playerSpawnX;
        player.y = playerSpawnY;
        player.vx = 0.0;
        player.vy = 0.0;
        player.onGround = false;
        leftPressed = false;
        rightPressed = false;
        jumpPressed = false;
        jumpQueued = false;
        hidePressed = false;
    }

    private boolean isPlayerHidden() {
        if (spectatorMode) {
            return false;
        }
        // Player can hide only while standing still on ground and holding hide key.
        return hidePressed && player != null && Math.abs(player.vx) < 0.0001 && player.onGround;
    }

    private void spawnRandomCops(int targetCount) {
        int h = levelMap.length;
        int w = levelMap[0].length;
        int added = 0;
        for (int i = 0; i < DEMO_RANDOM_COP_ATTEMPTS && added < targetCount; i++) {
            int x = ThreadLocalRandom.current().nextInt(1, Math.max(2, w - 1));
            int y = ThreadLocalRandom.current().nextInt(1, Math.max(2, h - 1));
            if (levelMap[y][x] != '.') {
                continue;
            }
            if (y + 1 >= h || levelMap[y + 1][x] != '#') {
                continue;
            }
            copSystem.addCop(x, y);
            added++;
        }
    }

    private void renderLoadingScreen(String text) {
        BufferStrategy bs = strategy;
        if (bs == null) {
            return;
        }
        Graphics g = bs.getDrawGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 36));
        int textW = g.getFontMetrics().stringWidth(text);
        int textX = Math.max(12, (getWidth() - textW) / 2);
        int textY = Math.max(48, getHeight() / 2);
        g.drawString(text, textX, textY);
        g.dispose();
        bs.show();
    }
}




