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

public final class PoliceFemaleWalkTest extends ApplicationAdapter {
    private static final int FRAME_COUNT = 6;
    private static final float FRAME_DURATION = 0.15f;
    private static final float MOVE_SPEED = 90f;
    private static final String BASE_PATH = "alkosmen/ui/police_female";

    private SpriteBatch batch;
    private final List<Texture> textures = new ArrayList<>();
    private TextureRegion[] rightFrames;
    private TextureRegion[] leftFrames;
    private TextureRegion[] frontFrames;
    private TextureRegion[] backFrames;
    private TextureRegion[] idleFrames;

    private float stateTime;
    private float x = 400f;
    private float y = 100f;
    private Direction direction = Direction.RIGHT;

    private enum Direction { LEFT, RIGHT, UP, DOWN }

    @Override
    public void create() {
        batch = new SpriteBatch();
        rightFrames = loadFrames("walk_right");
        frontFrames = loadFrames("walk_front");
        backFrames = loadFrames("walk_back");
        idleFrames = loadFrames("idle");

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
                throw new IllegalStateException("Police female frame not found: " + path);
            }
            Texture texture = new Texture(Gdx.files.internal(path));
            textures.add(texture);
            frames[i] = new TextureRegion(texture);
        }
        return frames;
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        boolean moving = false;

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            x += MOVE_SPEED * delta;
            direction = Direction.RIGHT;
            moving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            x -= MOVE_SPEED * delta;
            direction = Direction.LEFT;
            moving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            y += MOVE_SPEED * delta;
            direction = Direction.UP;
            moving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
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

        TextureRegion frame = !moving
                ? idleFrames[0]
                : switch (direction) {
                    case LEFT -> leftFrames[frameIndex];
                    case RIGHT -> rightFrames[frameIndex];
                    case UP -> backFrames[frameIndex];
                    case DOWN -> frontFrames[frameIndex];
                };

        float targetHeight = 360f;
        float targetWidth = targetHeight * frame.getRegionWidth() / (float) frame.getRegionHeight();

        x = Math.max(0f, Math.min(x, Gdx.graphics.getWidth() - targetWidth));
        y = Math.max(0f, Math.min(y, Gdx.graphics.getHeight() - targetHeight));

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(frame, x, y, targetWidth, targetHeight);
        batch.end();
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        for (Texture texture : textures) texture.dispose();
        textures.clear();
    }
}
