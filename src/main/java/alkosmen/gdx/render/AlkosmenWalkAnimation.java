package alkosmen.gdx.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

/** Shared frames and ground anchor for the walk test, player and New Park. */
public final class AlkosmenWalkAnimation implements Disposable {
    private static final int FRAME_COUNT = 6;
    private static final String BASE_PATH = "alkosmen/ui/alkosmen";
    private final List<Texture> textures = new ArrayList<>();
    private final TextureRegion[][] frames = new TextureRegion[3][FRAME_COUNT];
    private final CharacterRenderMetrics.FrameMetrics[][] metrics =
        new CharacterRenderMetrics.FrameMetrics[3][FRAME_COUNT];
    private final WalkCycle cycle = new WalkCycle(FRAME_COUNT);

    public AlkosmenWalkAnimation() {
        String[] folders = {"walk_right", "walk_front", "walk_back"};
        try {
            for (int row = 0; row < folders.length; row++) {
                for (int column = 0; column < FRAME_COUNT; column++) {
                    String path = String.format("%s/%s/%02d.png", BASE_PATH, folders[row], column);
                    Pixmap pixels = new Pixmap(Gdx.files.internal(path));
                    try {
                        metrics[row][column] = CharacterRenderMetrics.scan(
                            pixels, 0, 0, pixels.getWidth(), pixels.getHeight());
                        Texture texture = new Texture(pixels);
                        textures.add(texture);
                        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                        frames[row][column] = new TextureRegion(texture);
                    } finally {
                        pixels.dispose();
                    }
                }
            }
        } catch (RuntimeException exception) {
            dispose();
            throw exception;
        }
    }

    /** One complete six-frame cycle covers 0.72 character heights. */
    public void advance(float actualDistance, float visibleHeight) {
        cycle.advance(actualDistance, visibleHeight * 0.72f);
    }

    public void reset() {
        cycle.reset();
    }

    public CharacterRenderMetrics.Placement draw(
        SpriteBatch batch, Facing facing, float footX, float footY, float visibleHeight
    ) {
        int row = switch (facing) {
            case LEFT, RIGHT -> 0;
            case DOWN -> 1;
            case UP -> 2;
        };
        int column = cycle.frameIndex();
        // Normalize the opaque silhouette, not the PNG canvas (144px and 1024px coexist).
        return CharacterRenderMetrics.draw(batch, frames[row][column], metrics[row][column],
            footX, footY, visibleHeight, facing == Facing.LEFT);
    }

    @Override
    public void dispose() {
        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
    }

    public enum Facing {
        LEFT, RIGHT, UP, DOWN
    }
}
