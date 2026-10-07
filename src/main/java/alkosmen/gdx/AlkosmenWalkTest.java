package alkosmen.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.List;

/**
 * Изолированный тест ходьбы каноничного Алкосмена.
 */
public final class AlkosmenWalkTest extends ApplicationAdapter {
    private static final int FRAME_COUNT = 6;
    private static final float FRAME_DURATION = 0.15f;
    private static final float MOVE_SPEED = 100f;
    private static final float TARGET_HEIGHT = 360f;
    private static final String BASE_PATH = "alkosmen/ui/alkosmen";

    private SpriteBatch batch;
    private final List<Texture> textures = new ArrayList<>();

    private TextureRegion[] rightFrames;
    private TextureRegion[] leftFrames;
    private TextureRegion[] frontFrames;
    private TextureRegion[] backFrames;

    private float stateTime;
    private float x = 400f;
    private float y = 100f;
    private Direction direction = Direction.DOWN;

    private enum Direction {
        LEFT, RIGHT, UP, DOWN
    }

    @Override
    public void create() {
        batch = new SpriteBatch();

        rightFrames = loadFrames("walk_right");
        frontFrames = loadFrames("walk_front");
        backFrames = loadFrames("walk_back");

        leftFrames = new TextureRegion[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            leftFrames[i] = new TextureRegion(rightFrames[i]);
            leftFrames[i].flip(true, false);
        }
    }

    private TextureRegion[] loadFrames(String folder) {
        TextureRegion[] frames = new TextureRegion[FRAME_COUNT];

        for (int i = 0; i < FRAME_COUNT; i++) {
            String path = String.format("%s/%s/%02d.png", BASE_PATH, folder, i);

            if (!Gdx.files.internal(path).exists()) {
                throw new IllegalStateException("Кадр Алкосмена не найден: " + path);
            }

            Texture texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            textures.add(texture);
            frames[i] = new TextureRegion(texture);
        }

        return frames;
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        boolean moving = false;

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            x += MOVE_SPEED * delta;
            direction = Direction.RIGHT;
            moving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            x -= MOVE_SPEED * delta;
            direction = Direction.LEFT;
            moving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            y += MOVE_SPEED * delta;
            direction = Direction.UP;
            moving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            y -= MOVE_SPEED * delta;
            direction = Direction.DOWN;
            moving = true;
        }

        int frameIndex;
        if (moving) {
            stateTime += delta;
            frameIndex = (int) (stateTime / FRAME_DURATION) % FRAME_COUNT;
        } else {
            stateTime = 0f;
            frameIndex = 0;
        }

        TextureRegion frame = switch (direction) {
            case LEFT -> leftFrames[frameIndex];
            case RIGHT -> rightFrames[frameIndex];
            case UP -> backFrames[frameIndex];
            case DOWN -> frontFrames[frameIndex];
        };

        float targetWidth = TARGET_HEIGHT * frame.getRegionWidth() / (float) frame.getRegionHeight();

        x = Math.max(0f, Math.min(x, Gdx.graphics.getWidth() - targetWidth));
        y = Math.max(0f, Math.min(y, Gdx.graphics.getHeight() - TARGET_HEIGHT));

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(frame, x, y, targetWidth, TARGET_HEIGHT);
        batch.end();
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }

        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
    }
}
