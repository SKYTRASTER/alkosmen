package alkosmen;

import java.awt.Canvas;
import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Game extends Canvas implements Runnable {
    private static int Height;
    private static int Width;
    private static boolean running;

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

    public void start() {
        running = true;
        new Thread(this).start();
    }
}
