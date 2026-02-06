package alkosmen.maps;

public final class MapScaler {
    private MapScaler() {}

    public static char[][] scale(char[][] src, int k) {
        if (src == null || src.length == 0 || src[0].length == 0) return src;
        if (k <= 1) return src;

        int h = src.length, w = src[0].length;
        char[][] out = new char[h * k][w * k];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char c = src[y][x];
                for (int dy = 0; dy < k; dy++) {
                    for (int dx = 0; dx < k; dx++) {
                        out[y * k + dy][x * k + dx] = c;
                    }
                }
            }
        }
        return out;
    }
}

