package alkosmen.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Isolated preview using exactly the same frames, gait and anchor as the player. */
public final class AlkosmenWalkTest extends ApplicationAdapter {
    private static final float TARGET_HEIGHT = 360f;
    private static final float MOVE_SPEED = 300f;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private AlkosmenPlayer player;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        player = new AlkosmenPlayer(Gdx.graphics.getWidth() / 2f, 60f);
        player.setVisibleHeight(TARGET_HEIGHT);
        player.setSpeed(MOVE_SPEED);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        float margin = player.visibleHeight() * 0.5f;
        player.update(delta, body -> {
            float footX = body.x + AlkosmenPlayer.BODY_WIDTH / 2f;
            float footY = body.y - 8f;
            return footX < margin || footX > camera.viewportWidth - margin
                || footY < 20f || footY + player.visibleHeight() > camera.viewportHeight;
        });
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        player.render(batch);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        if (camera != null) {
            camera.setToOrtho(false, width, height);
            camera.update();
        }
    }

    @Override
    public void dispose() {
        if (player != null) player.dispose();
        if (batch != null) batch.dispose();
    }
}
