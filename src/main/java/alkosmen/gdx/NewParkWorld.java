package alkosmen.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * World data is deliberately separate from the game screen: artwork and NPCs can
 * replace the temporary geometry without changing movement or collision code.
 */
public final class NewParkWorld {
    public static final float WIDTH = 3840f;
    public static final float HEIGHT = 2560f;
    public static final float SPAWN_X = 870f;
    public static final float SPAWN_Y = 970f;

    private final List<ParkObject> objects;

    public NewParkWorld() {
        List<ParkObject> layout = new ArrayList<>();

        // Sverdlova 12 and the surrounding houses.
        layout.add(ParkObject.house("Sverdlova 12", 1060f, 1120f, 570f, 410f));
        layout.add(ParkObject.house("House", 2120f, 1560f, 500f, 350f));
        layout.add(ParkObject.house("House", 2920f, 660f, 520f, 390f));

        // Fences make the district readable now and become collision data later too.
        layout.add(ParkObject.fence(780f, 850f, 1060f, 34f));
        layout.add(ParkObject.fence(780f, 850f, 34f, 520f));
        layout.add(ParkObject.fence(1806f, 850f, 34f, 250f));
        layout.add(ParkObject.fence(1806f, 1280f, 34f, 380f));
        layout.add(ParkObject.fence(2020f, 1390f, 700f, 30f));
        layout.add(ParkObject.fence(2710f, 1390f, 30f, 600f));
        layout.add(ParkObject.fence(2810f, 510f, 720f, 30f));

        // The tree crowns are decoration; their trunks are the actual blockers.
        layout.add(ParkObject.tree(350f, 420f));
        layout.add(ParkObject.tree(540f, 1860f));
        layout.add(ParkObject.tree(860f, 2020f));
        layout.add(ParkObject.tree(1880f, 460f));
        layout.add(ParkObject.tree(2420f, 680f));
        layout.add(ParkObject.tree(3090f, 1940f));
        layout.add(ParkObject.tree(3490f, 1450f));
        layout.add(ParkObject.tree(3610f, 360f));

        layout.add(ParkObject.obstacle(ObjectKind.BENCH, 2280f, 930f, 180f, 52f));
        layout.add(ParkObject.obstacle(ObjectKind.PLAYGROUND, 3200f, 1740f, 220f, 180f));
        layout.add(ParkObject.obstacle(ObjectKind.FOUNTAIN, 1860f, 2050f, 190f, 190f));

        this.objects = Collections.unmodifiableList(layout);
    }

    public List<ParkObject> objects() {
        return objects;
    }

    public boolean blocks(Rectangle body) {
        if (body.x < 0f || body.y < 0f || body.x + body.width > WIDTH || body.y + body.height > HEIGHT) {
            return true;
        }
        for (ParkObject object : objects) {
            if (object.solid() && object.bounds().overlaps(body)) {
                return true;
            }
        }
        return false;
    }

    public enum ObjectKind {
        HOUSE(new Color(0.48f, 0.25f, 0.16f, 1f)),
        FENCE(new Color(0.28f, 0.16f, 0.09f, 1f)),
        TREE(new Color(0.08f, 0.30f, 0.13f, 1f)),
        BENCH(new Color(0.46f, 0.29f, 0.12f, 1f)),
        PLAYGROUND(new Color(0.66f, 0.31f, 0.13f, 1f)),
        FOUNTAIN(new Color(0.10f, 0.36f, 0.55f, 1f));

        private final Color color;

        ObjectKind(Color color) {
            this.color = color;
        }

        public Color color() {
            return color;
        }
    }

    public record ParkObject(ObjectKind kind, Rectangle bounds, boolean solid, String label) {
        public static ParkObject house(String label, float x, float y, float width, float height) {
            return obstacle(ObjectKind.HOUSE, x, y, width, height, label);
        }

        public static ParkObject fence(float x, float y, float width, float height) {
            return obstacle(ObjectKind.FENCE, x, y, width, height);
        }

        public static ParkObject tree(float x, float y) {
            return obstacle(ObjectKind.TREE, x, y, 82f, 82f);
        }

        public static ParkObject obstacle(ObjectKind kind, float x, float y, float width, float height) {
            return obstacle(kind, x, y, width, height, "");
        }

        public static ParkObject obstacle(ObjectKind kind, float x, float y, float width, float height, String label) {
            return new ParkObject(kind, new Rectangle(x, y, width, height), true, label);
        }
    }
}
