import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ExtractEboboClustered {
    private static final String[] NAMES = {"idle", "walk_left", "walk_right", "laugh", "attack", "hurt"};
    private static final int[] COUNTS = {5, 6, 5, 5, 5, 4};
    private static final int[] ROW_CENTERS_Y = {190, 335, 480, 625, 775, 920};

    private record Box(int minX, int minY, int maxX, int maxY, int area) {
        int cx() { return (minX + maxX) / 2; }
        int cy() { return (minY + maxY) / 2; }
        int w() { return maxX - minX + 1; }
        int h() { return maxY - minY + 1; }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: ExtractEboboClustered <sheet.png> <outputDir>");
        }
        BufferedImage src = ImageIO.read(new File(args[0]));
        File out = new File(args[1]);
        if (!out.exists() && !out.mkdirs()) {
            throw new IllegalStateException("Failed to create output dir: " + out);
        }

        List<Box> comps = findComponents(src, 8, 40);
        List<Box> spriteLike = new ArrayList<>();
        for (Box b : comps) {
            if (b.w() < 6 || b.h() < 18) continue;
            if (b.w() > 260 || b.h() > 260) continue;
            if (b.area < 70) continue;
            spriteLike.add(b);
        }

        List<List<Box>> rows = new ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) rows.add(new ArrayList<>());
        for (Box b : spriteLike) {
            int row = nearestRow(b.cy());
            if (Math.abs(b.cy() - ROW_CENTERS_Y[row]) <= 115) {
                rows.get(row).add(b);
            }
        }

        for (int r = 0; r < NAMES.length; r++) {
            File rowDir = new File(out, NAMES[r]);
            if (!rowDir.exists() && !rowDir.mkdirs()) {
                throw new IllegalStateException("Failed to create row dir: " + rowDir);
            }

            List<Box> merged = clusterAndMerge(rows.get(r), COUNTS[r]);
            merged.sort(Comparator.comparingInt(Box::cx));
            int idx = 0;
            for (Box b : merged) {
                int pad = 3;
                int x1 = clamp(b.minX - pad, 0, src.getWidth() - 1);
                int y1 = clamp(b.minY - pad, 0, src.getHeight() - 1);
                int x2 = clamp(b.maxX + 1 + pad, x1 + 1, src.getWidth());
                int y2 = clamp(b.maxY + 1 + pad, y1 + 1, src.getHeight());
                BufferedImage frame = src.getSubimage(x1, y1, x2 - x1, y2 - y1);
                ImageIO.write(frame, "png", new File(rowDir, String.format("%02d.png", idx++)));
            }
            System.out.println(NAMES[r] + "=" + merged.size());
        }
    }

    private static int nearestRow(int y) {
        int best = 0;
        int dBest = Integer.MAX_VALUE;
        for (int i = 0; i < ROW_CENTERS_Y.length; i++) {
            int d = Math.abs(y - ROW_CENTERS_Y[i]);
            if (d < dBest) {
                dBest = d;
                best = i;
            }
        }
        return best;
    }

    private static List<Box> clusterAndMerge(List<Box> boxes, int k) {
        List<Box> out = new ArrayList<>();
        if (boxes.isEmpty()) return out;

        int minX = boxes.stream().mapToInt(Box::minX).min().orElse(0);
        int maxX = boxes.stream().mapToInt(Box::maxX).max().orElse(minX + 1);
        double[] centers = new double[k];
        for (int i = 0; i < k; i++) {
            double t = k == 1 ? 0.5 : (double) i / (k - 1);
            centers[i] = minX + (maxX - minX) * t;
        }

        int[] assign = new int[boxes.size()];
        for (int iter = 0; iter < 20; iter++) {
            boolean changed = false;
            for (int i = 0; i < boxes.size(); i++) {
                int best = 0;
                double bestD = Double.MAX_VALUE;
                for (int c = 0; c < k; c++) {
                    double d = Math.abs(boxes.get(i).cx() - centers[c]);
                    if (d < bestD) {
                        bestD = d;
                        best = c;
                    }
                }
                if (assign[i] != best) {
                    assign[i] = best;
                    changed = true;
                }
            }

            double[] sum = new double[k];
            int[] cnt = new int[k];
            for (int i = 0; i < boxes.size(); i++) {
                sum[assign[i]] += boxes.get(i).cx();
                cnt[assign[i]]++;
            }
            for (int c = 0; c < k; c++) {
                if (cnt[c] > 0) centers[c] = sum[c] / cnt[c];
            }
            if (!changed) break;
        }

        for (int c = 0; c < k; c++) {
            int minx = Integer.MAX_VALUE, miny = Integer.MAX_VALUE;
            int maxx = Integer.MIN_VALUE, maxy = Integer.MIN_VALUE, area = 0;
            for (int i = 0; i < boxes.size(); i++) {
                if (assign[i] != c) continue;
                Box b = boxes.get(i);
                minx = Math.min(minx, b.minX);
                miny = Math.min(miny, b.minY);
                maxx = Math.max(maxx, b.maxX);
                maxy = Math.max(maxy, b.maxY);
                area += b.area;
            }
            if (minx <= maxx && miny <= maxy) {
                out.add(new Box(minx, miny, maxx, maxy, area));
            }
        }
        return out;
    }

    private static List<Box> findComponents(BufferedImage img, int alphaThr, int minPixels) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] vis = new boolean[h][w];
        int[] dx = {1, -1, 0, 0}, dy = {0, 0, 1, -1};
        List<Box> out = new ArrayList<>();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (vis[y][x]) continue;
                if (((img.getRGB(x, y) >>> 24) & 0xFF) <= alphaThr) continue;
                ArrayDeque<int[]> q = new ArrayDeque<>();
                q.add(new int[]{x, y});
                vis[y][x] = true;

                int minX = x, minY = y, maxX = x, maxY = y, area = 0;
                while (!q.isEmpty()) {
                    int[] p = q.removeFirst();
                    int px = p[0], py = p[1];
                    area++;
                    if (px < minX) minX = px;
                    if (py < minY) minY = py;
                    if (px > maxX) maxX = px;
                    if (py > maxY) maxY = py;

                    for (int i = 0; i < 4; i++) {
                        int nx = px + dx[i], ny = py + dy[i];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        if (vis[ny][nx]) continue;
                        if (((img.getRGB(nx, ny) >>> 24) & 0xFF) <= alphaThr) continue;
                        vis[ny][nx] = true;
                        q.addLast(new int[]{nx, ny});
                    }
                }
                if (area >= minPixels) out.add(new Box(minX, minY, maxX, maxY, area));
            }
        }
        return out;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
