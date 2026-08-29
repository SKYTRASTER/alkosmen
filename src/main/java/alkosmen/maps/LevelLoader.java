package alkosmen.maps;

import java.io.IOException;

public final class LevelLoader {

    public static char[][] load(String resourcePath) throws IOException {
        // для ClassLoader путь должен быть БЕЗ ведущего "/"
        String p = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;

        try (var in = LevelLoader.class.getClassLoader().getResourceAsStream(p)) {
            if (in == null) return null;

            var br = new java.io.BufferedReader(new java.io.InputStreamReader(in));
            java.util.List<String> lines = br.lines().toList();

            if (lines.isEmpty()) return null;

            int width = lines.get(0).length();
            if (width == 0) return null;

            char[][] map = new char[lines.size()][width];
            for (int y = 0; y < lines.size(); y++) {
                String line = lines.get(y);
                if (line.length() != width) {
                    throw new IOException("Bad map: different line lengths at row " + y);
                }
                map[y] = line.toCharArray();
            }
            return map;
        }
    }
}

