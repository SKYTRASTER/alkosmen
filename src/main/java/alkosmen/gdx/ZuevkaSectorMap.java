package alkosmen.gdx;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import alkosmen.gdx.items.StorageKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Geometry in source-image pixels, with Y converted to the libGDX world axis. */
final class ZuevkaSectorMap {
    static final String BASE = "alkosmen/maps/zuevka/sector_01/";
    final float width;
    final float height;
    final float spawnX;
    final float spawnY;
    private final List<Polygon> colliders = new ArrayList<>();
    private final List<String> colliderNames = new ArrayList<>();
    private final List<Interaction> interactions = new ArrayList<>();
    private final List<Occluder> occluders = new ArrayList<>();
    private final List<Flower> yardFlowers = new ArrayList<>();
    private final List<Rectangle> cleanedRegions = new ArrayList<>();
    private final List<Rectangle> yardClearRegions = new ArrayList<>();
    private final List<SceneBottle> sprites = new ArrayList<>();
    private final Polygon footprint = new Polygon(new float[8]);

    ZuevkaSectorMap(FileHandle file) {
        this(new JsonReader().parse(file));
    }

    ZuevkaSectorMap(JsonValue data) {
        width = data.getFloat("width");
        height = data.getFloat("height");
        spawnX = data.get("spawn").getFloat(0);
        spawnY = height - data.get("spawn").getFloat(1);
        for (JsonValue object : data.get("colliders")) {
            if (!object.getBoolean("enabled", true)) continue;
            JsonValue points = object.get("points");
            float[] vertices = new float[points.size * 2];
            int i = 0;
            for (JsonValue point : points) {
                vertices[i++] = point.getFloat(0);
                vertices[i++] = height - point.getFloat(1);
            }
            colliders.add(new Polygon(vertices));
            colliderNames.add(object.getString("id"));
        }
        for (JsonValue object : data.get("interactions")) {
            JsonValue point = object.get("point");
            interactions.add(new Interaction(object.getString("id"), object.getString("type"),
                point.getFloat(0), height - point.getFloat(1), object.getFloat("radius")));
        }
        for (JsonValue object : data.get("occluders")) {
            if (!object.getBoolean("enabled", true)) continue;
            JsonValue points = object.get("points");
            float[] vertices = new float[points.size * 2];
            int i = 0;
            for (JsonValue point : points) {
                vertices[i++] = point.getFloat(0);
                vertices[i++] = height - point.getFloat(1);
            }
            JsonValue anchor = object.get("anchor");
            occluders.add(new Occluder(object.getString("id"), anchor.getFloat(0),
                height - anchor.getFloat(1), vertices));
        }
        for (JsonValue object : data.get("yard_flowers")) {
            JsonValue bounds = object.get("bounds");
            JsonValue anchor = object.get("anchor");
            float boxHeight = bounds.getFloat(3);
            yardFlowers.add(new Flower(object.getString("id"),
                new Rectangle(bounds.getFloat(0), height - bounds.getFloat(1) - boxHeight,
                    bounds.getFloat(2), boxHeight),
                anchor.getFloat(0), height - anchor.getFloat(1)));
        }
        for (JsonValue object : data.get("cleaned_regions")) {
            JsonValue bounds = object.get("bounds");
            cleanedRegions.add(new Rectangle(bounds.getFloat(0), bounds.getFloat(1),
                bounds.getFloat(2), bounds.getFloat(3)));
        }
        for (JsonValue object : data.get("sprites")) {
            if (!object.getBoolean("enabled", true)) continue;
            if (!"bottle".equals(object.getString("type"))) {
                throw new IllegalArgumentException("Unsupported sector sprite type");
            }
            JsonValue rect = object.get("image_rect");
            JsonValue position = object.get("position");
            JsonValue size = object.get("size");
            float x = position.getFloat(0);
            float y = height - position.getFloat(1) - size.getFloat(1);
            sprites.add(new SceneBottle(object.getString("id"),
                rect.getInt(0), rect.getInt(1), rect.getInt(2), rect.getInt(3),
                x, y, size.getFloat(0), size.getFloat(1),
                new StorageKey(x + size.getFloat(0) / 2f, y, object.getFloat("pickup_radius"))));
        }
        for (JsonValue object : data.get("yard_clear_regions")) {
            JsonValue bounds = object.get("bounds");
            yardClearRegions.add(new Rectangle(bounds.getFloat(0), bounds.getFloat(1),
                bounds.getFloat(2), bounds.getFloat(3)));
        }
    }

    boolean blocks(Rectangle feet) {
        if (feet.x < 0 || feet.y < 0 || feet.x + feet.width > width || feet.y + feet.height > height) {
            return true;
        }
        float x = feet.x, y = feet.y, right = x + feet.width, top = y + feet.height;
        footprint.setVertices(new float[] {x, y, right, y, right, top, x, top});
        for (Polygon collider : colliders) {
            if (Intersector.overlapConvexPolygons(footprint, collider)) return true;
        }
        return false;
    }

    List<Polygon> colliders() {
        return Collections.unmodifiableList(colliders);
    }

    String colliderName(int index) {
        return colliderNames.get(index);
    }

    List<Interaction> interactions() {
        return Collections.unmodifiableList(interactions);
    }

    List<Occluder> occluders() {
        return Collections.unmodifiableList(occluders);
    }

    List<Flower> yardFlowers() {
        return Collections.unmodifiableList(yardFlowers);
    }

    List<Rectangle> cleanedRegions() {
        return Collections.unmodifiableList(cleanedRegions);
    }

    List<Rectangle> yardClearRegions() {
        return Collections.unmodifiableList(yardClearRegions);
    }

    List<SceneBottle> sprites() {
        return Collections.unmodifiableList(sprites);
    }

    SceneBottle collectNearestBottle(float x, float y) {
        SceneBottle nearest = null;
        float bestDistance = Float.POSITIVE_INFINITY;
        for (SceneBottle bottle : sprites) {
            if (bottle.pickup().collected() || !bottle.pickup().isNear(x, y)) continue;
            float dx = bottle.pickup().x() - x;
            float dy = bottle.pickup().y() - y;
            float distance = dx * dx + dy * dy;
            if (distance < bestDistance) {
                nearest = bottle;
                bestDistance = distance;
            }
        }
        if (nearest != null) nearest.pickup().collect();
        return nearest;
    }

    int collectedBottleCount() {
        int count = 0;
        for (SceneBottle bottle : sprites) {
            if (bottle.pickup().collected()) count++;
        }
        return count;
    }

    record Flower(String id, Rectangle bounds, float x, float y) { }

    record SceneBottle(String id, int sourceX, int sourceY, int sourceWidth, int sourceHeight,
                       float x, float y, float width, float height, StorageKey pickup) {
        boolean inFrontOf(float footY) {
            return footY > y;
        }
    }

    record Occluder(String id, float x, float y, float[] vertices) {
        boolean inFrontOf(float footY) {
            return footY > y;
        }
    }

    record Interaction(String id, String type, float x, float y, float radius) {
        boolean inRange(float playerX, float playerY) {
            float dx = playerX - x, dy = playerY - y;
            return dx * dx + dy * dy <= radius * radius;
        }
    }
}
