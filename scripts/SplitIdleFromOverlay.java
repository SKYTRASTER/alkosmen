import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class SplitIdleFromOverlay {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: SplitIdleFromOverlay <input.png> <outputDir>");
            System.exit(1);
        }

        BufferedImage src = ImageIO.read(new File(args[0]));
        File outDir = new File(args[1]);
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IllegalStateException("Cannot create output dir: " + outDir);
        }

        // Tuned for ebobo_overlay.png top idle row.
        int[] centersX = {140, 300, 460, 618, 780};
        int cropW = 178;
        int cropH = 210;
        int y = 96;

        for (int i = 0; i < centersX.length; i++) {
            int x = centersX[i] - cropW / 2;
            if (x < 0) x = 0;
            if (x + cropW > src.getWidth()) x = src.getWidth() - cropW;
            int yy = y;
            if (yy < 0) yy = 0;
            if (yy + cropH > src.getHeight()) yy = src.getHeight() - cropH;

            BufferedImage frame = new BufferedImage(cropW, cropH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frame.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, cropW, cropH, x, yy, x + cropW, yy + cropH, null);
            g.dispose();

            File out = new File(outDir, String.format("%02d.png", i));
            ImageIO.write(frame, "png", out);
            System.out.println("Wrote: " + out.getPath());
        }
    }
}
