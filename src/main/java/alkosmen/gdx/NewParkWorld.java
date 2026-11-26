package alkosmen.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Каркас мира Нового парка.
 *
 * Пока здесь только крупные поверхности и минимальные коллизии.
 * Дома, деревья, заборы, лавки и прочие ассеты будут накладываться позже
 * поверх этой геометрии, не меняя базовую логику движения.
 */
public final class NewParkWorld {
    public static final float UNITS_PER_METER = 100f;
    private static final float REFERENCE_ROAD_WIDTH_METERS = 5.5f;
    public static final float REGULAR_ROAD_WIDTH_METERS = REFERENCE_ROAD_WIDTH_METERS / 2.5f;
    public static final float SIDEWALK_WIDTH_METERS = 1.25f;
    private static final float REGULAR_ROAD_WIDTH_IMAGE_PIXELS = 12f;
    private static final float MAP_SCALE =
        units(REFERENCE_ROAD_WIDTH_METERS) / REGULAR_ROAD_WIDTH_IMAGE_PIXELS;

    // Northern sidewalk near the Sverdlova junction, in reference-image pixels.
    private static final float SPAWN_IMAGE_X = 660f;
    private static final float SPAWN_IMAGE_Y = 614f;

    public static float units(float meters) {
        return meters * UNITS_PER_METER;
    }

    private final List<SurfaceZone> surfaces;
    private final List<ParkObject> objects;
    private final CollisionMap collisionMap;
    private final NewParkMapLayout mapLayout;
    private final float width;
    private final float height;
    private final float rotationCos;
    private final float rotationSin;
    private final float minRotatedX;
    private final float minRotatedY;

    public NewParkWorld(NewParkMapLayout mapLayout) {
        this.mapLayout = Objects.requireNonNull(mapLayout, "mapLayout");
        NewParkMapLayout.Road mainRoad = mapLayout.roads().stream()
            .filter(road -> road.id().equals("sverdlova"))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Main road is missing"));
        List<NewParkMapLayout.MapPoint> axis = mainRoad.centerline();
        NewParkMapLayout.MapPoint first = axis.get(0);
        NewParkMapLayout.MapPoint last = axis.get(axis.size() - 1);
        float angle = (float) Math.atan2(last.y() - first.y(), last.x() - first.x());
        this.rotationCos = (float) Math.cos(angle);
        this.rotationSin = (float) Math.sin(angle);

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (float x : new float[] {0f, mapLayout.imageWidth()}) {
            for (float y : new float[] {0f, mapLayout.imageHeight()}) {
                float uprightY = mapLayout.imageHeight() - y;
                float rotatedX = x * rotationCos - uprightY * rotationSin;
                float rotatedY = x * rotationSin + uprightY * rotationCos;
                minX = Math.min(minX, rotatedX);
                minY = Math.min(minY, rotatedY);
                maxX = Math.max(maxX, rotatedX);
                maxY = Math.max(maxY, rotatedY);
            }
        }
        this.minRotatedX = minX;
        this.minRotatedY = minY;
        this.width = (maxX - minX) * MAP_SCALE;
        this.height = (maxY - minY) * MAP_SCALE;
        List<SurfaceZone> ground = new ArrayList<>();
        ground.add(new SurfaceZone(
            SurfaceKind.GRASS,
            new Rectangle(0f, 0f, width, height)
        ));
        this.surfaces = Collections.unmodifiableList(ground);

        // На этапе каркаса намеренно почти нет препятствий.
        // Мир ограничивается его границами. Объекты появятся позже отдельным слоем.
        List<ParkObject> colliders = new ArrayList<>();
        this.objects = Collections.unmodifiableList(colliders);
        this.collisionMap = new CollisionMap(this.objects);
    }

    public NewParkMapLayout mapLayout() {
        return mapLayout;
    }

    public float spawnX() {
        return toWorldX(SPAWN_IMAGE_X, SPAWN_IMAGE_Y);
    }

    public float spawnY() {
        return toWorldY(SPAWN_IMAGE_X, SPAWN_IMAGE_Y);
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float toWorldX(float imageX, float imageY) {
        float uprightY = mapLayout.imageHeight() - imageY;
        return (imageX * rotationCos - uprightY * rotationSin - minRotatedX) * MAP_SCALE;
    }

    public float toWorldY(float imageX, float imageY) {
        float uprightY = mapLayout.imageHeight() - imageY;
        return (imageX * rotationSin + uprightY * rotationCos - minRotatedY) * MAP_SCALE;
    }

    public float toWorldLength(float imageLength) {
        return imageLength * MAP_SCALE;
    }

    public float roadWidthUnits(float imageWidth) {
        return units(REGULAR_ROAD_WIDTH_METERS)
            * imageWidth / REGULAR_ROAD_WIDTH_IMAGE_PIXELS;
    }

    public List<SurfaceZone> surfaces() {
        return surfaces;
    }

    public List<ParkObject> objects() {
        return objects;
    }

    public boolean blocks(Rectangle body) {
        if (!insideMap(body.x, body.y)
            || !insideMap(body.x + body.width, body.y)
            || !insideMap(body.x, body.y + body.height)
            || !insideMap(body.x + body.width, body.y + body.height)) {
            return true;
        }

        return collisionMap.blocks(body);
    }

    private boolean insideMap(float worldX, float worldY) {
        float rotatedX = worldX / MAP_SCALE + minRotatedX;
        float rotatedY = worldY / MAP_SCALE + minRotatedY;
        float imageX = rotatedX * rotationCos + rotatedY * rotationSin;
        float uprightY = -rotatedX * rotationSin + rotatedY * rotationCos;
        return imageX >= -0.001f && imageX <= mapLayout.imageWidth() + 0.001f
            && uprightY >= -0.001f && uprightY <= mapLayout.imageHeight() + 0.001f;
    }

    public enum SurfaceKind {
        YARD(new Color(0.47f, 0.36f, 0.25f, 1f)),
        SIDEWALK(new Color(0.58f, 0.57f, 0.51f, 1f)),
        ROAD(new Color(0.22f, 0.25f, 0.28f, 1f)),
        GRASS(new Color(0.20f, 0.39f, 0.20f, 1f));

        private final Color color;

        SurfaceKind(Color color) {
            this.color = color;
        }

        public Color color() {
            return color;
        }
    }

    public record SurfaceZone(SurfaceKind kind, Rectangle bounds) {
    }

    public enum ObjectKind {
        HOUSE,
        FENCE,
        TREE,
        BUSH,
        GRATE,
        BENCH,
        PLAYGROUND,
        FOUNTAIN,
        KIOSK
    }

    public record ParkObject(ObjectKind kind, Rectangle bounds, boolean solid, String label) {
        public static ParkObject obstacle(
            ObjectKind kind,
            float x,
            float y,
            float width,
            float height
        ) {
            return new ParkObject(kind, new Rectangle(x, y, width, height), true, "");
        }

        public static ParkObject obstacle(
            ObjectKind kind,
            float x,
            float y,
            float width,
            float height,
            String label
        ) {
            return new ParkObject(kind, new Rectangle(x, y, width, height), true, label);
        }
    }
}
