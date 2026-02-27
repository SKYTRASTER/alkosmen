package alkosmen.service;

import alkosmen.objects.Player;

public final class Movement {
    private Movement() {
    }

    // возвращает true если шаг сделан
    public static boolean move(Player player, char[][] levelMap, int dx, int dy) {
        if (player == null || levelMap == null) return false;

        double nx = player.x + dx;
        double ny = player.y + dy;

        if (ny < 0 || ny >= levelMap.length) return false;
        if (nx < 0 || nx >= levelMap[0].length) return false;

        if (levelMap[(int) ny][(int) nx] == '#') return false;

        player.x = nx;
        player.y = ny;
        return true;
    }
}
