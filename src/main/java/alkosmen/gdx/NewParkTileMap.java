package alkosmen.gdx;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A first playable park sector. The TMX file owns surfaces and collision geometry. */
public final class NewParkTileMap implements Disposable {
    public static final float TILE_WORLD = 50f;
    public static final int TILES_X = 64, TILES_Y = 36;
    static final String MAP_PATH = "alkosmen/maps/new_park/first_sector.tmx";
    private static final float SCALE = TILE_WORLD / 16f;
    private final TiledMap map;
    private final OrthogonalTiledMapRenderer renderer;
    private final int[] groundLayers;
    private final TiledMapTileLayer walkable;
    private final List<Rectangle> solids = new ArrayList<>();
    private final List<SceneryTile> scenery = new ArrayList<>();

    public NewParkTileMap(SpriteBatch batch) {
        map = new TmxMapLoader().load(MAP_PATH);
        renderer = new OrthogonalTiledMapRenderer(map, SCALE, batch);
        walkable = tileLayer("paths");
        TiledMapTileLayer groundProps = new TiledMapTileLayer(TILES_X, TILES_Y, 16, 16);
        collectScenery(tileLayer("props"), groundProps);
        collectScenery(tileLayer("foreground"), groundProps);
        scenery.sort(Comparator.comparingDouble(SceneryTile::depth).reversed());
        map.getLayers().add(groundProps);
        groundLayers = new int[] {map.getLayers().getIndex("ground"),
            map.getLayers().getIndex("paths"), map.getLayers().getCount() - 1};
        MapLayer layer = map.getLayers().get("solid_objects");
        if (layer == null) throw new IllegalStateException("Missing solid_objects layer in TMX");
        for (MapObject o : layer.getObjects()) {
            if (!(o instanceof RectangleMapObject)) throw new IllegalStateException("Expected rectangle object");
            Rectangle r = ((RectangleMapObject) o).getRectangle();
            solids.add(worldRectangle(r));
        }
    }

    private TiledMapTileLayer tileLayer(String name) {
        if (map.getLayers().get(name) instanceof TiledMapTileLayer layer) return layer;
        throw new IllegalStateException("Missing tile layer: " + name);
    }

    private void collectScenery(TiledMapTileLayer layer, TiledMapTileLayer groundProps) {
        for (int y = 0; y < layer.getHeight(); y++) {
            for (int x = 0; x < layer.getWidth(); x++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell == null || cell.getTile() == null) continue;
                TiledMapTile tile = cell.getTile();
                Float offset = tile.getProperties().get("depthFromBottom", Float.class);
                if (offset == null) {
                    groundProps.setCell(x, y, cell);
                } else {
                    float bottom = y * TILE_WORLD;
                    scenery.add(new SceneryTile(tile, x * TILE_WORLD, bottom, bottom + offset * SCALE));
                }
            }
        }
    }

    public void renderGround(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render(groundLayers);
    }

    /** Draw north to south, inserting the hero at the Y coordinate of his feet. */
    public void renderScenery(SpriteBatch batch, AlkosmenPlayer hero) {
        boolean heroDrawn = false;
        for (SceneryTile part : scenery) {
            if (!heroDrawn && coversHero(part.depth(), hero.y())) {
                hero.render(batch);
                heroDrawn = true;
            }
            batch.draw(part.tile().getTextureRegion(), part.x(), part.y(), TILE_WORLD, TILE_WORLD);
        }
        if (!heroDrawn) hero.render(batch);
    }

    static boolean coversHero(float sceneryDepth, float footY) {
        return sceneryDepth < footY;
    }

    static Rectangle worldRectangle(Rectangle pixels) {
        return new Rectangle(pixels.x * SCALE, pixels.y * SCALE,
            pixels.width * SCALE, pixels.height * SCALE);
    }

    public boolean blocks(Rectangle body) {
        return blocks(walkable, solids, body);
    }

    static boolean blocks(TiledMapTileLayer walkable, List<Rectangle> solids, Rectangle feet) {
        double right = (double) feet.x + feet.width;
        double top = (double) feet.y + feet.height;
        if (feet.x < 0f || feet.y < 0f || right > walkable.getWidth() * TILE_WORLD
            || top > walkable.getHeight() * TILE_WORLD) return true;

        // Touching the border is legal. Crossing it requires a walkable neighbor.
        int minX = (int) (feet.x / TILE_WORLD);
        int minY = (int) (feet.y / TILE_WORLD);
        int endX = (int) Math.ceil(right / TILE_WORLD);
        int endY = (int) Math.ceil(top / TILE_WORLD);
        for (int y = minY; y < endY; y++) {
            for (int x = minX; x < endX; x++) {
                TiledMapTileLayer.Cell cell = walkable.getCell(x, y);
                if (cell == null || cell.getTile() == null || !Boolean.TRUE.equals(
                    cell.getTile().getProperties().get("walkable"))) return true;
            }
        }
        for (Rectangle obstacle : solids) if (obstacle.overlaps(feet)) return true;
        return false;
    }

    public List<Rectangle> colliders() {
        return solids;
    }

    public float width() { return TILES_X * TILE_WORLD; }
    public float height() { return TILES_Y * TILE_WORLD; }

    @Override public void dispose() { renderer.dispose(); map.dispose(); }

    private record SceneryTile(TiledMapTile tile, float x, float y, float depth) { }
}
