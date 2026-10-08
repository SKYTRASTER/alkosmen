package alkosmen.gdx.render;

/** Distance-driven gait: blocked movement stops the feet, faster movement speeds them up. */
public final class WalkCycle {
    private final int frameCount;
    private float phase;

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

    public int frameIndex() {
        return Math.min(frameCount - 1, (int) (phase * frameCount));
    }

    public void reset() {
        phase = 0f;
    }
}
