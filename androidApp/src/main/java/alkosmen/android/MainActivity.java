package alkosmen.android;

import android.os.Bundle;
import android.os.Build;
import android.widget.CheckBox;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;

import android.content.SharedPreferences;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class MainActivity extends AppCompatActivity {
    private static final String PREFS = "alkosmen_menu_settings";
    private static final String KEY_MENU_MUSIC = "menu_music";
    private static final String KEY_GAME_MUSIC = "game_music";
    private static final String UI_TEXTS_ASSET_PATH = "alkosmen/ui-texts.properties";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showMenu();
    }

    private void showMenu() {
        setContentView(R.layout.activity_main);

        Button start = findViewById(R.id.menu_start);
        Button settings = findViewById(R.id.menu_settings);
        Button exit = findViewById(R.id.menu_exit);
        LinearLayout menuPanel = findViewById(R.id.menu_panel);
        ImageView menuLogo = findViewById(R.id.menu_logo);
        applySharedMenuTexts(start, settings, exit);
        applyDesktopLikeMenuLayout(menuPanel, menuLogo);

        start.setOnClickListener(v -> startGame());
        settings.setOnClickListener(v -> openSettings());
        exit.setOnClickListener(v -> finishAffinity());
        enableImmersiveMode();
    }

    private void startGame() {
        setContentView(new AndroidGameView(this));
        enableImmersiveMode();
    }

    private void openSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean menuMusicEnabled = prefs.getBoolean(KEY_MENU_MUSIC, true);
        boolean gameMusicEnabled = prefs.getBoolean(KEY_GAME_MUSIC, false);

        CheckBox menuMusic = new CheckBox(this);
        menuMusic.setText(getString(R.string.menu_music_enabled));
        menuMusic.setChecked(menuMusicEnabled);

        CheckBox gameMusic = new CheckBox(this);
        gameMusic.setText(getString(R.string.game_music_enabled));
        gameMusic.setChecked(gameMusicEnabled);

        android.widget.LinearLayout content = new android.widget.LinearLayout(this);
        content.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        content.setPadding(pad, pad, pad, 0);
        content.addView(menuMusic);
        content.addView(gameMusic);

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_settings)
                .setView(content)
                .setPositiveButton(android.R.string.ok, (d, w) -> prefs.edit()
                        .putBoolean(KEY_MENU_MUSIC, menuMusic.isChecked())
                        .putBoolean(KEY_GAME_MUSIC, gameMusic.isChecked())
                        .apply())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onBackPressed() {
        View startButton = findViewById(R.id.menu_start);
        if (startButton == null) {
            showMenu();
            enableImmersiveMode();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersiveMode();
        }
    }

    private void applySharedMenuTexts(Button start, Button settings, Button exit) {
        Properties p = loadSharedUiTexts();
        start.setText(p.getProperty("menu.button.start", getString(R.string.menu_start)));
        settings.setText(p.getProperty("menu.button.settings", getString(R.string.menu_settings)));
        exit.setText(p.getProperty("menu.button.exit", getString(R.string.menu_exit)));
    }

    private Properties loadSharedUiTexts() {
        Properties p = new Properties();
        try (InputStreamReader reader = new InputStreamReader(
                getAssets().open(UI_TEXTS_ASSET_PATH), StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (IOException ignored) {
        }
        return p;
    }

    private void applyDesktopLikeMenuLayout(LinearLayout menuPanel, ImageView menuLogo) {
        FrameLayout root = findViewById(android.R.id.content);
        root.post(() -> {
            int w = root.getWidth();
            int h = root.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }

            int menuX = clamp(Math.round(w * 0.035f), 35, 80);
            int menuY = clamp(Math.round(h * 0.21f), 120, 220);
            int menuW = clamp(Math.round(w * 0.18f), 180, 300);

            FrameLayout.LayoutParams menuLp = (FrameLayout.LayoutParams) menuPanel.getLayoutParams();
            menuLp.leftMargin = menuX;
            menuLp.topMargin = menuY;
            menuLp.width = menuW;
            menuPanel.setLayoutParams(menuLp);

            int logoW = clamp(Math.round(w * 0.29f), 320, 480);
            int logoY = clamp(Math.round(h * 0.035f), 10, 48);
            int logoX = clamp(
                    (w - logoW) / 2 - Math.round(w * 0.03f),
                    0,
                    Math.max(0, w - logoW - 24)
            );

            int srcW = menuLogo.getDrawable() == null ? logoW : Math.max(1, menuLogo.getDrawable().getIntrinsicWidth());
            int srcH = menuLogo.getDrawable() == null ? 82 : Math.max(1, menuLogo.getDrawable().getIntrinsicHeight());
            int logoH = Math.max(1, Math.round((float) srcH * logoW / srcW));

            FrameLayout.LayoutParams logoLp = (FrameLayout.LayoutParams) menuLogo.getLayoutParams();
            logoLp.leftMargin = logoX;
            logoLp.topMargin = logoY;
            logoLp.width = logoW;
            logoLp.height = logoH;
            menuLogo.setLayoutParams(logoLp);
        });
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void enableImmersiveMode() {
        if (getWindow() == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller;
            try {
                controller = getWindow().getInsetsController();
            } catch (NullPointerException ignored) {
                controller = null;
            }
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                return;
            }
        }

        // Fallback for devices/ROMs where insets controller is null at launch.
        View decor = getWindow().getDecorView();
        if (decor == null) {
            return;
        }
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }
}
