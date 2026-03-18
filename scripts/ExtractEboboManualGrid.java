import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public final class ExtractEboboManualGrid {
    private record Row(String name, int count, int x1, int x2, int y1, int y2) {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: ExtractEboboManualGrid <sheet.png> <outputDir>");
        }

        BufferedImage src = ImageIO.read(new File(args[0]));
        File outRoot = new File(args[1]);
        if (!outRoot.exists() && !outRoot.mkdirs()) {
            throw new IllegalStateException("Failed to create output directory: " + outRoot);
        }

        Row[] rows = {
                new Row("idle", 5, 25, 551, 115, 258),
                new Row("walk_left", 6, 22, 554, 260, 400),
                new Row("walk_right", 5, 30, 548, 410, 548),
                new Row("laugh", 5, 35, 544, 559, 695),
                new Row("attack", 5, 35, 544, 708, 865),
                new Row("hurt", 4, 48, 531, 875, 1008)
        };

        for (Row row : rows) {
            File rowDir = new File(outRoot, row.name());
            if (!rowDir.exists() && !rowDir.mkdirs()) {
                throw new IllegalStateException("Failed to create row directory: " + rowDir);
            }

            int rowW = row.x2() - row.x1();
            double cellW = rowW / (double) row.count();
            int written = 0;

            for (int i = 0; i < row.count(); i++) {
                int cx1 = row.x1() + (int) Math.floor(i * cellW);
                int cx2 = row.x1() + (int) Math.floor((i + 1) * cellW);
                if (i == row.count() - 1) {
                    cx2 = row.x2();
                }

                int[] b = findAlphaBounds(src, cx1, cx2, row.y1(), row.y2(), 10);
                if (b == null) {
                    continue;
                }

                int pad = 3;
                int x1 = clamp(b[0] - pad, cx1, cx2 - 1);
                int y1 = clamp(b[1] - pad, row.y1(), row.y2() - 1);
                int x2 = clamp(b[2] + pad, x1 + 1, cx2);
                int y2 = clamp(b[3] + pad, y1 + 1, row.y2());

                BufferedImage frame = src.getSubimage(x1, y1, x2 - x1, y2 - y1);
                ImageIO.write(frame, "png", new File(rowDir, String.format("%02d.png", written++)));
            }

            System.out.println(row.name() + "=" + written);
        }
    }

    private static int[] findAlphaBounds(BufferedImage img, int x1, int x2, int y1, int y2, int alphaThreshold) {
        int minX = x2, minY = y2, maxX = x1, maxY = y1;
        boolean found = false;
        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                int a = (img.getRGB(x, y) >>> 24) & 0xFF;
                if (a <= alphaThreshold) {
                    continue;
                }
                found = true;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }
        if (!found) {
            return null;
        }
        return new int[] {minX, minY, maxX + 1, maxY + 1};
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
