package alkosmen.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import alkosmen.gdx.render.CharacterRenderMetrics;
import alkosmen.gdx.render.AlkosmenWalkAnimation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

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

    // Movement speed uses the same meters as the map and collision body.
    private static final float PLAYER_SPEED = NewParkWorld.units(1.35f);
    private static final float PLAYER_BODY_WIDTH = NewParkWorld.units(0.48f);
    private static final float PLAYER_BODY_HEIGHT = NewParkWorld.units(0.28f);
    private static final float PLAYER_BODY_Y_OFFSET = NewParkWorld.units(0.06f);
    private static final float PLAYER_VISIBLE_HEIGHT = NewParkWorld.units(1.7f);
    private static final Color WOOD_SIDEWALK = new Color(0.46f, 0.34f, 0.22f, 1f);
    private static final Color ROAD_SURFACE = new Color(0.26f, 0.28f, 0.27f, 1f);
    private static final Color RAIL_BED = new Color(0.28f, 0.25f, 0.21f, 1f);
    private static final Color RAIL_TIE = new Color(0.38f, 0.29f, 0.21f, 1f);
    private static final Color RAIL_STEEL = new Color(0.70f, 0.69f, 0.65f, 1f);

    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private AlkosmenWalkAnimation playerAnimation;
    private CharacterRenderMetrics.Placement playerPlacement;
    private NewParkWorld world;

    private float playerX;
    private float playerY;

    private boolean debugCollisions;
    private boolean debugPlayerAnchor;
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

        playerAnimation = new AlkosmenWalkAnimation();

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

        float moveX = 0f;
        float moveY = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) moveX += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) moveX -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) moveY += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) moveY -= 1f;

        if (moveX == 0f && moveY == 0f) {
            playerAnimation.reset();
            return;
        }
        if (Math.abs(moveX) >= Math.abs(moveY)) {
            playerDirection = moveX < 0f ? Direction.LEFT : Direction.RIGHT;
        } else {
            playerDirection = moveY < 0f ? Direction.DOWN : Direction.UP;
        }
        float length = (float) Math.hypot(moveX, moveY);
        float distance = PLAYER_SPEED * delta;
        float previousX = playerX;
        float previousY = playerY;
        moveAlongAxes(moveX / length * distance, moveY / length * distance);
        playerAnimation.advance(
            (float) Math.hypot(playerX - previousX, playerY - previousY), PLAYER_VISIBLE_HEIGHT);
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
            float width = world.roadWidthUnits(road.widthPx())
                + 2f * NewParkWorld.units(NewParkWorld.SIDEWALK_WIDTH_METERS);
            drawMapLine(road.centerline(), width, WOOD_SIDEWALK);
        }
        for (NewParkMapLayout.Road road : world.mapLayout().roads()) {
            drawMapLine(road.centerline(), world.roadWidthUnits(road.widthPx()), ROAD_SURFACE);
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
            NewParkMapLayout.MapPoint a0 = left.get(i - 1);
            NewParkMapLayout.MapPoint a1 = left.get(i);
            NewParkMapLayout.MapPoint b0 = right.get(i - 1);
            NewParkMapLayout.MapPoint b1 = right.get(i);
            drawImageLine(
                (a0.x() + b0.x()) / 2f, (a0.y() + b0.y()) / 2f,
                (a1.x() + b1.x()) / 2f, (a1.y() + b1.y()) / 2f,
                world.toWorldLength(24f)
            );
        }

        shapes.setColor(RAIL_TIE);
        for (int i = 1; i < count; i++) {
            NewParkMapLayout.MapPoint a0 = left.get(i - 1);
            NewParkMapLayout.MapPoint a1 = left.get(i);
            NewParkMapLayout.MapPoint b0 = right.get(i - 1);
            NewParkMapLayout.MapPoint b1 = right.get(i);
            float segmentLength = (float) Math.hypot(
                a1.x() - a0.x(), a1.y() - a0.y()
            ) * world.toWorldLength(1f);
            int ties = Math.max(1, (int) (segmentLength / world.toWorldLength(8f)));
            for (int tie = 0; tie < ties; tie++) {
                float t = tie / (float) ties;
                drawImageLine(
                    a0.x() + (a1.x() - a0.x()) * t,
                    a0.y() + (a1.y() - a0.y()) * t,
                    b0.x() + (b1.x() - b0.x()) * t,
                    b0.y() + (b1.y() - b0.y()) * t,
                    world.toWorldLength(2f)
                );
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
            drawImageLine(from.x(), from.y(), to.x(), to.y(), width);
        }
        for (NewParkMapLayout.MapPoint point : points) {
            shapes.circle(
                world.toWorldX(point.x(), point.y()),
                world.toWorldY(point.x(), point.y()),
                width / 2f
            );
        }
    }

    private void drawImageLine(
        float x1, float y1, float x2, float y2, float width
    ) {
        shapes.rectLine(
            world.toWorldX(x1, y1), world.toWorldY(x1, y1),
            world.toWorldX(x2, y2), world.toWorldY(x2, y2), width
        );
    }

    private void drawPlayer() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.28f);
        shapes.ellipse(playerX - 34f, playerY - 5f, 68f, 14f);
        shapes.end();

        batch.begin();
        playerPlacement = playerAnimation.draw(batch,
            AlkosmenWalkAnimation.Facing.valueOf(playerDirection.name()),
            playerX, playerY, PLAYER_VISIBLE_HEIGHT);
        batch.end();
    }

    private void drawCollisionDebug() {
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.YELLOW);
        float mapWidth = world.mapLayout().imageWidth();
        float mapHeight = world.mapLayout().imageHeight();
        float[][] corners = {
            {0f, 0f}, {mapWidth, 0f}, {mapWidth, mapHeight}, {0f, mapHeight}
        };
        for (int i = 0; i < corners.length; i++) {
            float[] from = corners[i];
            float[] to = corners[(i + 1) % corners.length];
            shapes.line(
                world.toWorldX(from[0], from[1]), world.toWorldY(from[0], from[1]),
                world.toWorldX(to[0], to[1]), world.toWorldY(to[0], to[1])
            );
        }

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
        if (playerAnimation != null) {
            playerAnimation.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
        if (shapes != null) {
            shapes.dispose();
        }
    }
}
