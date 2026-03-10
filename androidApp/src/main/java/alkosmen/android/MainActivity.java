package alkosmen.android;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;

import android.content.SharedPreferences;
import android.view.View;

public final class MainActivity extends AppCompatActivity {
    private static final String PREFS = "alkosmen_menu_settings";
    private static final String KEY_MENU_MUSIC = "menu_music";
    private static final String KEY_GAME_MUSIC = "game_music";

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

        start.setOnClickListener(v -> startGame());
        settings.setOnClickListener(v -> openSettings());
        exit.setOnClickListener(v -> finishAffinity());
    }

    private void startGame() {
        setContentView(new AndroidGameView(this));
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
            return;
        }
        super.onBackPressed();
    }
}
