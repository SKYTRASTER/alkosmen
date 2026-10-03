package alkosmen.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

public final class GdxMainMenu implements Disposable {
    private final Stage stage;
    private final BitmapFont font;
    private final Texture normalTexture;
    private final Texture hoverTexture;
    private final Texture downTexture;

    public GdxMainMenu(Viewport viewport, Runnable onStart, Runnable onSettings, Runnable onExit) {
        this.stage = new Stage(viewport);
        this.font = createFont();

        this.normalTexture = createButtonTexture(new Color(0.03f, 0.06f, 0.13f, 0.82f), new Color(0.25f, 0.90f, 1f, 0.80f));
        this.hoverTexture = createButtonTexture(new Color(0.10f, 0.16f, 0.28f, 0.94f), new Color(0.35f, 0.95f, 1f, 1f));
        this.downTexture = createButtonTexture(new Color(0.16f, 0.10f, 0.24f, 0.96f), new Color(1f, 0.78f, 0.35f, 1f));

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.overFontColor = new Color(1f, 0.88f, 0.42f, 1f);
        style.downFontColor = new Color(1f, 0.72f, 0.30f, 1f);
        style.up = new TextureRegionDrawable(normalTexture);
        style.over = new TextureRegionDrawable(hoverTexture);
        style.down = new TextureRegionDrawable(downTexture);

        float x = 105f;
        float width = 300f;
        float height = 58f;
        float gap = 18f;
        float startY = 500f;

        addButton("Старт", x, startY, width, height, style, onStart);
        addButton("Настройки", x, startY - height - gap, width, height, style, onSettings);
        addButton("Выход", x, startY - (height + gap) * 2f, width, height, style, onExit);

        Gdx.input.setInputProcessor(stage);
    }

    private void addButton(String text, float x, float y, float width, float height,
                           TextButton.TextButtonStyle style, Runnable action) {
        TextButton button = new TextButton(text, style);
        button.setBounds(x, y, width, height);
        button.addListener(event -> {
            if (!event.toString().equals("touchDown")) {
                return false;
            }
            action.run();
            return true;
        });
        stage.addActor(button);
    }

    public void render(float delta) {
        stage.act(delta);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    private static BitmapFont createFont() {
        String[] candidates = {
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/segoeui.ttf"
        };

        for (String path : candidates) {
            if (!Gdx.files.absolute(path).exists()) {
                continue;
            }

            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.absolute(path));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 32;
            parameter.characters =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
                "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
                "абвгдеёжзийклмнопрстуфхцчшщъыьэюя" +
                "0123456789 -_.,!?";
            BitmapFont result = generator.generateFont(parameter);
            generator.dispose();
            return result;
        }

        BitmapFont fallback = new BitmapFont();
        fallback.getData().setScale(2f);
        return fallback;
    }

    private static Texture createButtonTexture(Color fill, Color border) {
        Pixmap pixmap = new Pixmap(300, 58, Pixmap.Format.RGBA8888);
        pixmap.setColor(fill);
        pixmap.fill();
        pixmap.setColor(border);
        pixmap.drawRectangle(0, 0, 299, 57);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
        stage.dispose();
        font.dispose();
        normalTexture.dispose();
        hoverTexture.dispose();
        downTexture.dispose();
    }
}
