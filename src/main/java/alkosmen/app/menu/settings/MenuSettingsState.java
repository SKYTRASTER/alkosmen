package alkosmen.app.menu.settings;

public record MenuSettingsState(
        int width,
        int height,
        boolean menuMusicEnabled,
        boolean gameMusicEnabled
) {
}

