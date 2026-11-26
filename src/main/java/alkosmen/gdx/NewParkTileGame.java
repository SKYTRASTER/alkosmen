package alkosmen.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import alkosmen.gdx.ui.SprintHud;

/** Small playable map prototype without changing the original road-reference renderer. */
public final class NewParkTileGame extends ApplicationAdapter {
    private static final float VIEW_W = 1600f, VIEW_H = 900f;
    static final float SPAWN_X = 32.5f * NewParkTileMap.TILE_WORLD;
    static final float SPAWN_Y = (NewParkTileMap.TILES_Y - 27.5f) * NewParkTileMap.TILE_WORLD;
    private final SprintState sprint = new SprintState();
    private OrthographicCamera camera;
    private Viewport viewport;
    private NewParkTileMap map;
    private AlkosmenPlayer hero;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private SprintHud hud;
    private boolean showCollisions;

    @Override public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        batch = new SpriteBatch();
        map = new NewParkTileMap(batch);
        shapes = new ShapeRenderer();
        hud = new SprintHud();
        hud.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hero = new AlkosmenPlayer(SPAWN_X, SPAWN_Y);
        if (map.blocks(hero.collisionBody())) throw new IllegalStateException("Park spawn is blocked");
        follow();
    }

    @Override public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), .05f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) showCollisions = !showCollisions;
        boolean run = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
            || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        float px = hero.x(), py = hero.y();
        hero.setSpeed(AlkosmenPlayer.DEFAULT_SPEED * sprint.speedMultiplier(delta, run));
        hero.update(delta, map::blocks);
        boolean moved = Math.abs(hero.x() - px) + Math.abs(hero.y() - py) > .0001f;
        sprint.update(delta, run, moved);
        follow();
        Gdx.gl.glClearColor(.09f, .13f, .1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        camera.update();
        map.renderGround(camera);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        map.renderScenery(batch, hero);
        batch.end();
        if (showCollisions) {
            shapes.setProjectionMatrix(camera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(Color.YELLOW);
            for (Rectangle r : map.colliders()) shapes.rect(r.x,r.y,r.width,r.height);
            hero.drawDebug(shapes);
            shapes.end();
        }
        hud.render(shapes, batch, sprint);
    }

    private void follow() {
        float halfX=viewport.getWorldWidth()/2f, halfY=viewport.getWorldHeight()/2f;
        camera.position.set(
            MathUtils.clamp(hero.x(),halfX,Math.max(halfX,map.width()-halfX)),
            MathUtils.clamp(hero.y(),halfY,Math.max(halfY,map.height()-halfY)),0f);
    }

    @Override public void resize(int w,int h) {
        viewport.update(w,h);
        if (hud!=null) hud.resize(w,h);
        if (hero!=null) follow();
    }

    @Override public void dispose() {
        if(hero!=null) hero.dispose();
        if(hud!=null) hud.dispose();
        if(shapes!=null) shapes.dispose();
        if(batch!=null) batch.dispose();
        if(map!=null) map.dispose();
    }
}
