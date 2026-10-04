package alkosmen.gdx;

import alkosmen.gdx.items.StorageKey;
import alkosmen.gdx.render.CharacterRenderMetrics;
import alkosmen.gdx.story.StoryManager;
import alkosmen.gdx.story.StoryState;
import alkosmen.gdx.story.StoryTrigger;
import alkosmen.gdx.ui.ObjectiveHud;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.ArrayList;
import java.util.List;

/** First playable libGDX scene: Sverdlova 12 and New Park. */
public final class NewParkGame extends ApplicationAdapter {
    private static final float VIEWPORT_WIDTH = 1280f;
    private static final float VIEWPORT_HEIGHT = 720f;
    private static final float PLAYER_SPEED = 330f;
    private static final float PLAYER_DRAW_HEIGHT = 132f;
    private static final float ALKOSMEN_VISIBLE_HEIGHT = 150f;
    private static final float MALE_POLICE_VISIBLE_HEIGHT = 158f;
    private static final float FEMALE_POLICE_VISIBLE_HEIGHT = 153f;
    private static final float PLAYER_BODY_WIDTH = 36f;
    private static final float PLAYER_BODY_HEIGHT = 26f;
    private static final float FRAME_DURATION = 0.14f;
    private static final String PLAYER_ATLAS = "alkosmen/ui/sprites/alkosmen/walk_atlas_v1.png";
    private static final String PARK_BACKGROUND = "alkosmen/ui/levels/new_park_zuevka_v1.png";

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private Texture playerAtlas;
    private Texture parkBackground;
    private TextureRegion[][] playerFrames;
    private CharacterRenderMetrics.FrameMetrics[][] playerMetrics;
    private final List<Texture> policeTextures = new ArrayList<>();
    private TextureRegion[] malePoliceFrames;
    private TextureRegion[] femalePoliceFrames;
    private CharacterRenderMetrics.FrameMetrics[] malePoliceMetrics;
    private CharacterRenderMetrics.FrameMetrics[] femalePoliceMetrics;
    private NewParkWorld world;
    private StorageKey storageKey;
    private StoryManager story;
    private ObjectiveHud objectiveHud;

    private float playerX = NewParkWorld.SPAWN_X;
    private float playerY = NewParkWorld.SPAWN_Y;
    private float animationTime;
    private Direction direction = Direction.DOWN;
    private boolean debugCollisions;
    private boolean debugStoryState;
    private boolean debugCharacterMetrics;
    private PoliceScenePhase policeScene = PoliceScenePhase.WAITING;
    private float policeX;
    private float policeY;
    private float policeTargetX;
    private float policeSceneTimer;
    private float policeAnimationTime;
    private float stationObjectiveDelay = -1f;
    private String message;
    private float messageUntil;
    private CharacterRenderMetrics.Placement playerPlacement;
    private CharacterRenderMetrics.Placement malePolicePlacement;
    private CharacterRenderMetrics.Placement femalePolicePlacement;

    private enum Direction {
        LEFT(2), RIGHT(1), IDLE(0), UP(4), DOWN(3);

        private final int atlasRow;

        Direction(int atlasRow) {
            this.atlasRow = atlasRow;
        }
    }

    private enum PoliceScenePhase {
        WAITING,
        WALKING_IN,
        SPEAKING,
        FINISHED
    }

    private record PlayerAtlas(
        TextureRegion[][] frames,
        CharacterRenderMetrics.FrameMetrics[][] metrics
    ) {
    }

    private record LoadedFrames(
        TextureRegion[] frames,
        CharacterRenderMetrics.FrameMetrics[] metrics
    ) {
    }

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        objectiveHud = new ObjectiveHud();

