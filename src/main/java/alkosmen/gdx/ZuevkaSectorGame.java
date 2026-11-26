package alkosmen.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.ArrayList;
import java.util.List;
import alkosmen.gdx.ui.ObjectiveHud;

/** Independent prototype for the illustrated first Zuevka sector. */
public final class ZuevkaSectorGame extends ApplicationAdapter {
    private static final float VIEW_WIDTH = 1000f;
    private static final float VIEW_HEIGHT = 562.5f;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private PolygonSpriteBatch foregroundBatch;
    private final List<PolygonRegion> foreground = new ArrayList<>();
    private ShapeRenderer shapes;
    private BitmapFont debugFont;
    private BitmapFont hudFont;
    private Sound bottleSound;
    private Texture background;
    private Texture cleanedReference;
    private Texture bottleAtlas;
    private ShaderProgram cleanupShader;
    private final List<TextureRegion> spriteRegions = new ArrayList<>();
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
        cleanedReference = new Texture(Gdx.files.internal(ZuevkaSectorMap.BASE + "yard_cleaned_reference.png"));
        cleanedReference.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        bottleAtlas = new Texture(Gdx.files.internal("alkosmen/images/objects/bottle/bottle_tich_table.png"));
        bottleAtlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (ZuevkaSectorMap.SceneBottle sprite : map.sprites()) {
            spriteRegions.add(new TextureRegion(bottleAtlas, sprite.sourceX(), sprite.sourceY(),
                sprite.sourceWidth(), sprite.sourceHeight()));
        }
        cleanupShader = createCleanupShader();
        batch = new SpriteBatch();
        foregroundBatch = new PolygonSpriteBatch();
        TextureRegion source = new TextureRegion(background);
        EarClippingTriangulator triangulator = new EarClippingTriangulator();
        for (ZuevkaSectorMap.Occluder occluder : map.occluders()) {
            float[] vertices = occluder.vertices();
            short[] triangles = triangulator.computeTriangles(vertices).toArray();
            foreground.add(new PolygonRegion(source, vertices, triangles));
        }
        shapes = new ShapeRenderer();
        debugFont = new BitmapFont();
        hudFont = ObjectiveHud.createRussianFont();
        bottleSound = Gdx.audio.newSound(Gdx.files.internal("alkosmen/sounds/scratch_bottle.wav"));
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_WIDTH, VIEW_HEIGHT, camera);
        hero = new AlkosmenPlayer(map.spawnX, map.spawnY);
        hero.setVisibleHeight(120f);
        if (map.blocks(hero.collisionBody())) throw new IllegalStateException("Sector spawn is blocked");
        follow();
    }

    @Override public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), .05f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) showDebug = !showDebug;
        hero.update(delta, map::blocks);
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            ZuevkaSectorMap.SceneBottle bottle = map.collectNearestBottle(hero.x(), hero.y());
            if (bottle != null) {
                bottleSound.play();
            } else {
                for (ZuevkaSectorMap.Interaction object : map.interactions()) {
                    if (object.inRange(hero.x(), hero.y())) {
                        Gdx.app.log("Zuevka", "Точка " + object.id() + " (" + object.type() + "): пока заглушка");
                        break;
                    }
                }
            }
        }
        follow();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.setShader(cleanupShader);
        cleanedReference.bind(1);
        background.bind(0);
        batch.begin();
        cleanupShader.setUniformi("u_cleaned", 1);
        cleanupShader.setUniformf("u_imageSize", map.width, map.height);
        setCleanupBounds("u_flowerBounds", map.cleanedRegions().get(0));
        setCleanupBounds("u_tableBounds", map.cleanedRegions().get(1));
        batch.draw(background, 0, 0, map.width, map.height);
        batch.end();
        batch.setShader(null);
        batch.begin();
        for (int i = 0; i < map.sprites().size(); i++) {
            if (!map.sprites().get(i).inFrontOf(hero.y())) drawSprite(i);
        }
        hero.render(batch);
        batch.end();
        foregroundBatch.setProjectionMatrix(camera.combined);
        foregroundBatch.begin();
        for (int i = 0; i < map.occluders().size(); i++) {
            if (map.occluders().get(i).inFrontOf(hero.y())) {
                foregroundBatch.draw(foreground.get(i), 0, 0);
            }
        }
        foregroundBatch.end();
        batch.begin();
        for (int i = 0; i < map.sprites().size(); i++) {
            if (map.sprites().get(i).inFrontOf(hero.y())) drawSprite(i);
        }
        batch.end();
        if (showDebug) drawDebug();
        drawBottleCount();
    }

    private void setCleanupBounds(String name, Rectangle box) {
        cleanupShader.setUniformf(name, box.x, box.y, box.x + box.width, box.y + box.height);
    }

    private void drawSprite(int index) {
        ZuevkaSectorMap.SceneBottle sprite = map.sprites().get(index);
        if (!sprite.pickup().collected()) {
            batch.draw(spriteRegions.get(index), sprite.x(), sprite.y(), sprite.width(), sprite.height());
        }
    }

    private void drawBottleCount() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, "ТиЧ: " + map.collectedBottleCount() + "/" + map.sprites().size(),
            camera.position.x - viewport.getWorldWidth() / 2f + 18f,
            camera.position.y + viewport.getWorldHeight() / 2f - 18f);
        batch.end();
    }

    private static ShaderProgram createCleanupShader() {
        String vertex = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec4 v_color;
            varying vec2 v_texCoords;
            void main() {
                v_color = a_color;
                v_color.a = v_color.a * (255.0 / 254.0);
                v_texCoords = a_texCoord0;
                gl_Position = u_projTrans * a_position;
            }
            """;
        String fragment = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec4 v_color;
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform sampler2D u_cleaned;
            uniform vec2 u_imageSize;
            uniform vec4 u_flowerBounds;
            uniform vec4 u_tableBounds;
            float regionOpacity(vec2 point, vec4 bounds) {
                vec2 inside = min(point - bounds.xy, bounds.zw - point);
                return smoothstep(0.0, 12.0, min(inside.x, inside.y));
            }
            void main() {
                vec2 point = v_texCoords * u_imageSize;
                float opacity = max(regionOpacity(point, u_flowerBounds),
                                    regionOpacity(point, u_tableBounds));
                vec4 original = texture2D(u_texture, v_texCoords);
                vec4 cleaned = texture2D(u_cleaned, v_texCoords);
                gl_FragColor = v_color * mix(original, cleaned, opacity);
            }
            """;
        ShaderProgram shader = new ShaderProgram(vertex, fragment);
        if (!shader.isCompiled()) {
            String error = shader.getLog();
            shader.dispose();
            throw new IllegalStateException("Cannot compile sector background shader: " + error);
        }
        return shader;
    }

    private void drawDebug() {
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.RED);
        for (Polygon collider : map.colliders()) shapes.polygon(collider.getTransformedVertices());
        shapes.setColor(Color.GREEN);
        for (ZuevkaSectorMap.Occluder object : map.occluders()) {
            shapes.polygon(object.vertices());
            shapes.line(object.x() - 6, object.y(), object.x() + 6, object.y());
            shapes.line(object.x(), object.y() - 6, object.x(), object.y() + 6);
        }
        shapes.setColor(Color.MAGENTA);
        for (ZuevkaSectorMap.Flower flower : map.yardFlowers()) {
            shapes.rect(flower.bounds().x, flower.bounds().y, flower.bounds().width, flower.bounds().height);
            shapes.line(flower.x() - 6, flower.y(), flower.x() + 6, flower.y());
            shapes.line(flower.x(), flower.y() - 6, flower.x(), flower.y() + 6);
        }
        shapes.setColor(Color.ORANGE);
        for (ZuevkaSectorMap.SceneBottle sprite : map.sprites()) {
            if (!sprite.pickup().collected()) shapes.rect(sprite.x(), sprite.y(), sprite.width(), sprite.height());
        }
        hero.drawDebug(shapes);
        shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        debugFont.setColor(Color.RED);
        for (int i = 0; i < map.colliders().size(); i++) {
            Polygon collider = map.colliders().get(i);
            float[] vertices = collider.getTransformedVertices();
            debugFont.draw(batch, map.colliderName(i), vertices[0], vertices[1]);
        }
        debugFont.setColor(Color.GREEN);
        for (ZuevkaSectorMap.Occluder object : map.occluders()) {
            debugFont.draw(batch, object.id(), object.x() + 8, object.y() + 12);
        }
        debugFont.setColor(Color.MAGENTA);
        for (ZuevkaSectorMap.Flower flower : map.yardFlowers()) {
            debugFont.draw(batch, flower.id(), flower.bounds().x, flower.bounds().y + flower.bounds().height);
        }
        debugFont.setColor(Color.ORANGE);
        for (ZuevkaSectorMap.SceneBottle sprite : map.sprites()) {
            if (!sprite.pickup().collected()) {
                debugFont.draw(batch, sprite.id(), sprite.x(), sprite.y() + sprite.height());
            }
        }
        batch.end();
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
        if (cleanedReference != null) cleanedReference.dispose();
        if (bottleAtlas != null) bottleAtlas.dispose();
        if (cleanupShader != null) cleanupShader.dispose();
        if (batch != null) batch.dispose();
        if (foregroundBatch != null) foregroundBatch.dispose();
        if (shapes != null) shapes.dispose();
        if (debugFont != null) debugFont.dispose();
        if (hudFont != null) hudFont.dispose();
        if (bottleSound != null) bottleSound.dispose();
    }
}
