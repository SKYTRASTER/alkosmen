package alkosmen.game;

import alkosmen.persistence.LocalGameStore;

import java.util.List;

public final class TopDownPatrol {
    private double x;
    private double y;
    private int animationTick;
    private int facing;
    private final List<LocalGameStore.Waypoint> route;
    private int waypointIndex;
    private long waitUntil;

    public TopDownPatrol(double x, double y, int initialDirection, List<LocalGameStore.Waypoint> route) {
        this.x = x;
        this.y = y;
        this.facing = initialDirection < 0 ? 0 : 1;
        this.route = route == null ? List.of() : List.copyOf(route);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public int animationTick() {
        return animationTick;
    }

    public int facing() {
        return facing;
    }

    boolean hasRoute() {
        return !route.isEmpty();
    }

    LocalGameStore.Waypoint currentWaypoint() {
        return route.get(waypointIndex);
    }

    long waitUntil() {
        return waitUntil;
    }

    void waitUntil(long value) {
        waitUntil = value;
    }

    void advanceWaypoint() {
        if (!route.isEmpty()) {
            waypointIndex = (waypointIndex + 1) % route.size();
        }
        waitUntil = 0L;
    }

    void moveBy(double dx, double dy) {
        x += dx;
        y += dy;
        animationTick++;

        if (dx < 0.0) {
            facing = 0;
        } else if (dx > 0.0) {
            facing = 1;
        } else if (dy < 0.0) {
            facing = 2;
        } else if (dy > 0.0) {
            facing = 3;
        }
    }
}