        playerAtlas = new Texture(Gdx.files.internal(PLAYER_ATLAS));
        playerAtlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        Pixmap playerPixels = new Pixmap(Gdx.files.internal(PLAYER_ATLAS));
        PlayerAtlas playerAtlasFrames = splitPlayerAtlas(playerAtlas, playerPixels);
        playerPixels.dispose();
        playerFrames = playerAtlasFrames.frames();
        playerMetrics = playerAtlasFrames.metrics();
        parkBackground = new Texture(Gdx.files.internal(PARK_BACKGROUND));
        parkBackground.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        LoadedFrames malePolice = loadPoliceFrames("alkosmen/ui/police_male/walk_right");
        malePoliceFrames = malePolice.frames();
        malePoliceMetrics = malePolice.metrics();
        LoadedFrames femalePolice = loadPoliceFrames("alkosmen/ui/police_female/walk_right");
        femalePoliceFrames = femalePolice.frames();
        femalePoliceMetrics = femalePolice.metrics();
        world = new NewParkWorld();
        storageKey = new StorageKey(3070f, 900f, 84f);
        story = new StoryManager();
        updateCamera();
    }

    @Override
    public void render() {
        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        updatePlayer(delta);
        updateStory(delta);
        updateCamera();

        Gdx.gl.glClearColor(0.10f, 0.21f, 0.13f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawWorld();
        drawStorageKey();
        drawPoliceScene();
        drawPlayer();
        if (debugCollisions) {
            drawCollisionDebug();
        }
        if (debugCharacterMetrics) {
            drawCharacterMetrics();
        }
        drawHud();
    }

    private void updatePlayer(float delta) {
        float moveX = 0f;
        float moveY = 0f;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            debugStoryState = !debugStoryState;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            resetStory();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            debugCollisions = !debugCollisions;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            debugCharacterMetrics = !debugCharacterMetrics;
        }
        if (policeScene == PoliceScenePhase.WALKING_IN || policeScene == PoliceScenePhase.SPEAKING) {
            return;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            moveX -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            moveX += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            moveY += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            moveY -= 1f;
        }
        boolean moving = moveX != 0f || moveY != 0f;
        if (!moving) {
            animationTime = 0f;
            direction = Direction.IDLE;
            return;
        }

        if (Math.abs(moveX) > Math.abs(moveY)) {
            direction = moveX < 0f ? Direction.LEFT : Direction.RIGHT;
        } else {
            direction = moveY < 0f ? Direction.DOWN : Direction.UP;
        }

        float length = (float)Math.sqrt(moveX * moveX + moveY * moveY);
        moveX = moveX / length * PLAYER_SPEED * delta;
        moveY = moveY / length * PLAYER_SPEED * delta;
        moveAlongAxes(moveX, moveY);
        animationTime += delta;
    }

    private void updateStory(float delta) {
        if (!storageKey.collected() && storageKey.isNear(playerX, playerY)
            && Gdx.input.isKeyJustPressed(Input.Keys.E) && storageKey.collect()) {
            story.trigger(StoryTrigger.KEY_COLLECTED);
            showMessage("Ключ от камеры хранения. Вокзал.", 3.5f);
            startPoliceScene();
        }

        if (policeScene == PoliceScenePhase.WALKING_IN) {
            policeX = Math.max(policeTargetX, policeX - 250f * delta);
            policeAnimationTime += delta;
            if (policeX <= policeTargetX) {
                policeScene = PoliceScenePhase.SPEAKING;
                policeSceneTimer = 3.4f;
            }
        } else if (policeScene == PoliceScenePhase.SPEAKING) {
            policeSceneTimer -= delta;
            if (policeSceneTimer <= 0f) {
                policeScene = PoliceScenePhase.FINISHED;
                if (story.trigger(StoryTrigger.POLICE_SCENE_FINISHED)) {
                    stationObjectiveDelay = 1.0f;
                }
            }
        }

        if (stationObjectiveDelay >= 0f) {
            stationObjectiveDelay -= delta;
            if (stationObjectiveDelay <= 0f) {
                story.trigger(StoryTrigger.STATION_OBJECTIVE_READY);
            }
        }
    }

    private void startPoliceScene() {
        policeTargetX = playerX + 150f;
        policeX = playerX + 650f;
        policeY = playerY;
        policeScene = PoliceScenePhase.WALKING_IN;
        policeAnimationTime = 0f;
    }

    private void resetStory() {
        story.reset();
        storageKey.reset();
        policeScene = PoliceScenePhase.WAITING;
        stationObjectiveDelay = -1f;
        message = null;
        messageUntil = 0f;
        playerX = NewParkWorld.SPAWN_X;
        playerY = NewParkWorld.SPAWN_Y;
        animationTime = 0f;
        policeAnimationTime = 0f;
    }

    private void showMessage(String nextMessage, float seconds) {
        message = nextMessage;
        messageUntil = (float)(System.nanoTime() / 1_000_000_000.0) + seconds;
    }

    private void moveAlongAxes(float moveX, float moveY) {
        if (canStandAt(playerX + moveX, playerY)) {
            playerX += moveX;
        }
        if (canStandAt(playerX, playerY + moveY)) {
            playerY += moveY;
        }
    }

    private boolean canStandAt(float x, float y) {
        Rectangle body = new Rectangle(
            x - PLAYER_BODY_WIDTH / 2f,
            y + 8f,
            PLAYER_BODY_WIDTH,
            PLAYER_BODY_HEIGHT
        );
        return !world.blocks(body);
    }

    private void updateCamera() {
        float halfWidth = viewport.getWorldWidth() / 2f;
        float halfHeight = viewport.getWorldHeight() / 2f;
        camera.position.set(
            MathUtils.clamp(playerX, halfWidth, NewParkWorld.WIDTH - halfWidth),
            MathUtils.clamp(playerY + PLAYER_DRAW_HEIGHT * 0.20f, halfHeight, NewParkWorld.HEIGHT - halfHeight),
            0f
        );
    }

    private void drawWorld() {
        batch.begin();
        batch.draw(parkBackground, 0f, 0f, NewParkWorld.WIDTH, NewParkWorld.HEIGHT);
        batch.end();
    }

    private void drawPlayer() {
        int column = direction == Direction.IDLE ? 0 : (int)(animationTime / FRAME_DURATION) % playerFrames[direction.atlasRow].length;
        TextureRegion frame = playerFrames[direction.atlasRow][column];
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.24f);
        shapes.ellipse(playerX - 40f, playerY + 2f, 80f, 13f);
        shapes.end();

        batch.begin();
        playerPlacement = CharacterRenderMetrics.draw(
            batch, frame, playerMetrics[direction.atlasRow][column], playerX, playerY,
            ALKOSMEN_VISIBLE_HEIGHT, false
        );
        batch.end();
    }

    private void drawStorageKey() {
        if (storageKey.collected()) {
            return;
        }
        float pulse = 4f + MathUtils.sin((float)(System.nanoTime() / 500_000_000.0)) * 2f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(1f, 0.78f, 0.18f, 1f));
        shapes.circle(storageKey.x(), storageKey.y(), 12f + pulse);
        shapes.setColor(new Color(0.32f, 0.20f, 0.04f, 1f));
        shapes.rect(storageKey.x() - 4f, storageKey.y() - 34f, 8f, 28f);
        shapes.circle(storageKey.x(), storageKey.y() - 4f, 9f);
        shapes.end();
    }

    private void drawPoliceScene() {
        if (policeScene == PoliceScenePhase.WAITING) {
            return;
        }
        int frame = (int)(policeAnimationTime / FRAME_DURATION) % malePoliceFrames.length;

        batch.begin();
        // The pair walk in from the right, so the existing right-walk art is mirrored.
        malePolicePlacement = CharacterRenderMetrics.draw(
            batch, malePoliceFrames[frame], malePoliceMetrics[frame], policeX, policeY,
            MALE_POLICE_VISIBLE_HEIGHT, true
        );
        femalePolicePlacement = CharacterRenderMetrics.draw(
            batch, femalePoliceFrames[frame], femalePoliceMetrics[frame], policeX + 100f, policeY,
            FEMALE_POLICE_VISIBLE_HEIGHT, true
        );
        batch.end();
    }

    private void drawCollisionDebug() {
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.YELLOW);
        for (NewParkWorld.ParkObject object : world.objects()) {
            Rectangle bounds = object.bounds();
            shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        Rectangle body = new Rectangle(
            playerX - PLAYER_BODY_WIDTH / 2f,
            playerY + 8f,
            PLAYER_BODY_WIDTH,
            PLAYER_BODY_HEIGHT
        );
        shapes.setColor(Color.CYAN);
        shapes.rect(body.x, body.y, body.width, body.height);
        shapes.end();
    }

    private void drawCharacterMetrics() {
        shapes.begin(ShapeRenderer.ShapeType.Line);
        CharacterRenderMetrics.drawDebug(shapes, playerPlacement);
        CharacterRenderMetrics.drawDebug(shapes, malePolicePlacement);
        CharacterRenderMetrics.drawDebug(shapes, femalePolicePlacement);
        shapes.end();
    }

    private void drawHud() {
        String interaction = !storageKey.collected() && storageKey.isNear(playerX, playerY)
            ? "E — подобрать"
            : null;
        String activeMessage = policeScene == PoliceScenePhase.SPEAKING
            ? "Ищем белого с сумкой."
            : ((float)(System.nanoTime() / 1_000_000_000.0) < messageUntil ? message : null);
        objectiveHud.render(shapes, batch, story.objective(), interaction, activeMessage, story.state(), debugStoryState);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        objectiveHud.resize(width, height);
    }

    @Override
    public void dispose() {
        if (playerAtlas != null) {
            playerAtlas.dispose();
        }
        if (parkBackground != null) {
            parkBackground.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
        if (shapes != null) {
            shapes.dispose();
        }
        for (Texture texture : policeTextures) {
            texture.dispose();
        }
        policeTextures.clear();
        if (objectiveHud != null) {
            objectiveHud.dispose();
        }
    }

    private LoadedFrames loadPoliceFrames(String folder) {
        TextureRegion[] frames = new TextureRegion[6];
        CharacterRenderMetrics.FrameMetrics[] metrics = new CharacterRenderMetrics.FrameMetrics[frames.length];
        for (int index = 0; index < frames.length; index++) {
            String path = String.format("%s/%02d.png", folder, index);
            Texture texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            policeTextures.add(texture);
            frames[index] = new TextureRegion(texture);
            Pixmap pixels = new Pixmap(Gdx.files.internal(path));
            metrics[index] = CharacterRenderMetrics.scan(pixels, 0, 0, pixels.getWidth(), pixels.getHeight());
            pixels.dispose();
        }
        return new LoadedFrames(frames, metrics);
    }

    private static PlayerAtlas splitPlayerAtlas(Texture atlas, Pixmap pixels) {
        int columns = 4;
        int rows = 5;
        int frameWidth = atlas.getWidth() / columns;
        int fullFrameHeight = atlas.getHeight() / rows;
        int frameHeight = fullFrameHeight - 30;
        TextureRegion[][] frames = new TextureRegion[rows][columns];
        CharacterRenderMetrics.FrameMetrics[][] metrics = new CharacterRenderMetrics.FrameMetrics[rows][columns];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                frames[row][column] = new TextureRegion(
                    atlas,
                    column * frameWidth,
                    row * fullFrameHeight,
                    frameWidth,
                    frameHeight
                );
                metrics[row][column] = CharacterRenderMetrics.scan(
                    pixels, column * frameWidth, row * fullFrameHeight, frameWidth, frameHeight
                );
            }
        }
        return new PlayerAtlas(frames, metrics);
    }
}
