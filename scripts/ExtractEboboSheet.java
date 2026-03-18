import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ExtractEboboSheet {
    private static final String[] ROW_NAMES = {
            "idle",
            "walk_left",
            "walk_right",
            "laugh",
            "attack",
            "hurt"
    };
    private static final int[] EXPECTED_PER_ROW = {5, 6, 5, 5, 5, 4};

    private record Box(int minX, int minY, int maxX, int maxY, int area) {
        int width() { return maxX - minX + 1; }
        int height() { return maxY - minY + 1; }
        int cx() { return (minX + maxX) / 2; }
        int cy() { return (minY + maxY) / 2; }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: ExtractEboboSheet <sheet.png> <outputDir>");
        }
        File input = new File(args[0]);
        File outRoot = new File(args[1]);
        if (!outRoot.exists() && !outRoot.mkdirs()) {
            throw new IllegalStateException("Failed to create output dir: " + outRoot);
        }

        BufferedImage img = ImageIO.read(input);
        List<Box> boxes = findComponents(img, 8, 90);

        // Keep components in plausible sprite area (including separated FX parts).
        List<Box> sprites = new ArrayList<>();
        for (Box b : boxes) {
            if (b.width() < 8 || b.width() > 240) continue;
            if (b.height() < 8 || b.height() > 260) continue;
            if (b.minY < 110) continue; // drop title/header
            sprites.add(b);
        }

        // "Core" body components define per-row and per-frame anchors.
        List<Box> cores = new ArrayList<>();
        for (Box b : sprites) {
            if (b.area >= 700 && b.width() >= 34 && b.height() >= 48) {
                cores.add(b);
            }
        }
        cores.sort(Comparator.comparingInt(Box::cy).thenComparingInt(Box::cx));
        List<List<Box>> coreRows = clusterRows(cores, 65);
        if (coreRows.size() > ROW_NAMES.length) {
            coreRows = coreRows.subList(0, ROW_NAMES.length);
        }

        int rowCount = Math.min(coreRows.size(), ROW_NAMES.length);
        for (int r = 0; r < rowCount; r++) {
            List<Box> rowCores = coreRows.get(r);
            rowCores.sort(Comparator.comparingInt(Box::cx));
            List<Integer> anchors = new ArrayList<>();
            for (Box c : rowCores) anchors.add(c.cx());
            int expected = EXPECTED_PER_ROW[r];
            if (anchors.size() > expected) {
                anchors = anchors.subList(0, expected);
            }

            // Collect all components near this row (includes detached attack arcs/sparks).
            int avgY = rowCores.stream().mapToInt(Box::cy).sum() / rowCores.size();
            List<Box> rowAll = new ArrayList<>();
            for (Box b : sprites) {
                if (Math.abs(b.cy() - avgY) <= 95) {
                    rowAll.add(b);
                }
            }

            // Merge components into frame bins by nearest X anchor.
            List<Box> merged = new ArrayList<>();
            for (int i = 0; i < anchors.size(); i++) {
                int a = anchors.get(i);
                int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
                int area = 0;
                for (Box b : rowAll) {
                    int bi = nearestAnchor(anchors, b.cx());
                    if (bi != i) continue;
                    minX = Math.min(minX, b.minX);
                    minY = Math.min(minY, b.minY);
                    maxX = Math.max(maxX, b.maxX);
                    maxY = Math.max(maxY, b.maxY);
                    area += b.area;
                }
                if (minX <= maxX && minY <= maxY) {
                    merged.add(new Box(minX, minY, maxX, maxY, area));
                }
            }
            merged.sort(Comparator.comparingInt(Box::cx));

            File rowDir = new File(outRoot, ROW_NAMES[r]);
            if (!rowDir.exists() && !rowDir.mkdirs()) {
                throw new IllegalStateException("Failed to create row dir: " + rowDir);
            }
            int idx = 0;
            for (Box b : merged) {
                BufferedImage crop = cropWithMargin(img, b, 3);
                ImageIO.write(crop, "png", new File(rowDir, String.format("%02d.png", idx++)));
            }
            System.out.println(ROW_NAMES[r] + "=" + merged.size());
        }
    }

    private static int nearestAnchor(List<Integer> anchors, int x) {
        int bestIdx = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < anchors.size(); i++) {
            int d = Math.abs(anchors.get(i) - x);
            if (d < bestDist) {
                bestDist = d;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private static List<List<Box>> clusterRows(List<Box> boxes, int thresholdY) {
        List<List<Box>> rows = new ArrayList<>();
        for (Box b : boxes) {
            boolean placed = false;
            for (List<Box> row : rows) {
                int avgY = row.stream().mapToInt(Box::cy).sum() / row.size();
                if (Math.abs(b.cy() - avgY) <= thresholdY) {
                    row.add(b);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<Box> row = new ArrayList<>();
                row.add(b);
                rows.add(row);
            }
        }
        rows.sort(Comparator.comparingInt(r -> r.stream().mapToInt(Box::cy).sum() / r.size()));
        return rows;
    }

    private static BufferedImage cropWithMargin(BufferedImage src, Box b, int margin) {
        int x1 = Math.max(0, b.minX - margin);
        int y1 = Math.max(0, b.minY - margin);
        int x2 = Math.min(src.getWidth() - 1, b.maxX + margin);
        int y2 = Math.min(src.getHeight() - 1, b.maxY + margin);
        return src.getSubimage(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
    }

    private static List<Box> findComponents(BufferedImage img, int alphaThreshold, int minPixels) {
        int w = img.getWidth();
        int h = img.getHeight();
        boolean[][] seen = new boolean[h][w];
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        List<Box> out = new ArrayList<>();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (seen[y][x] || alpha(img.getRGB(x, y)) <= alphaThreshold) continue;

                ArrayDeque<int[]> q = new ArrayDeque<>();
                q.add(new int[]{x, y});
                seen[y][x] = true;

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
                        int nx = px + dx[i];
                        int ny = py + dy[i];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        if (seen[ny][nx]) continue;
                        if (alpha(img.getRGB(nx, ny)) <= alphaThreshold) continue;
                        seen[ny][nx] = true;
                        q.addLast(new int[]{nx, ny});
                    }
                }

                if (area >= minPixels) {
                    out.add(new Box(minX, minY, maxX, maxY, area));
                }
            }
        }
        return out;
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }
}
