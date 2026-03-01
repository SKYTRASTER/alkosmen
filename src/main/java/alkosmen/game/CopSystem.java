package alkosmen.game;

import alkosmen.objects.Player;

import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

public final class CopSystem {
    private static final double COLLISION_X = 0.55;
    private static final double COLLISION_Y = 0.75;
    private static final double LANE_TOLERANCE = 0.75;

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

    public Outcome tick(char[][] levelMap, Player player, boolean playerHidden, long now) {
        updatePatrol(levelMap);
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

    public void draw(Graphics g, int cell, double npcScale, Image npcSprite, float cameraX, float cameraY) {
        if (npcSprite == null) {
            return;
        }
        for (CopNpc cop : cops) {
            int copW = (int) Math.round(cell * npcScale);
            int copH = (int) Math.round(cell * npcScale);
            int drawX = (int) Math.round(cop.x * cell - cameraX - (copW - cell) / 2.0);
            int drawY = (int) Math.round(cop.y * cell - cameraY - (copH - cell));
            g.drawImage(npcSprite, drawX, drawY, copW, copH, null);
        }
    }

    private void updatePatrol(char[][] levelMap) {
        for (CopNpc cop : cops) {
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
