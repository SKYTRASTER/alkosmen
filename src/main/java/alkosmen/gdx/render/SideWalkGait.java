package alkosmen.gdx.render;

/**
 * Restrained six-frame side step built from the approved idle artwork.
 * The upper body stays intact; only the two lower-leg areas swing in opposite directions.
 * All offsets are in source pixels and scale with the original PNG canvas.
 */
public final class SideWalkGait {
    public static final int FRAME_COUNT = 6;
    private static final float[] STEP = {0f, 0.55f, 0.85f, 0f, -0.55f, -0.85f};

    private SideWalkGait() {
    }

    public static int offsetX(int frame, int x, int y, int width, int height, boolean facingLeft) {
        if (frame < 0 || frame >= FRAME_COUNT || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid side-walk frame or canvas size");
        }

        // The shorts end at roughly 71% of the approved 384x752 side reference.
        int hipY = Math.round(height * 0.71f);
        if (y <= hipY) {
            return 0;
        }

        float legLength = Math.max(1f, height * 0.96f - hipY);
        float progress = Math.min(1f, (y - hipY) / legLength);
        int splitX = Math.round(width * 0.55f);
        int nearLeg = x < splitX ? -1 : 1;
        if (facingLeft) {
            nearLeg = -nearLeg;
        }

        int maxStride = Math.max(1, Math.round(height * 0.024f));
        return Math.round(STEP[frame] * maxStride * progress * nearLeg);
    }
}
