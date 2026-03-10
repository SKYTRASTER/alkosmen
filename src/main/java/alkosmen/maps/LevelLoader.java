package alkosmen.maps;

import alkosmen.shared.GridMapParser;

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
            return GridMapParser.parseRectangular(lines).tiles();
        }
    }
}

