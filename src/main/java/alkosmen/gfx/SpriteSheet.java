package alkosmen.gfx;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public final class SpriteSheet {
    private final BufferedImage sheet;
    private final int tile;

    public SpriteSheet(String resourcePath, int tileSize) {
        this.tile = tileSize;
        try {
            URL url = SpriteSheet.class.getResource(resourcePath);
            if (url == null) throw new RuntimeException("Sheet not found: " + resourcePath);
            sheet = ImageIO.read(url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Image tile(int col, int row) {
        return sheet.getSubimage(col * tile, row * tile, tile, tile);
    }
}