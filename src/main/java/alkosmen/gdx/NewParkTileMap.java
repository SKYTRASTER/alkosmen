package alkosmen.gdx;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.List;

/** A first playable park sector. The TMX file owns surfaces and collision geometry. */
public final class NewParkTileMap implements Disposable {
    public static final float TILE_WORLD = 50f;
    public static final int TILES_X = 64, TILES_Y = 36;
    private static final float SCALE = TILE_WORLD / 16f;
    private final TiledMap map;
    private final OrthogonalTiledMapRenderer renderer;
    private final List<Rectangle> solids = new ArrayList<>();

    public NewParkTileMap() {
        map = new TmxMapLoader().load("alkosmen/maps/new_park/first_sector.tmx");
        renderer = new OrthogonalTiledMapRenderer(map, SCALE);
        MapLayer layer = map.getLayers().get("solid_objects");
        if (layer == null) throw new IllegalStateException("Missing solid_objects layer in TMX");
        for (MapObject o : layer.getObjects()) {
            if (!(o instanceof RectangleMapObject)) throw new IllegalStateException("Expected rectangle object");
            Rectangle r = ((RectangleMapObject) o).getRectangle();
            solids.add(new Rectangle(r.x * SCALE, r.y * SCALE, r.width * SCALE, r.height * SCALE));
        }
    }

    public void renderGround(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render(new int[] {0, 1, 2});
    }

    public void renderForeground(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render(new int[] {4});
    }

    public boolean blocks(Rectangle body) {
        if (body.x < 0f || body.y < 0f || body.x + body.width > width()
            || body.y + body.height > height()) return true;
        for (Rectangle obstacle : solids) if (obstacle.overlaps(body)) return true;
        return false;
    }

    public List<Rectangle> colliders() {
        return solids;
    }

    public float width() { return TILES_X * TILE_WORLD; }
    public float height() { return TILES_Y * TILE_WORLD; }

    @Override public void dispose() { renderer.dispose(); map.dispose(); }
}
