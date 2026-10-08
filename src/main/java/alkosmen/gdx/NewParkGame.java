package alkosmen.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import alkosmen.gdx.render.CharacterRenderMetrics;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;
import java.util.List;

/**
 * Каркас сцены Нового парка.
 *
 * Сейчас сцена специально не зависит от игровых ассетов:
 * сначала проверяем размеры мира, движение, камеру и базовую геометрию,
 * затем поверх готового каркаса добавляем настоящие текстуры и объекты.
 */
public final class NewParkGame extends ApplicationAdapter {
    private static final float VIEWPORT_WIDTH = NewParkWorld.units(16f);
    private static final float VIEWPORT_HEIGHT = NewParkWorld.units(9f);

    // Six frames form two steps; at 1.35 m/s a 5.5 m street takes about nine steps.
    private static final float PLAYER_SPEED = NewParkWorld.units(1.35f);
    private static final float PLAYER_BODY_WIDTH = NewParkWorld.units(0.48f);
    private static final float PLAYER_BODY_HEIGHT = NewParkWorld.units(0.28f);
    private static final float PLAYER_BODY_Y_OFFSET = NewParkWorld.units(0.06f);
    private static final float PLAYER_FOOT_WIDTH = NewParkWorld.units(0.55f);
    private static final int PLAYER_FRAME_COUNT = 6;
    private static final float PLAYER_FRAME_DURATION = 0.15f;
    private static final String PLAYER_BASE_PATH = "alkosmen/ui/alkosmen";
    private static final Color WOOD_SIDEWALK = new Color(0.46f, 0.34f, 0.22f, 1f);
    private static final Color ROAD_SURFACE = new Color(0.26f, 0.28f, 0.27f, 1f);
    private static final Color RAIL_BED = new Color(0.28f, 0.25f, 0.21f, 1f);
    private static final Color RAIL_TIE = new Color(0.38f, 0.29f, 0.21f, 1f);
    private static final Color RAIL_STEEL = new Color(0.70f, 0.69f, 0.65f, 1f);

    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private final List<Texture> playerTextures = new ArrayList<>();
    private TextureRegion[] rightFrames;
    private TextureRegion[] frontFrames;
    private TextureRegion[] backFrames;
    private CharacterRenderMetrics.FrameMetrics[] rightMetrics;
    private CharacterRenderMetrics.FrameMetrics[] frontMetrics;
    private CharacterRenderMetrics.FrameMetrics[] backMetrics;
    private CharacterRenderMetrics.Placement playerPlacement;
    private NewParkWorld world;

    private float playerX;
    private float playerY;
    private float playerSpriteScale;

    private boolean debugCollisions;
    private boolean debugPlayerAnchor;
    private float playerStateTime;
    private Direction playerDirection = Direction.DOWN;

    private enum Direction {
        LEFT, RIGHT, UP, DOWN
    }

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();

        PlayerFrames right = loadPlayerFrames("walk_right");
        rightFrames = right.frames();
        rightMetrics = right.metrics();
        PlayerFrames front = loadPlayerFrames("walk_front");
        frontFrames = front.frames();
        frontMetrics = front.metrics();
        playerSpriteScale = PLAYER_FOOT_WIDTH / front.footWidth();
        PlayerFrames back = loadPlayerFrames("walk_back");
        backFrames = back.frames();
        backMetrics = back.metrics();

        world = new NewParkWorld(NewParkMapLayout.load());
        playerX = world.spawnX();
        playerY = world.spawnY();
        if (world.blocks(playerBodyAt(playerX, playerY))) {
            throw new IllegalStateException("New Park spawn is outside the walkable world");
        }

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
        batch.setProjectionMatrix(camera.combined);

        drawGround();
        drawRoads();
        drawRailway();
        drawPlayer();

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

