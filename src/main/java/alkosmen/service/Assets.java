package alkosmen.service;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class Assets {

    private static final Map<String, Image> CACHE = new HashMap<>();

    private Assets() {}

    public static Image img(String path) {
        return CACHE.computeIfAbsent(path, Assets::load);
    }

    private static Image load(String path) {
        try {
            URL url = Assets.class.getResource(path);
            if (url == null) {
                throw new RuntimeException("Image not found: " + path);
            }
            return ImageIO.read(url);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load image: " + path, e);
        }
    }
}
