package alkosmen.gdx.items;

/** A one-time pickup used by the New Park opening scene. */
public final class StorageKey {
    private final float x;
    private final float y;
    private final float pickupRadius;
    private boolean collected;

    public StorageKey(float x, float y, float pickupRadius) {
        this.x = x;
        this.y = y;
        this.pickupRadius = pickupRadius;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public boolean collected() {
        return collected;
    }

    public boolean isNear(float playerX, float playerY) {
        float dx = playerX - x;
        float dy = playerY - y;
        return dx * dx + dy * dy <= pickupRadius * pickupRadius;
    }

    public boolean collect() {
        if (collected) {
            return false;
        }
        collected = true;
        return true;
    }

    public void reset() {
        collected = false;
    }
}
