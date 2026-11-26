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
    private static final int FRAME_COUNT = 6;
    private static final String BASE_PATH = "alkosmen/ui/alkosmen";

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

    public AlkosmenWalkAnimation() {
        try {
            for (Facing facing : Facing.values()) {
                int row = facing.ordinal();
                for (int frame = 0; frame < FRAME_COUNT; frame++) {
                    Pixmap idle = new Pixmap(Gdx.files.internal(idlePath(facing, frame)));
                    String walkingPath = walkPath(facing, frame);
                    if (walkingPath != null) {
                        register(new Pixmap(Gdx.files.internal(walkingPath)),
                            walkFrames, walkMetrics, row, frame);
                    }
                    register(idle, idleFrames, idleMetrics, row, frame);
                    if (walkingPath == null) {
                        walkFrames[row][frame] = idleFrames[row][frame];
                        walkMetrics[row][frame] = idleMetrics[row][frame];
                    }
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
        if (facing == Facing.DOWN) {
            return BASE_PATH + "/" + FRONT_WALK_SEQUENCE[frame];
        }
        if (frame != 1 && frame != 4) {
            return null;
        }
        return BASE_PATH + switch (facing) {
            case LEFT -> "/walk_generated_left/step.png";
            case RIGHT -> "/walk_generated_right/step.png";
            case UP -> frame == 1 ? "/walk_generated_back/step1.png" : "/walk_generated_back/step2.png";
            case DOWN -> throw new IllegalStateException("Front walk already handled");
        };
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
    public void update(float delta, float actualDistance, float visibleHeight) {
        cycle.update(delta, actualDistance, visibleHeight * 0.72f);
    }

    public boolean moving() {
        return cycle.moving();
    }

    public void reset() {
        cycle.reset();
    }

    public CharacterRenderMetrics.Placement draw(
        SpriteBatch batch, Facing facing, boolean moving,
        float footX, float footY, float visibleHeight
    ) {
        int row = facing.ordinal();
        int column = moving ? cycle.frameIndex() : cycle.idleFrameIndex();
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
