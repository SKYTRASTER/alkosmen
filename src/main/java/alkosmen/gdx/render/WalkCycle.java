package alkosmen.gdx.render;

/** Distance-driven gait: blocked movement stops the feet, faster movement speeds them up. */
public final class WalkCycle {
    private static final float IDLE_FRAME_SECONDS = 0.2f;
    private final int frameCount;
    private float phase;
    private float idleTime;
    private boolean moving;

    public WalkCycle(int frameCount) {
        if (frameCount < 1) {
            throw new IllegalArgumentException("Frame count must be positive");
        }
        this.frameCount = frameCount;
    }

    public void advance(float distance, float cycleDistance) {
        if (!Float.isFinite(distance) || distance < 0f
            || !Float.isFinite(cycleDistance) || cycleDistance <= 0f) {
            throw new IllegalArgumentException("Invalid walk distance");
        }
        if (distance <= 0.0001f) {
            return;
        }
        phase = (phase + distance / cycleDistance) % 1f;
    }

    /** Advance one visible frame on a fresh directional key press. */
    public void step() {
        phase = (frameIndex() + 1) % frameCount / (float) frameCount;
    }

    public int frameIndex() {
        return Math.min(frameCount - 1, (int) (phase * frameCount));
    }

    public void update(float delta, float distance, float cycleDistance) {
        if (distance > 0.0001f) {
            if (moving) {
                advance(distance, cycleDistance);
            } else {
                step();
            }
            idleTime = 0f;
            moving = true;
        } else if (moving) {
            // The first stopped frame always uses the neutral directional pose.
            moving = false;
        } else {
            idleTime = (idleTime + Math.max(0f, delta)) % (frameCount * IDLE_FRAME_SECONDS);
        }
    }

    public boolean moving() {
        return moving;
    }

    public int idleFrameIndex() {
        return Math.min(frameCount - 1, (int) (idleTime / IDLE_FRAME_SECONDS));
    }

    public void reset() {
        phase = 0f;
        idleTime = 0f;
        moving = false;
    }
}
