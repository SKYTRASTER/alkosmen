package alkosmen.game;

import java.util.List;
import java.util.Objects;

public final class PatrolSystem {
    private static final double DEFAULT_SPEED = 0.032;
    private static final double TARGET_EPSILON = 0.001;

    private final double speed;

    public PatrolSystem() {
        this(DEFAULT_SPEED);
    }

    public PatrolSystem(double speed) {
        if (speed <= 0.0) {
            throw new IllegalArgumentException("speed must be positive");
        }
        this.speed = speed;
    }

    public void update(List<TopDownPatrol> patrols, long now, Occupancy occupancy) {
        Objects.requireNonNull(patrols, "patrols");
        Objects.requireNonNull(occupancy, "occupancy");

        for (TopDownPatrol patrol : patrols) {
            moveAlongRoute(patrol, now, occupancy);
        }
    }

    private void moveAlongRoute(TopDownPatrol patrol, long now, Occupancy occupancy) {
        if (!patrol.hasRoute()) {
            return;
        }

        var target = patrol.currentWaypoint();
        double dx = target.x() - patrol.x();
        double dy = target.y() - patrol.y();

        if (Math.abs(dx) < TARGET_EPSILON && Math.abs(dy) < TARGET_EPSILON) {
            if (patrol.waitUntil() == 0L) {
                patrol.waitUntil(now + Math.max(1, target.pauseMs()));
            } else if (now >= patrol.waitUntil()) {
                patrol.advanceWaypoint();
            }
            return;
        }

        double stepX = Math.abs(dx) > TARGET_EPSILON
                ? Math.copySign(Math.min(speed, Math.abs(dx)), dx)
                : 0.0;
        double stepY = stepX == 0.0
                ? Math.copySign(Math.min(speed, Math.abs(dy)), dy)
                : 0.0;

        if (!occupancy.canOccupy(patrol.x() + stepX, patrol.y() + stepY)) {
            patrol.advanceWaypoint();
            return;
        }

        patrol.moveBy(stepX, stepY);
    }

    @FunctionalInterface
    public interface Occupancy {
        boolean canOccupy(double x, double y);
    }
}
