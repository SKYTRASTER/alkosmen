package alkosmen.shared;

import java.util.List;

public final class GridMapParser {
    private GridMapParser() {
    }

    public static GridMap parseRectangular(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Map lines are empty");
        }

        int rows = lines.size();
        int cols = lines.get(0).length();
        if (cols == 0) {
            throw new IllegalArgumentException("Map has zero width");
        }

        char[][] map = new char[rows][cols];
        for (int y = 0; y < rows; y++) {
            String line = lines.get(y);
            if (line.length() != cols) {
                throw new IllegalArgumentException(
                        "Bad map: different line lengths at row " + y
                                + " (expected " + cols + ", got " + line.length() + ")"
                );
            }
            map[y] = line.toCharArray();
        }
        return new GridMap(map);
    }
}
