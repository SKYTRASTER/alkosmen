package alkosmen.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Каркас сцены Нового парка.
 *
 * Сейчас сцена специально не зависит от игровых ассетов:
 * сначала проверяем размеры мира, движение, камеру и базовую геометрию,
 * затем поверх готового каркаса добавляем настоящие текстуры и объекты.
 */
public final class NewParkGame extends ApplicationAdapter {
    private static final float VIEWPORT_WIDTH = 1280f;
    private static final float VIEWPORT_HEIGHT = 720f;

    private static final float PLAYER_SPEED = 330f;
    private static final float PLAYER_BODY_WIDTH = 36f;
    private static final float PLAYER_BODY_HEIGHT = 26f;
    private static final float PLAYER_BODY_Y_OFFSET = 8f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer shapes;
    private NewParkWorld world;

    private float playerX = NewParkWorld.SPAWN_X;
    private float playerY = NewParkWorld.SPAWN_Y;

    private boolean debugCollisions;
    private boolean debugPlayerAnchor;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        shapes = new ShapeRenderer();
        world = new NewParkWorld();

        updateCamera();
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);

        updateInput(delta);
        updateCamera();

        Gdx.gl.glClearColor(0.08f, 0.09f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();
        shapes.setProjectionMatrix(camera.combined);

        drawGround();
        drawRoadMarks();
        drawPlayerPlaceholder();

        if (debugCollisions) {
            drawCollisionDebug();
        }
        if (debugPlayerAnchor) {
            drawPlayerAnchor();
        }
    }

    private void updateInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            debugCollisions = !debugCollisions;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            debugPlayerAnchor = !debugPlayerAnchor;
        }

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
            return;
        }

        float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
        moveX = moveX / length * PLAYER_SPEED * delta;
        moveY = moveY / length * PLAYER_SPEED * delta;

        moveAlongAxes(moveX, moveY);
    }

    private void moveAlongAxes(float moveX, float moveY) {
        if (canStandAt(playerX + moveX, playerY)) {
            playerX += moveX;
        }
        if (canStandAt(playerX, playerY + moveY)) {
            playerY += moveY;
        }
    }

    private boolean canStandAt(float x, float y) {
        return !world.blocks(playerBodyAt(x, y));
    }

    private Rectangle playerBodyAt(float x, float y) {
        return new Rectangle(
            x - PLAYER_BODY_WIDTH / 2f,
            y + PLAYER_BODY_Y_OFFSET,
            PLAYER_BODY_WIDTH,
            PLAYER_BODY_HEIGHT
        );
    }

    private void updateCamera() {
        float halfWidth = viewport.getWorldWidth() / 2f;
        float halfHeight = viewport.getWorldHeight() / 2f;

        camera.position.set(
            MathUtils.clamp(playerX, halfWidth, NewParkWorld.WIDTH - halfWidth),
            MathUtils.clamp(playerY, halfHeight, NewParkWorld.HEIGHT - halfHeight),
            0f
        );
    }

    private void drawGround() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (NewParkWorld.SurfaceZone zone : world.surfaces()) {
            Rectangle bounds = zone.bounds();
            shapes.setColor(zone.kind().color());
            shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        shapes.end();
    }

    private void drawRoadMarks() {
        float roadBottom = 520f;
        float roadHeight = 540f;
        float centerY = roadBottom + roadHeight / 2f;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0.92f, 0.76f, 0.25f, 1f));

        float dashWidth = 120f;
        float gap = 90f;
        for (float x = 0f; x < NewParkWorld.WIDTH; x += dashWidth + gap) {
            shapes.rect(x, centerY - 6f, dashWidth, 12f);
        }

        shapes.end();
    }

    private void drawPlayerPlaceholder() {
        // Тень/точка опоры.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.28f);
        shapes.ellipse(playerX - 32f, playerY - 5f, 64f, 14f);

        // Временная капсула вместо спрайта героя.
        shapes.setColor(new Color(0.92f, 0.89f, 0.80f, 1f));
        shapes.rect(playerX - 24f, playerY + 8f, 48f, 68f);

        shapes.setColor(new Color(0.78f, 0.18f, 0.15f, 1f));
        shapes.circle(playerX, playerY + 78f, 19f);

        shapes.end();
    }

    private void drawCollisionDebug() {
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.YELLOW);

        for (NewParkWorld.ParkObject object : world.objects()) {
            Rectangle bounds = object.bounds();
            shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        Rectangle body = playerBodyAt(playerX, playerY);
        shapes.setColor(Color.CYAN);
        shapes.rect(body.x, body.y, body.width, body.height);

        shapes.end();
    }

    private void drawPlayerAnchor() {
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.MAGENTA);
        shapes.line(playerX - 24f, playerY, playerX + 24f, playerY);
        shapes.line(playerX, playerY - 24f, playerX, playerY + 24f);
        shapes.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (shapes != null) {
            shapes.dispose();
        }
    }
}
