package alkosmen.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Independent prototype for the illustrated first Zuevka sector. */
public final class ZuevkaSectorGame extends ApplicationAdapter {
    private static final float VIEW_WIDTH = 1000f;
    private static final float VIEW_HEIGHT = 562.5f;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private Texture background;
    private AlkosmenPlayer hero;
    private ZuevkaSectorMap map;
    private boolean showDebug;

    @Override public void create() {
        map = new ZuevkaSectorMap(Gdx.files.internal(ZuevkaSectorMap.BASE + "objects.json"));
        background = new Texture(Gdx.files.internal(ZuevkaSectorMap.BASE + "background.png"));
        if (background.getWidth() != map.width || background.getHeight() != map.height) {
            throw new IllegalStateException("Sector geometry does not match background dimensions");
        }
        background.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, camera);
        hero = new AlkosmenPlayer(map.spawnX, map.spawnY);
        hero.setVisibleHeight(105f);
        if (map.blocks(hero.collisionBody())) throw new IllegalStateException("Sector spawn is blocked");
        follow();
    }

    @Override public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), .05f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) showDebug = !showDebug;
        hero.update(delta, map::blocks);
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            for (ZuevkaSectorMap.Interaction object : map.interactions()) {
                if (object.inRange(hero.x(), hero.y())) {
                    Gdx.app.log("Zuevka", "Точка " + object.id() + " (" + object.type() + "): пока заглушка");
                    break;
                }
            }
        }
        follow();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(background, 0, 0, map.width, map.height);
        hero.render(batch);
        batch.end();
        if (showDebug) drawDebug();
    }

    private void drawDebug() {
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.RED);
        for (Polygon collider : map.colliders()) shapes.polygon(collider.getTransformedVertices());
        shapes.setColor(Color.CYAN);
        for (ZuevkaSectorMap.Interaction object : map.interactions()) {
            shapes.circle(object.x(), object.y(), object.radius(), 40);
        }
        hero.drawDebug(shapes);
        shapes.end();
    }

    private void follow() {
        float halfWidth = viewport.getWorldWidth() / 2f;
        float halfHeight = viewport.getWorldHeight() / 2f;
        camera.position.set(MathUtils.clamp(hero.x(), halfWidth, map.width - halfWidth),
            MathUtils.clamp(hero.y(), halfHeight, map.height - halfHeight), 0f);
    }

    @Override public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (hero != null) follow();
    }

    @Override public void dispose() {
        if (hero != null) hero.dispose();
        if (background != null) background.dispose();
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
    }
}
