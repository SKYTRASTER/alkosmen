package alkosmen.gdx;

import alkosmen.gdx.render.CharacterRenderMetrics;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;

import java.util.function.Predicate;

/**
 * Reusable player entity for Alkosmen.
 *
 * <p>The world position is the point between the character's feet. Movement,
 * animation, collision body and sprite rendering live here instead of inside
 * an individual scene.</p>
 */
public final class AlkosmenPlayer implements Disposable {
    public static final float DEFAULT_SPEED = 330f;
    public static final float DEFAULT_VISIBLE_HEIGHT = 150f;
    public static final float BODY_WIDTH = 36f;
    public static final float BODY_HEIGHT = 26f;

    private static final float BODY_Y_OFFSET = 8f;
    private static final float FRAME_DURATION = 0.14f;
    private static final String ATLAS_PATH = "alkosmen/ui/sprites/alkosmen/walk_atlas_v1.png";

    private final Texture atlas;
    private final TextureRegion[][] frames;
    private final CharacterRenderMetrics.FrameMetrics[][] metrics;
    private final Rectangle collisionBody = new Rectangle();

    private float x;
    private float y;
    private float speed = DEFAULT_SPEED;
    private float visibleHeight = DEFAULT_VISIBLE_HEIGHT;
    private float animationTime;
    private Direction direction = Direction.DOWN;
    private CharacterRenderMetrics.Placement placement;

    public AlkosmenPlayer(float spawnX, float spawnY) {
        x = spawnX;
        y = spawnY;

        atlas = new Texture(Gdx.files.internal(ATLAS_PATH));
        atlas.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        Pixmap pixels = new Pixmap(Gdx.files.internal(ATLAS_PATH));
        TextureRegion canonicalFrame = new TextureRegion(atlas);
        CharacterRenderMetrics.FrameMetrics canonicalMetrics =
            CharacterRenderMetrics.scan(pixels, 0, 0, pixels.getWidth(), pixels.getHeight());
        pixels.dispose();

        frames = new TextureRegion[5][4];
        metrics = new CharacterRenderMetrics.FrameMetrics[5][4];
        for (int row = 0; row < frames.length; row++) {
            for (int column = 0; column < frames[row].length; column++) {
                frames[row][column] = canonicalFrame;
                metrics[row][column] = canonicalMetrics;
            }
        }
        updateCollisionBody();
    }

    /**
     * Reads WASD/arrows and moves the player.
     *
     * @param delta frame delta
     * @param blocked returns true when the proposed player collision body is blocked
     */
    public void update(float delta, Predicate<Rectangle> blocked) {
        float moveX = 0f;
        float moveY = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            moveX -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            moveX += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            moveY += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            moveY -= 1f;
        }

        if (moveX == 0f && moveY == 0f) {
            animationTime = 0f;
            direction = Direction.IDLE;
            return;
        }

        if (Math.abs(moveX) > Math.abs(moveY)) {
            direction = moveX < 0f ? Direction.LEFT : Direction.RIGHT;
        } else {
            direction = moveY < 0f ? Direction.DOWN : Direction.UP;
        }

        float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
        moveX = moveX / length * speed * delta;
        moveY = moveY / length * speed * delta;

        moveAlongAxes(moveX, moveY, blocked);
        animationTime += delta;
    }

    /**
     * Axis-separated movement lets the player slide along walls instead of
     * getting stuck on diagonal input.
     */
    public void moveAlongAxes(float moveX, float moveY, Predicate<Rectangle> blocked) {
        if (moveX != 0f) {
            Rectangle proposed = bodyAt(x + moveX, y);
            if (!blocked.test(proposed)) {
                x += moveX;
            }
        }

        if (moveY != 0f) {
            Rectangle proposed = bodyAt(x, y + moveY);
            if (!blocked.test(proposed)) {
                y += moveY;
            }
        }

        updateCollisionBody();
    }

    public CharacterRenderMetrics.Placement render(SpriteBatch batch) {
        int row = direction.atlasRow;
        int column = direction == Direction.IDLE
            ? 0
            : (int) (animationTime / FRAME_DURATION) % frames[row].length;

        TextureRegion frame = frames[row][column];
        placement = CharacterRenderMetrics.draw(
            batch,
            frame,
            metrics[row][column],
            x,
            y,
            visibleHeight,
            false
        );
        return placement;
    }

    public void drawDebug(ShapeRenderer shapes) {
        shapes.rect(collisionBody.x, collisionBody.y, collisionBody.width, collisionBody.height);
        CharacterRenderMetrics.drawDebug(shapes, placement);
    }

    public void reset(float spawnX, float spawnY) {
        x = spawnX;
        y = spawnY;
        animationTime = 0f;
        direction = Direction.DOWN;
        updateCollisionBody();
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float speed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = Math.max(0f, speed);
    }

    public float visibleHeight() {
        return visibleHeight;
    }

    public void setVisibleHeight(float visibleHeight) {
        this.visibleHeight = Math.max(1f, visibleHeight);
    }

    public Rectangle collisionBody() {
        return collisionBody;
    }

    public CharacterRenderMetrics.Placement placement() {
        return placement;
    }

    public Direction direction() {
        return direction;
    }

    private Rectangle bodyAt(float footX, float footY) {
        return new Rectangle(
            footX - BODY_WIDTH / 2f,
            footY + BODY_Y_OFFSET,
            BODY_WIDTH,
            BODY_HEIGHT
        );
    }

    private void updateCollisionBody() {
        collisionBody.set(
            x - BODY_WIDTH / 2f,
            y + BODY_Y_OFFSET,
            BODY_WIDTH,
            BODY_HEIGHT
        );
    }

    @Override
    public void dispose() {
        atlas.dispose();
    }

    private static AtlasData splitAtlas(Texture atlas, Pixmap pixels) {
        int columns = 4;
        int rows = 5;
        int frameWidth = atlas.getWidth() / columns;
        int fullFrameHeight = atlas.getHeight() / rows;
        int frameHeight = fullFrameHeight - 30;

        TextureRegion[][] frames = new TextureRegion[rows][columns];
        CharacterRenderMetrics.FrameMetrics[][] metrics =
            new CharacterRenderMetrics.FrameMetrics[rows][columns];

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                frames[row][column] = new TextureRegion(
                    atlas,
                    column * frameWidth,
                    row * fullFrameHeight,
                    frameWidth,
                    frameHeight
                );
                metrics[row][column] = CharacterRenderMetrics.scan(
                    pixels,
                    column * frameWidth,
                    row * fullFrameHeight,
                    frameWidth,
                    frameHeight
                );
            }
        }

        return new AtlasData(frames, metrics);
    }

    public enum Direction {
        LEFT(2),
        RIGHT(1),
        IDLE(0),
        UP(4),
        DOWN(3);

        private final int atlasRow;

        Direction(int atlasRow) {
            this.atlasRow = atlasRow;
        }
    }

    private record AtlasData(
        TextureRegion[][] frames,
        CharacterRenderMetrics.FrameMetrics[][] metrics
    ) {
    }
}
