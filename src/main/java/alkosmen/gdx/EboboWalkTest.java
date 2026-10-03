package alkosmen.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public final class EboboWalkTest extends ApplicationAdapter {
    private static final int FRAME_COUNT = 6;
    private static final float FRAME_DURATION = 0.12f;
    private static final float MOVE_SPEED = 120f;

    private SpriteBatch batch;
    private Texture[] textures;
    private TextureRegion[] rightFrames;
    private TextureRegion[] leftFrames;

    private float stateTime;
    private float x = 400f;

    @Override
    public void create() {
        batch = new SpriteBatch();

        textures = new Texture[FRAME_COUNT];
        rightFrames = new TextureRegion[FRAME_COUNT];
        leftFrames = new TextureRegion[FRAME_COUNT];

        for (int i = 0; i < FRAME_COUNT; i++) {
            String path = String.format("alkosmen/ui/ebobo/walk_right/%02d.png", i);

            if (!Gdx.files.internal(path).exists()) {
                throw new IllegalStateException("Ebobo walk frame not found: " + path);
            }

            textures[i] = new Texture(Gdx.files.internal(path));
            rightFrames[i] = new TextureRegion(textures[i]);

            leftFrames[i] = new TextureRegion(textures[i]);
            leftFrames[i].flip(true, false);
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        boolean movingRight = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean movingLeft = Gdx.input.isKeyPressed(Input.Keys.LEFT);

        if (movingRight || movingLeft) {
            stateTime += delta;
        }

        if (movingRight) {
            x += MOVE_SPEED * delta;
        } else if (movingLeft) {
            x -= MOVE_SPEED * delta;
        }

        int frameIndex = (int)(stateTime / FRAME_DURATION) % FRAME_COUNT;
        TextureRegion frame = movingLeft ? leftFrames[frameIndex] : rightFrames[frameIndex];

        float targetHeight = 360f;
        float targetWidth = targetHeight * frame.getRegionWidth() / (float)frame.getRegionHeight();

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(frame, x, 100f, targetWidth, targetHeight);
        batch.end();
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }

        if (textures != null) {
            for (Texture texture : textures) {
                if (texture != null) {
                    texture.dispose();
                }
            }
        }
    }
}
