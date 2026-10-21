package alkosmen.gdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/** Standalone desktop launcher for the tiled New Park test scene. */
public final class NewParkTilesLauncher {
    private NewParkTilesLauncher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Алкосмены - Новый парк (тайлы)");
        config.setWindowedMode(1280, 720);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(60);

        new Lwjgl3Application(new NewParkTileGame(), config);
    }
}
