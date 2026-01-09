package alkosmen.maps;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LevelLoader {
    public static char[][] load(String path) throws Exception {
        InputStream in = LevelLoader.class.getResourceAsStream(path);
        if (in == null) throw new RuntimeException("Map not found: " + path);

        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) lines.add(line);
        }

        int h = lines.size();
        int w = lines.get(0).length();
        char[][] map = new char[h][w];

        for (int y = 0; y < h; y++)
            map[y] = lines.get(y).toCharArray();

        return map;
    }
}
