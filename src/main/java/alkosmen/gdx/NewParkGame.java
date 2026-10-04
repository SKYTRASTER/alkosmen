package alkosmen.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** First playable libGDX scene: Sverdlova 12 and New Park. */
public final class NewParkGame extends ApplicationAdapter {
    private static final float VIEWPORT_WIDTH = 1280f;
    private static final float VIEWPORT_HEIGHT = 720f;
    private static final float PLAYER_SPEED = 330f;
    private static final float PLAYER_DRAW_HEIGHT = 132f;
    private static final float PLAYER_BODY_WIDTH = 36f;
    private static final float PLAYER_BODY_HEIGHT = 26f;
    private static final float FRAME_DURATION = 0.14f;
    private static final String PLAYER_ATLAS = "alkosmen/ui/sprites/alkosmen/walk_atlas_v1.png";

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont hudFont;
    private Texture playerAtlas;
    private TextureRegion[][] playerFrames;
    private NewParkWorld world;

    private float playerX = NewParkWorld.SPAWN_X;
    private float playerY = NewParkWorld.SPAWN_Y;
    private float animationTime;
    private Direction direction = Direction.DOWN;
    private boolean debugCollisions;

    private enum Direction {
        LEFT(2), RIGHT(1), IDLE(0), UP(4), DOWN(3);

        private final int atlasRow;

        Direction(int atlasRow) {
            this.atlasRow = atlasRow;
        }
    }

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        hudFont = new BitmapFont();

        playerAtlas = new Texture(Gdx.files.internal(PLAYER_ATLAS));
        playerAtlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        playerFrames = TextureRegion.split(
            playerAtlas,
            playerAtlas.getWidth() / 4,
            playerAtlas.getHeight() / 5
        );
        world = new NewParkWorld();
        updateCamera();
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        updatePlayer(delta);
        updateCamera();

        Gdx.gl.glClearColor(0.10f, 0.21f, 0.13f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawWorld();
        drawPlayer();
        if (debugCollisions) {
            drawCollisionDebug();
        }
        drawHud();
    }

    private void updatePlayer(float delta) {
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            debugCollisions = !debugCollisions;
        }

        boolean moving = moveX != 0f || moveY != 0f;
        if (!moving) {
            animationTime = 0f;
            return;
        }

        if (Math.abs(moveX) > Math.abs(moveY)) {
            direction = moveX < 0f ? Direction.LEFT : Direction.RIGHT;
        } else {
            direction = moveY < 0f ? Direction.DOWN : Direction.UP;
        }

        float length = (float)Math.sqrt(moveX * moveX + moveY * moveY);
        moveX = moveX / length * PLAYER_SPEED * delta;
        moveY = moveY / length * PLAYER_SPEED * delta;
        moveAlongAxes(moveX, moveY);
        animationTime += delta;
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
        Rectangle body = new Rectangle(
            x + (PLAYER_DRAW_HEIGHT * 0.42f - PLAYER_BODY_WIDTH / 2f),
            y + 8f,
            PLAYER_BODY_WIDTH,
            PLAYER_BODY_HEIGHT
        );
        return !world.blocks(body);
    }

    private void updateCamera() {
        float halfWidth = viewport.getWorldWidth() / 2f;
        float halfHeight = viewport.getWorldHeight() / 2f;
        camera.position.set(
            MathUtils.clamp(playerX + PLAYER_DRAW_HEIGHT * 0.42f, halfWidth, NewParkWorld.WIDTH - halfWidth),
            MathUtils.clamp(playerY + PLAYER_DRAW_HEIGHT * 0.20f, halfHeight, NewParkWorld.HEIGHT - halfHeight),
            0f
        );
    }

    private void drawWorld() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0.20f, 0.42f, 0.20f, 1f));
        shapes.rect(0f, 0f, NewParkWorld.WIDTH, NewParkWorld.HEIGHT);

        shapes.setColor(new Color(0.48f, 0.46f, 0.37f, 1f));
        shapes.rect(0f, 740f, NewParkWorld.WIDTH, 260f);
        shapes.rect(1880f, 0f, 280f, NewParkWorld.HEIGHT);
        shapes.setColor(new Color(0.78f, 0.72f, 0.51f, 1f));
        shapes.rect(720f, 960f, 1200f, 92f);
        shapes.rect(1690f, 980f, 92f, 1070f);

        for (NewParkWorld.ParkObject object : world.objects()) {
            Rectangle bounds = object.bounds();
            shapes.setColor(object.kind().color());
            shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
            if (object.kind() == NewParkWorld.ObjectKind.TREE) {
                shapes.setColor(new Color(0.14f, 0.48f, 0.18f, 1f));
                shapes.circle(bounds.x + bounds.width / 2f, bounds.y + bounds.height * 0.72f, bounds.width * 0.62f);
            }
        }
        shapes.end();
    }

    private void drawPlayer() {
        int column = direction == Direction.IDLE ? 0 : (int)(animationTime / FRAME_DURATION) % playerFrames[direction.atlasRow].length;
        TextureRegion frame = playerFrames[direction.atlasRow][column];
        float width = PLAYER_DRAW_HEIGHT * frame.getRegionWidth() / (float)frame.getRegionHeight();
        float drawX = playerX;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.24f);
        shapes.ellipse(drawX + width * 0.20f, playerY + 2f, width * 0.58f, 13f);
        shapes.end();

        batch.begin();
        batch.draw(frame, drawX, playerY, width, PLAYER_DRAW_HEIGHT);
        batch.end();
    }

    private void drawCollisionDebug() {
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.YELLOW);
        for (NewParkWorld.ParkObject object : world.objects()) {
            Rectangle bounds = object.bounds();
            shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        Rectangle body = new Rectangle(
            playerX + (PLAYER_DRAW_HEIGHT * 0.42f - PLAYER_BODY_WIDTH / 2f),
            playerY + 8f,
            PLAYER_BODY_WIDTH,
            PLAYER_BODY_HEIGHT
        );
        shapes.setColor(Color.CYAN);
        shapes.rect(body.x, body.y, body.width, body.height);
        shapes.end();
    }

    private void drawHud() {
        batch.begin();
        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, "NEW PARK / SVERDLOVA 12", camera.position.x - viewport.getWorldWidth() / 2f + 20f,
            camera.position.y + viewport.getWorldHeight() / 2f - 20f);
        hudFont.draw(batch, "WASD / arrows - walk    F3 - collisions", camera.position.x - viewport.getWorldWidth() / 2f + 20f,
            camera.position.y + viewport.getWorldHeight() / 2f - 42f);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (playerAtlas != null) {
            playerAtlas.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
        if (shapes != null) {
            shapes.dispose();
        }
        if (hudFont != null) {
            hudFont.dispose();
        }
    }
}
