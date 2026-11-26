package alkosmen.gdx;

/** Sprint stamina in seconds, independent of input and graphics. */
public final class SprintState {
    public static final float SPEED_MULTIPLIER = 1.8f;
    private static final float CAPACITY = 4f;
    private static final float RECOVERY_RATE = 0.8f;
    private static final float RECOVERY_DELAY = 0.75f;
    private static final float RESTART_THRESHOLD = 1.2f;
    private float remaining = CAPACITY;
    private float recoveryDelay;
    private boolean exhausted;
    private boolean sprinting;

    /** Blends the last partial sprint frame, so exhausting stamina cannot give a free full frame. */
    public float speedMultiplier(float delta, boolean requested) {
        validateDelta(delta);
        if (!requested || exhausted || delta == 0f) return 1f;
        return 1f + (SPEED_MULTIPLIER - 1f) * Math.min(1f, remaining / delta);
    }

    /** Charge only actual movement; pushing into a wall must not consume stamina. */
    public void update(float delta, boolean requested, boolean moved) {
        validateDelta(delta);
        sprinting = requested && moved && !exhausted && remaining > 0f && delta > 0f;
        if (sprinting) {
            remaining = Math.max(0f, remaining - delta);
            recoveryDelay = RECOVERY_DELAY;
            if (remaining <= 0.0001f) {
                remaining = 0f;
                exhausted = true;
            }
        } else {
            float recoveryTime = Math.max(0f, delta - recoveryDelay);
            recoveryDelay = Math.max(0f, recoveryDelay - delta);
            remaining = Math.min(CAPACITY, remaining + recoveryTime * RECOVERY_RATE);
            if (remaining >= RESTART_THRESHOLD) exhausted = false;
        }
    }

    public float fraction() { return remaining / CAPACITY; }
    public boolean exhausted() { return exhausted; }
    public boolean sprinting() { return sprinting; }

    private static void validateDelta(float delta) {
        if (!Float.isFinite(delta) || delta < 0f) {
            throw new IllegalArgumentException("Delta must be finite and nonnegative");
        }
    }
}
