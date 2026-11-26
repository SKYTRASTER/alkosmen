package alkosmen.gdx.render;

/**
 * Six walking poses derived from approved red-shirt idle art.
 * Procedural fallback until hand-drawn directional cycles are available.
 */
public final class SideWalkGait {
    public static final int FRAME_COUNT = 6;
    private static final float[] STEP = {0f, .65f, 1f, 0f, -.65f, -1f};

    private SideWalkGait() { }

    private static float progress(int y, int height) {
        final float hip = height * .68f;
        return Math.max(0f, Math.min(1f, (y - hip) / (height * .96f - hip)));
    }

    private static int leg(int x, int width, boolean facingLeft) {
        int split = Math.round(width * (facingLeft ? .45f : .55f));
        return x < split ? -1 : 1;
    }

    private static void validate(int frame, int width, int height) {
        if (frame < 0 || frame >= FRAME_COUNT || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid walk frame or canvas size");
        }
    }

    /** Swing the shoes forward/back while keeping the shoulders and waist stable. */
    public static int offsetX(int frame, int x, int y, int width, int height, boolean facingLeft) {
        validate(frame, width, height);
        float t = progress(y, height);
        return Math.round(leg(x, width, facingLeft) * STEP[frame]
            * height * .048f * t * t);
    }

    /** Lift the advancing shoe; the support shoe stays on the ground. */
    public static int offsetY(int frame, int x, int y, int width, int height, boolean facingLeft) {
        validate(frame, width, height);
        float t = progress(y, height);
        float forward = leg(x, width, facingLeft) * STEP[frame];
        return forward > 0f ? -Math.round(height * .016f * t * forward) : 0;
    }

    /** A smaller side-to-side swing when the character is seen from behind. */
    public static int backOffsetX(int frame, int x, int y, int width, int height) {
        validate(frame, width, height);
        float t = progress(y, height);
        int leg = x < width / 2 ? -1 : 1;
        return Math.round(leg * STEP[frame] * height * .013f * t * t);
    }

    /** Alternate raised shoes in the back view, without changing the ABIBAS backpack. */
    public static int backOffsetY(int frame, int x, int y, int width, int height) {
        validate(frame, width, height);
        float t = progress(y, height);
        int leg = x < width / 2 ? -1 : 1;
        float recovering = leg * STEP[frame];
        return recovering > 0f ? -Math.round(height * .020f * recovering * t * t) : 0;
    }
}
