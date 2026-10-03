package alkosmen.gdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public final class GdxDesktopLauncher {
    private GdxDesktopLauncher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Алкосмены — libGDX menu");
        config.setWindowedMode(1672, 941);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(60);

        new Lwjgl3Application(new AlkosmenGdxGame(), config);
    }
}
