package alkosmen.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Development menu for launching existing libGDX test scenes from one window.
 */
public final class SceneTestMenu implements Disposable {
    private final Stage stage;
    private final BitmapFont font;
    private final Texture normalTexture;
    private final Texture hoverTexture;
    private final Texture downTexture;

    public SceneTestMenu(
        Viewport viewport,
        Runnable onNewPark,
        Runnable onEbobo,
        Runnable onShurin,
        Runnable onMozol,
        Runnable onPoliceMale,
        Runnable onPoliceFemale,
        Runnable onBack
    ) {
        stage = new Stage(viewport);
        font = createFont();

        normalTexture = createButtonTexture(
            new Color(0.03f, 0.06f, 0.13f, 0.88f),
            new Color(0.25f, 0.90f, 1f, 0.80f)
        );
        hoverTexture = createButtonTexture(
            new Color(0.10f, 0.16f, 0.28f, 0.96f),
            new Color(0.35f, 0.95f, 1f, 1f)
        );
        downTexture = createButtonTexture(
            new Color(0.16f, 0.10f, 0.24f, 0.98f),
            new Color(1f, 0.78f, 0.35f, 1f)
        );

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.overFontColor = new Color(1f, 0.88f, 0.42f, 1f);
        buttonStyle.downFontColor = new Color(1f, 0.72f, 0.30f, 1f);
        buttonStyle.up = new TextureRegionDrawable(normalTexture);
        buttonStyle.over = new TextureRegionDrawable(hoverTexture);
        buttonStyle.down = new TextureRegionDrawable(downTexture);

        Label.LabelStyle titleStyle = new Label.LabelStyle(font, Color.WHITE);
        Label title = new Label("ТЕСТЫ СЦЕН", titleStyle);
        title.setBounds(105f, 680f, 420f, 60f);
        stage.addActor(title);

        float x = 105f;
        float width = 390f;
        float height = 54f;
        float gap = 12f;
        float y = 600f;

        addButton("Новый парк", x, y, width, height, buttonStyle, onNewPark);
        y -= height + gap;

        addButton("Вятский Ебобо", x, y, width, height, buttonStyle, onEbobo);
        y -= height + gap;
        addButton("Шурин", x, y, width, height, buttonStyle, onShurin);
        y -= height + gap;
        addButton("Мозоль", x, y, width, height, buttonStyle, onMozol);
        y -= height + gap;
        addButton("Полицейский", x, y, width, height, buttonStyle, onPoliceMale);
        y -= height + gap;
        addButton("Полицейская", x, y, width, height, buttonStyle, onPoliceFemale);
        y -= height + gap;

        addButton("Назад", x, y, width, height, buttonStyle, onBack);
    }

    private void addButton(
        String text,
        float x,
        float y,
        float width,
        float height,
        TextButton.TextButtonStyle style,
        Runnable action
    ) {
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

    public void activate() {
        Gdx.input.setInputProcessor(stage);
    }

    public void deactivate() {
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
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
            FreeTypeFontGenerator.FreeTypeFontParameter parameter =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 30;
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
        Pixmap pixmap = new Pixmap(390, 54, Pixmap.Format.RGBA8888);
        pixmap.setColor(fill);
        pixmap.fill();
        pixmap.setColor(border);
        pixmap.drawRectangle(0, 0, 389, 53);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        deactivate();
        stage.dispose();
        font.dispose();
        normalTexture.dispose();
        hoverTexture.dispose();
        downTexture.dispose();
    }
}
