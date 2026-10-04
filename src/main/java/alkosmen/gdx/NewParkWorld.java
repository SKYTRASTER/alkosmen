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
    public static final float HEIGHT = 1920f;
    public static final float SPAWN_X = 1320f;
    public static final float SPAWN_Y = 770f;

    private final List<ParkObject> objects;

    public NewParkWorld() {
        List<ParkObject> layout = new ArrayList<>();

        // Collision data matches the hand-painted New Park background.
        layout.add(ParkObject.house("Sverdlova 12", 1160f, 1040f, 780f, 370f));
        layout.add(ParkObject.house("House", 120f, 1330f, 540f, 360f));
        layout.add(ParkObject.house("House", 720f, 1110f, 540f, 340f));
        layout.add(ParkObject.house("House", 1510f, 1450f, 640f, 340f));
        layout.add(ParkObject.fence(90f, 970f, 860f, 30f));
        layout.add(ParkObject.fence(1040f, 930f, 970f, 30f));
        layout.add(ParkObject.fence(2130f, 720f, 30f, 840f));
        layout.add(ParkObject.fence(2470f, 780f, 30f, 780f));
        // Gap between both fence sections is the walkable New Park entrance.
        layout.add(ParkObject.fence(2540f, 710f, 440f, 28f));
        layout.add(ParkObject.fence(3220f, 710f, 440f, 28f));

        // Only the trunks block movement; leaves stay visual in the background.
        layout.add(ParkObject.tree(360f, 760f));
        layout.add(ParkObject.tree(760f, 1540f));
        layout.add(ParkObject.tree(970f, 820f));
        layout.add(ParkObject.tree(2130f, 1660f));
        layout.add(ParkObject.tree(2520f, 1550f));
        layout.add(ParkObject.tree(2920f, 860f));
        layout.add(ParkObject.tree(3440f, 1320f));
        layout.add(ParkObject.tree(3610f, 1690f));

        layout.add(ParkObject.obstacle(ObjectKind.BENCH, 2760f, 940f, 170f, 46f));
        layout.add(ParkObject.obstacle(ObjectKind.PLAYGROUND, 3370f, 1360f, 250f, 190f));
        layout.add(ParkObject.obstacle(ObjectKind.FOUNTAIN, 3040f, 1110f, 200f, 200f));

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
