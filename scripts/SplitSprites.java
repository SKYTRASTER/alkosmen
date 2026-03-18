import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SplitSprites {
    private record Box(int minX, int minY, int maxX, int maxY, int area) {
        int width() { return maxX - minX + 1; }
        int height() { return maxY - minY + 1; }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: SplitSprites <input.png> <outputDir> [alphaThreshold] [minPixels]");
        }
        File input = new File(args[0]);
        File outputDir = new File(args[1]);
        int alphaThreshold = args.length >= 3 ? Integer.parseInt(args[2]) : 12;
        int minPixels = args.length >= 4 ? Integer.parseInt(args[3]) : 1200;

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IllegalStateException("Failed to create output dir: " + outputDir);
        }

        BufferedImage src = ImageIO.read(input);
        int w = src.getWidth();
        int h = src.getHeight();
        boolean[][] visited = new boolean[h][w];
        List<Box> boxes = new ArrayList<>();

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (visited[y][x] || alpha(src.getRGB(x, y)) <= alphaThreshold) {
                    continue;
                }
                ArrayDeque<int[]> q = new ArrayDeque<>();
                q.add(new int[]{x, y});
                visited[y][x] = true;

                int minX = x, minY = y, maxX = x, maxY = y, pixels = 0;
                while (!q.isEmpty()) {
                    int[] p = q.removeFirst();
                    int px = p[0], py = p[1];
                    pixels++;
                    if (px < minX) minX = px;
                    if (py < minY) minY = py;
                    if (px > maxX) maxX = px;
                    if (py > maxY) maxY = py;

                    for (int i = 0; i < 4; i++) {
                        int nx = px + dx[i];
                        int ny = py + dy[i];
                        if (nx < 0 || nx >= w || ny < 0 || ny >= h || visited[ny][nx]) {
                            continue;
                        }
                        if (alpha(src.getRGB(nx, ny)) <= alphaThreshold) {
                            continue;
                        }
                        visited[ny][nx] = true;
                        q.addLast(new int[]{nx, ny});
                    }
                }

                if (pixels >= minPixels) {
                    boxes.add(new Box(minX, minY, maxX, maxY, pixels));
                }
            }
        }

        boxes.sort(Comparator.<Box>comparingInt(Box::minY).thenComparingInt(Box::minX));

        int idx = 0;
        for (Box b : boxes) {
            BufferedImage crop = src.getSubimage(b.minX, b.minY, b.width(), b.height());
            String name = String.format("%02d.png", idx++);
            ImageIO.write(crop, "png", new File(outputDir, name));
        }

        System.out.println("sprites=" + boxes.size());
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }
}
