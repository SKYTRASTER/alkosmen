package alkosmen.gdx;

import com.badlogic.gdx.math.Rectangle;

/** A world NPC anchored by the point between their feet. */
public final class NpcEntity {
    private final String id;
    private final String name;
    private final float x;
    private final float y;
    private final float interactionRadius;
    private final float bodyWidth;
    private final float bodyHeight;

    public NpcEntity(
        String id,
        String name,
        float x,
        float y,
        float interactionRadius,
        float bodyWidth,
        float bodyHeight
    ) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.interactionRadius = interactionRadius;
        this.bodyWidth = bodyWidth;
        this.bodyHeight = bodyHeight;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public Rectangle collisionBody() {
        return new Rectangle(x - bodyWidth / 2f, y + 8f, bodyWidth, bodyHeight);
    }

    public boolean isNear(float otherX, float otherY) {
        float dx = otherX - x;
        float dy = otherY - y;
        return dx * dx + dy * dy <= interactionRadius * interactionRadius;
    }
}
