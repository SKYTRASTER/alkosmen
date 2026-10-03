package alkosmen.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class EboboWalkTest extends ApplicationAdapter {
    private static final int FRAME_COUNT = 6;
    private static final float FRAME_DURATION = 0.12f;
    private static final float MOVE_SPEED = 120f;
    private static final String SPRITE_ZIP = "alkosmen/ui/ebobo/ebobo_walk_all.zip";

    private SpriteBatch batch;
    private final List<Texture> textures = new ArrayList<>();

    private TextureRegion[] rightFrames;
    private TextureRegion[] leftFrames;
    private TextureRegion[] frontFrames;
    private TextureRegion[] backFrames;

    private float stateTime;
    private float x = 400f;
    private float y = 100f;
    private Direction direction = Direction.RIGHT;

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
        boolean[] loaded = new boolean[FRAME_COUNT];

        try (InputStream raw = Gdx.files.internal(SPRITE_ZIP).read();
             ZipInputStream zip = new ZipInputStream(raw)) {

            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');

                for (int i = 0; i < FRAME_COUNT; i++) {
                    String expected = folder + "/" + String.format("%02d.png", i);
                    if (name.equals(expected)) {
                        byte[] bytes = readAll(zip);
                        Pixmap pixmap = new Pixmap(bytes, 0, bytes.length);
                        Texture texture = new Texture(pixmap);
                        pixmap.dispose();

                        textures.add(texture);
                        frames[i] = new TextureRegion(texture);
                        loaded[i] = true;
                        break;
                    }
                }

                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read Ebobo sprite archive: " + SPRITE_ZIP, e);
        }

        for (int i = 0; i < FRAME_COUNT; i++) {
            if (!loaded[i]) {
                throw new IllegalStateException(
                        "Ebobo frame not found in archive: "
                                + folder + "/" + String.format("%02d.png", i)
                );
            }
        }

        return frames;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;

        while ((read = input.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }

        return out.toByteArray();
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

        if (moving) {
            stateTime += delta;
        } else {
            stateTime = 0f;
        }

        int frameIndex = (int) (stateTime / FRAME_DURATION) % FRAME_COUNT;
        TextureRegion frame = switch (direction) {
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
        if (batch != null) {
            batch.dispose();
        }

        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
    }
}
