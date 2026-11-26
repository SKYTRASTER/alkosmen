package alkosmen.gdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public final class ZuevkaSectorLauncher {
    private ZuevkaSectorLauncher() { }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Алкосмены — Зуевка, сектор 01");
        config.setWindowedMode(1280, 720);
        config.setResizable(true);
        config.useVsync(true);
        new Lwjgl3Application(new ZuevkaSectorGame(), config);
    }
}
