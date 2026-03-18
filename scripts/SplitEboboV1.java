import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;

public class SplitEboboV1 {
    static class Box {
        int minX, minY, maxX, maxY, area;
        Box(int x, int y) { minX = maxX = x; minY = maxY = y; }
        void add(int x, int y) {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            area++;
        }
        int w() { return maxX - minX + 1; }
        int h() { return maxY - minY + 1; }
        int cx() { return (minX + maxX) / 2; }
        int cy() { return (minY + maxY) / 2; }
    }

    private static List<Box> components(BufferedImage img, int alphaThreshold) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] seen = new boolean[h][w];
        int[] qx = new int[w * h];
        int[] qy = new int[w * h];
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        List<Box> out = new ArrayList<>();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (seen[y][x]) continue;
                seen[y][x] = true;
                int a = (img.getRGB(x, y) >>> 24) & 0xFF;
                if (a < alphaThreshold) continue;
                int head = 0, tail = 0;
                qx[tail] = x;
                qy[tail] = y;
                tail++;
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
                        if (na >= alphaThreshold) {
                            qx[tail] = nx;
                            qy[tail] = ny;
                            tail++;
                        }
                    }
                }
                out.add(b);
            }
        }
        return out;
    }

    private static List<Box> splitWideByValley(BufferedImage img, Box b) {
        List<Box> result = new ArrayList<>();
        if (b.w() < 280) {
            result.add(b);
            return result;
        }

        int[] col = new int[b.w()];
        for (int x = b.minX; x <= b.maxX; x++) {
            int c = 0;
            for (int y = b.minY; y <= b.maxY; y++) {
                int a = (img.getRGB(x, y) >>> 24) & 0xFF;
                if (a >= 10) c++;
            }
            col[x - b.minX] = c;
        }

        int mid = col.length / 2;
        int left = Math.max(1, mid - col.length / 5);
        int right = Math.min(col.length - 2, mid + col.length / 5);
        int split = mid;
        int best = Integer.MAX_VALUE;
        for (int i = left; i <= right; i++) {
            if (col[i] < best) {
                best = col[i];
                split = i;
            }
        }

        Box b1 = new Box(b.minX, b.minY);
        b1.maxX = b.minX + split;
        b1.maxY = b.maxY;
        b1.minY = b.minY;
        b1.area = 1;

        Box b2 = new Box(b.minX + split + 1, b.minY);
        b2.maxX = b.maxX;
        b2.maxY = b.maxY;
        b2.minY = b.minY;
        b2.area = 1;

        result.add(b1);
        result.add(b2);
        return result;
    }

    private static BufferedImage crop(BufferedImage src, Box b, int padX, int padY) {
        int x = Math.max(0, b.minX - padX);
        int y = Math.max(0, b.minY - padY);
        int x2 = Math.min(src.getWidth() - 1, b.maxX + padX);
        int y2 = Math.min(src.getHeight() - 1, b.maxY + padY);
        int w = x2 - x + 1;
        int h = y2 - y + 1;

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, x, y, x + w, y + h, null);
        g.dispose();
        return out;
    }

    private static void clearDir(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) clearDir(f);
            f.delete();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: SplitEboboV1 <input.png> <outRoot>");
            System.exit(1);
        }
        BufferedImage src = ImageIO.read(new File(args[0]));
        File outRoot = new File(args[1]);
        outRoot.mkdirs();

        List<Box> comps = components(src, 10);
        List<Box> base = new ArrayList<>();
        for (Box b : comps) {
            if (b.area >= 30000 && b.h() >= 220) {
                base.add(b);
            }
        }
        base.sort(Comparator.comparingInt((Box b) -> b.minY).thenComparingInt(b -> b.minX));

        List<Box> expanded = new ArrayList<>();
        for (Box b : base) {
            expanded.addAll(splitWideByValley(src, b));
        }

        expanded.sort(Comparator.comparingInt(Box::cy).thenComparingInt(Box::cx));
        List<List<Box>> rows = new ArrayList<>();
        for (Box b : expanded) {
            boolean placed = false;
            for (List<Box> row : rows) {
                int avgY = 0;
                for (Box r : row) avgY += r.cy();
                avgY /= row.size();
                if (Math.abs(b.cy() - avgY) <= 120) {
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
        rows.sort(Comparator.comparingInt(row -> row.stream().mapToInt(Box::cy).sum() / row.size()));
        for (List<Box> row : rows) row.sort(Comparator.comparingInt(Box::cx));

        String[] names = {"walk_right", "walk_left", "idle"};
        for (int i = 0; i < rows.size() && i < names.length; i++) {
            File d = new File(outRoot, names[i]);
            d.mkdirs();
            clearDir(d);
            List<Box> row = rows.get(i);
            for (int j = 0; j < row.size(); j++) {
                BufferedImage frame = crop(src, row.get(j), 8, 8);
                ImageIO.write(frame, "png", new File(d, String.format("%02d.png", j)));
            }
            System.out.println(names[i] + ": " + row.size() + " frames");
        }
        System.out.println("Done: " + outRoot.getPath());
    }
}
