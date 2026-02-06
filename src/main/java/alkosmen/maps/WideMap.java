package alkosmen.maps;

public final class WideMap {
    private WideMap() {}

    // widthPass = 3 (ширина коридора)
    public static char[][] widen(char[][] src, int widthPass) {
        int h = src.length;
        int w = src[0].length;

        int cell = widthPass;      // проход 3×3
        int wall = 1;              // стена толщиной 1
        int outH = h * cell + (h + 1) * wall;
        int outW = w * cell + (w + 1) * wall;

        // всё заполняем стенами
        char[][] out = new char[outH][outW];
        for (int y = 0; y < outH; y++) {
            for (int x = 0; x < outW; x++) out[y][x] = '#';
        }

        // кладём каждую клетку как "площадку" 3×3
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char c = src[y][x];

                int ox = wall + x * (cell + wall);
                int oy = wall + y * (cell + wall);

                // если это не стена — делаем 3×3 проход
                if (c != '#') {
                    for (int dy = 0; dy < cell; dy++) {
                        for (int dx = 0; dx < cell; dx++) {
                            out[oy + dy][ox + dx] = '.';
                        }
                    }

                    // P и E ставим в центр блока (один символ)
                    if (c == 'P') out[oy + cell / 2][ox + cell / 2] = 'P';
                    if (c == 'E') out[oy + cell / 2][ox + cell / 2] = 'E';
                }
            }
        }

        // соединяем соседние проходы там, где в исходной карте был проход
        // (если в исходнике стены, соединения не будет — всё останется '#')
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (src[y][x] == '#') continue;

                int ox = wall + x * (cell + wall);
                int oy = wall + y * (cell + wall);

                // вправо
                if (x + 1 < w && src[y][x + 1] != '#') {
                    int wx = ox + cell; // стенка между блоками
                    for (int dy = 0; dy < cell; dy++) out[oy + dy][wx] = '.';
                }
                // вниз
                if (y + 1 < h && src[y + 1][x] != '#') {
                    int wy = oy + cell; // стенка между блоками
                    for (int dx = 0; dx < cell; dx++) out[wy][ox + dx] = '.';
                }
            }
        }

        return out;
    }
}
