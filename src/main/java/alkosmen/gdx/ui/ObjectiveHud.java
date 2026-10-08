package alkosmen.gdx.ui;

import alkosmen.gdx.story.Objective;
import alkosmen.gdx.story.StoryState;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/** Resolution-independent objective and interaction overlay. */
public final class ObjectiveHud implements Disposable {
    private final ScreenViewport viewport = new ScreenViewport();
    private final BitmapFont font = createRussianFont();

    public void render(
        ShapeRenderer shapes,
        SpriteBatch batch,
        Objective objective,
        String interaction,
        String message,
        StoryState storyState,
        boolean showDebugState
    ) {
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);
        float width = viewport.getWorldWidth();
        float height = viewport.getWorldHeight();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.02f, 0.04f, 0.08f, 0.82f);
        shapes.rect(12f, height - 82f, Math.min(520f, width - 24f), 70f);
        if (interaction != null || message != null) {
            shapes.setColor(0.03f, 0.05f, 0.10f, 0.90f);
            shapes.rect(12f, 12f, Math.min(700f, width - 24f), 58f);
        }
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Цель: " + objective.text(), 28f, height - 28f);
        font.setColor(new Color(0.78f, 0.88f, 1f, 1f));
        font.draw(batch, "WASD / стрелки - ходить    F3 - коллизии    F4 - опоры", 28f, height - 55f);
        if (interaction != null) {
            font.setColor(new Color(1f, 0.83f, 0.30f, 1f));
            font.draw(batch, interaction, 28f, 48f);
        }
        if (message != null) {
            font.setColor(new Color(1f, 0.92f, 0.64f, 1f));
            font.draw(batch, message, 28f, 48f);
        }
        if (showDebugState) {
            font.setColor(new Color(0.45f, 0.95f, 1f, 1f));
            font.draw(batch, "DEBUG StoryState: " + storyState, width - 310f, height - 28f);
        }
        batch.end();
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        font.dispose();
    }

    public static BitmapFont createRussianFont() {
        String[] candidates = {
            "C:/Windows/Fonts/segoeui.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/System/Library/Fonts/Supplemental/Arial.ttf"
        };
        for (String path : candidates) {
            if (!Gdx.files.absolute(path).exists()) {
                continue;
            }
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.absolute(path));
            FreeTypeFontGenerator.FreeTypeFontParameter parameters = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameters.size = 22;
            parameters.characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
                "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
                "абвгдеёжзийклмнопрстуфхцчшщъыьэюя" +
                "0123456789 :/-—";
            BitmapFont result = generator.generateFont(parameters);
            generator.dispose();
            return result;
        }
        return new BitmapFont();
    }
}
