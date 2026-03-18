import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;

public class AnalyzeAlphaComponents {
    static class Box {
        int minX, minY, maxX, maxY, area;
        Box(int x, int y) { minX = maxX = x; minY = maxY = y; area = 0; }
        void add(int x, int y) {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            area++;
        }
        int w() { return maxX - minX + 1; }
        int h() { return maxY - minY + 1; }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: AnalyzeAlphaComponents <png>");
            System.exit(1);
        }
        BufferedImage img = ImageIO.read(new File(args[0]));
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] seen = new boolean[h][w];
        List<Box> boxes = new ArrayList<>();

        int[] qx = new int[w * h];
        int[] qy = new int[w * h];
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (seen[y][x]) continue;
                int a = (img.getRGB(x, y) >>> 24) & 0xFF;
                if (a < 10) { seen[y][x] = true; continue; }

                int head = 0, tail = 0;
                qx[tail] = x; qy[tail] = y; tail++;
                seen[y][x] = true;
                Box b = new Box(x, y);
                while (head < tail) {
                    int cx = qx[head], cy = qy[head];
                    head++;
                    b.add(cx, cy);
                    for (int i = 0; i < 4; i++) {
                        int nx = cx + dx[i], ny = cy + dy[i];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h || seen[ny][nx]) continue;
                        seen[ny][nx] = true;
                        int na = (img.getRGB(nx, ny) >>> 24) & 0xFF;
                        if (na >= 10) {
                            qx[tail] = nx; qy[tail] = ny; tail++;
                        }
                    }
                }
                boxes.add(b);
            }
        }

        boxes.sort(Comparator.comparingInt((Box b) -> b.area).reversed());
        System.out.println("components=" + boxes.size());
        int limit = Math.min(80, boxes.size());
        for (int i = 0; i < limit; i++) {
            Box b = boxes.get(i);
            if (b.area < 100) break;
            System.out.printf(
                    "%02d area=%d bbox=(%d,%d)-(%d,%d) w=%d h=%d%n",
                    i, b.area, b.minX, b.minY, b.maxX, b.maxY, b.w(), b.h()
            );
        }
    }
}
