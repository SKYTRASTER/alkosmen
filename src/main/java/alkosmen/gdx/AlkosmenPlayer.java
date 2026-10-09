package alkosmen.gdx;

import alkosmen.gdx.render.CharacterRenderMetrics;
import alkosmen.gdx.render.AlkosmenWalkAnimation;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
    public static final float DEFAULT_SPEED = 135f;
    public static final float DEFAULT_VISIBLE_HEIGHT = 170f;
    public static final float BODY_WIDTH = 36f;
    public static final float BODY_HEIGHT = 26f;

    private static final float BODY_Y_OFFSET = 8f;
    private final AlkosmenWalkAnimation animation;
    private final Rectangle collisionBody = new Rectangle();

    private float x;
    private float y;
    private float speed = DEFAULT_SPEED;
    private float visibleHeight = DEFAULT_VISIBLE_HEIGHT;
    private Direction direction = Direction.DOWN;
    private boolean movingLastFrame;
    private CharacterRenderMetrics.Placement placement;

    public AlkosmenPlayer(float spawnX, float spawnY) {
        x = spawnX;
        y = spawnY;

        animation = new AlkosmenWalkAnimation();
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
            // A tap freezes the new frame rather than resetting to idle.
            movingLastFrame = false;
            return;
        }

        if (Math.abs(moveX) >= Math.abs(moveY)) {
            direction = moveX < 0f ? Direction.LEFT : Direction.RIGHT;
        } else {
            direction = moveY < 0f ? Direction.DOWN : Direction.UP;
        }

        float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
        moveX = moveX / length * speed * delta;
        moveY = moveY / length * speed * delta;

        float previousX = x;
        float previousY = y;
        moveAlongAxes(moveX, moveY, blocked);
        float actualDistance = (float) Math.hypot(x - previousX, y - previousY);
        if (!movingLastFrame && actualDistance > 0.0001f) {
            animation.step();
        } else {
            animation.advance(actualDistance, visibleHeight);
        }
        movingLastFrame = true;
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
        AlkosmenWalkAnimation.Facing facing = switch (direction) {
            case LEFT -> AlkosmenWalkAnimation.Facing.LEFT;
            case RIGHT -> AlkosmenWalkAnimation.Facing.RIGHT;
            case UP -> AlkosmenWalkAnimation.Facing.UP;
            case DOWN, IDLE -> AlkosmenWalkAnimation.Facing.DOWN;
        };
        placement = animation.draw(batch, facing, x, y, visibleHeight);
        return placement;
    }

    public void drawDebug(ShapeRenderer shapes) {
        shapes.rect(collisionBody.x, collisionBody.y, collisionBody.width, collisionBody.height);
        CharacterRenderMetrics.drawDebug(shapes, placement);
    }

    public void reset(float spawnX, float spawnY) {
        x = spawnX;
        y = spawnY;
        animation.reset();
        movingLastFrame = false;
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
        animation.dispose();
    }

    public enum Direction {
        LEFT, RIGHT, IDLE, UP, DOWN
    }
}
