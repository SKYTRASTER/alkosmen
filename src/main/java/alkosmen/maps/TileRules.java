package alkosmen.maps;

public final class TileRules {
    private TileRules() {}

    public static boolean isWalkable(char tile) {
        return tile == '.' || tile == 'E' || tile == 'P' || tile == ' ';
    }

    public static boolean isSolid(char tile) {
        return !isWalkable(tile);
    }
}
