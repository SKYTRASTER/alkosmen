package alkosmen.maps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class LevelLoader {

    public static char[][] load(String resourcePath) throws IOException {
        // для ClassLoader путь должен быть БЕЗ ведущего "/"
        String p = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;

        try (var in = LevelLoader.class.getClassLoader().getResourceAsStream(p)) {
            if (in == null) return null;

            var br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
            java.util.List<String> lines = br.lines().toList();

            if (lines.isEmpty()) return null;

            int rows = lines.size();
            int cols = lines.get(0).length();

            char[][] map = new char[rows][cols];
            for (int y = 0; y < rows; y++) {
                String line = lines.get(y);
                if (line.length() != cols) {
                    throw new IllegalArgumentException(
                            "Bad map: different line lengths at row " + y +
                                    " (expected " + cols + ", got " + line.length() + ")"
                    );
                }
                map[y] = line.toCharArray();
            }
            return map;
        }
    }
}

