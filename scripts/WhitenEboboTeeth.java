import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class WhitenEboboTeeth {
    private static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == rf) h = (60 * ((gf - bf) / d) + 360) % 360;
        else if (max == gf) h = 60 * (((bf - rf) / d) + 2);
        else h = 60 * (((rf - gf) / d) + 4);
        float s = max == 0 ? 0 : d / max;
        float v = max;
        return new float[]{h, s, v};
    }

    private static int whitenAt(BufferedImage img, int cx, int cy, int rx, int ry) {
        int changed = 0;
        for (int y = cy - ry; y <= cy + ry; y++) {
            for (int x = cx - rx; x <= cx + rx; x++) {
                if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) continue;
                int dx = x - cx, dy = y - cy;
                if ((dx * dx) / (double) (rx * rx) + (dy * dy) / (double) (ry * ry) > 1.0) continue;

                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a < 20) continue;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;

                float[] hsv = rgbToHsv(r, g, b);
                float h = hsv[0], s = hsv[1], v = hsv[2];
                boolean toothLike = h >= 18f && h <= 60f && s >= 0.18f && s <= 0.95f && v >= 0.18f && v <= 0.90f;
                if (!toothLike) continue;
                if (b > 130 || g < 60 || r < 70) continue;
                if (Math.abs(r - g) > 95) continue;

                int lum = (r * 30 + g * 59 + b * 11) / 100;
                if (lum < 40 || lum > 215) continue;
                int dark = 0;
                for (int yy = -2; yy <= 2; yy++) {
                    for (int xx = -2; xx <= 2; xx++) {
                        if (xx == 0 && yy == 0) continue;
                        int nx = x + xx, ny = y + yy;
                        if (nx < 0 || ny < 0 || nx >= img.getWidth() || ny >= img.getHeight()) continue;
                        int n = img.getRGB(nx, ny);
                        int na = (n >>> 24) & 0xFF;
                        if (na < 20) continue;
                        int nr = (n >>> 16) & 0xFF;
                        int ng = (n >>> 8) & 0xFF;
                        int nb = n & 0xFF;
                        int nLum = (nr * 30 + ng * 59 + nb * 11) / 100;
                        if (nLum < 52) dark++;
                    }
                }
                if (dark < 7) continue;

                int nr = (int) (r * 0.15 + 248 * 0.85);
                int ng = (int) (g * 0.15 + 245 * 0.85);
                int nb = (int) (b * 0.15 + 238 * 0.85);
                int tweak = (lum - 120) / 8;
                nr = Math.max(195, Math.min(255, nr + tweak));
                ng = Math.max(190, Math.min(252, ng + tweak));
                nb = Math.max(180, Math.min(246, nb + tweak));

                img.setRGB(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
                changed++;
            }
        }
        return changed;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: WhitenEboboTeeth <in.png> <out.png>");
            System.exit(1);
        }
        File in = new File(args[0]);
        File out = new File(args[1]);
        BufferedImage img = ImageIO.read(in);

        int[][] mouths = new int[][]{
                // idle (5)
                {140, 190, 14, 9}, {300, 190, 14, 9}, {460, 190, 14, 9}, {618, 190, 14, 9}, {780, 190, 14, 9},
                // walk left (6)
                {148, 392, 12, 8}, {286, 392, 12, 8}, {425, 392, 12, 8}, {564, 392, 12, 8}, {703, 392, 12, 8}, {842, 392, 12, 8},
                // walk right (5)
                {136, 605, 14, 9}, {296, 605, 14, 9}, {456, 605, 14, 9}, {614, 605, 14, 9}, {776, 605, 14, 9},
                // laugh (5)
                {132, 810, 16, 10}, {292, 810, 16, 10}, {452, 810, 16, 10}, {612, 810, 16, 10}, {772, 810, 16, 10},
                // attack (5)
                {132, 1020, 14, 9}, {292, 1020, 14, 9}, {452, 1020, 14, 9}, {612, 1020, 14, 9}, {772, 1020, 14, 9},
                // hurt (4)
                {178, 1230, 12, 8}, {338, 1230, 12, 8}, {498, 1230, 12, 8}, {658, 1230, 12, 8}
        };

        int changed = 0;
        for (int[] m : mouths) {
            changed += whitenAt(img, m[0], m[1], m[2], m[3]);
        }

        ImageIO.write(img, "png", out);
        System.out.println("changed=" + changed + " -> " + out.getPath());
    }
}
