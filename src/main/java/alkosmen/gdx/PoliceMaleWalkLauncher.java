package alkosmen.gdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public final class PoliceMaleWalkLauncher {
    private PoliceMaleWalkLauncher() {}

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Police Male Walk Test");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        new Lwjgl3Application(new PoliceMaleWalkTest(), config);
    }
}
