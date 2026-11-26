package alkosmen.gdx.ui;

import alkosmen.gdx.SprintState;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** Fixed HUD coordinates keep the stamina panel readable at different window sizes. */
public final class SprintHud implements Disposable {
    private final FitViewport viewport = new FitViewport(1280f, 720f);
    private final BitmapFont font = ObjectiveHud.createRussianFont();

    public void render(ShapeRenderer shapes, SpriteBatch batch, SprintState state) {
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.06f, 0.08f, 0.10f, 1f);
        shapes.rect(18f, 614f, 356f, 88f);
        shapes.setColor(0.19f, 0.22f, 0.24f, 1f);
        shapes.rect(34f, 650f, 320f, 12f);
        if (state.exhausted()) shapes.setColor(0.90f, 0.32f, 0.22f, 1f);
        else shapes.setColor(0.35f, 0.84f, 0.57f, 1f);
        shapes.rect(34f, 650f, 320f * state.fraction(), 12f);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        String label = state.exhausted() ? "Выдохся — отдышись" : "Выносливость";
        font.draw(batch, label, 34f, 689f);
        font.setColor(0.78f, 0.84f, 0.88f, 1f);
        font.draw(batch, "Shift — бежать", 34f, 640f);
        batch.end();
    }

    public void resize(int width, int height) { viewport.update(width, height, true); }

    @Override
    public void dispose() { font.dispose(); }
}