        float distance = PLAYER_SPEED * delta;
        boolean moving = false;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            moveAlongAxes(distance, 0f);
            playerDirection = Direction.RIGHT;
            moving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            moveAlongAxes(-distance, 0f);
            playerDirection = Direction.LEFT;
            moving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            moveAlongAxes(0f, distance);
            playerDirection = Direction.UP;
            moving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            moveAlongAxes(0f, -distance);
            playerDirection = Direction.DOWN;
            moving = true;
        }

        playerStateTime = moving ? playerStateTime + delta : 0f;
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
            MathUtils.clamp(playerX, halfWidth, world.width() - halfWidth),
            MathUtils.clamp(playerY, halfHeight, world.height() - halfHeight),
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

    private void drawRoads() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (NewParkMapLayout.Road road : world.mapLayout().roads()) {
            float width = world.toWorldLength(road.widthPx())
                + 2f * NewParkWorld.units(NewParkWorld.SIDEWALK_WIDTH_METERS);
            drawMapLine(road.centerline(), width, WOOD_SIDEWALK);
        }
        for (NewParkMapLayout.Road road : world.mapLayout().roads()) {
            drawMapLine(road.centerline(), world.toWorldLength(road.widthPx()), ROAD_SURFACE);
        }
        shapes.end();
    }

    private void drawRailway() {
        List<List<NewParkMapLayout.MapPoint>> rails = world.mapLayout().railwayLines();
        if (rails.size() != 2) {
            return;
        }
        List<NewParkMapLayout.MapPoint> left = rails.get(0);
        List<NewParkMapLayout.MapPoint> right = rails.get(1);
        int count = Math.min(left.size(), right.size());

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(RAIL_BED);
        for (int i = 1; i < count; i++) {
            float x1 = world.toWorldX((left.get(i - 1).x() + right.get(i - 1).x()) / 2f);
            float y1 = world.toWorldY((left.get(i - 1).y() + right.get(i - 1).y()) / 2f);
            float x2 = world.toWorldX((left.get(i).x() + right.get(i).x()) / 2f);
            float y2 = world.toWorldY((left.get(i).y() + right.get(i).y()) / 2f);
            shapes.rectLine(x1, y1, x2, y2, world.toWorldLength(24f));
        }

        shapes.setColor(RAIL_TIE);
        for (int i = 1; i < count; i++) {
            NewParkMapLayout.MapPoint a0 = left.get(i - 1);
            NewParkMapLayout.MapPoint a1 = left.get(i);
            NewParkMapLayout.MapPoint b0 = right.get(i - 1);
            NewParkMapLayout.MapPoint b1 = right.get(i);
            float segmentLength = (float) Math.hypot(
                world.toWorldX(a1.x() - a0.x()), world.toWorldLength(a1.y() - a0.y())
            );
            int ties = Math.max(1, (int) (segmentLength / world.toWorldLength(8f)));
            for (int tie = 0; tie < ties; tie++) {
                float t = tie / (float) ties;
                float ax = world.toWorldX(a0.x() + (a1.x() - a0.x()) * t);
                float ay = world.toWorldY(a0.y() + (a1.y() - a0.y()) * t);
                float bx = world.toWorldX(b0.x() + (b1.x() - b0.x()) * t);
                float by = world.toWorldY(b0.y() + (b1.y() - b0.y()) * t);
                shapes.rectLine(ax, ay, bx, by, world.toWorldLength(2f));
            }
        }

        drawMapLine(left, world.toWorldLength(1f), RAIL_STEEL);
        drawMapLine(right, world.toWorldLength(1f), RAIL_STEEL);
        shapes.end();
    }

    private void drawMapLine(
        List<NewParkMapLayout.MapPoint> points, float width, Color color
    ) {
        shapes.setColor(color);
        for (int i = 1; i < points.size(); i++) {
            NewParkMapLayout.MapPoint from = points.get(i - 1);
            NewParkMapLayout.MapPoint to = points.get(i);
            shapes.rectLine(
                world.toWorldX(from.x()), world.toWorldY(from.y()),
                world.toWorldX(to.x()), world.toWorldY(to.y()), width
            );
        }
        for (NewParkMapLayout.MapPoint point : points) {
            shapes.circle(world.toWorldX(point.x()), world.toWorldY(point.y()), width / 2f);
        }
    }

    private void drawPlayer() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.28f);
        shapes.ellipse(playerX - 34f, playerY - 5f, 68f, 14f);
        shapes.end();

        int frameIndex = (int) (playerStateTime / PLAYER_FRAME_DURATION) % PLAYER_FRAME_COUNT;
        TextureRegion frame = switch (playerDirection) {
            case LEFT, RIGHT -> rightFrames[frameIndex];
            case UP -> backFrames[frameIndex];
            case DOWN -> frontFrames[frameIndex];
        };
        CharacterRenderMetrics.FrameMetrics metrics = switch (playerDirection) {
            case LEFT, RIGHT -> rightMetrics[frameIndex];
            case UP -> backMetrics[frameIndex];
            case DOWN -> frontMetrics[frameIndex];
        };

        batch.begin();
        playerPlacement = CharacterRenderMetrics.drawAtScale(
            batch, frame, metrics, playerX, playerY, playerSpriteScale,
            playerDirection == Direction.LEFT
        );
        batch.end();
    }

    private PlayerFrames loadPlayerFrames(String folder) {
        TextureRegion[] frames = new TextureRegion[PLAYER_FRAME_COUNT];
        CharacterRenderMetrics.FrameMetrics[] metrics =
            new CharacterRenderMetrics.FrameMetrics[PLAYER_FRAME_COUNT];
        int footWidth = 0;

        for (int i = 0; i < PLAYER_FRAME_COUNT; i++) {
            String path = String.format("%s/%s/%02d.png", PLAYER_BASE_PATH, folder, i);
            if (!Gdx.files.internal(path).exists()) {
                throw new IllegalStateException("Кадр Алкосмена не найден: " + path);
            }
            Texture texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            playerTextures.add(texture);
            frames[i] = new TextureRegion(texture);

            Pixmap pixels = new Pixmap(Gdx.files.internal(path));
            metrics[i] = CharacterRenderMetrics.scan(
                pixels, 0, 0, pixels.getWidth(), pixels.getHeight()
            );
            if (i == 0) {
                footWidth = CharacterRenderMetrics.footWidth(pixels, 0, 0, metrics[i]);
            }
            pixels.dispose();
        }
        return new PlayerFrames(frames, metrics, footWidth);
    }

    private record PlayerFrames(
        TextureRegion[] frames,
        CharacterRenderMetrics.FrameMetrics[] metrics,
        int footWidth
    ) {
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
        CharacterRenderMetrics.drawDebug(shapes, playerPlacement);
        shapes.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        for (Texture texture : playerTextures) {
            texture.dispose();
        }
        playerTextures.clear();
        if (batch != null) {
            batch.dispose();
        }
        if (shapes != null) {
            shapes.dispose();
        }
    }
}
