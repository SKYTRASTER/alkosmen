package alkosmen.service;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;

public class BackgroundPanel extends JPanel {

    private Image background;

    public BackgroundPanel() {
        try {
            background = ImageIO.read(
                    BackgroundPanel.class.getResource("/alkosmen/images/menu.png")
            );
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Cannot load menu background!");
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
