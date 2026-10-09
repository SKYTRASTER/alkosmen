package alkosmen.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public final class AlkosmenGdxGame extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 1672f;
    private static final float WORLD_HEIGHT = 941f;

    private static final String BACKGROUND = "alkosmen/ui/menu/town_square_dance_bg_v2.png";
    private static final String BACKGROUND_FALLBACK = "alkosmen/ui/menu/town_square_dance_bg_v1.png";
    private static final String LOGO = "alkosmen/ui/menu/alkosmeny_title_logo_v1_transparent.png";
    private static final String HERO_FALLBACK = "alkosmen/ui/characters/white_alkosmen_player_red_nose_v2.png";
    private static final String MUSIC = "alkosmen/sounds/menu/night_training_1.mp3";

    private static final int[] DANCE_SEQUENCE = {
        0, 1, 2, 3, 4, 5, 6, 7,
        6, 5, 4, 3, 2, 1
    };

    private enum Mode {
        MAIN_MENU,
        SCENE_TEST_MENU,
        ACTIVE_SCENE_TEST
    }

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private GdxMainMenu mainMenu;
    private SceneTestMenu sceneTestMenu;
    private ApplicationAdapter activeSceneTest;
    private Mode mode = Mode.MAIN_MENU;

    private Texture background;
    private Texture logo;
    private Texture fallbackHero;
    private Texture[] danceFrames;
    private Music music;

    private static final float BACK_X = 32f;
    private static final float BACK_Y = 32f;
    private static final float BACK_W = 285f;
    private static final float BACK_H = 60f;
    private final Vector2 backPointer = new Vector2();
    private BitmapFont backFont;
    private String backText;

    private float dancePosition;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        initBackButton();

        background = loadBackground();
        logo = new Texture(Gdx.files.internal(LOGO));

        loadDanceFrames();
        loadMusic();

        mainMenu = new GdxMainMenu(
            viewport,
            this::onStartClicked,
            this::showSceneTests,
            this::onSettingsClicked,
            Gdx.app::exit
        );

        sceneTestMenu = new SceneTestMenu(
            viewport,
            () -> launchSceneTest(new NewParkGame()),
            () -> launchSceneTest(new AlkosmenWalkTest()),
            () -> launchSceneTest(new EboboWalkTest()),
            () -> launchSceneTest(new LenyaKultyshevWalkTest()),
            () -> launchSceneTest(new MozolWalkTest()),
            () -> launchSceneTest(new PoliceMaleWalkTest()),
            () -> launchSceneTest(new PoliceFemaleWalkTest()),
            this::showMainMenu
        );

        mainMenu.activate();
    }

    private Texture loadBackground() {
        String path = Gdx.files.internal(BACKGROUND).exists()
            ? BACKGROUND
            : BACKGROUND_FALLBACK;
        return new Texture(Gdx.files.internal(path));
    }

    private void loadDanceFrames() {
        Texture[] frames = new Texture[8];

        for (int i = 0; i < frames.length; i++) {
            String path = String.format("alkosmen/ui/menu/dance/%02d.png", i);
            if (!Gdx.files.internal(path).exists()) {
                disposeAll(frames);
                fallbackHero = new Texture(Gdx.files.internal(HERO_FALLBACK));
                danceFrames = null;
                return;
            }
            frames[i] = new Texture(Gdx.files.internal(path));
        }

        danceFrames = frames;
    }

    private void loadMusic() {
        if (!Gdx.files.internal(MUSIC).exists()) {
            return;
        }

        music = Gdx.audio.newMusic(Gdx.files.internal(MUSIC));
        music.setVolume(0.65f);
        music.setLooping(true);
        music.play();
    }

    @Override
    public void render() {
        if (mode == Mode.ACTIVE_SCENE_TEST) {
            renderActiveSceneTest();
            return;
        }

        renderMenuBackdrop();

        if (mode == Mode.MAIN_MENU) {
            mainMenu.render(Gdx.graphics.getDeltaTime());
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                Gdx.app.exit();
            }
        } else {
            sceneTestMenu.render(Gdx.graphics.getDeltaTime());
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                showMainMenu();
            }
        }
    }

    private void renderActiveSceneTest() {
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            backPointer.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(backPointer);
            if (backPointer.x >= BACK_X && backPointer.x <= BACK_X + BACK_W
                && backPointer.y >= BACK_Y && backPointer.y <= BACK_Y + BACK_H) {
                closeSceneTest();
                showMainMenu();
                return;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            closeSceneTest();
            return;
        }

        if (activeSceneTest != null) {
            activeSceneTest.render();
            renderBackButton();
        }
    }

    private void initBackButton() {
        backText = "Главное меню";
        for (String fontPath : new String[] {"C:/Windows/Fonts/arial.ttf", "C:/Windows/Fonts/segoeui.ttf"}) {
            if (!Gdx.files.absolute(fontPath).exists()) continue;
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.absolute(fontPath));
            try {
                FreeTypeFontGenerator.FreeTypeFontParameter settings =
                    new FreeTypeFontGenerator.FreeTypeFontParameter();
                settings.size = 30;
                settings.characters = "Главное меню";
                backFont = generator.generateFont(settings);
            } finally {
                generator.dispose();
            }
            return;
        }
        backFont = new BitmapFont();
        backFont.getData().setScale(2f);
        backText = "Main menu";
    }

    private void renderBackButton() {
        // A screen-space overlay, independent of the test scene's camera.
        viewport.apply();
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.03f, 0.06f, 0.13f, 0.90f);
        shapes.rect(BACK_X, BACK_Y, BACK_W, BACK_H);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.25f, 0.90f, 1f, 1f);
        shapes.rect(BACK_X, BACK_Y, BACK_W, BACK_H);
        shapes.end();
        batch.begin();
        backFont.setColor(Color.WHITE);
        backFont.draw(batch, backText, BACK_X + 18f, BACK_Y + 40f);
        batch.end();
    }

    private void renderMenuBackdrop() {
        updateDance();

        Gdx.gl.glClearColor(0.025f, 0.04f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);

        drawBackground();
        drawCharacter();
        drawLogo();
    }

    private void updateDance() {
        float fps = danceFps(music == null ? 0f : music.getPosition());
        dancePosition += Gdx.graphics.getDeltaTime() * fps;
    }

    private float danceFps(float seconds) {
        if (seconds < 15f) {
            return 4.0f;
        }
        if (seconds < 30f) {
            return 5.0f;
        }
        if (seconds < 50f) {
            return 6.5f;
        }
        if (seconds < 65f) {
            return 4.5f;
        }
        return 5.5f;
    }

    private void drawBackground() {
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(background, 0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();
    }

    private void drawCharacter() {
        Texture frame = currentFrame();
        if (frame == null) {
            return;
        }

        float targetHeight = WORLD_HEIGHT * 0.43f;
        float targetWidth = targetHeight * frame.getWidth() / (float) frame.getHeight();

        float phase = dancePosition / DANCE_SEQUENCE.length * (float) (Math.PI * 2.0);
        float sway = (float) Math.sin(phase) * 1.5f;
        float bob = Math.abs((float) Math.sin(phase)) * 1.0f;

        float centerX = WORLD_WIDTH * 0.67f + sway;
        float feetY = WORLD_HEIGHT * 0.09f + bob;
        float x = centerX - targetWidth / 2f;
        float y = feetY;

        drawShadow(centerX, feetY, targetWidth, targetHeight);

        batch.begin();
        batch.setColor(0.80f, 0.86f, 0.96f, 1f);
        batch.draw(frame, x, y, targetWidth, targetHeight);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawShadow(float centerX, float feetY, float targetWidth, float targetHeight) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.28f);
        shapes.ellipse(
            centerX - targetWidth * 0.27f,
            feetY - targetHeight * 0.012f,
            targetWidth * 0.54f,
            targetHeight * 0.065f
        );
        shapes.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawLogo() {
        float width = 620f;
        float height = width * logo.getHeight() / (float) logo.getWidth();

        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(logo, 60f, WORLD_HEIGHT - 95f - height, width, height);
        batch.end();
    }

    private void showSceneTests() {
        mainMenu.deactivate();
        mode = Mode.SCENE_TEST_MENU;
        sceneTestMenu.activate();
    }

    private void showMainMenu() {
        sceneTestMenu.deactivate();
        mode = Mode.MAIN_MENU;
        mainMenu.activate();
    }

    private void launchSceneTest(ApplicationAdapter test) {
        sceneTestMenu.deactivate();
        Gdx.input.setInputProcessor(null);

        if (music != null && music.isPlaying()) {
            music.pause();
        }

        activeSceneTest = test;
        mode = Mode.ACTIVE_SCENE_TEST;

        try {
            activeSceneTest.create();
            activeSceneTest.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        } catch (RuntimeException exception) {
            Gdx.app.error("SceneTests", "Не удалось запустить тестовую сцену", exception);
            activeSceneTest.dispose();
            activeSceneTest = null;
            mode = Mode.SCENE_TEST_MENU;
            resumeMenuMusic();
            sceneTestMenu.activate();
        }
    }

    private void closeSceneTest() {
        if (activeSceneTest != null) {
            activeSceneTest.dispose();
            activeSceneTest = null;
        }
        mode = Mode.SCENE_TEST_MENU;
        Gdx.input.setInputProcessor(null);
        resumeMenuMusic();
        sceneTestMenu.activate();
    }

    private void resumeMenuMusic() {
        if (music != null && !music.isPlaying()) {
            music.play();
        }
    }

    private void onStartClicked() {
        Gdx.app.log("Menu", "Старт нажат. Игровой экран libGDX еще переносится.");
    }

    private void onSettingsClicked() {
        Gdx.app.log("Menu", "Настройки нажаты. Экран настроек libGDX еще переносится.");
    }

    private Texture currentFrame() {
        if (danceFrames == null) {
            return fallbackHero;
        }

        int sequenceIndex = Math.floorMod((int) Math.floor(dancePosition), DANCE_SEQUENCE.length);
        return danceFrames[DANCE_SEQUENCE[sequenceIndex]];
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);

        if (mainMenu != null) {
            mainMenu.resize(width, height);
        }
        if (sceneTestMenu != null) {
            sceneTestMenu.resize(width, height);
        }
        if (mode == Mode.ACTIVE_SCENE_TEST && activeSceneTest != null) {
            activeSceneTest.resize(width, height);
        }
    }

    @Override
    public void pause() {
        if (mode == Mode.ACTIVE_SCENE_TEST && activeSceneTest != null) {
            activeSceneTest.pause();
        }
    }

    @Override
    public void resume() {
        if (mode == Mode.ACTIVE_SCENE_TEST && activeSceneTest != null) {
            activeSceneTest.resume();
        }
    }

    @Override
    public void dispose() {
        if (activeSceneTest != null) {
            activeSceneTest.dispose();
            activeSceneTest = null;
        }

        dispose(background);
        dispose(logo);
        dispose(fallbackHero);
        disposeAll(danceFrames);
        dispose(music);
        dispose(backFont);

        if (batch != null) {
            batch.dispose();
        }
        if (shapes != null) {
            shapes.dispose();
        }
        if (mainMenu != null) {
            mainMenu.dispose();
        }
        if (sceneTestMenu != null) {
            sceneTestMenu.dispose();
        }
    }

    private static void disposeAll(Texture[] textures) {
        if (textures == null) {
            return;
        }

        for (Texture texture : textures) {
            dispose(texture);
        }
    }

    private static void dispose(Disposable disposable) {
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
