package alkosmen.app.menu.settings;

import alkosmen.settings.Constants;

import javax.swing.JFrame;
import java.awt.Component;

public final class MenuSettingsApplier {
    private MenuSettingsApplier() {
    }

    public static void apply(MenuSettingsState settings, JFrame frame) {
        Constants.Width = settings.width();
        Constants.Height = settings.height();
        Constants.MenuMusicEnabled = settings.menuMusicEnabled();
        Constants.GameMusicEnabled = settings.gameMusicEnabled();

        frame.setSize(Constants.Width, Constants.Height);
        for (Component component : frame.getContentPane().getComponents()) {
            component.revalidate();
        }
        frame.revalidate();
        frame.repaint();
        frame.setLocationRelativeTo(null);
    }
}

