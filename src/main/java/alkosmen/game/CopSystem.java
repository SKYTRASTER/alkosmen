package alkosmen.game;

import alkosmen.objects.Player;

import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class CopSystem {
    private static final double COLLISION_X = 0.55;
    private static final double COLLISION_Y = 0.75;
    private static final double LANE_TOLERANCE = 0.75;
    private static final long WALK_FRAME_MS = 90L;

    private final List<CopNpc> cops = new ArrayList<>();
    private final double speed;
    private final double dropStep;
    private final double viewDistance;
    private final long caughtCooldownMs;
    private long lastCaughtAt;

    public CopSystem(double speed, double dropStep, double viewDistance, long caughtCooldownMs) {
        this.speed = speed;
        this.dropStep = dropStep;
        this.viewDistance = viewDistance;
        this.caughtCooldownMs = caughtCooldownMs;
    }

    public void reset() {
        cops.clear();
        lastCaughtAt = 0L;
    }

    public void addCop(int x, int y) {
        cops.add(new CopNpc(x, y));
    }

    public long getLastCaughtAt() {
        return lastCaughtAt;
    }

    public Outcome tick(char[][] levelMap, Player player, boolean playerHidden, long now, boolean observerMode) {
        updatePatrol(levelMap);
        if (observerMode) {
            return Outcome.NONE;
        }
        if (hasCollision(player)) {
            return Outcome.GAME_OVER;
        }
        if (playerHidden || now - lastCaughtAt < caughtCooldownMs) {
            return Outcome.NONE;
        }
        for (CopNpc cop : cops) {
            if (canSeePlayer(levelMap, cop, player)) {
                lastCaughtAt = now;
                return Outcome.CAUGHT;
            }
        }
        return Outcome.NONE;
    }

    public void draw(
            Graphics g,
            int cell,
            double npcScale,
            Image npcSprite,
            Image[] walkLeftFrames,
            Image[] walkRightFrames,
            float cameraX,
            float cameraY,
            long now
    ) {
        if (npcSprite == null) {
            return;
        }
        int frameIndex = Math.floorMod((int) (now / WALK_FRAME_MS), 8);
        boolean hasLeftFrames = walkLeftFrames != null && walkLeftFrames.length > 0;
        boolean hasRightFrames = walkRightFrames != null && walkRightFrames.length > 0;
        for (CopNpc cop : cops) {
            int copW = (int) Math.round(cell * npcScale);
            int copH = (int) Math.round(cell * npcScale);
            int drawX = (int) Math.round(cop.x * cell - cameraX - (copW - cell) / 2.0);
            int drawY = (int) Math.round(cop.y * cell - cameraY - (copH - cell));
            Image sprite = npcSprite;
            if (cop.dir < 0 && hasLeftFrames) {
                sprite = walkLeftFrames[Math.floorMod(frameIndex, walkLeftFrames.length)];
            } else if (cop.dir > 0 && hasRightFrames) {
                sprite = walkRightFrames[Math.floorMod(frameIndex, walkRightFrames.length)];
            }
            g.drawImage(sprite, drawX, drawY, copW, copH, null);
        }
    }

    public boolean hasCops() {
        return !cops.isEmpty();
    }

    public double getFocusX() {
        return cops.isEmpty() ? 0.0 : cops.get(0).x;
    }

    public double getFocusY() {
        return cops.isEmpty() ? 0.0 : cops.get(0).y;
    }

    private void updatePatrol(char[][] levelMap) {
        for (CopNpc cop : cops) {
            if (ThreadLocalRandom.current().nextDouble() < 0.012) {
                cop.dir *= -1;
            }
            double nx = cop.x + cop.dir * speed;
            if (isBlocked(levelMap, nx, cop.y)) {
                cop.dir *= -1;
                double ny = cop.y + dropStep;
                if (!isBlocked(levelMap, cop.x, ny)) {
                    cop.y = ny;
                }
                continue;
            }
            cop.x = nx;
        }
    }

    private boolean hasCollision(Player player) {
        for (CopNpc cop : cops) {
            if (Math.abs(cop.x - player.x) <= COLLISION_X && Math.abs(cop.y - player.y) <= COLLISION_Y) {
                return true;
            }
        }
        return false;
    }

    private boolean canSeePlayer(char[][] levelMap, CopNpc cop, Player player) {
        if (Math.abs(cop.y - player.y) > LANE_TOLERANCE) {
            return false;
        }
        double dx = player.x - cop.x;
        if (Math.abs(dx) > viewDistance) {
            return false;
        }
        if (dx * cop.dir < 0) {
            return false;
        }

        int rowY = (int) Math.floor(cop.y);
        int startX = (int) Math.floor(Math.min(cop.x, player.x));
        int endX = (int) Math.floor(Math.max(cop.x, player.x));
        for (int x = startX; x <= endX; x++) {
            if (!isInsideMap(levelMap, x, rowY)) {
                return false;
            }
            if (levelMap[rowY][x] == '#') {
                return false;
            }
        }
        return true;
    }

    private static boolean isBlocked(char[][] levelMap, double x, double y) {
        int tx = (int) Math.floor(x);
        int ty = (int) Math.floor(y);
        return !isInsideMap(levelMap, tx, ty) || levelMap[ty][tx] == '#';
    }

    private static boolean isInsideMap(char[][] levelMap, int x, int y) {
        return y >= 0 && y < levelMap.length && x >= 0 && x < levelMap[0].length;
    }

    public enum Outcome {
        NONE,
        CAUGHT,
        GAME_OVER
    }

    private static final class CopNpc {
        private double x;
        private double y;
        private int dir = -1;

        private CopNpc(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
