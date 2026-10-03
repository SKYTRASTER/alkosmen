package alkosmen.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.List;

public final class LenyaKultyshevWalkTest extends ApplicationAdapter {
    private static final int FRAME_COUNT = 6;
    private static final float FRAME_DURATION = 0.15f;
    private static final float MOVE_SPEED = 90f;
    private static final String BASE_PATH = "alkosmen/ui/lenya_kultyshev";

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
                throw new IllegalStateException("Lenya Kultyshev frame not found: " + path);
            }

            Pixmap source = new Pixmap(Gdx.files.internal(path));
            Pixmap cropped = cropTransparentPadding(source);
            source.dispose();

            Texture texture = new Texture(cropped);
            cropped.dispose();

            textures.add(texture);
            frames[i] = new TextureRegion(texture);
        }

        return frames;
    }

    private Pixmap cropTransparentPadding(Pixmap source) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int rgba = source.getPixel(x, y);
                int alpha = rgba & 0xff;

                if (alpha > 8) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return new Pixmap(source.getWidth(), source.getHeight(), source.getFormat());
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;

        Pixmap cropped = new Pixmap(width, height, source.getFormat());
        cropped.setBlending(Pixmap.Blending.None);
        cropped.drawPixmap(source, 0, 0, minX, minY, width, height);

        return cropped;
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

        stateTime += delta;
        int frameIndex = (int) (stateTime / FRAME_DURATION) % FRAME_COUNT;

        TextureRegion frame;
        if (!moving) {
            frame = idleFrames[frameIndex];
        } else {
            frame = switch (direction) {
                case LEFT -> leftFrames[frameIndex];
                case RIGHT -> rightFrames[frameIndex];
                case UP -> backFrames[frameIndex];
                case DOWN -> frontFrames[frameIndex];
            };
        }

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
        if (batch != null) {
            batch.dispose();
        }
        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
    }
}
