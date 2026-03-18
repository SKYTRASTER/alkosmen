import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public final class ExtractEboboTuned {
    private record Row(String name, int count, int cxStart, int cxEnd, int cy, int halfW, int halfH) {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: ExtractEboboTuned <sheet.png> <outputDir>");
        }
        BufferedImage src = ImageIO.read(new File(args[0]));
        File outRoot = new File(args[1]);
        if (!outRoot.exists() && !outRoot.mkdirs()) {
            throw new IllegalStateException("Failed to create output dir: " + outRoot);
        }

        Row[] rows = {
                new Row("idle", 5, 78, 495, 192, 48, 62),
                new Row("walk_left", 6, 70, 504, 334, 45, 60),
                new Row("walk_right", 5, 78, 494, 479, 52, 63),
                new Row("laugh", 5, 79, 494, 625, 52, 66),
                new Row("attack", 5, 79, 494, 775, 62, 74),
                new Row("hurt", 4, 95, 478, 919, 58, 69)
        };

        for (Row row : rows) {
            File rowDir = new File(outRoot, row.name());
            if (!rowDir.exists() && !rowDir.mkdirs()) {
                throw new IllegalStateException("Failed to create row dir: " + rowDir);
            }

            int written = 0;
            for (int i = 0; i < row.count(); i++) {
                double t = row.count() == 1 ? 0.0 : (double) i / (row.count() - 1);
                int cx = (int) Math.round(row.cxStart() + (row.cxEnd() - row.cxStart()) * t);
                int cy = row.cy();

                int x1 = clamp(cx - row.halfW(), 0, src.getWidth() - 1);
                int x2 = clamp(cx + row.halfW(), x1 + 1, src.getWidth());
                int y1 = clamp(cy - row.halfH(), 0, src.getHeight() - 1);
                int y2 = clamp(cy + row.halfH(), y1 + 1, src.getHeight());

                int[] b = findAlphaBounds(src, x1, x2, y1, y2, 10);
                if (b == null) {
                    continue;
                }

                int pad = 3;
                int fx1 = clamp(b[0] - pad, x1, x2 - 1);
                int fy1 = clamp(b[1] - pad, y1, y2 - 1);
                int fx2 = clamp(b[2] + pad, fx1 + 1, x2);
                int fy2 = clamp(b[3] + pad, fy1 + 1, y2);

                BufferedImage frame = src.getSubimage(fx1, fy1, fx2 - fx1, fy2 - fy1);
                ImageIO.write(frame, "png", new File(rowDir, String.format("%02d.png", written++)));
            }
            System.out.println(row.name() + "=" + written);
        }
    }

    private static int[] findAlphaBounds(BufferedImage img, int x1, int x2, int y1, int y2, int alphaThr) {
        int minX = x2, minY = y2, maxX = x1, maxY = y1;
        boolean found = false;
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                int a = (img.getRGB(x, y) >>> 24) & 0xFF;
                if (a <= alphaThr) continue;
                found = true;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }
        if (!found) return null;
        return new int[]{minX, minY, maxX + 1, maxY + 1};
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
