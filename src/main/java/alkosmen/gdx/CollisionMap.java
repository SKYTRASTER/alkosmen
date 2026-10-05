package alkosmen.gdx;

import com.badlogic.gdx.math.Rectangle;
import java.util.List;

/** Keeps movement checks separate from scene rendering and visual artwork. */
public final class CollisionMap {
    private final List<NewParkWorld.ParkObject> colliders;

    public CollisionMap(List<NewParkWorld.ParkObject> colliders) {
        this.colliders = List.copyOf(colliders);
    }

    public boolean blocks(Rectangle body) {
        for (NewParkWorld.ParkObject collider : colliders) {
            if (collider.solid() && collider.bounds().overlaps(body)) {
                return true;
            }
        }
        return false;
    }

    public List<NewParkWorld.ParkObject> colliders() {
        return colliders;
    }
}
