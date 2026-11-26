package alkosmen.gdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public final class AlkosmenWalkLauncher {
    private AlkosmenWalkLauncher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Алкосмен — тест ходьбы");
        config.setWindowedMode(1280, 720);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(60);

        new Lwjgl3Application(new AlkosmenWalkTest(), config);
    }
}
