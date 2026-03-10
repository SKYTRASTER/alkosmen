package alkosmen.shared;

public final class GridMap {
    private final char[][] tiles;
    private final int width;
    private final int height;

    public GridMap(char[][] tiles) {
        this.tiles = tiles;
        this.height = tiles.length;
        this.width = tiles.length == 0 ? 0 : tiles[0].length;
    }

    public char[][] tiles() {
        return tiles;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int[] findFirst(char target) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (tiles[y][x] == target) {
                    return new int[]{x, y};
                }
            }
        }
        return null;
    }
}
