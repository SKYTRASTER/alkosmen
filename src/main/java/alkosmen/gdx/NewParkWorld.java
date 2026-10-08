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
    public static final float WIDTH = 3840f;
    public static final float HEIGHT = 1920f;

    public static final float SPAWN_X = 1320f;
    public static final float SPAWN_Y = 780f;

    private final List<SurfaceZone> surfaces;
    private final List<ParkObject> objects;
    private final CollisionMap collisionMap;
    private final NewParkMapLayout mapLayout;

    public NewParkWorld(NewParkMapLayout mapLayout) {
        this.mapLayout = Objects.requireNonNull(mapLayout, "mapLayout");
        List<SurfaceZone> ground = new ArrayList<>();

        // Сначала только крупная геометрия уровня.
        ground.add(new SurfaceZone(
            SurfaceKind.YARD,
            new Rectangle(0f, 1180f, WIDTH, 740f)
        ));
        ground.add(new SurfaceZone(
            SurfaceKind.SIDEWALK,
            new Rectangle(0f, 1060f, WIDTH, 120f)
        ));
        ground.add(new SurfaceZone(
            SurfaceKind.ROAD,
            new Rectangle(0f, 520f, WIDTH, 540f)
        ));
        ground.add(new SurfaceZone(
            SurfaceKind.SIDEWALK,
            new Rectangle(0f, 400f, WIDTH, 120f)
        ));
        ground.add(new SurfaceZone(
            SurfaceKind.GRASS,
            new Rectangle(0f, 0f, WIDTH, 400f)
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

    public List<SurfaceZone> surfaces() {
        return surfaces;
    }

    public List<ParkObject> objects() {
        return objects;
    }

    public boolean blocks(Rectangle body) {
        if (body.x < 0f
            || body.y < 0f
            || body.x + body.width > WIDTH
            || body.y + body.height > HEIGHT) {
            return true;
        }

        return collisionMap.blocks(body);
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
