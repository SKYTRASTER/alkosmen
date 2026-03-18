package alkosmen.shared.game;

public final class GameCore {
    public static final double MOVE_SPEED = 0.12;
    public static final double GRAVITY = 0.035;
    public static final double JUMP_SPEED = -0.68;
    public static final double MAX_FALL_SPEED = 0.9;
    public static final long JUMP_BUFFER_MS = 140;
    public static final long COYOTE_TIME_MS = 120;
    public static final double JUMP_HOLD_GRAVITY_MULT = 0.55;

    private final char[][] levelMap;
    private final GameState state;
    private long lastOnGroundAt;
    private long jumpBufferUntil;
    private boolean jumpQueued;

    public GameCore(char[][] levelMap) {
        if (levelMap == null || levelMap.length == 0 || levelMap[0].length == 0) {
            throw new IllegalArgumentException("levelMap must be rectangular and not empty");
        }
        this.levelMap = levelMap;
        this.state = new GameState();
        initFromMap();
    }

    public GameState state() {
        return state;
    }

    public char[][] map() {
        return levelMap;
    }

    public void tick(GameInput input, long nowMs) {
        if (state.playerOnGround) {
            lastOnGroundAt = nowMs;
        }

        double targetVx = 0.0;
        if (input.leftPressed && !input.rightPressed) {
            targetVx = -MOVE_SPEED;
            state.playerDir = 0;
        } else if (input.rightPressed && !input.leftPressed) {
            targetVx = MOVE_SPEED;
            state.playerDir = 1;
        } else {
            state.playerDir = 2;
        }
        state.playerVx = targetVx;

        if (input.jumpJustPressed) {
            jumpQueued = true;
            jumpBufferUntil = nowMs + JUMP_BUFFER_MS;
        }

        boolean hasBufferedJump = jumpQueued || nowMs <= jumpBufferUntil;
        boolean canJumpNow = state.playerOnGround || (nowMs - lastOnGroundAt <= COYOTE_TIME_MS);
        if (hasBufferedJump && canJumpNow) {
            state.playerVy = JUMP_SPEED;
            state.playerOnGround = false;
            lastOnGroundAt = 0L;
            jumpBufferUntil = 0L;
        }
        jumpQueued = false;

        double gravityStep = GRAVITY;
        if (input.jumpPressed && state.playerVy < 0) {
            gravityStep *= JUMP_HOLD_GRAVITY_MULT;
        }
        state.playerVy = Math.min(MAX_FALL_SPEED, state.playerVy + gravityStep);

        moveHorizontal(state.playerVx);
        moveVertical(state.playerVy);

        int px = (int) Math.floor(state.playerX);
        int py = (int) Math.floor(state.playerY);
        if (isInsideMap(px, py) && levelMap[py][px] == 'B') {
            levelMap[py][px] = '.';
            state.score++;
        }
    }

    public boolean isPlayerHidden(boolean hidePressed) {
        return hidePressed && Math.abs(state.playerVx) < 0.0001 && state.playerOnGround;
    }

    private void initFromMap() {
        int bottles = 0;
        boolean foundSpawn = false;
        for (int y = 0; y < levelMap.length; y++) {
            for (int x = 0; x < levelMap[0].length; x++) {
                char t = levelMap[y][x];
                if (t == 'B') {
                    bottles++;
                } else if (t == 'P') {
                    state.spawnX = x;
                    state.spawnY = y;
                    state.playerX = x;
                    state.playerY = y;
                    levelMap[y][x] = '.';
                    foundSpawn = true;
                } else if (t == 'C') {
                    // Shared core keeps cop spawns neutral for now.
                    levelMap[y][x] = '.';
                }
            }
        }
        if (!foundSpawn) {
            throw new IllegalArgumentException("Map has no 'P' spawn tile");
        }
        state.bottleGoal = bottles;
        state.playerDir = 2;
    }

    private void moveHorizontal(double dx) {
        if (dx == 0.0) {
            return;
        }
        double nx = state.playerX + dx;
        int tx = (int) Math.floor(nx);
        int ty = (int) Math.floor(state.playerY);
        if (isSolid(tx, ty)) {
            if (dx > 0) {
                state.playerX = tx - 0.001;
            } else {
                state.playerX = tx + 1.001;
            }
            state.playerVx = 0.0;
            return;
        }
        state.playerX = nx;
    }

    private void moveVertical(double dy) {
        if (dy == 0.0) {
            return;
        }
        double ny = state.playerY + dy;
        int tx = (int) Math.floor(state.playerX);
        int ty = (int) Math.floor(ny);
        if (isSolid(tx, ty)) {
            if (dy > 0) {
                state.playerY = ty - 0.001;
                state.playerOnGround = true;
            } else {
                state.playerY = ty + 1.001;
            }
            state.playerVy = 0.0;
            return;
        }
        state.playerY = ny;
        state.playerOnGround = false;
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
}
