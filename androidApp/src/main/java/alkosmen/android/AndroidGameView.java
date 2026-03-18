package alkosmen.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import alkosmen.shared.GridMapParser;
import alkosmen.shared.game.GameCore;
import alkosmen.shared.game.GameInput;
import alkosmen.shared.game.GameState;

public final class AndroidGameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String DEMO_MAP_PATH = "alkosmen/maps/demo_level.txt";
    private static final int TILE = 32;
    private static final long FRAME_DELAY_MS = 16L;

    private final Paint wallPaint = paint(0xFF2E3545);
    private final Paint bottlePaint = paint(0xFFFFCE3A);
    private final Paint playerPaint = paint(0xFFDBE9FF);
    private final Paint floorTintPaint = paint(0x22000000);
    private final Paint hudTextPaint = paintText(36f, Color.WHITE);
    private final Paint controlsPaint = paint(0x26FFFFFF);

    private final GameInput input = new GameInput();
    private Thread loopThread;
    private volatile boolean running;
    private GameCore core;

    public AndroidGameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        try {
            core = new GameCore(loadMapFromAssets());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load demo map from assets", e);
        }
        running = true;
        loopThread = new Thread(this, "android-game-loop");
        loopThread.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        running = false;
        Thread t = loopThread;
        if (t != null) {
            try {
                t.join(400);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            long now = System.currentTimeMillis();
            core.tick(input, now);
            renderFrame();
            try {
                Thread.sleep(FRAME_DELAY_MS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            input.jumpJustPressed = false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            applyTouchState(x, y, true);
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            applyTouchState(x, y, false);
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            input.leftPressed = false;
            input.rightPressed = false;
            input.jumpPressed = false;
            input.hidePressed = false;
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void renderFrame() {
        SurfaceHolder holder = getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) {
            return;
        }
        try {
            drawWorld(canvas);
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawWorld(Canvas c) {
        c.drawColor(0xFF6EADE0);
        if (core == null) {
            return;
        }
        GameState s = core.state();
        char[][] map = core.map();
        int mapW = map[0].length * TILE;
        int mapH = map.length * TILE;

        float cameraX = (float) (s.playerX * TILE - getWidth() / 2.0 + TILE / 2.0);
        float cameraY = (float) (s.playerY * TILE - getHeight() / 2.0 + TILE / 2.0);
        cameraX = clamp(cameraX, 0f, Math.max(0, mapW - getWidth()));
        cameraY = clamp(cameraY, 0f, Math.max(0, mapH - getHeight()));

        int firstTileX = Math.max(0, (int) (cameraX / TILE));
        int firstTileY = Math.max(0, (int) (cameraY / TILE));
        int lastTileX = Math.min(map[0].length, firstTileX + getWidth() / TILE + 3);
        int lastTileY = Math.min(map.length, firstTileY + getHeight() / TILE + 3);

        for (int y = firstTileY; y < lastTileY; y++) {
            for (int x = firstTileX; x < lastTileX; x++) {
                char tile = map[y][x];
                float drawX = x * TILE - cameraX;
                float drawY = y * TILE - cameraY;

                if (tile == '#') {
                    c.drawRect(drawX, drawY, drawX + TILE, drawY + TILE, wallPaint);
                    c.drawRect(drawX, drawY, drawX + TILE, drawY + TILE, floorTintPaint);
                } else if (tile == 'B') {
                    c.drawCircle(drawX + TILE / 2f, drawY + TILE / 2f, TILE * 0.28f, bottlePaint);
                }
            }
        }

        float px = (float) (s.playerX * TILE - cameraX);
        float py = (float) (s.playerY * TILE - cameraY);
        c.drawRect(px + 4, py - TILE + 2, px + TILE - 4, py + TILE - 2, playerPaint);

        c.drawText("Score: " + s.score + "/" + s.bottleGoal, 20, 40, hudTextPaint);
        c.drawText("Shared core: desktop physics", 20, 80, hudTextPaint);
        drawTouchHints(c);
    }

    private void drawTouchHints(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        float topJumpH = h * 0.26f;
        float leftW = w * 0.33f;
        float rightX = w * 0.67f;
        float hideX = w * 0.33f;
        float hideW = w * 0.34f;
        float bottomY = h * 0.64f;

        c.drawRect(0, topJumpH, leftW, h, controlsPaint);
        c.drawRect(rightX, topJumpH, w, h, controlsPaint);
        c.drawRect(hideX, bottomY, hideX + hideW, h, controlsPaint);
        c.drawRect(0, 0, w, topJumpH, controlsPaint);

        c.drawText("LEFT", 22, h - 20, hudTextPaint);
        c.drawText("RIGHT", w - 130, h - 20, hudTextPaint);
        c.drawText("JUMP", w / 2f - 52, 52, hudTextPaint);
        c.drawText("HIDE", w / 2f - 46, h - 20, hudTextPaint);
    }

    private void applyTouchState(float x, float y, boolean isInitialPress) {
        float w = getWidth();
        float h = getHeight();
        input.leftPressed = y > h * 0.26f && x < w * 0.33f;
        input.rightPressed = y > h * 0.26f && x > w * 0.67f;
        boolean jumpNow = y <= h * 0.26f;
        if (isInitialPress && jumpNow) {
            input.jumpJustPressed = true;
        }
        input.jumpPressed = jumpNow;
        input.hidePressed = y > h * 0.64f && x >= w * 0.33f && x <= w * 0.67f;
    }

    private char[][] loadMapFromAssets() throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getContext().getAssets().open(DEMO_MAP_PATH)))) {
            List<String> lines = reader.lines().toList();
            return GridMapParser.parseRectangular(lines).tiles();
        }
    }

    private static Paint paint(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        return p;
    }

    private static Paint paintText(float sizePx, int color) {
        Paint p = paint(color);
        p.setTextSize(sizePx);
        return p;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
