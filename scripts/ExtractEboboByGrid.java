import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public final class ExtractEboboByGrid {
    private static final String[] NAMES = {"idle", "walk_left", "walk_right", "laugh", "attack", "hurt"};
    private static final int[] COUNTS = {5, 6, 5, 5, 5, 4};

    // Tuned for ebobo_overlay.png layout (height-normalized row bands).
    private static final double[][] ROW_BANDS = {
            {0.14, 0.30},
            {0.29, 0.45},
            {0.43, 0.59},
            {0.58, 0.74},
            {0.72, 0.89},
            {0.86, 0.995}
    };

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: ExtractEboboByGrid <sheet.png> <outputDir>");
        }
        BufferedImage src = ImageIO.read(new File(args[0]));
        File out = new File(args[1]);
        if (!out.exists() && !out.mkdirs()) {
            throw new IllegalStateException("Failed to create: " + out);
        }

        int w = src.getWidth();
        int h = src.getHeight();
        for (int r = 0; r < NAMES.length; r++) {
            int y1 = (int) Math.round(ROW_BANDS[r][0] * h);
            int y2 = (int) Math.round(ROW_BANDS[r][1] * h);
            y1 = clamp(y1, 0, h - 1);
            y2 = clamp(y2, y1 + 1, h);

            int[] rowX = findXBounds(src, 0, w, y1, y2, 8);
            int minX = rowX[0];
            int maxX = rowX[1];
            if (minX >= maxX) {
                continue;
            }

            File rowDir = new File(out, NAMES[r]);
            if (!rowDir.exists() && !rowDir.mkdirs()) {
                throw new IllegalStateException("Failed to create row dir: " + rowDir);
            }

            int count = COUNTS[r];
            double step = (maxX - minX + 1) / (double) count;
            int written = 0;
            for (int i = 0; i < count; i++) {
                int bx1 = (int) Math.floor(minX + i * step);
                int bx2 = (int) Math.ceil(minX + (i + 1) * step);
                bx1 = clamp(bx1, 0, w - 1);
                bx2 = clamp(bx2, bx1 + 1, w);

                int[] b = findBounds(src, bx1, bx2, y1, y2, 8);
                if (b[0] >= b[2] || b[1] >= b[3]) {
                    continue;
                }

                int m = 3;
                int cx1 = clamp(b[0] - m, 0, w - 1);
                int cy1 = clamp(b[1] - m, 0, h - 1);
                int cx2 = clamp(b[2] + m, cx1 + 1, w);
                int cy2 = clamp(b[3] + m, cy1 + 1, h);
                BufferedImage frame = src.getSubimage(cx1, cy1, cx2 - cx1, cy2 - cy1);
                ImageIO.write(frame, "png", new File(rowDir, String.format("%02d.png", written++)));
            }
            System.out.println(NAMES[r] + "=" + written);
        }
    }

    private static int[] findXBounds(BufferedImage img, int x1, int x2, int y1, int y2, int aThr) {
        int minX = x2, maxX = x1;
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                if (alpha(img.getRGB(x, y)) > aThr) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                }
            }
        }
        return new int[]{minX, maxX};
    }

    private static int[] findBounds(BufferedImage img, int x1, int x2, int y1, int y2, int aThr) {
        int minX = x2, minY = y2, maxX = x1, maxY = y1;
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                if (alpha(img.getRGB(x, y)) > aThr) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        return new int[]{minX, minY, maxX, maxY};
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
