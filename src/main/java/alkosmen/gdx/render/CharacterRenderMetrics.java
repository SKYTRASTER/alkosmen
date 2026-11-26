package alkosmen.gdx.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Renders character frames from their visible alpha bounds instead of their
 * full PNG canvas. Every character therefore shares a ground-point baseline.
 */
public final class CharacterRenderMetrics {
    private static final int ALPHA_THRESHOLD = 12;

    private CharacterRenderMetrics() {
    }

    public static FrameMetrics scan(Pixmap pixmap, int sourceX, int sourceY, int width, int height) {
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((pixmap.getPixel(sourceX + x, sourceY + y) & 0xff) <= ALPHA_THRESHOLD) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < minX || maxY < minY) {
            return new FrameMetrics(width, height, 0, 0, width, height, height);
        }
        return new FrameMetrics(width, height, minX, minY, maxX - minX + 1, maxY - minY + 1, maxY + 1);
    }

    /** Width of the opaque shoes/ground contact in the bottom fifth of the silhouette. */
    public static int footWidth(Pixmap pixmap, int sourceX, int sourceY, FrameMetrics metrics) {
        int bandHeight = Math.max(1, Math.round(metrics.visibleHeight() * 0.2f));
        int firstY = metrics.footBottomY() - bandHeight;
        int minX = metrics.canvasWidth();
        int maxX = -1;
        for (int y = firstY; y < metrics.footBottomY(); y++) {
            for (int x = 0; x < metrics.canvasWidth(); x++) {
                if ((pixmap.getPixel(sourceX + x, sourceY + y) & 0xff) > ALPHA_THRESHOLD) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }
            }
        }
        return maxX >= minX ? maxX - minX + 1 : metrics.visibleWidth();
    }
    public static Placement draw(
        SpriteBatch batch,
        TextureRegion frame,
        FrameMetrics metrics,
        float footX,
        float footY,
        float targetVisibleHeight,
        boolean flipX
    ) {
        return drawAtScale(
            batch, frame, metrics, footX, footY,
            targetVisibleHeight / metrics.visibleHeight(), flipX
        );
    }

    public static Placement drawAtScale(
        SpriteBatch batch,
        TextureRegion frame,
        FrameMetrics metrics,
        float footX,
        float footY,
        float scale,
        boolean flipX
    ) {
        float drawWidth = metrics.canvasWidth() * scale;
        float drawHeight = metrics.canvasHeight() * scale;
        float visibleCenterX = metrics.visibleX() + metrics.visibleWidth() / 2f;
        if (flipX) {
            visibleCenterX = metrics.canvasWidth() - visibleCenterX;
        }
        float drawX = footX - visibleCenterX * scale;
        float drawY = footY - (metrics.canvasHeight() - metrics.footBottomY()) * scale;

        if (flipX) {
            batch.draw(frame, drawX + drawWidth, drawY, -drawWidth, drawHeight);
        } else {
            batch.draw(frame, drawX, drawY, drawWidth, drawHeight);
        }

        float visibleLeft = drawX + (flipX
            ? metrics.canvasWidth() - metrics.visibleX() - metrics.visibleWidth()
            : metrics.visibleX()) * scale;
        return new Placement(drawX, drawY, drawWidth, drawHeight, visibleLeft, footY,
            metrics.visibleWidth() * scale, metrics.visibleHeight() * scale, footX, footY);
    }

    public static void drawDebug(ShapeRenderer shapes, Placement placement) {
        if (placement == null) {
            return;
        }
        shapes.setColor(Color.MAGENTA);
        shapes.rect(placement.visibleLeft(), placement.visibleBottom(), placement.visibleWidth(), placement.visibleHeight());
        shapes.setColor(Color.YELLOW);
        shapes.line(placement.footX() - 34f, placement.footY(), placement.footX() + 34f, placement.footY());
        shapes.setColor(Color.CYAN);
        shapes.line(placement.footX() - 7f, placement.footY(), placement.footX() + 7f, placement.footY());
        shapes.line(placement.footX(), placement.footY() - 7f, placement.footX(), placement.footY() + 7f);
    }

    public record FrameMetrics(
        int canvasWidth,
        int canvasHeight,
        int visibleX,
        int visibleY,
        int visibleWidth,
        int visibleHeight,
        int footBottomY
    ) {
    }

    public record Placement(
        float drawX,
        float drawY,
        float drawWidth,
        float drawHeight,
        float visibleLeft,
        float visibleBottom,
        float visibleWidth,
        float visibleHeight,
        float footX,
        float footY
    ) {
    }
}
