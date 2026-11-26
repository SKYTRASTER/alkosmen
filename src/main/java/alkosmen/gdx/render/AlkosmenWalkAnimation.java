package alkosmen.gdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

/** Shared approved Alkosmen frames and ground anchor for the player and New Park. */
public final class AlkosmenWalkAnimation implements Disposable {
    private static final int FRAME_COUNT = SideWalkGait.FRAME_COUNT;
    private static final String BASE_PATH = "alkosmen/ui/alkosmen";
    private static final float IDLE_FRAME_SECONDS = 0.2f;

    // Neutral frames divide the alternating steps in the frontal walk.
    private static final String[] FRONT_WALK_SEQUENCE = {
        "idle_front/00.png",
        "walk_front/01.png",
        "walk_front/02.png",
        "idle_front/03.png",
        "walk_front/03.png",
        "walk_front/04.png"
    };

    private final List<Texture> textures = new ArrayList<>();
    private final TextureRegion[][] walkFrames = new TextureRegion[Facing.values().length][FRAME_COUNT];
    private final TextureRegion[][] idleFrames = new TextureRegion[Facing.values().length][FRAME_COUNT];
    private final CharacterRenderMetrics.FrameMetrics[][] walkMetrics =
        new CharacterRenderMetrics.FrameMetrics[Facing.values().length][FRAME_COUNT];
    private final CharacterRenderMetrics.FrameMetrics[][] idleMetrics =
        new CharacterRenderMetrics.FrameMetrics[Facing.values().length][FRAME_COUNT];
    private final WalkCycle cycle = new WalkCycle(FRAME_COUNT);
    private float idleTime;

    public AlkosmenWalkAnimation() {
        try {
            for (Facing facing : Facing.values()) {
                int row = facing.ordinal();
                for (int frame = 0; frame < FRAME_COUNT; frame++) {
                    Pixmap idle = new Pixmap(Gdx.files.internal(idlePath(facing, frame)));
                    if (facing == Facing.DOWN) {
                        register(new Pixmap(Gdx.files.internal(walkPath(facing, frame))),
                            walkFrames, walkMetrics, row, frame);
                    } else {
                        // LEFT / RIGHT / UP come only from the approved red-shirt references.
                        // Old white-outfit walk animations are never loaded.
                        register(makeWalkStep(idle, frame, facing),
                            walkFrames, walkMetrics, row, frame);
                    }
                    register(idle, idleFrames, idleMetrics, row, frame);
                }
            }
        } catch (RuntimeException exception) {
            dispose();
            throw exception;
        }
    }

    private static String idlePath(Facing facing, int frame) {
        String directory = switch (facing) {
            case LEFT -> "idle_left";
            case RIGHT -> "idle_right";
            case UP -> "idle_back";
            case DOWN -> "idle_front";
        };
        return String.format("%s/%s/%02d.png", BASE_PATH, directory, frame);
    }

    private static String walkPath(Facing facing, int frame) {
        return switch (facing) {
            case DOWN -> BASE_PATH + "/" + FRONT_WALK_SEQUENCE[frame];
            case UP, LEFT, RIGHT -> throw new IllegalArgumentException(
                "Directional steps use approved idle artwork");
        };
    }

    /**
     * Build a directional walk pose from the approved idle frame.
     * Only the lower legs shift: the shirt, face, backpack and body size stay fixed.
     */
    private static Pixmap makeWalkStep(Pixmap original, int frame, Facing facing) {
        int width = original.getWidth();
        int height = original.getHeight();
        Pixmap result = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        result.setBlending(Pixmap.Blending.None);
        result.setColor(0f, 0f, 0f, 0f);
        result.fill();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgba = original.getPixel(x, y);
                if ((rgba & 0xff) == 0) {
                    continue;
                }
                int dx;
                int dy;
                if (facing == Facing.UP) {
                    dx = SideWalkGait.backOffsetX(frame, x, y, width, height);
                    dy = SideWalkGait.backOffsetY(frame, x, y, width, height);
                } else {
                    boolean facingLeft = facing == Facing.LEFT;
                    dx = SideWalkGait.offsetX(frame, x, y, width, height, facingLeft);
                    dy = SideWalkGait.offsetY(frame, x, y, width, height, facingLeft);
                }
                int toX = x + dx;
                int toY = y + dy;
                if (toX >= 0 && toX < width && toY >= 0 && toY < height) {
                    result.drawPixel(toX, toY, rgba);
                }
            }
        }
        return result;
    }

    private void register(Pixmap pixels, TextureRegion[][] frames,
                          CharacterRenderMetrics.FrameMetrics[][] metrics,
                          int row, int frame) {
        try {
            metrics[row][frame] = CharacterRenderMetrics.scan(
                pixels, 0, 0, pixels.getWidth(), pixels.getHeight());
            Texture texture = new Texture(pixels);
            textures.add(texture);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            frames[row][frame] = new TextureRegion(texture);
        } finally {
            pixels.dispose();
        }
    }

    /** One complete six-frame step cycle corresponds to 0.72 character heights. */
    public void advance(float actualDistance, float visibleHeight) {
        cycle.advance(actualDistance, visibleHeight * 0.72f);
    }

    public void step() {
        cycle.step();
    }

    public void advanceIdle(float delta) {
        idleTime += Math.max(0f, delta);
        if (idleTime >= FRAME_COUNT * IDLE_FRAME_SECONDS) {
            idleTime %= FRAME_COUNT * IDLE_FRAME_SECONDS;
        }
    }

    public void resetIdle() {
        idleTime = 0f;
    }

    public void reset() {
        cycle.reset();
        resetIdle();
    }

    public CharacterRenderMetrics.Placement draw(
        SpriteBatch batch, Facing facing, boolean moving,
        float footX, float footY, float visibleHeight
    ) {
        int row = facing.ordinal();
        int column = moving ? cycle.frameIndex()
            : Math.min(FRAME_COUNT - 1, (int) (idleTime / IDLE_FRAME_SECONDS));
        TextureRegion[][] frames = moving ? walkFrames : idleFrames;
        CharacterRenderMetrics.FrameMetrics[][] metrics = moving ? walkMetrics : idleMetrics;
        // Normalize the opaque silhouette instead of the PNG canvas.
        // The correct left/right profiles already exist; no mirroring.
        return CharacterRenderMetrics.draw(batch, frames[row][column], metrics[row][column],
            footX, footY, visibleHeight, false);
    }

    @Override
    public void dispose() {
        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
    }

    public enum Facing {
        LEFT, RIGHT, UP, DOWN
    }
}
